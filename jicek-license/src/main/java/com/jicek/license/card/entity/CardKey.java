package com.jicek.license.card.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卡密实体
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全说明（铁律 04/06）：
 * 1. cardCipher 字段为 AES-256-GCM 加密后的卡密，禁明文入库
 * 2. cardHash 字段为卡密 SHA-256 哈希，用于查询索引
 * 3. 卡密明文仅在生成时返回一次，前端必须保存
 * 4. 卡密传输必须 RSA-2048 加密（客户端公钥加密，服务端私钥解密）
 *
 * status：
 *  0 未使用
 *  1 已使用
 *  2 已封禁（所有 session 立即失效）
 *  3 已退款（所有 session 立即失效）
 *  4 已过期
 */
@Data
@TableName("jicek_card_key")
public class CardKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long cardTypeId;

    private Long softwareId;

    /** 卡号（前缀 + 随机字符，唯一） */
    private String cardNo;

    /** AES-256-GCM 加密后的卡密 */
    private String cardCipher;

    /** 卡密 SHA-256 哈希（查询索引用） */
    private String cardHash;

    /** 0未使用 1已使用 2已封禁 3已退款 4已过期 */
    private Integer status;

    /** 绑定的用户 ID */
    private Long boundUserId;

    /** 绑定设备 JSON */
    private String boundDevices;

    /** 到期时间 */
    private LocalDateTime expireTime;

    /** 首次使用时间 */
    private LocalDateTime firstUseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
