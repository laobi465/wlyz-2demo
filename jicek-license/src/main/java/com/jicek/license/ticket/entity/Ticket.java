package com.jicek.license.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单主表实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 双向工单：
 *  - 终端用户→开发者：creator_type=1, target=1
 *  - 开发者→管理员：creator_type=2, target=2
 *
 * 状态机（受控流转）：
 *  0待处理 → 1处理中 → 2已回复 → 3已关闭
 *  任意状态可 → 3已关闭
 *
 * category：1换机申请 2充值问题 3卡密问题 4其他
 */
@Data
@TableName("jicek_ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 工单号（TK+时间戳+随机） */
    private String ticketNo;

    private String title;

    private String content;

    /** 1换机申请 2充值问题 3卡密问题 4其他 */
    private Integer category;

    /** 1开发者 2管理员 */
    private Integer target;

    /** 0待处理 1处理中 2已回复 3已关闭 */
    private Integer status;

    /** 1终端用户 2开发者 */
    private Integer creatorType;

    private Long creatorId;

    private String creatorName;

    private Long softwareId;

    private Long deviceId;

    private Long handlerId;

    private LocalDateTime handlerTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
