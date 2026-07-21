package com.jicek.license.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA-2048 加密服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 用途：卡密传输加密（客户端公钥加密，服务端私钥解密）
 * 算法：RSA/ECB/OAEPWithSHA-256AndMGF1Padding（推荐，禁用 PKCS1）
 * 密钥长度：2048 位
 *
 * 安全说明：
 * 1. 使用 OAEP 填充模式，禁用 PKCS1Padding（已被证明不安全）
 * 2. 服务端持有私钥（环境变量注入），客户端持公钥
 * 3. 私钥禁入 git，禁入库（铁律 04）
 */
public class RsaCryptoService {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String ALGORITHM = "RSA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    /**
     * 构造方法
     * @param privateKeyBase64 PKCS#8 Base64 私钥（服务端解密用）
     * @param publicKeyBase64  X.509 Base64 公钥（可选，仅用于服务端验证签名场景）
     */
    public RsaCryptoService(String privateKeyBase64, String publicKeyBase64) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
                this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            } else {
                this.privateKey = null;
            }

            if (publicKeyBase64 != null && !publicKeyBase64.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
                this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            } else {
                this.publicKey = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("RSA 密钥初始化失败", e);
        }
    }

    /**
     * 服务端解密（用私钥）
     * @param cipherBase64 Base64 密文
     * @return 明文
     */
    public String decryptByPrivateKey(String cipherBase64) {
        if (privateKey == null) {
            throw new IllegalStateException("RSA 私钥未配置，无法解密");
        }
        if (cipherBase64 == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(cipherBase64));
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA 解密失败", e);
        }
    }

    /**
     * 服务端加密（用公钥，少用，仅用于向客户端下发敏感数据场景）
     * @param plaintext 明文
     * @return Base64 密文
     */
    public String encryptByPublicKey(String plaintext) {
        if (publicKey == null) {
            throw new IllegalStateException("RSA 公钥未配置，无法加密");
        }
        if (plaintext == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }
}
