package com.jicek.license.pay.adapter;

import com.jicek.license.pay.dto.PayNotifyDTO;
import com.jicek.license.pay.dto.PayRequestDTO;
import com.jicek.license.pay.dto.PayResponseDTO;
import com.jicek.license.pay.entity.PayConfig;

import java.util.Map;

/**
 * 支付适配器接口
 * 作者: 极策k  日期: 2026-07-21
 *
 * 策略模式：未来扩展 V2 时新增 PayAdapterV2Impl 即可
 */
public interface PayAdapter {

    /**
     * 发起支付（API 模式，返回二维码/跳转 URL）
     */
    PayResponseDTO createPay(PayRequestDTO request, PayConfig config);

    /**
     * 验证异步回调签名
     * @param params 回调参数（含 sign）
     * @param config 支付配置
     * @return true 验签通过
     */
    boolean verifyNotifySign(Map<String, String> params, PayConfig config);

    /**
     * 解析回调数据为 DTO
     */
    PayNotifyDTO parseNotify(Map<String, String> params);

    /**
     * 查询订单状态（调用易支付 api.php?act=query）
     */
    String queryOrder(String outTradeNo, PayConfig config);

    /**
     * 发起退款（调用易支付 api.php?act=refund）
     */
    boolean refund(String outTradeNo, String reason, PayConfig config);

    /**
     * 适配器版本标识
     */
    String version();
}
