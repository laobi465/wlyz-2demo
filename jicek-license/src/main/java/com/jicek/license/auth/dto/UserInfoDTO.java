package com.jicek.license.auth.dto;

import lombok.Data;

/**
 * 当前登录用户信息 DTO
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class UserInfoDTO {

    private Long userId;
    /** 1开发者 2管理员 */
    private Integer role;
    private Long tenantId;
    private String username;
    private String nickname;
    private String email;
    private Integer status;
}
