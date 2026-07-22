package com.jicek.license.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单回复实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 审计表：仅 INSERT + SELECT，禁 UPDATE/DELETE
 * replierType：1用户 2开发者 3管理员
 */
@Data
@TableName("jicek_ticket_reply")
public class TicketReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketId;

    /** 1用户 2开发者 3管理员 */
    private Integer replierType;

    private Long replierId;

    private String replierName;

    private String content;

    private LocalDateTime createTime;
}
