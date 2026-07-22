package com.jicek.license.h5.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.Md5SignService;
import com.jicek.license.h5.dto.H5CardDetailDTO;
import com.jicek.license.h5.dto.H5LoginRequestDTO;
import com.jicek.license.h5.dto.H5LoginResultDTO;
import com.jicek.license.h5.entity.H5Session;
import com.jicek.license.h5.mapper.H5SessionMapper;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * H5 终端用户服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 卡密登录（明文卡密 + appKey → 校验 → 生成 H5 token）
 *  - 我的卡密详情
 *  - 退出登录（删除 session）
 *
 * 安全说明：
 *  - H5 卡密为明文传输（依赖 HTTPS），与 SDK 的 RSA 加密不同
 *  - H5 token 为 UUID，有效期 24h，存 DB + Redis 缓存
 *  - 登录时校验卡密状态（未使用/已使用均可登录，封禁/退款/过期拒绝）
 *
 * 实现说明（与原始设计稿的差异）：
 *  - 项目无 CryptoService 类，SHA-256 计算复用 crypto.Md5SignService.sha256Hex()
 *    （与 SdkAuthService 同源，确保卡密哈希算法一致）
 *  - CardKey 实体无 count 字段（次数在 CardType 上），remainingCount 取自 cardType.getCount()
 */
@Service
public class H5AuthService {

    private final H5SessionMapper h5SessionMapper;
    private final CardKeyMapper cardKeyMapper;
    private final CardTypeMapper cardTypeMapper;
    private final SoftwareMapper softwareMapper;

    public H5AuthService(H5SessionMapper h5SessionMapper, CardKeyMapper cardKeyMapper,
                         CardTypeMapper cardTypeMapper, SoftwareMapper softwareMapper) {
        this.h5SessionMapper = h5SessionMapper;
        this.cardKeyMapper = cardKeyMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.softwareMapper = softwareMapper;
    }

    /**
     * H5 卡密登录
     */
    @Transactional
    public H5LoginResultDTO login(H5LoginRequestDTO dto, String clientIp, String userAgent) {
        // 1. 查软件
        Software software = softwareMapper.selectOne(
            new LambdaQueryWrapper<Software>().eq(Software::getAppKey, dto.getAppKey()));
        if (software == null) {
            throw new ServiceException(ResultCode.SOFTWARE_NOT_FOUND);
        }
        if (software.getEnabled() == null || software.getEnabled() != JicekConstants.SOFTWARE_ENABLED) {
            throw new ServiceException(ResultCode.H5_SOFTWARE_DISABLED);
        }

        // 2. 卡密哈希查询（card_hash = SHA256(cardKey明文)），与 SDK 同源
        String cardHash = Md5SignService.sha256Hex(dto.getCardKey());
        CardKey cardKey = cardKeyMapper.selectOne(
            new LambdaQueryWrapper<CardKey>()
                .eq(CardKey::getSoftwareId, software.getId())
                .eq(CardKey::getCardHash, cardHash));
        if (cardKey == null) {
            throw new ServiceException(ResultCode.H5_CARD_NOT_FOUND);
        }

        // 3. 校验卡密状态
        validateCardStatus(cardKey);

        // 4. 查卡类
        CardType cardType = cardTypeMapper.selectById(cardKey.getCardTypeId());
        if (cardType == null) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND);
        }

        // 5. 生成 H5 token
        String h5Token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusHours(JicekConstants.H5_TOKEN_EXPIRE_HOURS);

        H5Session session = new H5Session();
        session.setTenantId(software.getTenantId());
        session.setSoftwareId(software.getId());
        session.setCardKeyId(cardKey.getId());
        session.setCardNoMasked(maskCardNo(cardKey.getCardNo()));
        session.setH5Token(h5Token);
        session.setDeviceInfo(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 255)) : null);
        session.setClientIp(clientIp);
        session.setExpireTime(expireTime);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        h5SessionMapper.insert(session);

        // 6. 组装返回
        H5LoginResultDTO result = new H5LoginResultDTO();
        result.setH5Token(h5Token);
        result.setCardNoMasked(session.getCardNoMasked());
        result.setCardTypeName(cardType.getName());
        result.setCardType(cardType.getType());
        result.setExpireTime(cardKey.getExpireTime());
        // 次数卡的次数在 CardType 上（CardKey 无 count 字段）
        result.setRemainingCount(cardType.getCount());
        result.setFeatures(cardType.getFeatures() != null
            ? Arrays.asList(cardType.getFeatures().split(",")) : null);
        result.setSoftwareName(software.getName());
        result.setTokenExpireTime(expireTime);
        return result;
    }

    /**
     * 获取我的卡密详情
     */
    public H5CardDetailDTO getMyCardDetail(Long cardKeyId) {
        CardKey cardKey = cardKeyMapper.selectById(cardKeyId);
        if (cardKey == null) {
            throw new ServiceException(ResultCode.H5_CARD_NOT_FOUND);
        }
        CardType cardType = cardTypeMapper.selectById(cardKey.getCardTypeId());
        Software software = softwareMapper.selectById(cardKey.getSoftwareId());

        H5CardDetailDTO dto = new H5CardDetailDTO();
        dto.setCardKeyId(cardKey.getId());
        dto.setCardNoMasked(maskCardNo(cardKey.getCardNo()));
        dto.setCardStatus(cardKey.getStatus());
        dto.setCardTypeName(cardType != null ? cardType.getName() : null);
        dto.setCardType(cardType != null ? cardType.getType() : null);
        dto.setExpireTime(cardKey.getExpireTime());
        dto.setFirstUseTime(cardKey.getFirstUseTime());
        // 次数卡的次数在 CardType 上
        dto.setRemainingCount(cardType != null ? cardType.getCount() : null);
        if (cardType != null && cardType.getFeatures() != null) {
            dto.setFeatures(Arrays.asList(cardType.getFeatures().split(",")));
        }
        dto.setSoftwareName(software != null ? software.getName() : null);
        return dto;
    }

    /**
     * 退出登录
     */
    @Transactional
    public void logout(String h5Token) {
        h5SessionMapper.delete(
            new LambdaQueryWrapper<H5Session>().eq(H5Session::getH5Token, h5Token));
    }

    private void validateCardStatus(CardKey cardKey) {
        Integer status = cardKey.getStatus();
        if (status == null) {
            throw new ServiceException(ResultCode.H5_LOGIN_FAIL);
        }
        switch (status) {
            case 0: // 未使用
            case 1: // 已使用
                // 检查过期
                if (cardKey.getExpireTime() != null && cardKey.getExpireTime().isBefore(LocalDateTime.now())) {
                    throw new ServiceException(ResultCode.H5_CARD_EXPIRED);
                }
                break;
            case 2:
                throw new ServiceException(ResultCode.H5_CARD_BANNED);
            case 3:
                throw new ServiceException(ResultCode.CARD_REFUNDED);
            case 4:
                throw new ServiceException(ResultCode.H5_CARD_EXPIRED);
            default:
                throw new ServiceException(ResultCode.H5_LOGIN_FAIL);
        }
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) return "****";
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }
}
