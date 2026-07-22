package com.jicek.license.deploy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动触发部署请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class ManualDeployDTO {

    /** 租户 ID（必填） */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /** 分支名（可选，默认 main） */
    private String branch;
}
