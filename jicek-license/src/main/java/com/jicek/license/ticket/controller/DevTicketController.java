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
 * 开发者工单 Controller（单向工单：开发者→管理员）
 * 作者: 极策k  日期: 2026-07-22
 *
 * v0.6.1 简化：取消终端用户→开发者方向，仅保留开发者→管理员方向。
 * 开发者向管理员提交工单（如申请支付通道、解封账号等），并可查看/回复自己提交的工单。
 *
 * 路由前缀：/api/dev/ticket
 *
 * 接口：
 *  POST /submit         开发者向管理员提交工单（target=2, creatorType=2）
 *  GET  /submit/page    查询自己提交给管理员的工单
 *  GET  /submit/{id}    提交工单详情（含回复列表）
 *  POST /submit/reply   开发者补充回复（replierType=2）
 *
 * 安全说明（铁律 04/06/13）：
 *  - creatorType 固定为 TICKET_CREATOR_DEV(2)，target 固定为 TICKET_TARGET_ADMIN(2)，由 Controller 设定，防越权
 *  - replierType 固定为 TICKET_REPLIER_DEV(2)（开发者补充信息）
 *  - devUserId 为开发者用户 ID（鉴权后传入，当前暂用请求参数）
 *  - 租户隔离：所有查询带 tenantId
 *  - 管理员端回复接口（replierType=3）待管理员后台框架就绪后补全
 */
@RestController
@RequestMapping("/api/dev/ticket")
public class DevTicketController {

    private final TicketService ticketService;

    public DevTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

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
                devUserId,
                category,
                status,
                current,
                size));
    }

    /**
     * 提交工单详情（含回复列表）
     */
    @GetMapping("/submit/{tenantId}/{ticketId}")
    public R<TicketDetailDTO> submitDetail(@PathVariable Long tenantId,
                                            @PathVariable Long ticketId) {
        return R.ok(ticketService.detail(ticketId, tenantId));
    }

    /**
     * 开发者补充回复（补充信息，工单状态回到「处理中」提醒管理员有新信息）
     */
    @PostMapping("/submit/reply")
    public R<Long> submitReply(@RequestBody @Valid TicketReplyDTO dto,
                                @RequestParam Long tenantId,
                                @RequestParam Long devUserId) {
        return R.ok(ticketService.reply(
                dto,
                tenantId,
                JicekConstants.TICKET_REPLIER_DEV,
                devUserId));
    }
}
