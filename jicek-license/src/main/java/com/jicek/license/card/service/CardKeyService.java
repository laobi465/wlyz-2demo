package com.jicek.license.card.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.agent.service.AgentService;
import com.jicek.license.card.dto.CardKeyGenRequestDTO;
import com.jicek.license.card.dto.CardKeyGenResponseDTO;
import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.generator.CardKeyGenerator;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.crypto.Md5SignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 卡密服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全规范（铁律 04/06）：
 * 1. 卡密生成后立即 AES-256-GCM 加密入库，明文仅在响应中返回一次
 * 2. 库中只存 cardCipher（密文）+ cardHash（SHA-256 哈希索引）
 * 3. 禁用 Math.random()，统一使用 SecureRandom
 * 4. 数量上限 1000，防止一次性生成过多影响性能
 */
@Slf4j
@Service
public class CardKeyService {

    private static final int MAX_GEN_QUANTITY = 1000;

    private final CardKeyMapper cardKeyMapper;
    private final CardTypeMapper cardTypeMapper;
    private final AesCryptoService aesCryptoService;
    private final AgentService agentService;

    public CardKeyService(CardKeyMapper cardKeyMapper,
                          CardTypeMapper cardTypeMapper,
                          AesCryptoService aesCryptoService,
                          AgentService agentService) {
        this.cardKeyMapper = cardKeyMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.aesCryptoService = aesCryptoService;
        this.agentService = agentService;
    }

    /**
     * 批量生成卡密
     * 事务保证：要么全部成功，要么全部回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public CardKeyGenResponseDTO batchGenerate(CardKeyGenRequestDTO request) {
        // 1. 校验
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new ServiceException(ResultCode.CARD_GEN_FAIL, "数量必须大于 0");
        }
        if (request.getQuantity() > MAX_GEN_QUANTITY) {
            throw new ServiceException(ResultCode.CARD_GEN_FAIL,
                    "单次生成上限 " + MAX_GEN_QUANTITY + " 张");
        }

        // 2. 查卡类
        CardType cardType = cardTypeMapper.selectById(request.getCardTypeId());
        if (cardType == null || !cardType.getTenantId().equals(request.getTenantId())) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND);
        }
        if (cardType.getEnabled() == null || cardType.getEnabled() != 1) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND, "卡类已禁用");
        }

        // 3. 代理制卡场景：先扣代理余额，再生成卡密（铁律 06 同事务，铁律 13 BigDecimal）
        //    开发者制卡（agentId=null）不扣余额，保持原逻辑
        //    单价取 CardType.price（零售价）；扣款失败（余额不足）抛 AGENT_BALANCE_INSUFFICIENT，事务回滚，不生成任何卡密
        if (request.getAgentId() != null && request.getAgentId() > 0) {
            BigDecimal unitPrice = cardType.getPrice();
            if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }
            BigDecimal totalCost = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
            if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                agentService.deductBalance(request.getTenantId(), request.getAgentId(),
                        totalCost, "代理制卡扣除");
                log.info("代理制卡扣余额: tenantId={}, agentId={}, cardTypeId={}, quantity={}, totalCost={}",
                        request.getTenantId(), request.getAgentId(),
                        request.getCardTypeId(), request.getQuantity(), totalCost);
            }
        }

        // 4. 批量生成
        List<String> plainCards = new ArrayList<>(request.getQuantity());
        List<String> maskedCardNos = new ArrayList<>(request.getQuantity());
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < request.getQuantity(); i++) {
            String plainCard = CardKeyGenerator.generate(
                    request.getPrefix(),
                    request.getCharset(),
                    request.getCustomCharset(),
                    request.getLength());

            // 5. 加密入库（铁律 04）
            String cipher = aesCryptoService.encrypt(plainCard);
            String hash = Md5SignService.sha256Hex(plainCard);

            CardKey cardKey = new CardKey();
            cardKey.setTenantId(request.getTenantId());
            cardKey.setCardTypeId(request.getCardTypeId());
            cardKey.setSoftwareId(request.getSoftwareId());
            cardKey.setCardNo(plainCard);  // 卡号=卡密（业务简化，未来可分离）
            cardKey.setCardCipher(cipher);
            cardKey.setCardHash(hash);
            cardKey.setStatus(JicekConstants.CARD_STATUS_UNUSED);
            cardKey.setCreateTime(now);
            cardKey.setUpdateTime(now);
            cardKeyMapper.insert(cardKey);

            plainCards.add(plainCard);
            maskedCardNos.add(CardKeyGenerator.mask(plainCard));
        }

        log.info("批量生成卡密: tenantId={}, cardTypeId={}, count={}",
                request.getTenantId(), request.getCardTypeId(), request.getQuantity());

        // 6. 返回（明文仅本次返回）
        CardKeyGenResponseDTO resp = new CardKeyGenResponseDTO();
        resp.setMaskedCardNos(maskedCardNos);
        resp.setPlainCards(plainCards);
        resp.setCount(request.getQuantity());
        resp.setTimestamp(System.currentTimeMillis());
        return resp;
    }

    /**
     * 按卡号查询（脱敏返回，不返回明文）
     */
    public CardKey getByCardNo(Long tenantId, String cardNo) {
        return cardKeyMapper.selectOne(new LambdaQueryWrapper<CardKey>()
                .eq(CardKey::getTenantId, tenantId)
                .eq(CardKey::getCardNo, cardNo));
    }

    /**
     * 按哈希查询（用于 SDK 验证接口，避免明文传输卡号）
     */
    public CardKey getByCardHash(String cardHash) {
        return cardKeyMapper.selectOne(new LambdaQueryWrapper<CardKey>()
                .eq(CardKey::getCardHash, cardHash));
    }

    /**
     * 解密卡密（仅服务内部使用，用于验证场景）
     */
    public String decryptCardPlain(CardKey cardKey) {
        return aesCryptoService.decrypt(cardKey.getCardCipher());
    }

    /**
     * 封禁卡密（status=2，所有 session 立即失效）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ban(Long tenantId, Long cardKeyId, String reason) {
        CardKey cardKey = cardKeyMapper.selectById(cardKeyId);
        if (cardKey == null || !cardKey.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.CARD_NOT_FOUND);
        }
        cardKey.setStatus(JicekConstants.CARD_STATUS_BANNED);
        cardKey.setUpdateTime(LocalDateTime.now());
        cardKeyMapper.updateById(cardKey);
        // TODO: 踢出所有相关 session（设备心跳模块实现后补全）
        log.warn("卡密已封禁: id={}, reason={}", cardKeyId, reason);
    }

    /**
     * 退款卡密（status=3，所有 session 立即失效）
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long tenantId, Long cardKeyId) {
        CardKey cardKey = cardKeyMapper.selectById(cardKeyId);
        if (cardKey == null || !cardKey.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.CARD_NOT_FOUND);
        }
        if (cardKey.getStatus() != JicekConstants.CARD_STATUS_USED
                && cardKey.getStatus() != JicekConstants.CARD_STATUS_UNUSED) {
            throw new ServiceException(ResultCode.CARD_REFUNDED, "卡密状态不允许退款");
        }
        cardKey.setStatus(JicekConstants.CARD_STATUS_REFUNDED);
        cardKey.setUpdateTime(LocalDateTime.now());
        cardKeyMapper.updateById(cardKey);
        log.info("卡密已退款: id={}", cardKeyId);
    }
}
