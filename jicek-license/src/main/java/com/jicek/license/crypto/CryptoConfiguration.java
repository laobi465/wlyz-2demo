package com.jicek.license.crypto;

import com.jicek.license.config.JicekProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 加密服务 Bean 配置
 * 作者: 极策k  日期: 2026-07-21
 *
 * 集中管理加密相关 Bean，确保单例 + 启动时校验密钥
 * 国密 SmCryptoService 为可选实现，仅当 jicek.crypto.sm.enabled=true 时注册（默认关闭，不影响现有 AES/RSA）
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

    /**
     * 国密加密服务（可选）
     * 密钥从环境变量 JICEK_SM4_KEY / JICEK_SM2_PRIVATE_KEY 经 application.yml 绑定到 JicekProperties（铁律 04）
     * 未配置密钥时仅 warn 不阻止启动；未启用时本 Bean 不注册
     */
    @Bean
    @ConditionalOnProperty(name = "jicek.crypto.sm.enabled", havingValue = "true")
    public SmCryptoService smCryptoService(JicekProperties properties) {
        JicekProperties.Sm sm = properties.getCrypto().getSm();
        return new SmCryptoService(sm.getSm4Key(), sm.getSm2PrivateKey());
    }
}
