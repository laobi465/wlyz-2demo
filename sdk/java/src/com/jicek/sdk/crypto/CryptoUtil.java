package com.jicek.sdk.crypto;

import com.jicek.sdk.JicekException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密辅助（HMAC-SHA256 + RSA-2048-OAEP + SHA-256）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全说明：
 * - HMAC 比对使用常量时间比较（防时序攻击）
 * - RSA 使用 OAEP 填充（禁 PKCS1）
 * - SHA-256 输出小写十六进制
 */
public class CryptoUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String RSA = "RSA";

    /** HMAC-SHA256 签名，返回 Base64 */
    public static String hmacSign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new JicekException(500, "HMAC 签名失败", e);
        }
    }

    /** SHA-256，返回 64 字符小写十六进制 */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new JicekException(500, "SHA-256 计算失败", e);
        }
    }

    /** RSA-2048-OAEP 加密（卡密传输），返回 Base64 */
    public static String rsaEncrypt(String plaintext, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance(RSA)
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Cipher cipher = Cipher.getInstance(RSA_OAEP);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new JicekException(500, "RSA 加密失败", e);
        }
    }

    /** 构造签名原文：METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256 */
    public static String buildSignPayload(String method, String path,
                                           String timestamp, String nonce, String bodySha256) {
        return method + "\n"
                + path + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + (bodySha256 == null ? "" : bodySha256);
    }

    /** 常量时间比较（防时序攻击） */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
