package com.jicek.license.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * MD5 签名服务（彩虹易支付 V1 专用）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 用途：彩虹易支付 V1 接口签名计算与验证
 * 算法：MD5（V1 协议强制要求，不可改）
 *
 * 签名规则（来自官方文档 pay.v8jisu.cn/doc/v1_legacy_api.html）：
 * 1. 待签名参数不包含 sign、sign_type、空值参数
 * 2. 参数按字典序升序排列（ASCII）
 * 3. 拼接成 key1=value1&key2=value2 格式（无 URL 编码）
 * 4. 末尾拼接商户密钥：...&key=商户密钥
 * 5. 对拼接后的字符串做 MD5，得到 32 位小写十六进制
 *
 * 注意：仅用于易支付 V1 兼容场景，其他场景一律使用 HMAC-SHA256
 */
public class Md5SignService {

    private static final String ALGORITHM = "MD5";

    /**
     * 计算彩虹易支付 V1 签名
     * @param params 参与签名的参数（含 sign 也无所谓，会自动过滤）
     * @param merchantKey 商户密钥
     * @return 32 位小写十六进制签名
     */
    public static String sign(Map<String, String> params, String merchantKey) {
        // 1. 过滤空值、sign、sign_type，并按字典序排序
        TreeMap<String, String> sortedMap = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if ("sign".equalsIgnoreCase(key) || "sign_type".equalsIgnoreCase(key)) {
                continue;
            }
            sortedMap.put(key, value);
        }

        // 2. 拼接 key1=value1&key2=value2
        String payload = sortedMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // 3. 末尾拼接商户密钥
        String signPayload = payload + "&key=" + merchantKey;

        // 4. MD5
        return md5Hex(signPayload);
    }

    /**
     * 验证彩虹易支付 V1 签名
     * @param params 全部参数（含 sign）
     * @param merchantKey 商户密钥
     * @return true 验证通过
     */
    public static boolean verify(Map<String, String> params, String merchantKey) {
        String signFromRequest = params.get("sign");
        if (signFromRequest == null || signFromRequest.isEmpty()) {
            return false;
        }
        String expectedSign = sign(params, merchantKey);
        // 常量时间比较，防时序攻击
        return MessageDigest.isEqual(
                signFromRequest.getBytes(StandardCharsets.UTF_8),
                expectedSign.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算 MD5，返回 32 位小写十六进制
     */
    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }

    /**
     * SHA-256 哈希（用于卡密哈希索引）
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }
}
