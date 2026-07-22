package com.jicek.license.auth.interceptor;

import com.jicek.license.common.constant.JicekConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 鉴权注解（标记 Controller 方法或类需要鉴权）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 用法：
 *   @AuthRequired                          // 任意已登录用户可访问
 *   @AuthRequired(role = JicekConstants.ROLE_DEV)    // 仅开发者
 *   @AuthRequired(role = JicekConstants.ROLE_ADMIN)  // 仅管理员
 *
 * 拦截器扫描 HandlerMethod 上的注解：
 *  - 未标注 → 不鉴权（兼容现有裸传参数接口，过渡期）
 *  - 标注但无 token → 抛 AUTH_TOKEN_MISSING(9001)
 *  - 标注但 token 角色不匹配 → 抛 AUTH_TOKEN_WRONG_ROLE(9003)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRequired {

    /**
     * 要求的角色，0 表示任意已登录用户
     */
    int role() default 0;
}
