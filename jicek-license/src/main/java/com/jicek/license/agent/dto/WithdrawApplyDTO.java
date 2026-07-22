package com.jicek.license.agent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提现申请 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全：
 * - amount 必须 ≥ 最低提现金额（Service 校验，默认 10 元）
 * - amount 必须 ≤ 代理可用余额
 * - payType 仅允许 alipay/wxpay/bank
 */
@Data
public class WithdrawApplyDTO {

    @NotNull
    private Long tenantId;

    @NotNull
    private Long agentId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "alipay|wxpay|bank")
    private String payType;

    @NotBlank
    @Size(max = 128)
    private String payAccount;

    @Size(max = 64)
    private String payName;

    /** 手续费（开发者预设费率，由后端覆写，前端传入仅用于展示） */
    private BigDecimal fee;
}
