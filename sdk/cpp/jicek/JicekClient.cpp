// 极策k C++ SDK 实现
// 作者: 极策k  日期: 2026-07-21
//
// 依赖：OpenSSL（HMAC/SHA256/RSA-OAEP）+ libcurl（HTTP）
// 编译：g++ -std=c++17 ... -lcrypto -lcurl -lpthread

#include "JicekClient.h"

#include <openssl/hmac.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include <openssl/evp.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/pem.h>
#include <openssl/err.h>

#include <curl/curl.h>

#include <cstring>
#include <sstream>
#include <chrono>
#include <random>
#include <cstdio>
#include <array>
#include <memory>
#include <cstdlib>
#include <fstream>
#include <cstdlib>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#include <sys/utsname.h>
#endif

namespace jicek {

namespace {

// ---------- Base64 ----------
std::string base64Encode(const unsigned char* data, size_t len) {
    BIO* b64 = BIO_new(BIO_f_base64());
    BIO* mem = BIO_new(BIO_s_mem());
    b64 = BIO_push(b64, mem);
    BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    BIO_write(b64, data, (int)len);
    BIO_flush(b64);
    BUF_MEM* bptr = nullptr;
    BIO_get_mem_ptr(b64, &bptr);
    std::string out(bptr->data, bptr->length);
    BIO_free_all(b64);
    return out;
}

std::string base64Decode(const std::string& in) {
    BIO* b64 = BIO_new(BIO_f_base64());
    BIO* mem = BIO_new_mem_buf(in.data(), (int)in.size());
    b64 = BIO_push(b64, mem);
    BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    std::string out(in.size(), '\0');
    int n = BIO_read(b64, &out[0], (int)in.size());
    if (n > 0) out.resize(n); else out.clear();
    BIO_free_all(b64);
    return out;
}

// ---------- Hex ----------
std::string toHex(const unsigned char* data, size_t len) {
    static const char* kHex = "0123456789abcdef";
    std::string out(len * 2, '0');
    for (size_t i = 0; i < len; ++i) {
        out[2 * i] = kHex[(data[i] >> 4) & 0xF];
        out[2 * i + 1] = kHex[data[i] & 0xF];
    }
    return out;
}

// ---------- SHA256 ----------
std::string sha256Hex(const std::string& input) {
    unsigned char md[SHA256_DIGEST_LENGTH];
    SHA256(reinterpret_cast<const unsigned char*>(input.data()), input.size(), md);
    return toHex(md, SHA256_DIGEST_LENGTH);
}

// ---------- HMAC-SHA256 → Base64 ----------
std::string hmacSha256Base64(const std::string& data, const std::string& secret) {
    unsigned char md[EVP_MAX_MD_SIZE];
    unsigned int mdLen = 0;
    HMAC(EVP_sha256(),
         secret.data(), (int)secret.size(),
         reinterpret_cast<const unsigned char*>(data.data()), data.size(),
         md, &mdLen);
    return base64Encode(md, mdLen);
}

// ---------- RSA-OAEP 加密 ----------
std::string rsaEncryptOaep(const std::string& plaintext, const std::string& publicKeyB64) {
    std::string derData = base64Decode(publicKeyB64);
    const unsigned char* p = reinterpret_cast<const unsigned char*>(derData.data());
    EVP_PKEY* evpKey = d2i_PUBKEY(nullptr, &p, (long)derData.size());
    if (!evpKey) throw JicekException(500, "RSA 公钥解析失败");

    EVP_PKEY_CTX* ctx = EVP_PKEY_CTX_new(evpKey, nullptr);
    if (!ctx) { EVP_PKEY_free(evpKey); throw JicekException(500, "RSA CTX 创建失败"); }

    std::string result;
    try {
        if (EVP_PKEY_encrypt_init(ctx) <= 0) throw JicekException(500, "RSA encrypt_init 失败");
        if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING) <= 0)
            throw JicekException(500, "RSA 设置 OAEP 失败");
        if (EVP_PKEY_CTX_set_rsa_oaep_md(ctx, EVP_sha256()) <= 0)
            throw JicekException(500, "RSA 设置 OAEP hash 失败");
        if (EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, EVP_sha256()) <= 0)
            throw JicekException(500, "RSA 设置 MGF1 失败");

