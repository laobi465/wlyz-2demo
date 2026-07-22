package com.jicek.license.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.agent.dto.AgentSaveDTO;
import com.jicek.license.agent.dto.AgentTreeNode;
import com.jicek.license.agent.entity.Agent;
import com.jicek.license.agent.entity.Commission;
import com.jicek.license.agent.service.AgentService;
import com.jicek.license.agent.service.CommissionService;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 开发者代理管理 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 提供代理 CRUD、树形查询、封禁/解封、充值、分润流水查询
 * 注意：提现审核走 DevWithdrawController
 */
@RestController
@RequestMapping("/api/dev/agent")
public class DevAgentController {

    private final AgentService agentService;
    private final CommissionService commissionService;

    public DevAgentController(AgentService agentService, CommissionService commissionService) {
        this.agentService = agentService;
        this.commissionService = commissionService;
    }

    /**
     * 创建代理
     */
    @PostMapping
    public R<Long> create(@Valid @RequestBody AgentSaveDTO dto) {
        return R.ok(agentService.createAgent(dto));
    }

    /**
     * 更新代理
     */
    @PutMapping
    public R<Void> update(@Valid @RequestBody AgentSaveDTO dto) {
        agentService.updateAgent(dto);
        return R.ok();
    }

    /**
     * 代理分页查询（扁平）
     */
    @GetMapping("/page")
    public R<Page<Agent>> page(@RequestParam Long tenantId,
                                @RequestParam(required = false) Long parentId,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return R.ok(agentService.pageAgents(tenantId, parentId, status, page, size));
    }

    /**
     * 代理树形查询（多级）
     */
    @GetMapping("/tree")
    public R<List<AgentTreeNode>> tree(@RequestParam Long tenantId,
                                        @RequestParam(defaultValue = "0") Long rootParentId) {
        return R.ok(agentService.buildAgentTree(tenantId, rootParentId));
    }

    /**
     * 代理详情
     */
    @GetMapping("/{tenantId}/{agentId}")
    public R<Agent> get(@PathVariable Long tenantId, @PathVariable Long agentId) {
        return R.ok(agentService.getAgentById(tenantId, agentId));
    }

    /**
     * 封禁代理
     */
    @PostMapping("/ban")
    public R<Void> ban(@RequestParam Long tenantId, @RequestParam Long agentId) {
        agentService.banAgent(tenantId, agentId);
        return R.ok();
    }

    /**
     * 解封代理
     */
    @PostMapping("/unban")
    public R<Void> unban(@RequestParam Long tenantId, @RequestParam Long agentId) {
        agentService.unbanAgent(tenantId, agentId);
        return R.ok();
    }

    /**
     * 代理充值
     */
    @PostMapping("/recharge")
    public R<Void> recharge(@RequestParam Long tenantId,
                             @RequestParam Long agentId,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String remark) {
        agentService.recharge(tenantId, agentId, amount, remark);
        return R.ok();
    }

    /**
     * 代理的分润流水分页查询
     */
    @GetMapping("/commission/page")
    public R<Page<Commission>> commissionPage(@RequestParam Long tenantId,
                                                @RequestParam(required = false) Long agentId,
                                                @RequestParam(required = false) Long sourceAgentId,
                                                @RequestParam(required = false) Integer type,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return R.ok(commissionService.pageCommissions(
                tenantId, agentId, sourceAgentId, type, status, page, size));
    }

    /**
     * 重新生成代理邀请码
     */
    @PostMapping("/{tenantId}/{agentId}/regenerate-invite-code")
    public R<String> regenerateInviteCode(@PathVariable Long tenantId, @PathVariable Long agentId) {
        return R.ok(agentService.regenerateInviteCode(tenantId, agentId));
    }
}
