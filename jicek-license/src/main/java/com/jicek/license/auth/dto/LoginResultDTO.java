package com.jicek.license.auth.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 返回 JWT token + 当前用户基础信息，前端持久化后所有受保护接口携带 Authorization 头。
 */
@Data
public class LoginResultDTO {

    /** JWT token（前端存 localStorage，请求头 Authorization: Bearer {token}） */
    private String token;
    /** token 类型，固定 Bearer */
    private String tokenType = "Bearer";
    /** 过期时间（秒） */
    private long expiresIn;
    /** 用户ID */
    private Long userId;
    /** 角色：1开发者 2管理员 */
    private Integer role;
    /** 租户ID（仅开发者有） */
    private Long tenantId;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
}
