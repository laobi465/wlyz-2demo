package com.jicek.license.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单状态机服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 状态机（不可逆，铁律 06）：
 *   0 待支付 → 1 已支付（仅异步回调验签通过时）
 *   0 待支付 → 2 失败（异步回调返回失败）
 *   0 待支付 → 4 已关闭（定时任务超时扫描）
 *   1 已支付 → 3 已退款（管理员手动发起 + V1 api.php?act=refund 成功）
 *
 * 状态流转规则：
 * - 仅 status=0 才能转为 1/2/4
 * - 仅 status=1 才能转为 3
 * - 已退款/已关闭/失败 终态，不可再变
 */
@Slf4j
@Service
public class PayOrderStateMachineService {

    private final PayOrderMapper payOrderMapper;
    private final JicekProperties properties;

    public PayOrderStateMachineService(PayOrderMapper payOrderMapper, JicekProperties properties) {
        this.payOrderMapper = payOrderMapper;
        this.properties = properties;
    }

    /**
     * 创建订单（初始状态 0 待支付）
     */
    public PayOrder createOrder(PayOrder order) {
        order.setStatus(JicekConstants.ORDER_STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        payOrderMapper.insert(order);
        log.info("订单已创建: outTradeNo={}, amount={}", order.getOutTradeNo(), order.getAmount());
        return order;
    }

    /**
     * 标记为已支付（仅 status=0 可流转）
     * @return true 流转成功，false 订单不存在或状态非法
     */
    public boolean markAsPaid(String outTradeNo, String tradeNo, BigDecimal amount) {
        PayOrder order = getByOutTradeNo(outTradeNo);
        if (order == null) {
            log.warn("订单不存在: outTradeNo={}", outTradeNo);
            return false;
        }
        if (order.getStatus() != JicekConstants.ORDER_STATUS_PENDING) {
            log.warn("订单状态非待支付，无法转为已支付: outTradeNo={}, status={}",
                    outTradeNo, order.getStatus());
            return false;
        }
        // 金额必须一致（防篡改）
        if (amount != null && order.getAmount().compareTo(amount) != 0) {
            log.error("订单金额不匹配: outTradeNo={}, 期望={}, 实际={}",
                    outTradeNo, order.getAmount(), amount);
            throw new ServiceException(ResultCode.PAY_AMOUNT_MISMATCH);
        }
        order.setStatus(JicekConstants.ORDER_STATUS_PAID);
        order.setTradeNo(tradeNo);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        int rows = payOrderMapper.updateById(order);
        log.info("订单已支付: outTradeNo={}, tradeNo={}", outTradeNo, tradeNo);
        return rows > 0;
    }

    /**
     * 标记为失败（仅 status=0 可流转）
     */
    public boolean markAsFailed(String outTradeNo, String reason) {
        PayOrder order = getByOutTradeNo(outTradeNo);
        if (order == null || order.getStatus() != JicekConstants.ORDER_STATUS_PENDING) {
            return false;
        }
        order.setStatus(JicekConstants.ORDER_STATUS_FAILED);
        order.setUpdateTime(LocalDateTime.now());
        int rows = payOrderMapper.updateById(order);
        log.warn("订单失败: outTradeNo={}, reason={}", outTradeNo, reason);
        return rows > 0;
    }

    /**
     * 标记为已关闭（超时，仅 status=0 可流转）
     */
    public boolean markAsClosed(String outTradeNo) {
        PayOrder order = getByOutTradeNo(outTradeNo);
        if (order == null || order.getStatus() != JicekConstants.ORDER_STATUS_PENDING) {
            return false;
        }
        order.setStatus(JicekConstants.ORDER_STATUS_CLOSED);
        order.setUpdateTime(LocalDateTime.now());
        int rows = payOrderMapper.updateById(order);
        log.info("订单已关闭: outTradeNo={}", outTradeNo);
        return rows > 0;
    }

    /**
     * 标记为已退款（仅 status=1 可流转）
     * 注意：调用方必须先成功调用易支付 api.php?act=refund 后再调用本方法
     */
    public boolean markAsRefunded(String outTradeNo) {
        PayOrder order = getByOutTradeNo(outTradeNo);
        if (order == null) {
            throw new ServiceException(ResultCode.PAY_ORDER_NOT_FOUND);
        }
        if (order.getStatus() != JicekConstants.ORDER_STATUS_PAID) {
            throw new ServiceException(ResultCode.PAY_ORDER_STATUS_INVALID,
                    "订单状态非已支付，无法退款: " + order.getStatus());
        }
        order.setStatus(JicekConstants.ORDER_STATUS_REFUNDED);
        order.setUpdateTime(LocalDateTime.now());
        int rows = payOrderMapper.updateById(order);
        log.info("订单已退款: outTradeNo={}", outTradeNo);
        return rows > 0;
    }

    /**
     * 按订单号查询
     */
    public PayOrder getByOutTradeNo(String outTradeNo) {
        return payOrderMapper.selectOne(
                new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOutTradeNo, outTradeNo));
    }

    /**
     * 判断订单是否已超时（按配置 order-timeout-minutes）
     */
    public boolean isTimeout(PayOrder order) {
        if (order.getStatus() != JicekConstants.ORDER_STATUS_PENDING) {
            return false;
        }
        LocalDateTime deadline = order.getCreateTime()
                .plusMinutes(properties.getPay().getOrderTimeoutMinutes());
        return LocalDateTime.now().isAfter(deadline);
    }
}
