package com.jicek.license.pay.controller;

import com.jicek.license.pay.service.PayNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付异步回调控制器
 * 作者: 极策k  日期: 2026-07-21
 *
 * 彩虹易支付 V1 异步通知规范：
 * - GET 方式回传参数（也兼容 POST form）
 * - 必须返回纯字符串 "success"（无 BOM、无空格、无 HTML）
 * - 返回非 success 会触发重试
 *
 * 路径：/pay/notify/{tenantId}
 * 注：tenantId 用于支持多租户路由，但实际验签以订单查出的 tenantId 为准
 */
@Slf4j
@RestController
@RequestMapping("/pay/notify")
public class PayNotifyController {

    private final PayNotifyService payNotifyService;

    public PayNotifyController(PayNotifyService payNotifyService) {
        this.payNotifyService = payNotifyService;
    }

    /**
     * 兼容 GET 方式回调（V1 默认方式）
     */
    @org.springframework.web.bind.annotation.GetMapping(value = "/{tenantId}")
    public ResponseEntity<String> handleGetNotify(
            @org.springframework.web.bind.annotation.PathVariable Long tenantId,
            HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("收到异步回调[GET]: tenantId={}", tenantId);
        String result = payNotifyService.handleNotify(params);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(result);
    }

    /**
     * 兼容 POST 方式回调
     */
    @PostMapping(value = "/{tenantId}")
    public ResponseEntity<String> handlePostNotify(
            @org.springframework.web.bind.annotation.PathVariable Long tenantId,
            HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("收到异步回调[POST]: tenantId={}", tenantId);
        String result = payNotifyService.handleNotify(params);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(result);
    }

    /**
     * 提取请求参数（兼容 query string 和 form data）
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
