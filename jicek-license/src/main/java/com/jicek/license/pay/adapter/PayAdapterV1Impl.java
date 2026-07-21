package com.jicek.license.pay.adapter;

import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.crypto.Md5SignService;
import com.jicek.license.pay.dto.PayNotifyDTO;
import com.jicek.license.pay.dto.PayRequestDTO;
import com.jicek.license.pay.dto.PayResponseDTO;
import com.jicek.license.pay.entity.PayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 彩虹易支付 V1 适配器实现
 * 作者: 极策k  日期: 2026-07-21
 *
 * 接口规范来源：彩虹易支付 V1 旧版接口官方文档
 *   文档地址：pay.v8jisu.cn/doc/v1_legacy_api.html
 *   请求格式：application/x-www-form-urlencoded
 *   返回格式：JSON
 *   签名算法：MD5
 *   异步通知：GET 方式，需返回字符串 "success"
 *
 * 关键端点：
 *   /submit.php  页面跳转支付（form 表单跳转）
 *   /mapi.php    API 接口支付（返回二维码/跳转 URL）
 *   /api.php?act=query    查询订单
 *   /api.php?act=refund   发起退款
 */
@Slf4j
@Component
public class PayAdapterV1Impl implements PayAdapter {

    private static final String SIGN_TYPE = "MD5";

    private final AesCryptoService aesCryptoService;
    private final RestTemplate restTemplate;

    public PayAdapterV1Impl(AesCryptoService aesCryptoService) {
        this.aesCryptoService = aesCryptoService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public PayResponseDTO createPay(PayRequestDTO request, PayConfig config) {
        // 1. 校验支付通道
        validateChannel(request.getPayType(), config);

        // 2. 解密商户密钥
        String merchantKey = decryptMerchantKey(config);

        // 3. 构造请求参数
        Map<String, String> params = buildPayParams(request, config);

        // 4. 计算签名
        String sign = Md5SignService.sign(params, merchantKey);
        params.put("sign", sign);
        params.put("sign_type", SIGN_TYPE);

        // 5. 调用 /mapi.php（API 接口模式）
        String url = config.getGatewayUrl() + JicekConstants.EPAY_V1_MAPI_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            log.error("调用易支付 mapi.php 失败: url={}", url, e);
            throw new ServiceException(ResultCode.FAIL, "调用支付网关失败: " + e.getMessage());
        }

        // 6. 解析响应（JSON 格式）
        return parsePayResponse(resp.getBody(), request);
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params, PayConfig config) {
        String merchantKey = decryptMerchantKey(config);
        return Md5SignService.verify(params, merchantKey);
    }

    @Override
    public PayNotifyDTO parseNotify(Map<String, String> params) {
        return PayNotifyDTO.fromMap(params);
    }

    @Override
    public String queryOrder(String outTradeNo, PayConfig config) {
        String merchantKey = decryptMerchantKey(config);

        Map<String, String> params = new TreeMap<>();
        params.put("act", JicekConstants.EPAY_V1_ACT_QUERY);
        params.put("pid", String.valueOf(config.getPid()));
        params.put("out_trade_no", outTradeNo);
        String sign = Md5SignService.sign(params, merchantKey);
        params.put("sign", sign);
        params.put("sign_type", SIGN_TYPE);

        String url = config.getGatewayUrl() + JicekConstants.EPAY_V1_API_PATH;
        String query = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        try {
            return restTemplate.getForObject(url + "?" + query, String.class);
        } catch (Exception e) {
            log.error("查询订单失败: outTradeNo={}", outTradeNo, e);
            throw new ServiceException(ResultCode.PAY_ORDER_NOT_FOUND);
        }
    }

