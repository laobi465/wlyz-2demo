package com.jicek.license.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 开发者登录需带 tenantId；管理员登录不需要 tenantId（平台全局账号）。
 */
@Data
public class LoginDTO {

    /** 租户ID（开发者登录必填，管理员登录忽略） */
    private Long tenantId;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度超限")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度 8-64")
    private String password;
}
