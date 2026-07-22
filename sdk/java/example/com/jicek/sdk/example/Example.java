package com.jicek.sdk.example;

import com.jicek.sdk.JicekClient;
import com.jicek.sdk.JicekException;
import com.jicek.sdk.model.HeartbeatResult;
import com.jicek.sdk.model.VerifyResult;

/**
 * Java SDK 使用示例
 * 作者: 极策k  日期: 2026-07-21
 */
public class Example {

    public static void main(String[] args) {
        // 配置（从环境变量或配置文件读取，禁硬编码）
        String serverUrl = System.getenv().getOrDefault("JICEK_SERVER_URL", "http://127.0.0.1:8080");
        String appKey = System.getenv("JICEK_APP_KEY");
        String signSecret = System.getenv("JICEK_SIGN_SECRET");
        String rsaPublicKey = System.getenv("JICEK_RSA_PUBLIC_KEY");
        String cardKey = System.getenv("JICEK_CARD_KEY");

        try (JicekClient client = JicekClient.builder()
                .serverUrl(serverUrl)
                .appKey(appKey)
                .signSecret(signSecret)
                .rsaPublicKey(rsaPublicKey)
                .build()) {

            // 设置心跳回调
            client.setHeartbeatCallback(new JicekClient.HeartbeatCallback() {
                @Override public void onSuccess(HeartbeatResult r) {
                    System.out.println("[心跳] 成功，下次间隔: " + r.getNextInterval() + "s");
                }
                @Override public void onFailure(JicekException e) {
                    System.err.println("[心跳] 失败: " + e.getCode() + " " + e.getMessage());
                }
                @Override public void onDisconnect() {
                    System.err.println("[心跳] 断开，请重新验证");
                }
                @Override public void onDeviceBanned() {
                    System.err.println("[安全] 设备已封禁");
                }
            });

            // 卡密验证
            VerifyResult result = client.verifyCard(cardKey);
            System.out.println("验证成功，到期时间: " + result.getExpireTime());
            System.out.println("剩余次数: " + result.getRemainCount());

            // 启动心跳
            client.startHeartbeat();

            // 业务运行...
            Thread.sleep(60_000);

            // 退出
            client.logout();
        } catch (JicekException e) {
            System.err.println("验证失败: " + e.getCode() + " " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