        size_t outLen = 0;
        if (EVP_PKEY_encrypt(ctx, nullptr, &outLen,
                              reinterpret_cast<const unsigned char*>(plaintext.data()),
                              plaintext.size()) <= 0)
            throw JicekException(500, "RSA 加密探测失败");

        std::vector<unsigned char> out(outLen);
        if (EVP_PKEY_encrypt(ctx, out.data(), &outLen,
                              reinterpret_cast<const unsigned char*>(plaintext.data()),
                              plaintext.size()) <= 0)
            throw JicekException(500, "RSA 加密失败");

        result = base64Encode(out.data(), outLen);
    } catch (...) {
        EVP_PKEY_CTX_free(ctx);
        EVP_PKEY_free(evpKey);
        throw;
    }
    EVP_PKEY_CTX_free(ctx);
    EVP_PKEY_free(evpKey);
    return result;
}

// ---------- UUID ----------
std::string genUuid() {
    std::random_device rd;
    std::mt19937_64 gen(rd());
    std::stringstream ss;
    ss << std::hex << gen() << gen();
    return ss.str();
}

// ---------- 简易 JSON ----------
std::string jsonEscape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if ((unsigned char)c < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out.push_back(c);
                }
        }
    }
    return out;
}

// 极简 JSON 值提取（仅支持顶层字段，嵌套对象返回原始字符串）
std::string jsonExtract(const std::string& json, const std::string& key) {
    std::string pat = "\"" + key + "\"";
    size_t pos = json.find(pat);
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos + pat.size());
    if (pos == std::string::npos) return "";
    pos++;
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) pos++;
    if (pos >= json.size()) return "";
    if (json[pos] == '"') {
        size_t end = pos + 1;
        while (end < json.size() && json[end] != '"') {
            if (json[end] == '\\' && end + 1 < json.size()) end += 2;
            else end++;
        }
        return json.substr(pos + 1, end - pos - 1);
    }
    // 数字
    size_t end = pos;
    while (end < json.size() && (std::isdigit((unsigned char)json[end]) || json[end] == '-' || json[end] == '.')) end++;
    return json.substr(pos, end - pos);
}

long long jsonExtractLong(const std::string& json, const std::string& key) {
    std::string v = jsonExtract(json, key);
    if (v.empty()) return 0;
    try { return std::stoll(v); } catch (...) { return 0; }
}

// ---------- Shell 执行 ----------
std::string execCmd(const std::string& cmd) {
#ifdef _WIN32
    std::string full = "cmd /c " + cmd;
#else
    std::string full = "/bin/sh -c \"" + cmd + "\"";
#endif
    std::array<char, 256> buf;
    std::string result;
    FILE* pipe =
#ifdef _WIN32
        _popen(full.c_str(), "r");
#else
        popen(full.c_str(), "r");
#endif
    if (!pipe) return "";
    while (fgets(buf.data(), (int)buf.size(), pipe)) result += buf.data();
#ifdef _WIN32
    _pclose(pipe);
#else
    pclose(pipe);
#endif
    // 去掉末尾换行
    while (!result.empty() && (result.back() == '\n' || result.back() == '\r')) result.pop_back();
    return result;
}

} // anonymous namespace

// ==================== 设备指纹采集 ====================

