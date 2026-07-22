package com.jicek.license.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺商品详情 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 含冗余字段：cardTypeName（卡类名称）/ cardType（卡类中文名）/ type（卡类类型）/ duration / count /
 * features / expireHint（到期说明）。
 */
@Data
public class ShopProductDTO {

    private Long id;
    private Long shopId;
    private Long cardTypeId;
    private BigDecimal price;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 冗余字段：卡类名称 */
    private String cardTypeName;

    /** 冗余字段：卡类中文名（时长卡/次数卡/功能卡/永久卡） */
    private String cardType;

    /** 冗余字段：卡类类型 1时长卡 2次数卡 3功能卡 4永久卡 */
    private Integer type;

    /** 冗余字段：时长（秒），时长卡用 */
    private Integer duration;

    /** 冗余字段：次数，次数卡用 */
    private Integer count;

    /** 冗余字段：功能列表 JSON，功能卡用 */
    private String features;

    /** 冗余字段：到期说明（如 "30 天" / "永久" / "100 次"） */
    private String expireHint;
}
