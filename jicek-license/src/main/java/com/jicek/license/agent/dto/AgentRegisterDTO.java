package com.jicek.license.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代理注册 DTO（v0.13.0 邀请码注册）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 代理通过邀请码注册，需指定所属软件（appKey 关联的开发者租户）。
 */
@Data
public class AgentRegisterDTO {
    /** 软件 AppKey（用于定位开发者租户） */
    @NotBlank(message = "AppKey 不能为空")
    private String appKey;

    /** 邀请码 */
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（明文，后端 BCrypt 加密） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 联系方式 */
    private String contact;
}
