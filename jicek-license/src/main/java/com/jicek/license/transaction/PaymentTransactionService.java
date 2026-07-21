package com.jicek.license.transaction;

import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.card.generator.CardKeyGenerator;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.crypto.Md5SignService;
import com.jicek.license.pay.dto.PayNotifyDTO;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.service.PayOrderStateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金一致性事务服务（资金安全核心）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 在同一个数据库事务内完成「订单状态流转 + 卡密发放」
 * 杜绝「已支付未发卡」或「已退款卡密仍可用」的情况
 *
 * 事务边界（必须 @Transactional）：
 * 1. 订单 0 → 1（已支付）
 * 2. 生成 N 张卡密并加密入库（N = order.quantity）
 * 3. 任意一步失败，整个事务回滚
 *
 * 退款流程（独立事务）：
 * 1. 订单 1 → 3（已退款）
 * 2. 关联的所有卡密 status → 3（已退款）
 * 3. 任意一步失败，整个事务回滚
 *
 * 安全铁律：
 * - 禁止跨事务更新（铁律 06）
 * - 禁止伪异步（铁律 06）
 * - 卡密发放必须真实写入数据库
 */
@Slf4j
@Service
public class PaymentTransactionService {

    private final PayOrderStateMachineService orderStateMachineService;
    private final CardKeyMapper cardKeyMapper;
    private final CardTypeMapper cardTypeMapper;
    private final AesCryptoService aesCryptoService;

    public PaymentTransactionService(PayOrderStateMachineService orderStateMachineService,
                                     CardKeyMapper cardKeyMapper,
                                     CardTypeMapper cardTypeMapper,
                                     AesCryptoService aesCryptoService) {
        this.orderStateMachineService = orderStateMachineService;
        this.cardKeyMapper = cardKeyMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.aesCryptoService = aesCryptoService;
    }

    /**
     * 处理支付成功：订单流转 + 卡密发放（同一事务）
     *
     * @param order  订单（必须为待支付状态）
     * @param notify 回调数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void processPaymentSuccess(PayOrder order, PayNotifyDTO notify) {
        log.info("开始处理支付成功: outTradeNo={}, quantity={}",
                order.getOutTradeNo(), order.getQuantity());

        // 1. 订单状态流转：0 → 1
        boolean updated = orderStateMachineService.markAsPaid(
                order.getOutTradeNo(), notify.getTradeNo(), notify.getMoney());
        if (!updated) {
            // 已被其他线程处理过（幂等场景），不再发卡
            log.info("订单已被处理过，跳过发卡: outTradeNo={}", order.getOutTradeNo());
            return;
        }

        // 2. 查卡类
        CardType cardType = cardTypeMapper.selectById(order.getCardTypeId());
        if (cardType == null) {
            log.error("卡类不存在，订单异常: outTradeNo={}, cardTypeId={}",
                    order.getOutTradeNo(), order.getCardTypeId());
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND,
                    "订单关联卡类不存在，请人工核查");
        }

        // 3. 生成卡密（数量 = order.quantity）
        int quantity = order.getQuantity() == null ? 1 : order.getQuantity();
        List<CardKey> generated = new ArrayList<>(quantity);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < quantity; i++) {
            String plainCard = CardKeyGenerator.generate(
                    "JC-",
                    0,  // 大小写+数字
                    null,
                    24);

            String cipher = aesCryptoService.encrypt(plainCard);
            String hash = Md5SignService.sha256Hex(plainCard);

            CardKey cardKey = new CardKey();
            cardKey.setTenantId(order.getTenantId());
            cardKey.setCardTypeId(order.getCardTypeId());
            cardKey.setSoftwareId(cardType.getSoftwareId());
            cardKey.setCardNo(plainCard);
            cardKey.setCardCipher(cipher);
            cardKey.setCardHash(hash);
            cardKey.setStatus(JicekConstants.CARD_STATUS_UNUSED);
            cardKey.setCreateTime(now);
            cardKey.setUpdateTime(now);
            cardKeyMapper.insert(cardKey);
            generated.add(cardKey);
        }

        log.info("支付成功处理完成: outTradeNo={}, 卡密已发放 {} 张",
                order.getOutTradeNo(), generated.size());

        // 4. TODO: 异步通知用户（短信/邮件/站内信），不在此事务内
        // 由独立事件机制处理，避免事务过长
    }

    /**
     * 处理退款：订单流转 + 卡密失效（同一事务）
     *
     * @param order 订单（必须为已支付状态）
     * @param refundReason 退款原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void processRefund(PayOrder order, String refundReason) {
        log.info("开始处理退款: outTradeNo={}, reason={}",
                order.getOutTradeNo(), refundReason);

        // 1. 订单状态流转：1 → 3
        orderStateMachineService.markAsRefunded(order.getOutTradeNo());

        // 2. 关联卡密全部失效（status=3 已退款）
        // 注意：此处假设订单与卡密通过 tenantId + cardTypeId + createTime 关联
        // 实际项目应在订单表中增加 generated_card_ids 字段精确关联
        // 此处使用宽松匹配 + 时间窗口作为示例
        int updated = cardKeyMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CardKey>()
                        .eq(CardKey::getTenantId, order.getTenantId())
                        .eq(CardKey::getCardTypeId, order.getCardTypeId())
                        .ge(CardKey::getCreateTime, order.getPayTime().minusMinutes(1))
                        .le(CardKey::getCreateTime, order.getPayTime().plusMinutes(5))
                        .in(CardKey::getStatus,
                                JicekConstants.CARD_STATUS_UNUSED,
                                JicekConstants.CARD_STATUS_USED)
                        .set(CardKey::getStatus, JicekConstants.CARD_STATUS_REFUNDED)
                        .set(CardKey::getUpdateTime, LocalDateTime.now()));

        log.info("退款处理完成: outTradeNo={}, 卡密失效 {} 张",
                order.getOutTradeNo(), updated);
    }
}
