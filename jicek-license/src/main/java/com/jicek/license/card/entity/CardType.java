package com.jicek.license.card.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卡类实体
 * 作者: 极策k  日期: 2026-07-21
 *
 * type 字段：
 *  1 时长卡（duration 字段为秒数）
 *  2 次数卡（count 字段为次数）
 *  3 功能卡（features JSON 数组）
 *  4 永久卡（无到期时间）
 *
 * bindStrategy 字段：
 *  0 不绑定
 *  1 首次登录绑定
 *  2 指定 N 台（maxDevices 字段）
 */
@Data
@TableName("jicek_card_type")
public class CardType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 所属软件 */
    private Long softwareId;

    /** 卡类名称 */
    private String name;

    /** 1时长卡 2次数卡 3功能卡 4永久卡 */
    private Integer type;

    /** 时长（秒），时长卡用 */
    private Integer duration;

    /** 次数，次数卡用 */
    private Integer count;

    /** 功能列表 JSON，功能卡用 */
    private String features;

    /** 价格 */
    private BigDecimal price;

    /** 绑定策略：0不绑定 1首次登录 2指定N台 */
    private Integer bindStrategy;

    /** 最大设备数 */
    private Integer maxDevices;

    /** 是否启用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
