package com.jicek.license.sdk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.crypto.Md5SignService;
import com.jicek.license.sdk.auth.SoftwareContext;
import com.jicek.license.sdk.dto.SdkLoginResultDTO;
import com.jicek.license.software.entity.Software;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * SDK 业务服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 卡密登录（终端用户在开发者软件内用卡密登录）
 *  - 后续扩展：公告拉取、更新检查、云函数调用、云变量读写
 *
 * 安全铁律：
 *  - 卡密密文通过 X-Card-Cipher 头传输（RSA-2048-OAEP 加密），服务端用软件独立 RSA 私钥解密
 *  - cardHash = SHA-256(卡密明文)，禁明文查库（防 SQL 注入泄露）
 *  - 卡密明文永不日志输出（防日志泄露）
 *  - SoftwareContext.requireSoftware() 获取当前软件（由 SdkAuthFilter 注入）
 */
@Service
public class SdkAuthService {

    private static final Logger log = LoggerFactory.getLogger(SdkAuthService.class);

    private final CardKeyMapper cardKeyMapper;
    private final CardTypeMapper cardTypeMapper;
    private final AesCryptoService aesCryptoService;
    private final ObjectMapper objectMapper;

    public SdkAuthService(CardKeyMapper cardKeyMapper,
                          CardTypeMapper cardTypeMapper,
                          AesCryptoService aesCryptoService,
                          ObjectMapper objectMapper) {
        this.cardKeyMapper = cardKeyMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.aesCryptoService = aesCryptoService;
        this.objectMapper = objectMapper;
    }

