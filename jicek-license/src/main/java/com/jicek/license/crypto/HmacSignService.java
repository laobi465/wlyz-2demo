package com.jicek.license.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC-SHA256 签名服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 用途：SDK 接口签名验证、防篡改、防重放
 * 算法：HMAC-SHA256
 * 密钥长度：256 位
 *
 * 签名规范（SDK 接口）：
 *   签名原文 = METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256
 *   签名值 = Base64(HMAC-SHA256(密钥, 签名原文))
 *
 * 安全说明：
 * 1. 使用常量时间比较，防时序攻击
 * 2. Nonce 5 分钟内不可重复（Redis 缓存）
 * 3. 时间戳 ±300s 内有效
 */
public class HmacSignService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec keySpec;

    /**
     * 构造方法
     * @param hmacKeyBase64 Base64 编码的 256 位密钥
     */
    public HmacSignService(String hmacKeyBase64) {
        if (hmacKeyBase64 == null || hmacKeyBase64.isBlank()) {
            throw new IllegalArgumentException("HMAC 主密钥未配置，请检查环境变量 JICEK_HMAC_KEY");
        }
        byte[] keyBytes = Base64.getDecoder().decode(hmacKeyBase64);
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 计算签名
     * @param data 待签名数据
     * @return Base64 签名值
     */
    public String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 签名计算失败", e);
        }
    }

    /**
     * 使用指定密钥计算签名（用于多租户/多软件场景，每软件独立签名密钥）
     * @param data 待签名数据
     * @param secretKey 签名密钥（明文，已从加密存储中解密）
     * @return Base64 签名值
     */
    public String signWithSecret(String data, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("签名密钥不能为空");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(spec);
            byte[] signBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 签名计算失败", e);
        }
    }

    /**
     * 验证签名（常量时间比较，防时序攻击）
     * @param data 原始数据
     * @param signatureBase64 待验证的 Base64 签名
     * @return true 验证通过
     */
    public boolean verify(String data, String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        String expected = sign(data);
        return constantTimeEquals(expected, signatureBase64);
    }

    /**
     * 使用指定密钥验证签名（多租户/多软件场景）
     * @param data 原始数据
     * @param signatureBase64 待验证的 Base64 签名
     * @param secretKey 签名密钥（明文）
     * @return true 验证通过
     */
    public boolean verify(String data, String signatureBase64, String secretKey) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        String expected = signWithSecret(data, secretKey);
        return constantTimeEquals(expected, signatureBase64);
    }

    /**
     * 构造 SDK 接口签名原文
     * @param method HTTP 方法 (GET/POST/...)
     * @param path 请求路径（含 query string）
     * @param timestamp 13 位时间戳
     * @param nonce UUID
     * @param bodySha256 请求体 SHA-256（无 body 时为空字符串）
     * @return 签名原文
     */
    public static String buildSignPayload(String method, String path,
                                          String timestamp, String nonce, String bodySha256) {
        return method + "\n"
                + path + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + (bodySha256 == null ? "" : bodySha256);
    }

    /**
     * 常量时间字符串比较，防时序攻击
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(aBytes, bBytes);
    }
}
