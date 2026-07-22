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

    /** 部署配置 */
    private Deploy deploy = new Deploy();

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

    @Data
    public static class Deploy {
        /** GitHub Webhook Secret（环境变量注入，禁硬编码，铁律 04） */
        private String webhookSecret;
        /** 项目根目录（git 仓库根，用于 git pull / mvn build） */
        private String projectRoot;
        /** 重启模式：docker / btpanel / none */
        private String restartMode = "none";
        /** Docker 容器名（restartMode=docker 时用） */
        private String dockerContainer;
        /** 宝塔 API URL（restartMode=btpanel 时用） */
        private String btpanelApiUrl;
        /** 宝塔 API Key（restartMode=btpanel 时用） */
        private String btpanelApiKey;
        /** 健康检查基础 URL（如 http://127.0.0.1:8080） */
        private String healthCheckBaseUrl;
        /** 是否启用部署功能（默认 false，需显式开启） */
        private boolean enabled = false;
    }
}
