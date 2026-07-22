package com.jicek.license.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内嵌卡网 - 店铺商品实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 业务规则：
 *  - 同店铺下 card_type_id 唯一（uk_shop_card）
 *  - price 可覆盖卡类原售价
 *  - status：0下架 1上架（下架后 H5 店铺不展示该商品）
 *  - sort_order 越大越靠前
 */
@Data
@TableName("jicek_shop_product")
public class ShopProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 所属店铺 */
    private Long shopId;

    /** 关联卡类 */
    private Long cardTypeId;

    /** 店铺售价（覆盖卡类售价） */
    private BigDecimal price;

    /** 排序值，越大越靠前 */
    private Integer sortOrder;

    /** 0下架 1上架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
