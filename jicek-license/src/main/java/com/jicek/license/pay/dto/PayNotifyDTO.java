package com.jicek.license.pay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 彩虹易支付异步通知 DTO
 * 作者: 极策k  日期: 2026-07-21
 *
 * 来自彩虹易支付 V1 官方文档（GET 方式回传）：
 *   pid         商户ID
 *   trade_no    易支付流水号
 *   out_trade_no 商户订单号
 *   type        支付方式 (alipay/wxpay/qqpay)
 *   name        商品名称
 *   money       金额
 *   trade       交易状态 (TRADE_SUCCESS)
 *   param       业务扩展参数（原样回传）
 *   sign        签名
 *   sign_type   签名类型 (MD5)
 */
@Data
public class PayNotifyDTO {

    private String pid;
    private String tradeNo;
    private String outTradeNo;
    private String type;
    private String name;
    private BigDecimal money;
    private String trade;
    private String param;
    private String sign;
    private String signType;

    /**
     * 从请求参数 Map 构造（支持 GET 和 POST form）
     */
    public static PayNotifyDTO fromMap(Map<String, String> params) {
        PayNotifyDTO dto = new PayNotifyDTO();
        dto.setPid(params.get("pid"));
        dto.setTradeNo(params.get("trade_no"));
        dto.setOutTradeNo(params.get("out_trade_no"));
        dto.setType(params.get("type"));
        dto.setName(params.get("name"));
        String money = params.get("money");
        if (money != null && !money.isEmpty()) {
            dto.setMoney(new BigDecimal(money));
        }
        dto.setTrade(params.get("trade"));
        dto.setParam(params.get("param"));
        dto.setSign(params.get("sign"));
        dto.setSignType(params.get("sign_type"));
        return dto;
    }
}
