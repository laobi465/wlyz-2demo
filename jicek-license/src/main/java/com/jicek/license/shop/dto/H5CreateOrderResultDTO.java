package com.jicek.license.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * H5 下单结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * payUrl 为占位字段：当前 v0.13.0 仅创建本地订单（status=0 待支付），
 * 后续接入支付网关（彩虹易支付 V1）时填入实际支付链接，现置空字符串。
 */
@Data
public class H5CreateOrderResultDTO {

    /** 商户订单号 */
    private String outTradeNo;

    /** 总金额 */
    private BigDecimal amount;

    /** 支付通道 */
    private String payType;

    /** 支付链接（占位，后续接入支付网关时填入） */
    private String payUrl;

    /** 冗余字段：店铺名称 */
    private String shopName;

    /** 冗余字段：卡类名称 */
    private String cardTypeName;

    /** 购买数量 */
    private Integer quantity;
}
