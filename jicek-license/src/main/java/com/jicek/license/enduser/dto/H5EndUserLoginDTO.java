package com.jicek.license.enduser.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * H5 终端用户账号登录请求 DTO（v0.14.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户通过 appKey + username + password 登录，与卡密登录并存。
 * appKey 用于解析出 tenantId + softwareId，再按 (tenantId, softwareId, username) 定位用户。
 * 密码明文传输（H5 走 HTTPS，与卡密登录一致）。
 */
@Data
public class H5EndUserLoginDTO {

    @NotBlank(message = "AppKey 不能为空")
    private String appKey;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
