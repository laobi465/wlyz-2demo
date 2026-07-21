package com.jicek.license.pay.dto;

import lombok.Data;

/**
 * 发起支付响应 DTO
 * 作者: 极策k  日期: 2026-07-21
 */
@Data
public class PayResponseDTO {

    /** 商户订单号 */
    private String outTradeNo;

    /** 跳转 URL（页面跳转模式） */
    private String redirectUrl;

    /** 二维码 URL（API 模式，可前端生成二维码） */
    private String qrcodeUrl;

    /** 金额 */
    private String amount;

    /** 支付通道 */
    private String payType;

    /** 模式：redirect=页面跳转, qrcode=API二维码 */
    private String mode;
}
