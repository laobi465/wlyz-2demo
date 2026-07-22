package com.jicek.license.enduser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端用户实体（独立账号体系，v0.14.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应表 jicek_end_user，与卡密登录体系并存：
 *  - 终端用户绑定到具体软件（software_id），同软件内用户名唯一
 *  - tenantId + softwareId + username 三元唯一
 *  - 密码使用 BCrypt 哈希存储（cn.hutool.crypto.digest.BCrypt.hashpw）
 *  - status：0封禁 1正常（封禁后不可登录）
 */
@Data
@TableName("jicek_end_user")
public class EndUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long softwareId;
    private String username;
    /** BCrypt 密码哈希 */
    private String passwordHash;
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
