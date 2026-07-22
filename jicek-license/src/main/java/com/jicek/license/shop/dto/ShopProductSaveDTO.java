package com.jicek.license.shop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺商品新建/编辑 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * tenantId 从 AuthContext 获取，前端禁传。
 */
@Data
public class ShopProductSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    /** 所属店铺 ID */
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    /** 关联卡类 ID */
    @NotNull(message = "卡类ID不能为空")
    private Long cardTypeId;

    /** 店铺售价（覆盖卡类原售价） */
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须 ≥ 0.01")
    private BigDecimal price;

    /** 排序值，越大越靠前（可选） */
    private Integer sortOrder;

    /** 状态：0下架 1上架（可选，新建时默认 1） */
    private Integer status;
}
