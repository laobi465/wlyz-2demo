// 极策k网络验证 C++ SDK 异常 + 类型定义
// 作者: 极策k  日期: 2026-07-21
#pragma once

#include <string>
#include <vector>
#include <functional>
#include <atomic>
#include <memory>

namespace jicek {

class JicekException : public std::runtime_error {
public:
    int code;
    std::string msg;
    JicekException(int c, const std::string& m)
        : std::runtime_error("[" + std::to_string(c) + "] " + m), code(c), msg(m) {}
};

struct Config {
    std::string serverUrl;
    std::string appKey;
    std::string signSecret;
    std::string rsaPublicKey;  // Base64 SPKI
    int timeoutMs = 10000;
};

struct VerifyResult {
    bool valid = false;
    std::string sessionId;
    std::string expireTime;
    int remainCount = 0;
    bool hasRemainCount = false;
    std::vector<std::string> features;
    std::string msg;
};

struct HeartbeatResult {
    int nextInterval = 60;
    long long serverTime = 0;
};

struct FingerprintResult {
    std::string fingerprint;
    std::string encryptedDetail;
    int isVm = 0;
    std::string vmExtra;
    std::string osType;
    std::string osVersion;
    std::string deviceName;
    std::string clientVersion;
};

struct HeartbeatCallback {
    std::function<void(const HeartbeatResult&)> onSuccess;
    std::function<void(const JicekException&)> onFailure;
    std::function<void()> onDisconnect;
    std::function<void()> onDeviceBanned;
};

} // namespace jicek
