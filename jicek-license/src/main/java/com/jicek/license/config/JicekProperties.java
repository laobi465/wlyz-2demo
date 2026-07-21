package com.jicek.license.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 极策k 配置属性
 * 作者: 极策k  日期: 2026-07-21
 *
 * 所有敏感字段必须通过环境变量注入，禁硬编码（铁律 04）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jicek")
public class JicekProperties {

    /** 加密配置 */
    private Crypto crypto = new Crypto();

    /** 支付配置 */
    private Pay pay = new Pay();

    /** 卡密配置 */
    private Card card = new Card();

    /** 回调配置 */
    private Notify notify = new Notify();

    @Data
    public static class Crypto {
        /** AES-256 主密钥 Base64 */
        private String aesKey;
        /** RSA 私钥 PKCS#8 Base64 */
        private String rsaPrivateKey;
        /** RSA 公钥 X.509 Base64 */
        private String rsaPublicKey;
        /** HMAC-SHA256 主密钥 Base64 */
        private String hmacKey;
    }

    @Data
    public static class Pay {
        /** 订单超时时间（分钟） */
        private int orderTimeoutMinutes = 15;
        /** 同步跳转地址 */
        private String returnUrl;
        /** 是否启用防重放 */
        private boolean enableAntiReplay = true;
        /** 时间戳容差（秒） */
        private int timestampToleranceSeconds = 300;
    }

    @Data
    public static class Card {
        /** 卡密查询频率限制（每分钟） */
        private int queryRateLimit = 10;
        /** 默认字符集 */
        private int defaultCharset = 0;
        /** 默认长度 */
        private int defaultLength = 24;
    }

    @Data
    public static class Notify {
        /** Redis 锁前缀 */
        private String lockPrefix = "jicek:pay:notify:lock:";
        /** 锁过期时间（秒） */
        private int lockTimeout = 30;
    }
}