class FingerprintCollector {
public:
    FingerprintResult collect(const std::string& rsaPublicKey) {
        std::string cpu = collectCpu();
        std::string mb = collectMainboard();
        std::string disk = collectDisk();
        std::string mac = collectMac();
        std::string bios = collectBios();

        std::string hCpu = sha256Hex(cpu);
        std::string hMb = sha256Hex(mb);
        std::string hDisk = sha256Hex(disk);
        std::string hMac = sha256Hex(mac);
        std::string hBios = sha256Hex(bios);

        std::string vmExtra = detectVmExtra();
        bool isVm = !vmExtra.empty();

        std::string fpInput = hCpu + hMb + hDisk + hMac + hBios;
        if (isVm) fpInput += vmExtra;
        std::string fingerprint = sha256Hex(fpInput);

        std::string detailJson =
            "{\"cpu\":\"" + hCpu +
            "\",\"mainboard\":\"" + hMb +
            "\",\"disk\":\"" + hDisk +
            "\",\"mac\":\"" + hMac +
            "\",\"bios\":\"" + hBios + "\"}";

        std::string encryptedDetail = rsaEncryptOaep(detailJson, rsaPublicKey);

        FingerprintResult r;
        r.fingerprint = fingerprint;
        r.encryptedDetail = encryptedDetail;
        r.isVm = isVm ? 1 : 0;
        r.vmExtra = vmExtra;
        r.osType = osType();
        r.osVersion = osVersion();
        r.deviceName = hostname();
        r.clientVersion = "jicek-sdk-cpp-0.3.1";
        return r;
    }

private:
    static std::string collectCpu() {
#ifdef _WIN32
        return execCmd("wmic cpu get ProcessorId /value");
#else
        return execCmd("cat /proc/cpuinfo 2>/dev/null | grep -i serial | head -1");
#endif
    }
    static std::string collectMainboard() {
#ifdef _WIN32
        return execCmd("wmic baseboard get SerialNumber /value");
#else
        return execCmd("cat /sys/class/dmi/id/board_serial 2>/dev/null");
#endif
    }
    static std::string collectDisk() {
#ifdef _WIN32
        return execCmd("wmic diskdrive get SerialNumber /value");
#else
        return execCmd("cat /sys/block/sda/device/serial 2>/dev/null");
#endif
    }
    static std::string collectMac() {
#ifdef _WIN32
        return execCmd("getmac /fo csv /nh | head -1");
#else
        return execCmd("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address 2>/dev/null");
#endif
    }
    static std::string collectBios() {
#ifdef _WIN32
        return execCmd("wmic bios get SerialNumber /value");
#else
        return execCmd("cat /sys/class/dmi/id/product_uuid 2>/dev/null");
#endif
    }
    static std::string detectVmExtra() {
#ifdef _WIN32
        std::string model = execCmd("wmic computersystem get Model /value");
        if (model.find("Virtual") != std::string::npos || model.find("VMware") != std::string::npos) {
            std::string uuid = execCmd("wmic csproduct get UUID /value");
            return "vm:" + uuid;
        }
        return "";
#else
        std::string cgroup = execCmd("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1");
        if (cgroup.find("docker") != std::string::npos) {
            size_t pos = cgroup.find("docker/");
            if (pos != std::string::npos) return "container:" + cgroup.substr(pos + 7);
        }
        std::string vmUuid = execCmd("dmidecode -s system-uuid 2>/dev/null");
        if (!vmUuid.empty() && vmUuid != "Not Settable" && vmUuid != "Not Specified") {
            return "vm:" + vmUuid;
        }
        return "";
#endif
    }
    static std::string osType() {
#ifdef _WIN32
        return "windows";
#elif defined(__APPLE__)
        return "macos";
#else
        return "linux";
#endif
    }
    static std::string osVersion() {
#ifndef _WIN32
        utsname u;
        if (uname(&u) == 0) return std::string(u.release);
        return "";
#else
        return execCmd("ver");
#endif
    }
    static std::string hostname() {
#ifdef _WIN32
        char buf[256] = {0};
        DWORD sz = sizeof(buf);
        if (GetComputerNameA(buf, &sz)) return buf;
        return "unknown";
#else
        char buf[256] = {0};
        if (gethostname(buf, sizeof(buf)) == 0) return buf;
        return "unknown";
#endif
    }
};

// ==================== HTTP 实现（libcurl） ====================

class HttpImpl {
public:
    HttpImpl() { curl_global_init(CURL_GLOBAL_DEFAULT); }
    ~HttpImpl() { curl_global_cleanup(); }

