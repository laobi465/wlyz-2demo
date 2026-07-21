package com.jicek.license.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.mapper.PayOrderMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开发者控制台仪表盘 Controller
 * 作者: 极策k  日期: 2026-07-21
 *
 * 提供今日验证量、在线设备、收入、卡密销量等汇总数据
 */
@RestController
@RequestMapping("/api/dev/dashboard")
public class DevDashboardController {

    private final PayOrderMapper payOrderMapper;
    private final CardKeyMapper cardKeyMapper;

    public DevDashboardController(PayOrderMapper payOrderMapper, CardKeyMapper cardKeyMapper) {
        this.payOrderMapper = payOrderMapper;
        this.cardKeyMapper = cardKeyMapper;
    }

    /**
     * 控制台汇总数据
     */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam Long tenantId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        LocalDateTime todayEnd = todayStart.plusDays(1);

        Map<String, Object> data = new HashMap<>();

        // 今日订单数
        Long todayOrderCount = payOrderMapper.selectCount(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getTenantId, tenantId)
                        .eq(PayOrder::getStatus, JicekConstants.ORDER_STATUS_PAID)
                        .ge(PayOrder::getPayTime, todayStart)
                        .lt(PayOrder::getPayTime, todayEnd));
        data.put("todayOrderCount", todayOrderCount);

        // 今日收入
        List<PayOrder> todayOrders = payOrderMapper.selectList(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getTenantId, tenantId)
                        .eq(PayOrder::getStatus, JicekConstants.ORDER_STATUS_PAID)
                        .ge(PayOrder::getPayTime, todayStart)
                        .lt(PayOrder::getPayTime, todayEnd));
        BigDecimal todayIncome = todayOrders.stream()
                .map(PayOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("todayIncome", todayIncome);

        // 今日已退款
        List<PayOrder> refundOrders = payOrderMapper.selectList(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getTenantId, tenantId)
                        .eq(PayOrder::getStatus, JicekConstants.ORDER_STATUS_REFUNDED)
                        .ge(PayOrder::getUpdateTime, todayStart)
                        .lt(PayOrder::getUpdateTime, todayEnd));
        BigDecimal todayRefund = refundOrders.stream()
                .map(PayOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("todayRefund", todayRefund);

        // 今日净收入
        data.put("todayNetIncome", todayIncome.subtract(todayRefund));

        // 今日生成卡密数
        Long todayCardCount = cardKeyMapper.selectCount(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .ge(CardKey::getCreateTime, todayStart)
                        .lt(CardKey::getCreateTime, todayEnd));
        data.put("todayCardCount", todayCardCount);

        // 卡密状态分布
        Map<String, Long> cardStatus = new HashMap<>();
        cardStatus.put("unused", cardKeyMapper.selectCount(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(CardKey::getStatus, JicekConstants.CARD_STATUS_UNUSED)));
        cardStatus.put("used", cardKeyMapper.selectCount(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(CardKey::getStatus, JicekConstants.CARD_STATUS_USED)));
        cardStatus.put("banned", cardKeyMapper.selectCount(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(CardKey::getStatus, JicekConstants.CARD_STATUS_BANNED)));
        data.put("cardStatus", cardStatus);

        return R.ok(data);
    }
}
