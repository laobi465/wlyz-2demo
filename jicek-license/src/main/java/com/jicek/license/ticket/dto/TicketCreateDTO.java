package com.jicek.license.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单创建 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * creatorType 由 Controller 按入口设定，不由前端传入（防越权）：
 *  - H5 端（/api/h5/ticket）：creatorType=1 终端用户
 *  - Dev 端（/api/dev/ticket）：creatorType=2 开发者（向管理员提单）
 */
@Data
public class TicketCreateDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String content;

    /** 1换机申请 2充值问题 3卡密问题 4其他 */
    @NotNull(message = "分类不能为空")
    private Integer category;

    /** 关联软件（可选，换机申请/卡密问题建议填写） */
    private Long softwareId;

    /** 关联设备（换机申请时填） */
    private Long deviceId;

    /** 创建者名称（前端传入，如用户名/代理名） */
    private String creatorName;
}
