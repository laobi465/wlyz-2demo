package com.jicek.license.enduser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 终端用户重置密码 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 由开发者后台调用，无需校验原密码。
 */
@Data
public class EndUserResetPasswordDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 字符")
    private String newPassword;
}
