package com.jicek.license.config;

import com.jicek.license.auth.interceptor.JwtAuthInterceptor;
import com.jicek.license.h5.auth.H5AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（注册 JWT 鉴权拦截器 + H5 鉴权拦截器）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 拦截规则：
 *  - JwtAuthInterceptor 拦截 /api/dev/** 和 /api/admin/** 所有开发者/管理员接口
 *    放行 /api/auth/**（登录接口）、/api/sdk/**（SDK 签名鉴权）、/api/h5/**（H5 接口）、
 *    /api/pay/notify/**（支付回调）、/api/deploy/webhook（GitHub Webhook）、/actuator/**
 *    实际是否鉴权由 @AuthRequired 注解决定（未标注则放行，兼容现有接口过渡期）
 *  - H5AuthInterceptor 拦截 /api/h5/** 中除登录/代理注册/店铺公开查询外的所有接口（X-H5-Token 鉴权）
 *    放行 /api/h5/auth/login（卡密登录，公开接口）、/api/h5/agent/register（代理注册，公开接口）、
 *    /api/h5/shop/info（店铺公开查询，无需 X-H5-Token；下单 /api/h5/shop/order 仍需鉴权）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final H5AuthInterceptor h5AuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor, H5AuthInterceptor h5AuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.h5AuthInterceptor = h5AuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 鉴权拦截器（开发者/管理员后台接口）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/dev/**", "/api/admin/**")
                .excludePathPatterns(
                        "/api/auth/**",          // 登录接口
                        "/api/sdk/**",           // SDK 签名鉴权
                        "/api/h5/**",            // H5 终端用户接口（由 H5AuthInterceptor 单独鉴权）
                        "/api/pay/notify/**",    // 支付异步回调
                        "/api/deploy/webhook",   // GitHub Webhook（独立 HMAC 验签）
                        "/actuator/**"           // 健康检查 / 监控
                );

        // H5 鉴权拦截器（X-H5-Token，v0.13.0）
        // 拦截 /api/h5/** 下所有需要鉴权的接口，放行公开接口
        registry.addInterceptor(h5AuthInterceptor)
                .addPathPatterns("/api/h5/**")
                .excludePathPatterns(
                        "/api/h5/auth/login",            // 卡密登录（公开接口，无需 X-H5-Token）
                        "/api/h5/agent/register",        // 代理注册（邀请码注册，公开接口）
                        "/api/h5/shop/info"              // 店铺信息查询（公开，无需 X-H5-Token；下单 /api/h5/shop/order 仍需鉴权）
                );
    }
}
