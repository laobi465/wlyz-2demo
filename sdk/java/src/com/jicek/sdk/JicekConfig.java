package com.jicek.sdk;

/**
 * 极策k SDK 配置
 * 作者: 极策k  日期: 2026-07-21
 */
public class JicekConfig {

    private final String serverUrl;
    private final String appKey;
    private final String signSecret;
    private final String rsaPublicKey;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;

    private JicekConfig(Builder b) {
        this.serverUrl = b.serverUrl;
        this.appKey = b.appKey;
        this.signSecret = b.signSecret;
        this.rsaPublicKey = b.rsaPublicKey;
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.requestTimeoutMs = b.requestTimeoutMs;
    }

    public String getServerUrl() { return serverUrl; }
    public String getAppKey() { return appKey; }
    public String getSignSecret() { return signSecret; }
    public String getRsaPublicKey() { return rsaPublicKey; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serverUrl;
        private String appKey;
        private String signSecret;
        private String rsaPublicKey;
        private int connectTimeoutMs = 5000;
        private int requestTimeoutMs = 10000;

        public Builder serverUrl(String v) { this.serverUrl = v; return this; }
        public Builder appKey(String v) { this.appKey = v; return this; }
        public Builder signSecret(String v) { this.signSecret = v; return this; }
        public Builder rsaPublicKey(String v) { this.rsaPublicKey = v; return this; }
        public Builder connectTimeoutMs(int v) { this.connectTimeoutMs = v; return this; }
        public Builder requestTimeoutMs(int v) { this.requestTimeoutMs = v; return this; }

        public JicekConfig build() {
            if (serverUrl == null || serverUrl.isBlank()) {
                throw new IllegalArgumentException("serverUrl 必填");
            }
            if (appKey == null || appKey.isBlank()) {
                throw new IllegalArgumentException("appKey 必填");
            }
            if (signSecret == null || signSecret.isBlank()) {
                throw new IllegalArgumentException("signSecret 必填");
            }
            if (rsaPublicKey == null || rsaPublicKey.isBlank()) {
                throw new IllegalArgumentException("rsaPublicKey 必填");
            }
            return new JicekConfig(this);
        }
    }
}