    /**
     * 卡密登录
     *
     * @param cardCipherBase64 RSA-2048-OAEP 加密的卡密密文（Base64，来自 X-Card-Cipher 头）
     * @return 登录结果（卡类信息 + 软件配置）
     */
    @Transactional(rollbackFor = Exception.class)
    public SdkLoginResultDTO login(String cardCipherBase64) {
        Software software = SoftwareContext.requireSoftware();

        // 1. 校验卡密密文头
        if (cardCipherBase64 == null || cardCipherBase64.isBlank()) {
            throw new ServiceException(ResultCode.SDK_CARD_CIPHER_MISSING);
        }

        // 2. RSA 解密卡密明文（用软件独立私钥）
        String rsaPrivateKeyPlain = aesCryptoService.decrypt(software.getRsaPrivateKey());
        String cardPlain;
        try {
            cardPlain = rsaDecrypt(rsaPrivateKeyPlain, cardCipherBase64);
        } catch (Exception e) {
            log.warn("【SDK登录】卡密 RSA 解密失败 softwareId={}", software.getId());
            throw new ServiceException(ResultCode.CARD_DECRYPT_FAIL);
        }

        // 3. cardHash 查库（禁明文查库）
        String cardHash = Md5SignService.sha256Hex(cardPlain);
        CardKey cardKey = cardKeyMapper.selectOne(new LambdaQueryWrapper<CardKey>()
                .eq(CardKey::getCardHash, cardHash));
        if (cardKey == null) {
            throw new ServiceException(ResultCode.CARD_NOT_FOUND);
        }

        // 4. 校验软件归属
        if (!cardKey.getSoftwareId().equals(software.getId())) {
            log.warn("【SDK登录】卡密不属于当前软件 cardKeyId={} softwareId={} expected={}",
                    cardKey.getId(), cardKey.getSoftwareId(), software.getId());
            throw new ServiceException(ResultCode.SDK_CARD_NOT_BELONG_TO_SOFTWARE);
        }

        // 5. 校验卡密状态
        validateCardStatus(cardKey);

        // 6. 查卡类
        CardType cardType = cardTypeMapper.selectById(cardKey.getCardTypeId());
        if (cardType == null) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND);
        }

        // 7. 首次使用处理
        LocalDateTime now = LocalDateTime.now();
        if (cardKey.getStatus() == JicekConstants.CARD_STATUS_UNUSED) {
            cardKey.setFirstUseTime(now);
            cardKey.setStatus(JicekConstants.CARD_STATUS_USED);
            // 时长卡：计算到期时间
            if (cardType.getType() == JicekConstants.CARD_TYPE_DURATION && cardType.getDuration() != null) {
                cardKey.setExpireTime(now.plusSeconds(cardType.getDuration()));
            }
            cardKey.setUpdateTime(now);
            cardKeyMapper.updateById(cardKey);
            log.info("【SDK登录】卡密首次使用 cardKeyId={} cardTypeId={}", cardKey.getId(), cardType.getId());
        }

        // 8. 再次校验到期（时长卡可能已过期）
        if (cardKey.getExpireTime() != null && cardKey.getExpireTime().isBefore(now)) {
            // 标记为已过期
            cardKey.setStatus(JicekConstants.CARD_STATUS_EXPIRED);
            cardKey.setUpdateTime(now);
            cardKeyMapper.updateById(cardKey);
            throw new ServiceException(ResultCode.CARD_EXPIRED);
        }

        // 9. 构造返回
        return buildLoginResult(cardKey, cardType, software, now);
    }

    /** 校验卡密状态（封禁/退款直接拒绝） */
    private void validateCardStatus(CardKey cardKey) {
        int status = cardKey.getStatus();
        if (status == JicekConstants.CARD_STATUS_BANNED) {
            throw new ServiceException(ResultCode.CARD_BANNED);
        }
        if (status == JicekConstants.CARD_STATUS_REFUNDED) {
            throw new ServiceException(ResultCode.CARD_REFUNDED);
        }
        if (status == JicekConstants.CARD_STATUS_EXPIRED) {
            throw new ServiceException(ResultCode.CARD_EXPIRED);
        }
        // 0未使用 / 1已使用 均允许登录
    }

    /** 构造登录结果 */
    private SdkLoginResultDTO buildLoginResult(CardKey cardKey, CardType cardType,
                                                Software software, LocalDateTime now) {
        SdkLoginResultDTO result = new SdkLoginResultDTO();
        result.setCardKeyId(cardKey.getId());
        result.setCardNoMasked(maskCardNo(cardKey.getCardNo()));
        result.setCardStatus(cardKey.getStatus());
        result.setCardTypeId(cardType.getId());
        result.setCardTypeName(cardType.getName());
        result.setCardType(cardType.getType());
        result.setExpireTime(cardKey.getExpireTime());

        // 次数卡：返回总次数（remainingCount 字段暂未在 CardKey 表实现，返回 CardType.count 作为参考）
        if (cardType.getType() == JicekConstants.CARD_TYPE_COUNT) {
            result.setRemainingCount(cardType.getCount());
        }

        // 功能卡：解析 features JSON
        if (cardType.getType() == JicekConstants.CARD_TYPE_FEATURE && cardType.getFeatures() != null) {
            try {
                List<String> features = objectMapper.readValue(cardType.getFeatures(),
                        new TypeReference<List<String>>() {});
                result.setFeatures(features);
            } catch (Exception e) {
                log.warn("【SDK登录】功能卡 features JSON 解析失败 cardTypeId={}", cardType.getId());
            }
        }

        // 软件配置下发
        result.setHeartbeatInterval(software.getHeartbeatInterval());
        result.setMaxConcurrent(software.getMaxConcurrent());
        result.setSoftwareVersion(software.getVersion());
        result.setSoftwareMinVersion(software.getMinVersion());
        result.setServerTime(now);
        return result;
    }

    /** RSA-2048-OAEP 解密（用软件独立私钥） */
    private String rsaDecrypt(String privateKeyBase64, String cipherBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(cipherBase64));
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /** 卡号脱敏：前 4 + **** + 后 4 */
    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) {
            return "****";
        }
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }
}
