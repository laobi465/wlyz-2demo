package com.jicek.license.config;

import com.jicek.license.auth.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（注册 JWT 鉴权拦截器）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 拦截规则：
 *  - 拦截 /api/dev/** 和 /api/admin/** 所有开发者/管理员接口
 *  - 放行 /api/auth/**（登录接口）、/api/sdk/**（SDK 签名鉴权）、/api/h5/**（H5 接口）、
 *    /api/pay/notify/**（支付回调）、/api/deploy/webhook（GitHub Webhook）、/actuator/**
 *  - 实际是否鉴权由 @AuthRequired 注解决定（未标注则放行，兼容现有接口过渡期）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/dev/**", "/api/admin/**")
                .excludePathPatterns(
                        "/api/auth/**",          // 登录接口
                        "/api/sdk/**",           // SDK 签名鉴权
                        "/api/h5/**",            // H5 终端用户接口
                        "/api/pay/notify/**",    // 支付异步回调
                        "/api/deploy/webhook",   // GitHub Webhook（独立 HMAC 验签）
                        "/actuator/**"           // 健康检查 / 监控
                );
    }
}
