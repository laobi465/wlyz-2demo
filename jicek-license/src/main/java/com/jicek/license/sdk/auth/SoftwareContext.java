package com.jicek.license.sdk.auth;

import com.jicek.license.software.entity.Software;

/**
 * SDK 请求上下文（ThreadLocal）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 持有当前 SDK 请求关联的 Software 实体（含已解密的 signSecret / rsaPrivateKey 明文）。
 * 由 SdkAuthFilter 在签名校验通过后注入，请求结束 finally 中必须清理（防线程池串号）。
 *
 * 与 AuthContext 区别：
 *  - AuthContext 持有后台用户（开发者/管理员）身份，用于 /api/dev/** /api/admin/**
 *  - SoftwareContext 持有 SDK 请求关联的软件，用于 /api/sdk/**
 */
public class SoftwareContext {

    private static final ThreadLocal<SoftwareContext> CONTEXT = new ThreadLocal<>();

    private final Software software;
    /** 已解密的签名密钥明文（用于后续业务层验签场景） */
    private final String signSecretPlain;
    /** 客户端 IP（从请求头穿透获取） */
    private final String clientIp;

    private SoftwareContext(Software software, String signSecretPlain, String clientIp) {
        this.software = software;
        this.signSecretPlain = signSecretPlain;
        this.clientIp = clientIp;
    }

    public static void set(Software software, String signSecretPlain, String clientIp) {
        CONTEXT.set(new SoftwareContext(software, signSecretPlain, clientIp));
    }

    public static SoftwareContext current() {
        return CONTEXT.get();
    }

    public static Software requireSoftware() {
        SoftwareContext ctx = CONTEXT.get();
        if (ctx == null || ctx.software == null) {
            throw new IllegalStateException("SoftwareContext 未初始化，请检查 SdkAuthFilter 是否已拦截该路径");
        }
        return ctx.software;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public Software getSoftware() {
        return software;
    }

    public String getSignSecretPlain() {
        return signSecretPlain;
    }

    public String getClientIp() {
        return clientIp;
    }
}
