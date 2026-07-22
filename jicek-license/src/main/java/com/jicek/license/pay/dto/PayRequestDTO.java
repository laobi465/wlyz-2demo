package com.jicek.license.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起支付请求 DTO
 * 作者: 极策k  日期: 2026-07-21
 */
@Data
public class PayRequestDTO {

    /** 租户ID（从登录态获取，前端禁传） */
    @NotNull
    private Long tenantId;

    /** 卡类ID */
    @NotNull
    private Long cardTypeId;

    /** 购买数量 */
    @Min(1)
    private Integer quantity = 1;

    /** 支付通道：alipay/wxpay/qqpay/unionpay（用户无权选，由配置自动选） */
    private String payType;

    /** 用户 IP（由后端注入） */
    private String userIp;

    /** 设备类型：pc/mobile */
    private String device;

    /** 业务扩展参数（原样回传） */
    private String param;

    /** 订单金额（后端计算后回填，便于支付适配器读取） */
    private BigDecimal amount;

    /** 商户订单号（后端生成后回填，便于支付适配器读取） */
    private String outTradeNo;
}
