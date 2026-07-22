package com.jicek.license.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 提现审核 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * action 取值：
 * - approve：审核通过（0→1），等待打款
 * - reject：审核拒绝（0→2），余额退回
 * - payout：打款成功（1→3）
 * - fail：打款失败（1→4），余额退回
 */
@Data
public class WithdrawAuditDTO {

    @NotNull
    private Long tenantId;

    @NotNull
    private Long withdrawId;

    @NotNull
    @Pattern(regexp = "approve|reject|payout|fail")
    private String action;

    /** 审核备注（拒绝原因/通过说明） */
    private String auditRemark;

    /** 打款流水号（payout 时填写） */
    private String tradeNo;

    /** 打款失败原因（fail 时填写） */
    private String failReason;
}
