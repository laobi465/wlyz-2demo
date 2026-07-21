package com.jicek.license.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全说明（铁律 04/06）：
 * 1. deviceFingerprint 为 5 维 SHA-256 融合哈希（CPU+主板+硬盘+网卡+BIOS）
 * 2. deviceInfo 为设备详情 JSON，AES-256-GCM 加密存储（含原始 5 维哈希，用于审计）
 * 3. bindCode 为换机码，16 位随机字符，绑定时生成，解绑时校验
 *
 * status：0 正常 1 封禁
 * onlineStatus：0 离线 1 在线
 */
@Data
@TableName("jicek_device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long softwareId;

    /** 绑定用户 ID（未绑定则 null） */
    private Long userId;

    /** 设备指纹（5 维 SHA-256 融合，64 字符） */
    private String deviceFingerprint;

    /** 设备详情 JSON（AES-256-GCM 加密） */
    private String deviceInfo;

    /** 设备名称（客户端自报） */
    private String deviceName;

    /** 操作系统类型：windows/linux/macos/android/ios */
    private String osType;

    /** 操作系统版本 */
    private String osVersion;

    /** 客户端版本 */
    private String clientVersion;

    /** 是否虚拟机：0 否 1 是 */
    private Integer isVm;

    /** VM/容器补充维度（VM UUID / 容器 ID） */
    private String vmExtra;

    /** 首次绑定 IP */
    private String bindIp;

    /** 换机码（16 位随机字符） */
    private String bindCode;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 0 离线 1 在线 */
    private Integer onlineStatus;

    /** 0 正常 1 封禁 */
    private Integer status;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
