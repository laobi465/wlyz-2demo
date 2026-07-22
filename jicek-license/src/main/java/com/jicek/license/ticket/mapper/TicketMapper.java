package com.jicek.license.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.ticket.entity.Ticket;

/**
 * 工单 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 说明：工单主表支持受控 UPDATE（status/handlerId/handlerTime/closeTime/updateTime），
 * 与审计回复表（仅 INSERT+SELECT）区分。
 */
public interface TicketMapper extends BaseMapper<Ticket> {
}
