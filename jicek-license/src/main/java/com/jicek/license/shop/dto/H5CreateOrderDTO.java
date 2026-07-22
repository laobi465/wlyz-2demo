package com.jicek.license.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * H5 下单请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 需 X-H5-Token 鉴权（由 H5AuthInterceptor 拦截），tenantId 从 H5AuthContext 获取。
 */
@Data
public class H5CreateOrderDTO {

    /** 店铺商品 ID（jicek_shop_product.id） */
    @NotNull(message = "店铺商品ID不能为空")
    private Long shopProductId;

    /** 购买数量（1-99） */
    @Min(value = 1, message = "购买数量必须 ≥ 1")
    @Max(value = 99, message = "购买数量必须 ≤ 99")
    private Integer quantity;

    /** 支付通道：alipay|wxpay|qqpay|unionpay */
    @NotBlank(message = "支付通道不能为空")
    @Pattern(regexp = "^(alipay|wxpay|qqpay|unionpay)$", message = "支付通道仅支持 alipay/wxpay/qqpay/unionpay")
    private String payType;
}
