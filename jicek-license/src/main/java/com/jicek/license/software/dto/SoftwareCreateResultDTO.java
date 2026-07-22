package com.jicek.license.software.dto;

import lombok.Data;

/**
 * 软件创建结果 DTO（仅创建/轮换时返回，含敏感明文）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全铁律：
 *  - signSecret 明文仅在创建/轮换时返回**一次**，后续查询接口返回脱敏值
 *  - rsaPrivateKey 明文仅在创建/轮换时返回**一次**，便于客户端备份
 *  - 前端必须提示用户「请立即保存，关闭后无法再次查看」
 */
@Data
public class SoftwareCreateResultDTO {

    private Long id;
    private Long tenantId;
    private String name;
    private String appKey;

    /** 签名密钥明文（仅此一次返回） */
    private String signSecret;

    /** RSA 公钥（明文） */
    private String rsaPublicKey;

    /** RSA 私钥明文（仅此一次返回，PKCS#8 Base64） */
    private String rsaPrivateKey;

    private String version;
    private String minVersion;
    private Integer heartbeatInterval;
    private Integer maxConcurrent;
    private Integer enabled;
}
