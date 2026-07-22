package com.jicek.license.sdk.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SDK 卡密登录结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 返回卡密状态 + 卡类信息 + 软件配置，客户端 SDK 据此判断是否允许使用软件。
 * 不返回任何密钥（signSecret / rsaPrivateKey 永不下发到客户端）。
 */
@Data
public class SdkLoginResultDTO {

    /** 卡密 ID */
    private Long cardKeyId;

    /** 卡号（脱敏：前 4 + **** + 后 4） */
    private String cardNoMasked;

    /** 卡密状态：0未使用 1已使用 2已封禁 3已退款 4已过期（登录成功后应为 1） */
    private Integer cardStatus;

    /** 卡类 ID */
    private Long cardTypeId;

    /** 卡类名称 */
    private String cardTypeName;

    /** 卡类类型：1时长卡 2次数卡 3功能卡 4永久卡 */
    private Integer cardType;

    /** 到期时间（永久卡为 null） */
    private LocalDateTime expireTime;

    /** 剩余次数（次数卡用，其他类型为 null） */
    private Integer remainingCount;

    /** 功能列表（功能卡用，其他类型为 null） */
    private List<String> features;

    /* ============ 软件配置下发给客户端 ============ */

    /** 心跳间隔（秒，客户端据此定时发送心跳） */
    private Integer heartbeatInterval;

    /** 最大并发会话数 */
    private Integer maxConcurrent;

    /** 软件当前版本 */
    private String softwareVersion;

    /** 软件最低支持版本（客户端版本低于此值应拒绝登录） */
    private String softwareMinVersion;

    /** 服务器当前时间（用于客户端校时） */
    private LocalDateTime serverTime;
}
