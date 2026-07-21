package com.jicek.license.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 用途：卡密入库加密、商户密钥加密存储
 * 模式：GCM（提供机密性 + 完整性 + 真实性）
 * 密钥长度：256 位
 * IV 长度：12 字节（GCM 推荐值）
 * 认证标签长度：128 位
 *
 * 安全说明：
 * 1. 每次加密使用随机 IV，IV 不复用（铁律）
 * 2. IV 与密文拼接后 Base64 输出，便于存储
 * 3. 主密钥从环境变量注入，禁硬编码（铁律 04）
 */
public class AesCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom;

    /**
     * 构造方法
     * @param aesKeyBase64 Base64 编码的 256 位密钥
     */
    public AesCryptoService(String aesKeyBase64) {
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
            throw new IllegalArgumentException("AES 主密钥未配置，请检查环境变量 JICEK_AES_KEY");
        }
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES 密钥长度必须为 32 字节（256 位），当前: " + keyBytes.length);
        }
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * 加密
     * @param plaintext 明文
     * @return Base64(IV + 密文+TAG)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] output = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, output, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, output, IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密
     * @param ciphertextBase64 Base64(IV + 密文+TAG)
     * @return 明文
     */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) {
            return null;
        }
        try {
            byte[] input = Base64.getDecoder().decode(ciphertextBase64);
            if (input.length < IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足，IV 损坏");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[input.length - IV_LENGTH];
            System.arraycopy(input, 0, iv, 0, IV_LENGTH);
            System.arraycopy(input, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }
}
