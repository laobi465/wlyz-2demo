package com.jicek.license.deploy.dto;

import lombok.Data;

/**
 * Webhook 触发结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * GitHub Webhook 接收后立即返回，部署过程异步执行
 */
@Data
public class WebhookResultDTO {

    /** 是否接收成功（仅表示 Webhook 已接收，不代表部署成功） */
    private boolean accepted;

    /** 部署日志 ID（用于后续查询状态） */
    private Long deployLogId;

    /** 提示信息 */
    private String message;

    public static WebhookResultDTO accepted(Long deployLogId, String message) {
        WebhookResultDTO dto = new WebhookResultDTO();
        dto.setAccepted(true);
        dto.setDeployLogId(deployLogId);
        dto.setMessage(message);
        return dto;
    }

    public static WebhookResultDTO ignored(String message) {
        WebhookResultDTO dto = new WebhookResultDTO();
        dto.setAccepted(false);
        dto.setMessage(message);
        return dto;
    }
}