    @Override
    public boolean refund(String outTradeNo, String reason, PayConfig config) {
        String merchantKey = decryptMerchantKey(config);

        Map<String, String> params = new TreeMap<>();
        params.put("act", JicekConstants.EPAY_V1_ACT_REFUND);
        params.put("pid", String.valueOf(config.getPid()));
        params.put("out_trade_no", outTradeNo);
        if (reason != null && !reason.isEmpty()) {
            params.put("reason", reason);
        }
        String sign = Md5SignService.sign(params, merchantKey);
        params.put("sign", sign);
        params.put("sign_type", SIGN_TYPE);

        String url = config.getGatewayUrl() + JicekConstants.EPAY_V1_API_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(form, headers), String.class);
            // 简化判断：返回内容包含 code=1 视为成功
            String body = resp.getBody();
            return body != null && body.contains("\"code\":1");
        } catch (Exception e) {
            log.error("退款失败: outTradeNo={}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public String version() {
        return "V1";
    }

    /* ============ 私有方法 ============ */

    private void validateChannel(String channel, PayConfig config) {
        if (channel == null || channel.isEmpty()) {
            throw new ServiceException(ResultCode.PAY_CHANNEL_DISABLED, "支付通道为空");
        }
        if (config.getEnabledChannels() == null
                || !config.getEnabledChannels().contains(channel)) {
            throw new ServiceException(ResultCode.PAY_CHANNEL_DISABLED, "通道未启用: " + channel);
        }
    }

    private String decryptMerchantKey(PayConfig config) {
        try {
            return aesCryptoService.decrypt(config.getMerchantKey());
        } catch (Exception e) {
            log.error("商户密钥解密失败: tenantId={}", config.getTenantId(), e);
            throw new ServiceException(ResultCode.PAY_MERCHANT_KEY_ERROR);
        }
    }

    private Map<String, String> buildPayParams(PayRequestDTO request, PayConfig config) {
        Map<String, String> params = new HashMap<>();
        params.put("pid", String.valueOf(config.getPid()));
        params.put("type", request.getPayType());
        params.put("out_trade_no", generateOutTradeNo(request));
        params.put("notify_url", config.getNotifyUrl());
        params.put("return_url", config.getReturnUrl());
        params.put("name", "极策k卡密-" + request.getCardTypeId());
        params.put("money", request.getAmount().toPlainString());
        params.put("device", JicekConstants.DEVICE_MOBILE.equalsIgnoreCase(request.getDevice())
                ? JicekConstants.DEVICE_MOBILE : JicekConstants.DEVICE_PC);
        if (request.getParam() != null && !request.getParam().isEmpty()) {
            params.put("param", request.getParam());
        }
        return params;
    }

    private String generateOutTradeNo(PayRequestDTO request) {
        // 极策k 订单号格式：JC + yyyyMMddHHmmss + 6位随机
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        return "JC" + timestamp + random;
    }

    private PayResponseDTO parsePayResponse(String body, PayRequestDTO request) {
        if (body == null || body.isEmpty()) {
            throw new ServiceException(ResultCode.FAIL, "支付网关返回空");
        }
        // V1 mapi.php 返回 JSON: {"code":1,"msg":"...","qrcode":"...","img":"..."}
        // 此处用简单字符串解析，避免引入 Jackson 复杂依赖
        PayResponseDTO resp = new PayResponseDTO();
        resp.setOutTradeNo(request.getOutTradeNo() != null ? request.getOutTradeNo() : null);
        resp.setAmount(request.getAmount() != null ? request.getAmount().toPlainString() : null);
        resp.setPayType(request.getPayType());

        if (body.contains("\"code\":1") || body.contains("\"code\": 1")) {
            resp.setMode("qrcode");
            // 提取 qrcode 字段值
            String qrcode = extractJsonField(body, "qrcode");
            resp.setQrcodeUrl(qrcode);
        } else {
            String msg = extractJsonField(body, "msg");
            log.error("易支付创建订单失败: body={}", body);
            throw new ServiceException(ResultCode.FAIL, "创建支付订单失败: " + msg);
        }
        return resp;
    }

    private String extractJsonField(String json, String field) {
        // 简单 JSON 字段提取，避免引入复杂依赖
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) {
            key = "\"" + field + "\" :";  // 容忍空格
            idx = json.indexOf(key);
        }
        if (idx < 0) {
            return null;
        }
        int start = idx + key.length();
        // 跳过空白
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }
}
