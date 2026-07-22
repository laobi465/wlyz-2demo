package com.jicek.license.auth.interceptor;

import com.jicek.license.common.constant.JicekConstants;

/**
 * 鉴权上下文（ThreadLocal 持有当前登录用户信息）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 由 JwtAuthInterceptor 在请求前置阶段填充，请求结束清理。
 * Service / Controller 通过 AuthContext.current() 获取当前用户身份，避免裸传参数。
 *
 * 铁律：必须 finally 清理 ThreadLocal，防止线程池复用导致身份串号。
 */
public class AuthContext {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser current() {
        return HOLDER.get();
    }

    public static AuthUser require() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("AuthContext 未初始化，请检查拦截器配置");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户是否为开发者
     */
    public static boolean isDev() {
        AuthUser u = HOLDER.get();
        return u != null && u.getRole() == JicekConstants.ROLE_DEV;
    }

    /**
     * 当前用户是否为管理员
     */
    public static boolean isAdmin() {
        AuthUser u = HOLDER.get();
        return u != null && u.getRole() == JicekConstants.ROLE_ADMIN;
    }

    /**
     * 获取当前租户ID（开发者有，管理员返回 null）
     */
    public static Long currentTenantId() {
        AuthUser u = HOLDER.get();
        return u != null ? u.getTenantId() : null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long currentUserId() {
        AuthUser u = HOLDER.get();
        return u != null ? u.getUserId() : null;
    }

    /**
     * 鉴权用户信息（从 JWT claims 解析）
     */
    public static class AuthUser {
        private final Long userId;
        private final int role;
        private final Long tenantId;
        private final String username;

        public AuthUser(Long userId, int role, Long tenantId, String username) {
            this.userId = userId;
            this.role = role;
            this.tenantId = tenantId;
            this.username = username;
        }

        public Long getUserId() { return userId; }
        public int getRole() { return role; }
        public Long getTenantId() { return tenantId; }
        public String getUsername() { return username; }
    }
}
