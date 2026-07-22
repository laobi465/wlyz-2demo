package com.jicek.license.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单回复 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * replierType 由 Controller 按入口设定，不由前端传入：
 *  - H5 端回复：replierType=1 用户
 *  - Dev 端回复：replierType=2 开发者
 *  - Admin 端回复：replierType=3 管理员（待 Admin Controller 实现）
 */
@Data
public class TicketReplyDTO {

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotBlank(message = "回复内容不能为空")
    private String content;

    /** 回复者名称（前端传入，如用户名/开发者名） */
    private String replierName;
}
