package com.jicek.license.software.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 软件新建/更新 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 新建时：id 为空，Service 自动生成 appKey / signSecret / RSA 密钥对
 * 更新时：id 非空，仅允许修改 name/version/minVersion/heartbeatInterval/maxConcurrent/enabled
 *         appKey / signSecret / RSA 密钥不可通过此接口修改（需调用专用轮换接口）
 *
 * tenantId 由 Service 从 AuthContext 获取，前端禁传（防越权）
 */
@Data
public class SoftwareSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    /** 软件名称（同租户下唯一） */
    @NotBlank(message = "软件名称不能为空")
    @Size(max = 64, message = "软件名称最长 64 字符")
    private String name;

    /** 当前版本 */
    @Size(max = 20, message = "版本号最长 20 字符")
    private String version;

    /** 最低支持版本 */
    @Size(max = 20, message = "最低版本号最长 20 字符")
    private String minVersion;

    /** 心跳间隔（秒，5-300） */
    @Min(value = 5, message = "心跳间隔最少 5 秒")
    @Max(value = 300, message = "心跳间隔最多 300 秒")
    private Integer heartbeatInterval = 60;

    /** 最大并发会话数（≥ 1） */
    @Min(value = 1, message = "最大并发会话数至少 1")
    private Integer maxConcurrent = 1;

    /** 0禁用 1启用 */
    @Min(value = 0, message = "enabled 仅支持 0 或 1")
    @Max(value = 1, message = "enabled 仅支持 0 或 1")
    private Integer enabled = 1;
}
