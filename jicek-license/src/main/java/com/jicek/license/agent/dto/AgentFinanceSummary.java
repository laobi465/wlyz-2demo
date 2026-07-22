package com.jicek.license.agent.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 代理资金汇总 DTO（控制台/我的资金页用）
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class AgentFinanceSummary {

    private Long agentId;

    private String username;

    private BigDecimal balance;

    private BigDecimal frozenBalance;

    private BigDecimal totalEarnings;

    private BigDecimal totalWithdraw;

    /** 待审核提现金额 */
    private BigDecimal pendingWithdraw;

    /** 本月分润 */
    private BigDecimal monthCommission;

    /** 本月销售订单数 */
    private Integer monthOrderCount;
}
