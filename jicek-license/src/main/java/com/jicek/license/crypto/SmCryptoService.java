package com.jicek.license.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;
import com.jicek.license.common.constant.JicekConstants;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 国密加密服务（SM2 / SM3 / SM4，可选实现）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 用途：作为 AesCryptoService / RsaCryptoService 的国密可选补充（不替换现有实现）。
 *   - SM4-CBC：卡密入库对称加密可选（对标 SPEC.md 6.6 国密可选-对称 SM4 128 CBC）
 *   - SM2：卡密传输非对称加密可选（对标 SPEC.md 6.6 国密可选-非对称 SM2 256）
 *   - SM3：摘要可选，替代 SHA-256（对标 SPEC.md 6.6 国密可选-摘要 SM3 256）
 *
 * 启用条件：jicek.crypto.sm.enabled=true（@ConditionalOnProperty，默认关闭）
 * 底层依赖：Hutool（cn.hutool.crypto.SmUtil / SM4 / SM2）+ BouncyCastle Provider
 *
 * 安全说明（铁律 04/06/13）：
 * 1. 密钥全部环境变量注入（JICEK_SM4_KEY / JICEK_SM2_PRIVATE_KEY），禁硬编码
 * 2. 未配置密钥时仅 warn 不阻止启动（与 RsaCryptoService 一致），调用时抛 IllegalStateException
 * 3. SM4 每次加密使用随机 IV，IV 与密文拼接后 Base64 输出（与 AesCryptoService 风格一致）
 * 4. SM2 公钥由私钥派生（EC 点乘），无需单独配置公钥环境变量
 */
@Slf4j
public class SmCryptoService {

    private final byte[] sm4Key;
    private final SM2 sm2;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造方法
     *
     * @param sm4KeyBase64    Base64 编码的 SM4 密钥（16 字节 / 128 位），未配置时为 null
     * @param sm2PrivateKeyHex SM2 私钥 hex（32 字节 / 256 位），未配置时为 null
     */
    public SmCryptoService(String sm4KeyBase64, String sm2PrivateKeyHex) {
        // SM4 密钥（可选）
        if (sm4KeyBase64 != null && !sm4KeyBase64.isBlank()) {
            byte[] k = Base64.getDecoder().decode(sm4KeyBase64);
            if (k.length != JicekConstants.SM4_KEY_LENGTH) {
                throw new IllegalArgumentException(
                        "SM4 密钥长度必须为 " + JicekConstants.SM4_KEY_LENGTH + " 字节（128 位），当前: " + k.length);
            }
            this.sm4Key = k;
        } else {
            log.warn("SM4 密钥未配置（环境变量 JICEK_SM4_KEY），SM4 相关功能不可用");
            this.sm4Key = null;
        }

        // SM2 私钥（可选，公钥由私钥派生）
        if (sm2PrivateKeyHex != null && !sm2PrivateKeyHex.isBlank()) {
            this.sm2 = buildSm2FromPrivateKeyHex(sm2PrivateKeyHex.trim());
        } else {
            log.warn("SM2 私钥未配置（环境变量 JICEK_SM2_PRIVATE_KEY），SM2 相关功能不可用");
            this.sm2 = null;
        }
    }

    /* ============ SM4 对称加密（CBC） ============ */

    /**
     * SM4-CBC 加密
     *
     * @param plaintext 明文
     * @return Base64(IV + 密文)
     */
    public String sm4Encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (sm4Key == null) {
            throw new IllegalStateException("SM4 密钥未配置，无法加密");
        }
        try {
            byte[] iv = new byte[JicekConstants.SM4_IV_LENGTH];
            secureRandom.nextBytes(iv);

            SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, sm4Key, iv);
            byte[] cipherText = sm4.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文（与 AesCryptoService 风格一致）
            byte[] output = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            throw new RuntimeException("SM4 加密失败", e);
        }
    }

    /**
     * SM4-CBC 解密
     *
     * @param ciphertextBase64 Base64(IV + 密文)
     * @return 明文
     */
    public String sm4Decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) {
            return null;
        }
        if (sm4Key == null) {
            throw new IllegalStateException("SM4 密钥未配置，无法解密");
        }
        try {
            byte[] input = Base64.getDecoder().decode(ciphertextBase64);
            if (input.length < JicekConstants.SM4_IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足，IV 损坏");
            }

            byte[] iv = new byte[JicekConstants.SM4_IV_LENGTH];
            byte[] cipherText = new byte[input.length - JicekConstants.SM4_IV_LENGTH];
            System.arraycopy(input, 0, iv, 0, iv.length);
            System.arraycopy(input, iv.length, cipherText, 0, cipherText.length);

            SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, sm4Key, iv);
            byte[] plainBytes = sm4.decrypt(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 解密失败", e);
        }
    }

    /* ============ SM2 非对称加密 ============ */

    /**
     * SM2 加密（用公钥）
     *
     * @param plaintext 明文
     * @return Base64 密文（C1C3C2）
     */
    public String sm2Encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (sm2 == null) {
            throw new IllegalStateException("SM2 私钥未配置，无法加密（公钥由私钥派生）");
        }
        try {
            return sm2.encryptBase64(plaintext, StandardCharsets.UTF_8, KeyType.PublicKey);
        } catch (Exception e) {
            throw new RuntimeException("SM2 加密失败", e);
        }
    }

    /**
     * SM2 解密（用私钥）
     *
     * @param ciphertextBase64 Base64 密文（C1C3C2）
     * @return 明文
     */
    public String sm2Decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) {
            return null;
        }
        if (sm2 == null) {
            throw new IllegalStateException("SM2 私钥未配置，无法解密");
        }
        try {
            return sm2.decryptStr(ciphertextBase64, KeyType.PrivateKey);
        } catch (Exception e) {
            throw new RuntimeException("SM2 解密失败", e);
        }
    }

    /* ============ SM3 摘要 ============ */

    /**
     * SM3 摘要（替代 SHA-256 可选）
     *
     * @param input 原文
     * @return 64 字符 hex 摘要
     */
    public String sm3Hex(String input) {
        if (input == null) {
            return null;
        }
        return SmUtil.sm3().digestHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /* ============ 私有方法 ============ */

    /**
     * 由 SM2 私钥 hex 构造 SM2（公钥由私钥经 EC 点乘派生，无需单独配置公钥）
     */
    private SM2 buildSm2FromPrivateKeyHex(String privateKeyHex) {
        byte[] dBytes = HexUtil.decodeHex(privateKeyHex);
        if (dBytes.length != JicekConstants.SM2_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM2 私钥长度必须为 " + JicekConstants.SM2_KEY_LENGTH + " 字节（256 位）hex，当前: " + dBytes.length);
        }
        BigInteger d = new BigInteger(1, dBytes);
        ECDomainParameters domain = SmUtil.SM2_DOMAIN_PARAMS;
        // 公钥 Q = d * G
        ECPoint q = domain.getG().multiply(d).normalize();
        byte[] pubBytes = q.getEncoded(false); // 未压缩格式：0x04 || X || Y
        return new SM2(dBytes, pubBytes);
    }
}
