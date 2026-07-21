package com.jicek.license.device.dto;

import lombok.Data;

/**
 * 设备解绑请求 DTO（换机场景）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 解绑流程：
 * 1. 用户在新设备上输入换机码 bindCode
 * 2. 服务端校验 bindCode 有效性 + 是否过期（默认 24h）
 * 3. 服务端解绑旧设备 + 绑定新设备（同事务）
 * 4. 换机码使用后立即失效，禁止重复使用
 */
@Data
public class DeviceUnbindRequestDTO {

    private Long tenantId;

    private Long softwareId;

    /** 换机码（16 位） */
    private String bindCode;

    /** 新设备指纹数据 */
    private DeviceFingerprintDTO newFingerprint;

    /** 用户 IP */
    private String clientIp;
}
