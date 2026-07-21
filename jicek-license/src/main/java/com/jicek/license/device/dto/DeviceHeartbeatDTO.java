package com.jicek.license.device.dto;

import lombok.Data;

/**
 * 设备心跳 DTO
 * 作者: 极策k  日期: 2026-07-21
 *
 * 心跳规则（铁律 06）：
 * 1. 心跳间隔由服务端控制（5-300s），禁客户端固定值
 * 2. 服务端根据软件配置 software.heartbeatInterval 下发
 * 3. 心跳超时 = 3 * interval，超时设备置为离线
 * 4. 心跳需携带签名（HMAC-SHA256），防伪造
 */
@Data
public class DeviceHeartbeatDTO {

    private Long tenantId;

    private Long softwareId;

    /** 设备指纹 */
    private String fingerprint;

    /** 客户端时间戳（毫秒） */
    private Long timestamp;

    /** HMAC-SHA256 签名（fingerprint + timestamp + 软件签名密钥） */
    private String sign;

    /** 随机数（防重放，5 分钟内不可重复） */
    private String nonce;
}
