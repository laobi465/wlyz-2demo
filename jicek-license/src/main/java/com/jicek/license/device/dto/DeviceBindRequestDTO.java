package com.jicek.license.device.dto;

import lombok.Data;

/**
 * 设备绑定请求 DTO
 * 作者: 极策k  日期: 2026-07-21
 *
 * 流程：
 * 1. 终端用户使用卡密登录时触发绑定
 * 2. 服务端校验卡密有效性 + 卡类绑定策略
 * 3. 服务端校验设备指纹是否已被封禁
 * 4. 服务端校验该用户已绑定设备数是否超限（cardType.maxDevices）
 * 5. 绑定成功生成换机码（bindCode），返回给客户端
 */
@Data
public class DeviceBindRequestDTO {

    private Long tenantId;

    private Long softwareId;

    /** 卡密明文（客户端 RSA 加密传输，服务端解密） */
    private String encryptedCardKey;

    /** 设备指纹数据 */
    private DeviceFingerprintDTO fingerprint;

    /** 用户 IP（服务端从 request 获取，禁信任客户端） */
    private String clientIp;
}
