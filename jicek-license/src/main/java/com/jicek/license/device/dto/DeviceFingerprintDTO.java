package com.jicek.license.device.dto;

import lombok.Data;

/**
 * 设备指纹上报 DTO（客户端采集后 RSA 加密上报）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全流程（铁律 06）：
 * 1. 客户端采集 5 维原始数据（CPU/主板/硬盘/网卡/BIOS 序列号）
 * 2. 客户端对每维单独 SHA-256 哈希
 * 3. 客户端拼接 5 维哈希后再 SHA-256 = 最终指纹
 * 4. 客户端用 RSA 公钥加密 5 维原始哈希 JSON = encryptedDetail
 * 5. 客户端发送 {fingerprint, encryptedDetail} 到服务端
 * 6. 服务端 RSA 私钥解密 encryptedDetail，独立计算最终指纹
 * 7. 服务端比对客户端 fingerprint 与本地计算结果（防篡改）
 *
 * VM/容器场景（isVm=1）：
 * - 客户端额外采集 VM UUID / 容器 ID 作为补充维度
 * - vmExtra 字段不为空时，最终指纹 = SHA-256(5维哈希 + vmExtra)
 */
@Data
public class DeviceFingerprintDTO {

    /** 客户端计算的最终指纹（SHA-256，64 字符） */
    private String fingerprint;

    /** RSA-2048 加密后的 5 维哈希 JSON（Base64） */
    private String encryptedDetail;

    /** 客户端采集的元信息（明文，用于展示） */
    private String deviceName;

    /** windows/linux/macos/android/ios */
    private String osType;

    /** 操作系统版本 */
    private String osVersion;

    /** 客户端版本 */
    private String clientVersion;

    /** 是否虚拟机/容器：0 否 1 是 */
    private Integer isVm;

    /** VM/容器补充维度（VM UUID / 容器 ID），非 VM 时为空 */
    private String vmExtra;
}
