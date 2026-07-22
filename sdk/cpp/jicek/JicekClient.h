// 极策k C++ SDK 主类声明
// 作者: 极策k  日期: 2026-07-21
#pragma once

#include "JicekTypes.h"
#include <atomic>
#include <thread>
#include <mutex>
#include <condition_variable>

namespace jicek {

class FingerprintCollector;
class HttpImpl;

class JicekClient {
public:
    explicit JicekClient(const Config& config);
    ~JicekClient();

    void setHeartbeatCallback(const HeartbeatCallback& cb) { callback_ = cb; }

    VerifyResult verifyCard(const std::string& cardKey);
    HeartbeatResult heartbeat();

    void startHeartbeat();
    void stopHeartbeat();
    void logout();

private:
    Config config_;
    std::unique_ptr<FingerprintCollector> fpCollector_;
    std::unique_ptr<HttpImpl> http_;
    HeartbeatCallback callback_;

    std::string sessionId_;
    std::atomic<int> heartbeatInterval_{60};
    std::atomic<int> failCount_{0};

    std::thread heartbeatThread_;
    std::atomic<bool> heartbeatRunning_{false};
    std::mutex mtx_;
    std::condition_variable cv_;

    void heartbeatLoop();
    std::string doPost(const std::string& path, const std::string& jsonBody);
    std::string buildSignedHeaders(const std::string& method, const std::string& path,
                                    const std::string& body, const std::string& deviceId,
                                    /*out*/ std::vector<std::pair<std::string, std::string>>& headers);
};

} // namespace jicek
