package com.jicek.license.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付配置实体（每个租户一行）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全说明：
 * 1. merchantKey 字段加密存储（AES-256-GCM），禁明文（铁律 04）
 * 2. 加密在 Service 层完成，入库前必须为密文
 */
@Data
@TableName("jicek_pay_config")
public class PayConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 易支付网关地址（如 https://pay.example.com） */
    private String gatewayUrl;

    /** 商户 ID（彩虹易支付 pid） */
    private Long pid;

    /** 商户密钥（AES-256-GCM 加密存储） */
    private String merchantKey;

    /** 异步通知地址（系统自动生成） */
    private String notifyUrl;

    /** 同步跳转地址 */
    private String returnUrl;

    /** 启用的支付通道，逗号分隔：alipay,wxpay,qqpay,unionpay */
    private String enabledChannels;

    /** 是否启用：0禁用 1启用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
