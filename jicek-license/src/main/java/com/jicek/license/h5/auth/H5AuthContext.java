package com.jicek.license.h5.auth;

/**
 * H5 请求上下文（ThreadLocal）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 持有当前 H5 请求的会话信息，finally 必须清理。
 */
public class H5AuthContext {
    private static final ThreadLocal<H5SessionInfo> HOLDER = new ThreadLocal<>();

    public static void set(H5SessionInfo info) { HOLDER.set(info); }
    public static H5SessionInfo get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public static Long currentTenantId() {
        H5SessionInfo info = HOLDER.get();
        return info != null ? info.getTenantId() : null;
    }
    public static Long currentSoftwareId() {
        H5SessionInfo info = HOLDER.get();
        return info != null ? info.getSoftwareId() : null;
    }
    public static Long currentCardKeyId() {
        H5SessionInfo info = HOLDER.get();
        return info != null ? info.getCardKeyId() : null;
    }

    public static class H5SessionInfo {
        private final Long tenantId;
        private final Long softwareId;
        private final Long cardKeyId;
        private final String h5Token;
        public H5SessionInfo(Long tenantId, Long softwareId, Long cardKeyId, String h5Token) {
            this.tenantId = tenantId; this.softwareId = softwareId;
            this.cardKeyId = cardKeyId; this.h5Token = h5Token;
        }
        public Long getTenantId() { return tenantId; }
        public Long getSoftwareId() { return softwareId; }
        public Long getCardKeyId() { return cardKeyId; }
        public String getH5Token() { return h5Token; }
    }
}
