package com.jicek.license.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.ticket.entity.TicketReply;

/**
 * 工单回复 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 审计表，仅 INSERT + SELECT，禁 UPDATE/DELETE（铁律 06）
 */
public interface TicketReplyMapper extends BaseMapper<TicketReply> {
}
