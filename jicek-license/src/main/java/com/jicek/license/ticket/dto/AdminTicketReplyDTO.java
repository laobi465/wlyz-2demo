package com.jicek.license.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员回复工单 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 工单ID 由路径参数传入，此处仅含回复内容。
 * replierType 由 Service 固定为 TICKET_REPLIER_ADMIN，不由前端传入。
 */
@Data
public class AdminTicketReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 4096, message = "回复内容超过长度限制")
    private String content;
}
