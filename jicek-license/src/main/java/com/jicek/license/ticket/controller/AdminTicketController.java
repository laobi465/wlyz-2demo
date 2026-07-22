package com.jicek.license.ticket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.ticket.dto.AdminTicketReplyDTO;
import com.jicek.license.ticket.dto.TicketDetailDTO;
import com.jicek.license.ticket.entity.Ticket;
import com.jicek.license.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员工单 Controller（v0.15.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 路由前缀：/api/admin/ticket
 * 全部 @AuthRequired(role=ROLE_ADMIN)，仅管理员可访问。
 *
 * 接口：
 *  GET  /page          分页查询所有租户工单（支持 tenantId/category/status 筛选）
 *  GET  /{id}          工单详情（含回复列表）
 *  POST /{id}/reply    管理员回复工单（replierType=TICKET_REPLIER_ADMIN，状态→已回复）
 *  POST /{id}/close    关闭工单（状态→已关闭）
 *
 * 安全说明（铁律 04/06/13/09）：
 *  - 管理员可查看全部租户工单，不限 tenantId
 *  - 管理员回复 replierType 固定为 TICKET_REPLIER_ADMIN(3)，replierId 从 AuthContext 获取
 *  - 审计表 jicek_ticket_reply 仅 INSERT
 */
@RestController
@RequestMapping("/api/admin/ticket")
@AuthRequired(role = JicekConstants.ROLE_ADMIN)
public class AdminTicketController {

    private final TicketService ticketService;

    public AdminTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 分页查询所有租户工单
     */
    @GetMapping("/page")
    public R<Page<Ticket>> page(@RequestParam(defaultValue = "1") int current,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) Long tenantId,
                                 @RequestParam(required = false) Integer category,
                                 @RequestParam(required = false) Integer status) {
        return R.ok(ticketService.adminPage(current, size, tenantId, category, status));
    }

    /**
     * 工单详情（含回复列表）
     */
    @GetMapping("/{id}")
    public R<TicketDetailDTO> get(@PathVariable Long id) {
        return R.ok(ticketService.adminGet(id));
    }

    /**
     * 管理员回复工单
     */
    @PostMapping("/{id}/reply")
    public R<Long> reply(@PathVariable Long id,
                          @Valid @RequestBody AdminTicketReplyDTO dto) {
        return R.ok(ticketService.adminReply(id, dto.getContent()));
    }

    /**
     * 关闭工单
     */
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id) {
        ticketService.adminClose(id);
        return R.ok();
    }
}
