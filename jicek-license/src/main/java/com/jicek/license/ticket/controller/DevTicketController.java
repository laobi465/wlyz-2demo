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
 * 开发者后台工单 Controller（双向工单双角色）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 开发者在工单系统中承担双重角色：
 *  1. 处理者：处理终端用户提交的工单（target=1开发者），回复/关闭
 *  2. 提交者：向管理员提交工单（target=2管理员，creatorType=2开发者），如申请支付通道/解封账号
 *
 * 路由前缀：/api/dev/ticket
 *
 * 接口分组：
 *  - [处理者] GET  /receive/page   查询终端用户提交的工单（target=1）
 *  - [处理者] GET  /receive/{id}   工单详情
 *  - [处理者] POST /receive/reply  回复终端用户工单
 *  - [处理者] POST /receive/close  关闭工单
 *  - [提交者] POST /submit         开发者向管理员提交工单
 *  - [提交者] GET  /submit/page    查询自己提交给管理员的工单
 *  - [提交者] GET  /submit/{id}    提交工单详情
 *
 * 安全说明（铁律 04/06/13）：
 *  - 处理角色 replierType 固定为 TICKET_REPLIER_DEV(2)
 *  - 提交角色 creatorType 固定为 TICKET_CREATOR_DEV(2)，target 固定为 TICKET_TARGET_ADMIN(2)
 *  - devUserId 为开发者用户 ID（鉴权后传入，当前暂用请求参数）
 *  - 租户隔离：所有查询带 tenantId
 */
@RestController
@RequestMapping("/api/dev/ticket")
public class DevTicketController {

    private final TicketService ticketService;

    public DevTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /* ============ [处理者] 处理终端用户工单（target=1） ============ */

    /**
     * 查询终端用户提交的工单列表（开发者作为处理方）
     */
    @GetMapping("/receive/page")
    public R<Page<Ticket>> receivePage(@RequestParam Long tenantId,
                                        @RequestParam(required = false) Integer category,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) Integer creatorType,
                                        @RequestParam(defaultValue = "1") int current,
                                        @RequestParam(defaultValue = "20") int size) {
        // target=1 表示工单目标是开发者
        return R.ok(ticketService.page(
                tenantId,
                creatorType,
                null,
                JicekConstants.TICKET_TARGET_DEV,
                category,
                status,
                current,
                size));
    }

    /**
     * 工单详情（含回复列表）
     */
    @GetMapping("/receive/{tenantId}/{ticketId}")
    public R<TicketDetailDTO> receiveDetail(@PathVariable Long tenantId,
                                             @PathVariable Long ticketId) {
        return R.ok(ticketService.detail(ticketId, tenantId));
    }

    /**
     * 开发者回复终端用户工单
     */
    @PostMapping("/receive/reply")
    public R<Long> receiveReply(@RequestBody @Valid TicketReplyDTO dto,
                                 @RequestParam Long tenantId,
                                 @RequestParam Long devUserId) {
        return R.ok(ticketService.reply(
                dto,
                tenantId,
                JicekConstants.TICKET_REPLIER_DEV,
                devUserId));
    }

    /**
     * 关闭工单（开发者关闭已处理完的工单）
     */
    @PostMapping("/receive/close")
    public R<Void> receiveClose(@RequestParam Long tenantId,
                                 @RequestParam Long ticketId,
                                 @RequestParam Long devUserId) {
        ticketService.close(ticketId, tenantId, devUserId);
        return R.ok();
    }

    /* ============ [提交者] 开发者向管理员提交工单（target=2） ============ */

    /**
     * 开发者向管理员提交工单（如申请支付通道、解封账号等）
     */
    @PostMapping("/submit")
    public R<Long> submit(@RequestBody @Valid TicketCreateDTO dto,
                          @RequestParam Long devUserId) {
        return R.ok(ticketService.createTicket(
                dto,
                JicekConstants.TICKET_CREATOR_DEV,
                devUserId,
                JicekConstants.TICKET_TARGET_ADMIN));
    }

    /**
     * 查询开发者自己提交给管理员的工单
     */
    @GetMapping("/submit/page")
    public R<Page<Ticket>> submitPage(@RequestParam Long tenantId,
                                       @RequestParam Long devUserId,
                                       @RequestParam(required = false) Integer category,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") int current,
                                       @RequestParam(defaultValue = "20") int size) {
        return R.ok(ticketService.page(
                tenantId,
                JicekConstants.TICKET_CREATOR_DEV,
                devUserId,
                JicekConstants.TICKET_TARGET_ADMIN,
                category,
                status,
                current,
                size));
    }

    /**
     * 提交工单详情
     */
    @GetMapping("/submit/{tenantId}/{ticketId}")
    public R<TicketDetailDTO> submitDetail(@PathVariable Long tenantId,
                                            @PathVariable Long ticketId) {
        return R.ok(ticketService.detail(ticketId, tenantId));
    }
}
