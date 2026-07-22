package com.jicek.license.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置开发者密码 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 由管理员后台调用，无需校验原密码。密码长度与开发者登录一致（8-64）。
 */
@Data
public class DevUserResetPasswordDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度 8-64 字符")
    private String newPassword;
}
