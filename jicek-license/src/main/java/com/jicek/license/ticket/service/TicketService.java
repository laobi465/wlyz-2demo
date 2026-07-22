package com.jicek.license.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.ticket.dto.TicketCreateDTO;
import com.jicek.license.ticket.dto.TicketDetailDTO;
import com.jicek.license.ticket.dto.TicketReplyDTO;
import com.jicek.license.ticket.entity.Ticket;
import com.jicek.license.ticket.entity.TicketReply;
import com.jicek.license.ticket.mapper.TicketMapper;
import com.jicek.license.ticket.mapper.TicketReplyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工单服务（单向工单：开发者→管理员）
 * 作者: 极策k  日期: 2026-07-22
 *
 * v0.6.1 简化：取消终端用户→开发者方向，仅保留开发者→管理员。
 *
 * 核心逻辑：
 *  - createTicket：开发者向管理员创建工单（creatorType=2, target=2）
 *  - reply：开发者补充信息（replierType=2，状态→处理中）/ 管理员回复（replierType=3，状态→已回复，待管理员 Controller 实现）
 *  - page：分页查询（开发者查自己提交的工单）
 *  - detail：工单详情 + 回复列表（按时间升序）
 *
 * 状态机（受控流转，铁律 06）：
 *  0待处理 →[开发者补充]→ 1处理中 →[管理员回复]→ 2已回复 →[关闭]→ 3已关闭
 *  任意状态可 →[关闭]→ 3已关闭
 *  已关闭禁回复（TICKET_ALREADY_CLOSED）
 *
 * 审计铁律：
 *  - jicek_ticket_reply 仅 INSERT + SELECT，禁 UPDATE/DELETE
 *  - jicek_ticket 仅受控 UPDATE（status/handlerId/handlerTime/closeTime/updateTime）
 */