    std::string post(const std::string& url, const std::string& body,
                     const std::vector<std::pair<std::string, std::string>>& headers,
                     int timeoutMs) {
        CURL* curl = curl_easy_init();
        if (!curl) throw JicekException(500, "curl 初始化失败");

        struct curl_slist* hdrList = nullptr;
        for (const auto& kv : headers) {
            std::string h = kv.first + ": " + kv.second;
            hdrList = curl_slist_append(hdrList, h.c_str());
        }

        std::string resp;
        try {
            curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
            curl_easy_setopt(curl, CURLOPT_POST, 1L);
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str());
            curl_easy_setopt(curl, CURLOPT_HTTPHEADER, hdrList);
            curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, timeoutMs);
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &writeCb);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &resp);

            CURLcode rc = curl_easy_perform(curl);
            curl_slist_free_all(hdrList);
            if (rc != CURLE_OK) {
                curl_easy_cleanup(curl);
                throw JicekException(500, std::string("网络异常: ") + curl_easy_strerror(rc));
            }
            long code = 0;
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);
            curl_easy_cleanup(curl);
            if (code != 200) throw JicekException((int)code, "HTTP 请求失败: " + resp);
        } catch (...) {
            curl_easy_cleanup(curl);
            throw;
        }
        return resp;
    }

private:
    static size_t writeCb(char* ptr, size_t size, size_t nmemb, void* userdata) {
        size_t total = size * nmemb;
        static_cast<std::string*>(userdata)->append(ptr, total);
        return total;
    }
};

// ==================== JicekClient 实现 ====================

JicekClient::JicekClient(const Config& config)
    : config_(config),
      fpCollector_(new FingerprintCollector()),
      http_(new HttpImpl()) {
    if (config_.serverUrl.empty()) throw std::invalid_argument("serverUrl 必填");
    if (config_.appKey.empty()) throw std::invalid_argument("appKey 必填");
    if (config_.signSecret.empty()) throw std::invalid_argument("signSecret 必填");
    if (config_.rsaPublicKey.empty()) throw std::invalid_argument("rsaPublicKey 必填");
}

JicekClient::~JicekClient() {
    stopHeartbeat();
}

VerifyResult JicekClient::verifyCard(const std::string& cardKey) {
    FingerprintResult fp = fpCollector_->collect(config_.rsaPublicKey);
    std::string cardCipher = rsaEncryptOaep(cardKey, config_.rsaPublicKey);

    std::ostringstream body;
    body << "{"
         << "\"fingerprint\":\"" << jsonEscape(fp.fingerprint) << "\","
         << "\"encryptedDetail\":\"" << jsonEscape(fp.encryptedDetail) << "\","
         << "\"cardCipher\":\"" << jsonEscape(cardCipher) << "\","
         << "\"deviceName\":\"" << jsonEscape(fp.deviceName) << "\","
         << "\"osType\":\"" << jsonEscape(fp.osType) << "\","
         << "\"osVersion\":\"" << jsonEscape(fp.osVersion) << "\","
         << "\"clientVersion\":\"" << jsonEscape(fp.clientVersion) << "\","
         << "\"isVm\":" << fp.isVm << ","
         << "\"vmExtra\":\"" << jsonEscape(fp.vmExtra) << "\""
         << "}";

    std::string resp = doPost("/api/sdk/card/verify", body.str());

    VerifyResult r;
    r.valid = true;
    r.sessionId = jsonExtract(resp, "sessionId");
    r.expireTime = jsonExtract(resp, "expireTime");
    sessionId_ = r.sessionId;
    return r;
}

HeartbeatResult JicekClient::heartbeat() {
    FingerprintResult fp = fpCollector_->collect(config_.rsaPublicKey);
    long long timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    std::string nonce = genUuid();

    std::ostringstream body;
    body << "{"
         << "\"tenantId\":0,"
         << "\"softwareId\":0,"
         << "\"fingerprint\":\"" << jsonEscape(fp.fingerprint) << "\","
         << "\"timestamp\":" << timestamp << ","
         << "\"nonce\":\"" << nonce << "\""
         << "}";
    std::string jsonBody = body.str();

    std::vector<std::pair<std::string, std::string>> headers;
    buildSignedHeaders("POST", "/api/sdk/device/heartbeat", jsonBody, fp.fingerprint, headers);
    headers.push_back({"X-Sign-Secret", config_.signSecret});
    headers.push_back({"X-Heartbeat-Interval", std::to_string(heartbeatInterval_.load())});

    std::string url = config_.serverUrl + "/api/sdk/device/heartbeat";
    std::string resp = http_->post(url, jsonBody, headers, config_.timeoutMs);

    int code = (int)jsonExtractLong(resp, "code");
    if (code != 200) {
        throw JicekException(code, jsonExtract(resp, "msg"));
    }

    HeartbeatResult r;
    r.nextInterval = (int)jsonExtractLong(resp, "nextInterval");
    if (r.nextInterval <= 0) r.nextInterval = 60;
    r.serverTime = jsonExtractLong(resp, "serverTime");
    heartbeatInterval_ = r.nextInterval;
    return r;
}

