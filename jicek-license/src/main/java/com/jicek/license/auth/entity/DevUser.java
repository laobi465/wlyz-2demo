package com.jicek.license.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开发者用户实体（租户账号）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应表 jicek_dev_user，每个开发者一个 tenantId，username 在租户内唯一。
 */
@Data
@TableName("jicek_dev_user")
public class DevUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String username;
    /** BCrypt 密码哈希 */
    private String passwordHash;
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
