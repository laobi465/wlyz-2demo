// 极策k C++ SDK 使用示例
// 作者: 极策k  日期: 2026-07-21
#include "jicek/JicekClient.h"

#include <iostream>
#include <thread>
#include <chrono>
#include <cstdlib>

int main() {
    const char* serverUrl = std::getenv("JICEK_SERVER_URL");
    const char* appKey = std::getenv("JICEK_APP_KEY");
    const char* signSecret = std::getenv("JICEK_SIGN_SECRET");
    const char* rsaPublicKey = std::getenv("JICEK_RSA_PUBLIC_KEY");
    const char* cardKey = std::getenv("JICEK_CARD_KEY");

    jicek::Config cfg;
    cfg.serverUrl = serverUrl ? serverUrl : "http://127.0.0.1:8080";
    cfg.appKey = appKey ? appKey : "";
    cfg.signSecret = signSecret ? signSecret : "";
    cfg.rsaPublicKey = rsaPublicKey ? rsaPublicKey : "";

    jicek::JicekClient client(cfg);
    client.setHeartbeatCallback({
        [](const jicek::HeartbeatResult& r) { std::cout << "[心跳] 成功 " << r.nextInterval << "s\n"; },
        [](const jicek::JicekException& e) { std::cerr << "[心跳] 失败 " << e.code << " " << e.msg << "\n"; },
        []() { std::cerr << "[心跳] 断开\n"; },
        []() { std::cerr << "[安全] 设备已封禁\n"; },
    });

    try {
        auto result = client.verifyCard(cardKey ? cardKey : "");
        std::cout << "验证成功\n  到期: " << result.expireTime
                  << "\n  session: " << result.sessionId << "\n";
        client.startHeartbeat();
        std::this_thread::sleep_for(std::chrono::seconds(60));
    } catch (const jicek::JicekException& e) {
        std::cerr << "验证失败: " << e.code << " " << e.msg << "\n";
    }
    client.logout();
    return 0;
}
