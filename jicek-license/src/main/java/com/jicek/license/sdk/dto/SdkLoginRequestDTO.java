package com.jicek.license.sdk.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SDK 卡密登录请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户在开发者软件内输入卡密 → 客户端 SDK 用软件 RSA 公钥加密卡密
 * → 通过 X-Card-Cipher 请求头传输密文 → 服务端用软件 RSA 私钥解密
 *
 * 本 DTO 为请求体，仅含设备指纹等非敏感辅助字段。卡密密文走请求头（X-Card-Cipher），
 * 不放在 body 中（铁律：敏感凭证走头 + RSA 加密，body 仅放业务数据）。
 *
 * body 可为空（纯卡密登录无额外参数），也可含设备信息（用于首次登录自动绑定）。
 */
@Data
public class SdkLoginRequestDTO {

    /** 设备指纹（64 字符 SHA-256，可选；首次登录绑定时必填） */
    @Size(max = 128, message = "设备指纹长度非法")
    private String deviceFingerprint;

    /** 设备名称（客户端自报，可选） */
    @Size(max = 128, message = "设备名称过长")
    private String deviceName;

    /** 操作系统类型：windows/linux/macos/android/ios */
    @Size(max = 32, message = "操作系统类型过长")
    private String osType;

    /** 客户端版本 */
    @Size(max = 32, message = "客户端版本过长")
    private String clientVersion;
}