@Slf4j
@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketReplyMapper ticketReplyMapper;

    public TicketService(TicketMapper ticketMapper, TicketReplyMapper ticketReplyMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketReplyMapper = ticketReplyMapper;
    }

    /**
     * 创建工单（开发者→管理员）
     *
     * @param dto         创建参数
     * @param creatorType 创建者类型（由 Controller 设定，当前固定为 TICKET_CREATOR_DEV）
     * @param creatorId   创建者 ID（开发者用户 ID）
     * @param target      工单目标（由 Controller 设定，当前固定为 TICKET_TARGET_ADMIN）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateDTO dto, int creatorType, Long creatorId, int target) {
        // 参数校验
        validateCategory(dto.getCategory());
        validateTarget(target);
        validateCreatorType(creatorType);
        if (dto.getTitle().length() > JicekConstants.TICKET_TITLE_MAX_LENGTH) {
            throw new ServiceException(ResultCode.TICKET_CONTENT_TOO_LONG, "标题超过长度限制");
        }
        if (dto.getContent().length() > JicekConstants.TICKET_CONTENT_MAX_LENGTH) {
            throw new ServiceException(ResultCode.TICKET_CONTENT_TOO_LONG, "内容超过长度限制");
        }

        Ticket ticket = new Ticket();
        ticket.setTenantId(dto.getTenantId());
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(dto.getTitle());
        ticket.setContent(dto.getContent());
        ticket.setCategory(dto.getCategory());
        ticket.setTarget(target);
        ticket.setStatus(JicekConstants.TICKET_STATUS_PENDING);
        ticket.setCreatorType(creatorType);
        ticket.setCreatorId(creatorId);
        ticket.setCreatorName(dto.getCreatorName());
        ticket.setSoftwareId(dto.getSoftwareId());
        ticket.setDeviceId(dto.getDeviceId());
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);

        ticketMapper.insert(ticket);
        log.info("工单创建成功: ticketNo={}, tenantId={}, creatorId={}, target={}",
                ticket.getTicketNo(), ticket.getTenantId(), creatorId, target);
        return ticket.getId();
    }

    /**
     * 回复工单
     *
     * @param dto         回复参数
     * @param tenantId    租户 ID
     * @param replierType 回复者类型（2开发者 3管理员，由 Controller 设定）
     * @param replierId   回复者 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long reply(TicketReplyDTO dto, Long tenantId, int replierType, Long replierId) {
        Ticket ticket = getTicketOrThrow(dto.getTicketId(), tenantId);

        // 已关闭禁回复
        if (ticket.getStatus() == JicekConstants.TICKET_STATUS_CLOSED) {
            throw new ServiceException(ResultCode.TICKET_ALREADY_CLOSED);
        }
        if (dto.getContent().length() > JicekConstants.TICKET_CONTENT_MAX_LENGTH) {
            throw new ServiceException(ResultCode.TICKET_CONTENT_TOO_LONG, "回复内容超过长度限制");
        }

        // 插入回复（审计表，仅 INSERT）
        TicketReply reply = new TicketReply();
        reply.setTenantId(tenantId);
        reply.setTicketId(dto.getTicketId());
        reply.setReplierType(replierType);
        reply.setReplierId(replierId);
        reply.setReplierName(dto.getReplierName());
        reply.setContent(dto.getContent());
        reply.setCreateTime(LocalDateTime.now());
        ticketReplyMapper.insert(reply);

        // 受控更新工单状态
        int newStatus = decideStatusAfterReply(replierType);
        updateTicketStatusOnReply(ticket, newStatus, replierId);

        log.info("工单回复成功: ticketId={}, replierType={}, replierId={}, newStatus={}",
                dto.getTicketId(), replierType, replierId, newStatus);
        return reply.getId();
    }

    /**
     * 分页查询工单（开发者查询自己提交的工单）
     *
     * @param tenantId   租户 ID
     * @param creatorId  创建者 ID（开发者用户 ID）
     * @param category   分类（可选）
     * @param status     状态（可选）
     * @param current    当前页
     * @param size       每页条数
     */
    public Page<Ticket> page(Long tenantId, Long creatorId,
                              Integer category, Integer status,
                              int current, int size) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<>();
        qw.eq(Ticket::getTenantId, tenantId)
                .eq(Ticket::getCreatorId, creatorId)
                .eq(Ticket::getCreatorType, JicekConstants.TICKET_CREATOR_DEV)
                .eq(Ticket::getTarget, JicekConstants.TICKET_TARGET_ADMIN)
                .eq(category != null, Ticket::getCategory, category)
                .eq(status != null, Ticket::getStatus, status)
                .orderByDesc(Ticket::getCreateTime);
        return ticketMapper.selectPage(new Page<>(current, size), qw);
    }

    /**
     * 工单详情（主表 + 回复列表）
     */
    public TicketDetailDTO detail(Long ticketId, Long tenantId) {
        Ticket ticket = getTicketOrThrow(ticketId, tenantId);

        LambdaQueryWrapper<TicketReply> qw = new LambdaQueryWrapper<>();
        qw.eq(TicketReply::getTenantId, tenantId)
                .eq(TicketReply::getTicketId, ticketId)
                .orderByAsc(TicketReply::getCreateTime);
        List<TicketReply> replies = ticketReplyMapper.selectList(qw);

        TicketDetailDTO dto = new TicketDetailDTO();
        dto.setTicket(ticket);
        dto.setReplies(replies);
        return dto;
    }

    /* ============ 私有工具方法 ============ */

    private Ticket getTicketOrThrow(Long ticketId, Long tenantId) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<>();
        qw.eq(Ticket::getId, ticketId).eq(Ticket::getTenantId, tenantId);
        Ticket ticket = ticketMapper.selectOne(qw);
        if (ticket == null) {
            throw new ServiceException(ResultCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    /**
     * 根据回复者类型决定工单新状态：
     *  - 开发者回复（replierType=2，补充信息）：工单回到「处理中」（提醒管理员有新信息）
     *  - 管理员回复（replierType=3，处理回复）：工单变为「已回复」
     */
    private int decideStatusAfterReply(int replierType) {
        if (replierType == JicekConstants.TICKET_REPLIER_DEV) {
            return JicekConstants.TICKET_STATUS_PROCESSING;
        }
        return JicekConstants.TICKET_STATUS_REPLIED;
    }

    /**
     * 回复时受控更新工单状态：
     *  - 首次处理：设置 handlerId + handlerTime
     *  - 状态流转
     */
    private void updateTicketStatusOnReply(Ticket ticket, int newStatus, Long handlerId) {
        LambdaUpdateWrapper<Ticket> uw = new LambdaUpdateWrapper<>();
        uw.eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getTenantId, ticket.getTenantId())
                .set(Ticket::getStatus, newStatus)
                .set(Ticket::getUpdateTime, LocalDateTime.now());
        if (ticket.getHandlerId() == null) {
            uw.set(Ticket::getHandlerId, handlerId);
            uw.set(Ticket::getHandlerTime, LocalDateTime.now());
        }
        ticketMapper.update(null, uw);
    }

    /**
     * 生成工单号：TK + yyyyMMddHHmmss + 4位随机数
     */
    private String generateTicketNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return JicekConstants.TICKET_NO_PREFIX + ts + rand;
    }

    private void validateCategory(Integer category) {
        if (category == null
                || (category != JicekConstants.TICKET_CATEGORY_CHANGE_DEVICE
                && category != JicekConstants.TICKET_CATEGORY_RECHARGE
                && category != JicekConstants.TICKET_CATEGORY_CARD
                && category != JicekConstants.TICKET_CATEGORY_OTHER)) {
            throw new ServiceException(ResultCode.TICKET_CATEGORY_INVALID);
        }
    }

    private void validateTarget(int target) {
        if (target != JicekConstants.TICKET_TARGET_ADMIN) {
            throw new ServiceException(ResultCode.TICKET_TARGET_INVALID);
        }
    }

    private void validateCreatorType(int creatorType) {
        if (creatorType != JicekConstants.TICKET_CREATOR_DEV) {
            throw new ServiceException(ResultCode.TICKET_CREATOR_TYPE_INVALID);
        }
    }
}
