package com.jicek.sdk;

import com.jicek.sdk.crypto.CryptoUtil;
import com.jicek.sdk.fingerprint.FingerprintCollector;
import com.jicek.sdk.fingerprint.FingerprintResult;
import com.jicek.sdk.http.HttpUtil;
import com.jicek.sdk.model.BindResult;
import com.jicek.sdk.model.HeartbeatResult;
import com.jicek.sdk.model.VerifyResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 极策k 客户端主类
 * 作者: 极策k  日期: 2026-07-21
 *
 * 三件套：
 * 1. 卡密验证（verifyCard）
 * 2. 心跳保活（heartbeat / startHeartbeat）
 * 3. 设备绑定/换机（bindDevice / unbindDevice）
 *
 * 使用示例：
 *   JicekClient client = JicekClient.builder()...build();
 *   VerifyResult r = client.verifyCard("JC-XXXX");
 *   client.startHeartbeat();
 */
public class JicekClient implements AutoCloseable {

    private final JicekConfig config;
    private final HttpUtil httpUtil;
    private final FingerprintCollector fingerprintCollector;

    /** 当前 session（验证成功后填充） */
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    /** 当前心跳间隔（秒） */
    private final AtomicReference<Integer> heartbeatInterval = new AtomicReference<>(60);
    /** 心跳调度器 */
    private ScheduledExecutorService heartbeatScheduler;
    /** 心跳是否运行 */
    private final AtomicBoolean heartbeatRunning = new AtomicBoolean(false);
    /** 失败重试计数 */
    private final AtomicReference<Integer> failCount = new AtomicReference<>(0);

    /** 回调接口 */
    public interface HeartbeatCallback {
        void onSuccess(HeartbeatResult result);
        void onFailure(JicekException e);
        void onDisconnect();        // 连续 5 次失败
        void onDeviceBanned();      // 设备封禁
    }

    private HeartbeatCallback heartbeatCallback;

    private JicekClient(JicekConfig config) {
        this.config = config;
        this.httpUtil = new HttpUtil(config.getConnectTimeoutMs());
        this.fingerprintCollector = new FingerprintCollector();
    }

    public static JicekConfig.Builder builder() {
        return new JicekClientBuilder();
    }

