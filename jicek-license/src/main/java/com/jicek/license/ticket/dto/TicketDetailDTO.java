package com.jicek.license.ticket.dto;

import com.jicek.license.ticket.entity.Ticket;
import com.jicek.license.ticket.entity.TicketReply;
import lombok.Data;

import java.util.List;

/**
 * 工单详情 DTO（工单主表 + 回复列表）
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class TicketDetailDTO {

    /** 工单主表信息 */
    private Ticket ticket;

    /** 回复列表（按时间升序） */
    private List<TicketReply> replies;
}
