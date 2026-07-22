package com.jicek.license.ticket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.ticket.dto.TicketCreateDTO;
import com.jicek.license.ticket.dto.TicketDetailDTO;
import com.jicek.license.ticket.dto.TicketReplyDTO;
import com.jicek.license.ticket.entity.Ticket;
import com.jicek.license.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * H5 终端用户工单 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应 UI-DESIGN.md 6.4 节「终端用户 H5 > 工单（联系开发者）」：
 *  终端用户向开发者提交工单（target=1开发者），并可查看/回复自己的工单。
 *
 * 路由前缀：/api/h5/ticket
 *
 * 安全说明（铁律 04/06/13）：
 *  - creatorType 固定为 TICKET_CREATOR_USER(1)，由 Controller 设定，防越权
 *  - target 固定为 TICKET_TARGET_DEV(1)，终端用户只能向开发者提单
 *  - replierType 固定为 TICKET_REPLIER_USER(1)
 *  - userId 为终端用户 ID（H5 鉴权后传入，当前 H5 鉴权骨架待实现，暂用请求参数）
 *  - 租户隔离：所有查询带 tenantId
 *
 * 注：H5 整体框架（鉴权/路由/布局）待后续版本实现，本 Controller 提供后端 API，
 *     前端 H5 页面待 H5 框架就绪后补全。
 */
@RestController
@RequestMapping("/api/h5/ticket")
public class H5TicketController {

    private final TicketService ticketService;

    public H5TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 终端用户提交工单（向开发者）
     *
     * @param userId 终端用户 ID（H5 鉴权后从 token 解析，当前暂用请求参数）
     */
    @PostMapping
    public R<Long> create(@RequestBody @Valid TicketCreateDTO dto,
                          @RequestParam Long userId) {
        return R.ok(ticketService.createTicket(
                dto,
                JicekConstants.TICKET_CREATOR_USER,
                userId,
                JicekConstants.TICKET_TARGET_DEV));
    }

    /**
     * 终端用户查看自己的工单列表
     */
    @GetMapping("/page")
    public R<Page<Ticket>> page(@RequestParam Long tenantId,
                                @RequestParam Long userId,
                                @RequestParam(required = false) Integer category,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(defaultValue = "1") int current,
                                @RequestParam(defaultValue = "20") int size) {
        return R.ok(ticketService.page(
                tenantId,
                JicekConstants.TICKET_CREATOR_USER,
                userId,
                JicekConstants.TICKET_TARGET_DEV,
                category,
                status,
                current,
                size));
    }

    /**
     * 工单详情（含回复列表）
     */
    @GetMapping("/{tenantId}/{ticketId}")
    public R<TicketDetailDTO> detail(@PathVariable Long tenantId,
                                      @PathVariable Long ticketId) {
        return R.ok(ticketService.detail(ticketId, tenantId));
    }

    /**
     * 终端用户回复工单（补充信息）
     */
    @PostMapping("/reply")
    public R<Long> reply(@RequestBody @Valid TicketReplyDTO dto,
                         @RequestParam Long tenantId,
                         @RequestParam Long userId) {
        return R.ok(ticketService.reply(
                dto,
                tenantId,
                JicekConstants.TICKET_REPLIER_USER,
                userId));
    }
}