    /** 卡密验证 */
    public VerifyResult verifyCard(String cardKey) {
        FingerprintResult fp = fingerprintCollector.collect(config.getRsaPublicKey());
        String cardCipher = CryptoUtil.rsaEncrypt(cardKey, config.getRsaPublicKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fingerprint", fp.getFingerprint());
        body.put("encryptedDetail", fp.getEncryptedDetail());
        body.put("cardCipher", cardCipher);
        body.put("deviceName", fp.getDeviceName());
        body.put("osType", fp.getOsType());
        body.put("osVersion", fp.getOsVersion());
        body.put("clientVersion", fp.getClientVersion());
        body.put("isVm", fp.getIsVm());
        body.put("vmExtra", fp.getVmExtra());

        String jsonBody = JsonUtil.toJson(body);
        String resp = doPost("/api/sdk/card/verify", jsonBody);
        Map<String, Object> data = JsonUtil.parseResponse(resp);

        VerifyResult result = new VerifyResult();
        result.setValid(true);
        result.setSessionId((String) data.get("sessionId"));
        result.setExpireTime((String) data.get("expireTime"));
        Object remain = data.get("remainCount");
        if (remain instanceof Number) {
            result.setRemainCount(((Number) remain).intValue());
        }
        result.setMsg((String) data.get("msg"));
        sessionId.set(result.getSessionId());
        return result;
    }

    /** 单次心跳 */
    public HeartbeatResult heartbeat() {
        FingerprintResult fp = fingerprintCollector.collect(config.getRsaPublicKey());
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", 0);
        body.put("softwareId", 0);
        body.put("fingerprint", fp.getFingerprint());
        body.put("timestamp", timestamp);
        body.put("nonce", nonce);

        String jsonBody = JsonUtil.toJson(body);
        Map<String, String> headers = buildSignedHeaders("POST", "/api/sdk/device/heartbeat",
                jsonBody, fp.getFingerprint());
        headers.put("X-Sign-Secret", config.getSignSecret());
        headers.put("X-Heartbeat-Interval", String.valueOf(heartbeatInterval.get()));

        String resp = httpUtil.request(config.getServerUrl() + "/api/sdk/device/heartbeat",
                "POST", jsonBody, headers);
        Map<String, Object> data = JsonUtil.parseResponse(resp);

        HeartbeatResult result = new HeartbeatResult();
        Object next = data.get("nextInterval");
        if (next instanceof Number) {
            result.setNextInterval(((Number) next).intValue());
        } else {
            result.setNextInterval(60);
        }
        Object st = data.get("serverTime");
        if (st instanceof Number) {
            result.setServerTime(((Number) st).longValue());
        }
        return result;
    }

    /** 启动守护线程自动心跳 */
    public void startHeartbeat() {
        if (!heartbeatRunning.compareAndSet(false, true)) {
            return;
        }
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jicek-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduleNext(heartbeatInterval.get());
    }

    /** 停止心跳 */
    public void stopHeartbeat() {
        if (heartbeatRunning.compareAndSet(true, false) && heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
    }

    /** 退出登录 */
    public void logout() {
        if (sessionId.get() != null) {
            try {
                doPost("/api/sdk/auth/logout",
                        "{\"sessionId\":\"" + sessionId.get() + "\"}");
            } catch (Exception ignored) {
                // 退出失败不抛异常
            }
        }
        stopHeartbeat();
        sessionId.set(null);
    }

    @Override
    public void close() {
        stopHeartbeat();
    }

    public void setHeartbeatCallback(HeartbeatCallback cb) {
        this.heartbeatCallback = cb;
    }

    /* ============ 内部方法 ============ */

    private void scheduleNext(int delaySec) {
        if (!heartbeatRunning.get()) return;
        heartbeatScheduler.schedule(() -> {
            try {
                HeartbeatResult r = heartbeat();
                failCount.set(0);
                heartbeatInterval.set(r.getNextInterval());
                if (heartbeatCallback != null) {
                    heartbeatCallback.onSuccess(r);
                }
                scheduleNext(r.getNextInterval());
            } catch (JicekException e) {
                int fails = failCount.updateAndGet(v -> v + 1);
                if (e.getCode() == 3002 && heartbeatCallback != null) {
                    heartbeatCallback.onDeviceBanned();
                    stopHeartbeat();
                    return;
                }
                if (heartbeatCallback != null) {
                    heartbeatCallback.onFailure(e);
                }
                if (fails >= 5) {
                    if (heartbeatCallback != null) {
                        heartbeatCallback.onDisconnect();
                    }
                    stopHeartbeat();
                    return;
                }
                // 指数退避：1, 2, 4, 8, 最大 30
                int backoff = Math.min((int) Math.pow(2, fails), 30);
                scheduleNext(backoff);
            }
        }, delaySec, TimeUnit.SECONDS);
    }

    private String doPost(String path, String jsonBody) {
        FingerprintResult fp = fingerprintCollector.collect(config.getRsaPublicKey());
        Map<String, String> headers = buildSignedHeaders("POST", path, jsonBody, fp.getFingerprint());
        return httpUtil.request(config.getServerUrl() + path, "POST", jsonBody, headers);
    }

    private Map<String, String> buildSignedHeaders(String method, String path,
                                                    String body, String deviceId) {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodySha256 = body == null || body.isEmpty() ? "" : CryptoUtil.sha256Hex(body);

        String signPayload = CryptoUtil.buildSignPayload(method, path,
                String.valueOf(timestamp), nonce, bodySha256);
        String signature = CryptoUtil.hmacSign(signPayload, config.getSignSecret());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-App-Key", config.getAppKey());
        headers.put("X-Timestamp", String.valueOf(timestamp));
        headers.put("X-Nonce", nonce);
        headers.put("X-Signature", signature);
        headers.put("X-Device-Id", deviceId);
        return headers;
    }

    /**
     * Builder 桥接（让 JicekClient.builder() 返回的 Builder 同时能 build 出 JicekClient）
     */
    private static class JicekClientBuilder extends JicekConfig.Builder {
        public JicekClient build() {
            JicekConfig cfg = super.build();
            return new JicekClient(cfg);
        }
    }
}
