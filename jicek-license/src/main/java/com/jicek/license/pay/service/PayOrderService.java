package com.jicek.license.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.pay.adapter.PayAdapter;
import com.jicek.license.pay.dto.PayRequestDTO;
import com.jicek.license.pay.dto.PayResponseDTO;
import com.jicek.license.pay.entity.PayConfig;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.mapper.PayOrderMapper;
import com.jicek.license.transaction.PaymentTransactionService;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardTypeMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单业务服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 1. 发起支付（构造订单 + 调用易支付）
 * 2. 订单分页查询
 * 3. 退款发起（管理员二次确认后）
 * 4. 订单超时关闭
 */
@Slf4j
@Service
public class PayOrderService {

    @Resource
    private PayOrderMapper payOrderMapper;

    @Resource
    private PayConfigService payConfigService;

    @Resource
    private PayOrderStateMachineService stateMachineService;

    @Resource
    private CardTypeMapper cardTypeMapper;

    @Resource
    private PayAdapter payAdapterV1;

    @Resource
    private PaymentTransactionService paymentTransactionService;

    /**
     * 发起支付
     */
    public PayResponseDTO createPay(PayRequestDTO request) {
        // 1. 校验支付配置
        PayConfig config = payConfigService.getByTenantId(request.getTenantId());
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new ServiceException(ResultCode.PAY_CONFIG_NOT_FOUND, "支付配置未启用");
        }

        // 2. 校验通道
        if (request.getPayType() == null || request.getPayType().isEmpty()) {
            throw new ServiceException(ResultCode.PAY_CHANNEL_DISABLED, "支付通道为空");
        }
        if (config.getEnabledChannels() == null
                || !config.getEnabledChannels().contains(request.getPayType())) {
            throw new ServiceException(ResultCode.PAY_CHANNEL_DISABLED, "通道未启用: " + request.getPayType());
        }

        // 3. 查卡类
        CardType cardType = cardTypeMapper.selectById(request.getCardTypeId());
        if (cardType == null || !cardType.getTenantId().equals(request.getTenantId())) {
            throw new ServiceException(ResultCode.CARD_TYPE_NOT_FOUND);
        }

        // 4. 计算金额
        BigDecimal amount = cardType.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        request.setAmount(amount);

        // 5. 创建订单
        PayOrder order = new PayOrder();
        order.setTenantId(request.getTenantId());
        order.setOutTradeNo(generateOutTradeNo());
        order.setCardTypeId(request.getCardTypeId());
        order.setQuantity(request.getQuantity());
        order.setAmount(amount);
        order.setPayType(request.getPayType());
        order.setUserIp(request.getUserIp());
        order.setDevice(request.getDevice());
        order.setParam(request.getParam());
        stateMachineService.createOrder(order);

        request.setOutTradeNo(order.getOutTradeNo());

        // 6. 调用易支付
        PayResponseDTO resp = payAdapterV1.createPay(request, config);
        resp.setOutTradeNo(order.getOutTradeNo());
        return resp;
    }

    /**
     * 分页查询订单
     */
    public Page<PayOrder> page(long current, long size, Long tenantId, Integer status) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .eq(tenantId != null, PayOrder::getTenantId, tenantId)
                .eq(status != null, PayOrder::getStatus, status)
                .orderByDesc(PayOrder::getCreateTime);
        return payOrderMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 退款（管理员二次确认后调用）
     */
    public void refund(String outTradeNo, String reason) {
        PayOrder order = stateMachineService.getByOutTradeNo(outTradeNo);
        if (order == null) {
            throw new ServiceException(ResultCode.PAY_ORDER_NOT_FOUND);
        }
        if (order.getStatus() != JicekConstants.ORDER_STATUS_PAID) {
            throw new ServiceException(ResultCode.PAY_ORDER_STATUS_INVALID,
                    "订单状态非已支付，无法退款");
        }

        // 1. 调用易支付退款
        PayConfig config = payConfigService.getByTenantId(order.getTenantId());
        boolean success = payAdapterV1.refund(outTradeNo, reason, config);
        if (!success) {
            throw new ServiceException(ResultCode.PAY_REFUND_FAIL, "易支付退款失败");
        }

        // 2. 资金事务：订单流转 + 卡密失效
        paymentTransactionService.processRefund(order, reason);
    }

    /**
     * 关闭超时订单（定时任务调用）
     */
    public int closeTimeoutOrders() {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, JicekConstants.ORDER_STATUS_PENDING);
        Page<PayOrder> page = payOrderMapper.selectPage(new Page<>(1, 500), wrapper);
        int count = 0;
        for (PayOrder order : page.getRecords()) {
            if (stateMachineService.isTimeout(order)) {
                if (stateMachineService.markAsClosed(order.getOutTradeNo())) {
                    count++;
                }
            }
        }
        return count;
    }

    private String generateOutTradeNo() {
        String timestamp = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d",
                new java.security.SecureRandom().nextInt(1000000));
        return "JC" + timestamp + random;
    }
}
