package com.jicek.license.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.agent.dto.WithdrawApplyDTO;
import com.jicek.license.agent.dto.WithdrawAuditDTO;
import com.jicek.license.agent.entity.Withdraw;
import com.jicek.license.agent.service.WithdrawService;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者提现审核 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 提供：
 * - 代理提交提现申请（POST /apply）
 * - 开发者审核（POST /audit，action: approve/reject/payout/fail）
 * - 提现申请分页查询
 * - 提现详情
 */
@RestController
@RequestMapping("/api/dev/withdraw")
public class DevWithdrawController {

    private final WithdrawService withdrawService;

    public DevWithdrawController(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    /**
     * 代理申请提现
     */
    @PostMapping("/apply")
    public R<Long> apply(@Valid @RequestBody WithdrawApplyDTO dto) {
        return R.ok(withdrawService.applyWithdraw(dto));
    }

    /**
     * 开发者审核提现
     */
    @PostMapping("/audit")
    public R<Void> audit(@Valid @RequestBody WithdrawAuditDTO dto,
                          @RequestParam(defaultValue = "1") Long auditBy) {
        withdrawService.auditWithdraw(dto, auditBy);
        return R.ok();
    }

    /**
     * 提现申请分页查询
     */
    @GetMapping("/page")
    public R<Page<Withdraw>> page(@RequestParam Long tenantId,
                                   @RequestParam(required = false) Long agentId,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return R.ok(withdrawService.pageWithdraws(tenantId, agentId, status, page, size));
    }

    /**
     * 提现申请详情
     */
    @GetMapping("/{tenantId}/{withdrawId}")
    public R<Withdraw> get(@PathVariable Long tenantId, @PathVariable Long withdrawId) {
        return R.ok(withdrawService.getWithdrawById(tenantId, withdrawId));
    }

    /**
     * 代理待审核提现总金额（用于代理资金汇总）
     */
    @GetMapping("/pending-amount")
    public R<java.math.BigDecimal> pendingAmount(@RequestParam Long tenantId,
                                                   @RequestParam Long agentId) {
        return R.ok(withdrawService.sumPendingAmount(tenantId, agentId));
    }
}
