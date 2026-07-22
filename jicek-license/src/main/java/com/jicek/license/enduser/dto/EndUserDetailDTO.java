package com.jicek.license.enduser.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端用户详情 DTO（查询返回）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明：永不返回 passwordHash
 * softwareName 为冗余字段（join jicek_software 取 name）
 */
@Data
public class EndUserDetailDTO {

    private Long id;
    private Long tenantId;
    private Long softwareId;
    /** 冗余：软件名称（展示用） */
    private String softwareName;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    /** 0封禁 1正常 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
