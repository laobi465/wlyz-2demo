package com.jicek.license.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开发者用户详情 DTO（不含密码哈希，铁律 09 防泄露）
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class DevUserDetailDTO {

    private Long id;
    private Long tenantId;
    private String username;
    private String nickname;
    private String email;
    /** 0封禁 1正常 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