void JicekClient::startHeartbeat() {
    bool expected = false;
    if (!heartbeatRunning_.compare_exchange_strong(expected, true)) return;
    heartbeatThread_ = std::thread(&JicekClient::heartbeatLoop, this);
}

void JicekClient::stopHeartbeat() {
    if (!heartbeatRunning_.exchange(false)) return;
    cv_.notify_all();
    if (heartbeatThread_.joinable()) heartbeatThread_.join();
}

void JicekClient::logout() {
    if (!sessionId_.empty()) {
        try {
            std::string body = "{\"sessionId\":\"" + jsonEscape(sessionId_) + "\"}";
            doPost("/api/sdk/auth/logout", body);
        } catch (...) { /* 忽略 */ }
    }
    stopHeartbeat();
    sessionId_.clear();
}

void JicekClient::heartbeatLoop() {
    while (heartbeatRunning_.load()) {
        try {
            HeartbeatResult r = heartbeat();
            failCount_ = 0;
            if (callback_.onSuccess) callback_.onSuccess(r);
        } catch (const JicekException& e) {
            int fails = ++failCount_;
            if (e.code == 3002) {
                if (callback_.onDeviceBanned) callback_.onDeviceBanned();
                heartbeatRunning_ = false;
                return;
            }
            if (callback_.onFailure) callback_.onFailure(e);
            if (fails >= 5) {
                if (callback_.onDisconnect) callback_.onDisconnect();
                heartbeatRunning_ = false;
                return;
            }
            int backoff = 1 << fails;
            if (backoff > 30) backoff = 30;
            std::unique_lock<std::mutex> lk(mtx_);
            cv_.wait_for(lk, std::chrono::seconds(backoff),
                         [this] { return !heartbeatRunning_.load(); });
            continue;
        }
        int interval = heartbeatInterval_.load();
        std::unique_lock<std::mutex> lk(mtx_);
        cv_.wait_for(lk, std::chrono::seconds(interval),
                     [this] { return !heartbeatRunning_.load(); });
    }
}

std::string JicekClient::doPost(const std::string& path, const std::string& jsonBody) {
    FingerprintResult fp = fpCollector_->collect(config_.rsaPublicKey);
    std::vector<std::pair<std::string, std::string>> headers;
    buildSignedHeaders("POST", path, jsonBody, fp.fingerprint, headers);
    std::string url = config_.serverUrl + path;
    std::string resp = http_->post(url, jsonBody, headers, config_.timeoutMs);
    int code = (int)jsonExtractLong(resp, "code");
    if (code != 200) throw JicekException(code, jsonExtract(resp, "msg"));
    return resp;
}

std::string JicekClient::buildSignedHeaders(const std::string& method, const std::string& path,
                                              const std::string& body, const std::string& deviceId,
                                              std::vector<std::pair<std::string, std::string>>& headers) {
    long long timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    std::string nonce = genUuid();
    std::string bodySha = body.empty() ? "" : sha256Hex(body);
    std::string payload = method + "\n" + path + "\n" +
                          std::to_string(timestamp) + "\n" + nonce + "\n" + bodySha;
    std::string signature = hmacSha256Base64(payload, config_.signSecret);

    headers.push_back({"X-App-Key", config_.appKey});
    headers.push_back({"X-Timestamp", std::to_string(timestamp)});
    headers.push_back({"X-Nonce", nonce});
    headers.push_back({"X-Signature", signature});
    headers.push_back({"X-Device-Id", deviceId});
    headers.push_back({"Content-Type", "application/json; charset=UTF-8"});
    return signature;
}

} // namespace jicek
