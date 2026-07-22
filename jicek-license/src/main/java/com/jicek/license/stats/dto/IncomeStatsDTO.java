package com.jicek.license.stats.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收入统计 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 数据源：jicek_pay_order（status=1 已支付），金额汇总
 * 维度：channel(支付通道) / cardType(卡类) / agent(代理，预留)
 *
 * 金额一律 BigDecimal（铁律 13：金额精度）
 */
@Data
public class IncomeStatsDTO {

    /** 当前统计维度 */
    private String dimension;

    /** 分项明细 */
    private List<IncomeItem> items;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 总订单数 */
    private Long totalCount;

    /**
     * 收入分项
     */
    @Data
    public static class IncomeItem {
        /** 维度键（通道名/卡类名/代理名） */
        private String name;
        /** 维度原始键（通道 code/卡类 ID/代理 ID） */
        private String key;
        /** 金额 */
        private BigDecimal amount;
        /** 订单数 */
        private Long count;
    }
}
