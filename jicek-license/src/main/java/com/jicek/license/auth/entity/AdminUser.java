package com.jicek.license.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户实体（平台超管）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应表 jicek_admin_user，平台全局账号，无 tenantId 隔离。
 * role：1超级管理员 2运营
 */
@Data
@TableName("jicek_admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    /** BCrypt 密码哈希 */
    private String passwordHash;
    private String nickname;
    /** 1超级管理员 2运营 */
    private Integer role;
    /** 0封禁 1正常 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
