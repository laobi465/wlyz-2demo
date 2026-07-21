package com.jicek.license.crypto;

import com.jicek.license.config.JicekProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 加密服务 Bean 配置
 * 作者: 极策k  日期: 2026-07-21
 *
 * 集中管理加密相关 Bean，确保单例 + 启动时校验密钥
 */
@Configuration
public class CryptoConfiguration {

    @Bean
    public AesCryptoService aesCryptoService(JicekProperties properties) {
        return new AesCryptoService(properties.getCrypto().getAesKey());
    }

    @Bean
    public RsaCryptoService rsaCryptoService(JicekProperties properties) {
        return new RsaCryptoService(
                properties.getCrypto().getRsaPrivateKey(),
                properties.getCrypto().getRsaPublicKey());
    }

    @Bean
    public HmacSignService hmacSignService(JicekProperties properties) {
        return new HmacSignService(properties.getCrypto().getHmacKey());
    }
}
