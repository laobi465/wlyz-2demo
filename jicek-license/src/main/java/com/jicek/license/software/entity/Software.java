package com.jicek.license.software.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 软件实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 字段安全说明：
 *  - appKey：明文存储，客户端可见（用于 SDK 请求头 X-App-Key）
 *  - signSecret：AES-256-GCM 加密存储（SDK 接口签名密钥，禁明文）
 *  - rsaPublicKey：明文存储（客户端加密卡密用）
 *  - rsaPrivateKey：AES-256-GCM 加密存储（服务端解密卡密用，禁明文）
 *
 * 关联：卡类 / 设备 / 云函数 / 卡密 均通过 softwareId 关联本表
 */
@Data
@TableName("jicek_software")
public class Software {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 软件名称（同租户下唯一） */
    private String name;

    /** 应用 Key（32 字符，明文存储，客户端可见） */
    private String appKey;

    /** 签名密钥（AES-256-GCM 加密存储，SDK HMAC-SHA256 签名用） */
    private String signSecret;

    /** RSA 公钥（Base64 X.509，明文存储，客户端加密卡密用） */
    private String rsaPublicKey;

    /** RSA 私钥（Base64 PKCS#8 + AES-256-GCM 加密存储，服务端解密卡密用） */
    private String rsaPrivateKey;

    /** 当前版本 */
    private String version;

    /** 最低支持版本 */
    private String minVersion;

    /** 心跳间隔（秒，5-300） */
    private Integer heartbeatInterval;

    /** 最大并发会话数 */
    private Integer maxConcurrent;

    /** 0禁用 1启用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
