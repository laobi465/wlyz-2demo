package com.jicek.license.software.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 软件详情 DTO（查询返回）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明：
 *  - signSecret：脱敏展示（前 4 字符 + ****），明文仅在创建/轮换时返回一次
 *  - rsaPrivateKey：永不返回（服务端内部解密用）
 *  - rsaPublicKey：明文返回（客户端加密卡密需要）
 *  - appKey：明文返回（客户端 SDK 请求头需要）
 */
@Data
public class SoftwareDetailDTO {

    private Long id;
    private Long tenantId;
    private String name;
    private String appKey;

    /** 签名密钥（脱敏：前 4 字符 + ****） */
    private String signSecretMasked;

    /** RSA 公钥（明文，客户端加密卡密用） */
    private String rsaPublicKey;

    private String version;
    private String minVersion;
    private Integer heartbeatInterval;
    private Integer maxConcurrent;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
