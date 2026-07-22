/**
 * 极策k SDK 代码模板生成器（v0.12.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 一键对接开发者的软件代码：
 *  - 输入 appKey + rsaPublicKey + serverUrl + softwareName
 *  - 输出 9 种语言的「单文件快速接入」代码
 *  - signSecret 为脱敏字段，模板中留占位符由开发者手动填入
 *
 * 覆盖接口：
 *  - POST /api/sdk/card/login        卡密登录（X-Card-Cipher 头携带 RSA 加密卡密）
 *  - POST /api/sdk/device/heartbeat  心跳保活
 *  - GET  /api/sdk/announcement      公告拉取
 *  - GET  /api/sdk/update/check      更新检查
 *
 * 签名规范：
 *  - payload = METHOD\nPATH\nTIMESTAMP(13位毫秒)\nNONCE(UUID去横线)\nBODY_SHA256_HEX
 *  - signature = HMAC-SHA256(payload, signSecret) → Base64
 *  - 请求头：X-App-Key / X-Timestamp / X-Nonce / X-Signature / X-Device-Id
 *  - 卡密登录额外头：X-Card-Cipher（RSA-2048-OAEP/SHA256 加密的卡密 Base64）
 */

/** 模板生成参数 */
export interface SdkCodeTemplateParams {
  /** 软件 AppKey（明文，自动填入） */
  appKey: string
  /** RSA 公钥（明文 Base64 DER SPKI，自动填入；Lua/Shell/EPL 转 PEM） */
  rsaPublicKey: string
  /** 服务端地址，如 https://api.jicek.com */
  serverUrl: string
  /** 软件名称（用于注释展示） */
  softwareName: string
}

/** 支持的语言 */
export type SdkLanguage =
  | 'python' | 'csharp' | 'cpp' | 'go' | 'java'
  | 'nodejs' | 'lua' | 'shell' | 'epl'

export interface SdkLanguageMeta {
  key: SdkLanguage
  label: string
  fileExt: string
  desc: string
}

/** 语言列表（Tab 顺序） */
export const SDK_LANGUAGES: SdkLanguageMeta[] = [
  { key: 'python', label: 'Python', fileExt: '.py', desc: 'Python 3.9+，需 cryptography 库' },
  { key: 'csharp', label: 'C# / .NET', fileExt: '.cs', desc: '.NET 8+ / .NET Framework 4.8+' },
  { key: 'cpp', label: 'C++ (Windows)', fileExt: '.cpp', desc: '需 OpenSSL + libcurl' },
  { key: 'go', label: 'Go', fileExt: '.go', desc: 'Go 1.21+，零三方依赖' },
  { key: 'java', label: 'Java', fileExt: '.java', desc: 'JDK 17+，完整 SDK 见 /sdk/java' },
  { key: 'nodejs', label: 'Node.js', fileExt: '.js', desc: 'Node 18+，零三方依赖' },
  { key: 'lua', label: 'Lua', fileExt: '.lua', desc: 'Lua 5.1+，需 luaossl + LuaSocket' },
  { key: 'shell', label: 'Shell', fileExt: '.sh', desc: 'Bash，需 curl + openssl' },
  { key: 'epl', label: '易语言', fileExt: '.e', desc: '需精易模块 v10.0+' }
]

/** 生成指定语言的接入代码 */
export function generateSdkCode(lang: SdkLanguage, params: SdkCodeTemplateParams): string {
  const p = normalizeParams(params)
  switch (lang) {
    case 'python': return generatePython(p)
    case 'csharp': return generateCsharp(p)
    case 'cpp': return generateCpp(p)
    case 'go': return generateGo(p)
    case 'java': return generateJava(p)
    case 'nodejs': return generateNodejs(p)
    case 'lua': return generateLua(p)
    case 'shell': return generateShell(p)
    case 'epl': return generateEpl(p)
  }
}

function normalizeParams(p: SdkCodeTemplateParams): Required<SdkCodeTemplateParams> {
  return {
    appKey: p.appKey || 'YOUR_APP_KEY',
    rsaPublicKey: p.rsaPublicKey || 'YOUR_RSA_PUBLIC_KEY_BASE64',
    serverUrl: (p.serverUrl || 'https://api.jicek.com').replace(/\/+$/, ''),
    softwareName: p.softwareName || '我的软件'
  }
}

/** 生成头部注释（统一提示） */
function headerComment(p: Required<SdkCodeTemplateParams>, lang: string): string {
  const now = new Date().toISOString().slice(0, 19).replace('T', ' ')
  return [
    `极策k 网络验证 - ${p.softwareName} 接入代码（${lang}）`,
    `生成时间: ${now}`,
    `服务端: ${p.serverUrl}`,
    `AppKey: ${p.appKey}`,
    ``,
    `【重要】signSecret 为签名密钥，平台脱敏存储无法自动填入。`,
    `请从软件创建/轮换签名密钥时保存的明文中获取，替换下方 SIGN_SECRET 占位符。`,
    `RSA 公钥已自动填入（用于加密卡密传输）。`
  ].join('\n')
}

/* ============================ Python ============================ */
function generatePython(p: Required<SdkCodeTemplateParams>): string {
  return `# ${headerComment(p, 'Python').replace(/\n/g, '\n# ')}
# 依赖: pip install cryptography
# 用法: python jicek_quick_start.py

import base64
import hashlib
import hmac
import json
import time
import uuid
from urllib import request as urlreq
from urllib.error import HTTPError, URLError

# ============ 配置（自动填入，请补充 SIGN_SECRET） ============
SERVER_URL = "${p.serverUrl}"
APP_KEY = "${p.appKey}"
SIGN_SECRET = "在此填入签名密钥明文"  # 从创建/轮换时保存的密钥中获取
RSA_PUBLIC_KEY = "${p.rsaPublicKey}"  # Base64 DER SPKI
SOFTWARE_NAME = "${p.softwareName}"


# ============ 工具函数 ============
def sha256_hex(data: str) -> str:
    return hashlib.sha256(data.encode("utf-8")).hexdigest()


def hmac_sha256_b64(data: str, secret: str) -> str:
    sign = hmac.new(secret.encode("utf-8"), data.encode("utf-8"), hashlib.sha256).digest()
    return base64.b64encode(sign).decode("ascii")


def rsa_encrypt_oaep(plaintext: str, pub_key_b64: str) -> str:
    from cryptography.hazmat.primitives import serialization, hashes
    from cryptography.hazmat.primitives.asymmetric import padding
    pub = serialization.load_der_public_key(base64.b64decode(pub_key_b64))
    cipher = pub.encrypt(plaintext.encode("utf-8"),
        padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None))
    return base64.b64encode(cipher).decode("ascii")


def build_headers(method: str, path: str, body: str, device_id: str = "") -> dict:
    ts = str(int(time.time() * 1000))
    nonce = uuid.uuid4().hex
    body_sha = sha256_hex(body) if body else ""
    payload = f"{method}\\n{path}\\n{ts}\\n{nonce}\\n{body_sha}"
    return {
        "X-App-Key": APP_KEY,
        "X-Timestamp": ts,
        "X-Nonce": nonce,
        "X-Signature": hmac_sha256_b64(payload, SIGN_SECRET),
        "X-Device-Id": device_id or nonce,
        "Content-Type": "application/json; charset=UTF-8",
    }


def http_request(method: str, path: str, body: str, extra_headers: dict = None) -> dict:
    url = SERVER_URL + path
    headers = build_headers(method, path, body)
    if extra_headers:
        headers.update(extra_headers)
    req = urlreq.Request(url, data=body.encode("utf-8") if body else None, method=method, headers=headers)
    try:
        with urlreq.urlopen(req, timeout=10) as resp:
            root = json.loads(resp.read().decode("utf-8"))
    except HTTPError as e:
        root = json.loads(e.read().decode("utf-8", errors="replace"))
    except URLError as e:
        raise RuntimeError(f"网络异常: {e.reason}")
    if root.get("code") != 200:
        raise RuntimeError(f"[{root.get('code')}] {root.get('msg')}")
    return root.get("data") or {}


# ============ 核心接口 ============
def login(card_key: str) -> dict:
    """卡密登录（卡密经 RSA-OAEP 加密后放 X-Card-Cipher 头）"""
    card_cipher = rsa_encrypt_oaep(card_key, RSA_PUBLIC_KEY)
    body = json.dumps({"deviceName": "python-client", "clientVersion": "1.0.0"}, separators=(",", ":"))
    return http_request("POST", "/api/sdk/card/login", body, {"X-Card-Cipher": card_cipher})


def heartbeat(session_id: str) -> dict:
    """心跳保活"""
    body = json.dumps({"sessionId": session_id, "timestamp": int(time.time() * 1000)}, separators=(",", ":"))
    return http_request("POST", "/api/sdk/device/heartbeat", body)


def get_announcement() -> dict:
    """拉取公告"""
    return http_request("GET", "/api/sdk/announcement", "")


def check_update(client_version: str, channel: str = "stable") -> dict:
    """检查更新"""
    return http_request("GET", f"/api/sdk/update/check?clientVersion={client_version}&channel={channel}", "")


# ============ 使用示例 ============
if __name__ == "__main__":
    print(f"=== ${p.softwareName} 接入测试 ===")
    # 1. 卡密登录
    result = login("在此填入测试卡密")
    print(f"登录成功: sessionId={result.get('sessionId')}, 到期={result.get('expireTime')}")
    sid = result.get("sessionId", "")

    # 2. 心跳
    hb = heartbeat(sid)
    print(f"心跳成功: 下次间隔={hb.get('nextInterval')}秒")

    # 3. 公告
    ann = get_announcement()
    print(f"公告: {ann}")

    # 4. 更新检查
    upd = check_update("1.0.0")
    print(f"更新检查: 有更新={upd.get('hasUpdate')}")
`
}

/* ============================ C# / .NET ============================ */
function generateCsharp(p: Required<SdkCodeTemplateParams>): string {
  return `// ${headerComment(p, 'C# / .NET')}
// 用法: dotnet run  或  在 Visual Studio 中运行
// 完整 SDK（含设备指纹采集）见 /sdk/csharp/JicekSdk/

using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace ${p.softwareName.replace(/[^a-zA-Z0-9]/g, '') || 'JicekApp'}
{
    class Program
    {
        // ============ 配置（自动填入，请补充 SignSecret） ============
        const string ServerUrl = "${p.serverUrl}";
        const string AppKey = "${p.appKey}";
        const string SignSecret = "在此填入签名密钥明文";
        const string RsaPublicKey = "${p.rsaPublicKey}";

        static readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(10) };

        static async Task Main(string[] args)
        {
            Console.WriteLine($"=== ${p.softwareName} 接入测试 ===");
            // 1. 卡密登录
            var loginResult = await Login("在此填入测试卡密");
            Console.WriteLine($"登录成功: sessionId={loginResult.GetProperty("sessionId").GetString()}");
            var sid = loginResult.GetProperty("sessionId").GetString();

            // 2. 心跳
            var hb = await Heartbeat(sid!);
            Console.WriteLine($"心跳成功: 下次间隔={hb.GetProperty("nextInterval").GetInt32()}秒");

            // 3. 公告
            var ann = await GetAsync("/api/sdk/announcement");
            Console.WriteLine($"公告: {ann}");

            // 4. 更新检查
            var upd = await GetAsync("/api/sdk/update/check?clientVersion=1.0.0&channel=stable");
            Console.WriteLine($"更新检查: {upd}");
        }

        static async Task<JsonElement> Login(string cardKey)
        {
            var cardCipher = RsaEncryptOaep(cardKey, RsaPublicKey);
            var body = JsonSerializer.Serialize(new { deviceName = "csharp-client", clientVersion = "1.0.0" });
            return await PostWithHeader("/api/sdk/card/login", body, ("X-Card-Cipher", cardCipher));
        }

        static async Task<JsonElement> Heartbeat(string sessionId)
        {
            var body = JsonSerializer.Serialize(new { sessionId, timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            return await PostWithHeader("/api/sdk/device/heartbeat", body);
        }

        static async Task<JsonElement> GetAsync(string path)
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, ServerUrl + path);
            foreach (var (k, v) in BuildSignedHeaders("GET", path, "")) req.Headers.TryAddWithoutValidation(k, v);
            return await SendAndParse(req);
        }

        static async Task<JsonElement> PostWithHeader(string path, string body, params (string, string)[] extra)
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, ServerUrl + path);
            foreach (var (k, v) in BuildSignedHeaders("POST", path, body)) req.Headers.TryAddWithoutValidation(k, v);
            foreach (var (k, v) in extra) req.Headers.TryAddWithoutValidation(k, v);
            req.Content = new StringContent(body, Encoding.UTF8, "application/json");
            return await SendAndParse(req);
        }

        static async Task<JsonElement> SendAndParse(HttpRequestMessage req)
        {
            using var resp = await http.SendAsync(req);
            var text = await resp.Content.ReadAsStringAsync();
            using var doc = JsonDocument.Parse(text);
            var root = doc.RootElement.Clone();
            if (root.GetProperty("code").GetInt32() != 200)
                throw new Exception($"[{root.GetProperty("code")}] {root.GetProperty("msg")}");
            return root.TryGetProperty("data", out var d) ? d.Clone() : root;
        }

        static Dictionary<string, string> BuildSignedHeaders(string method, string path, string body)
        {
            var ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds().ToString();
            var nonce = Guid.NewGuid().ToString("N");
            var bodySha = string.IsNullOrEmpty(body) ? "" : Sha256Hex(body);
            var payload = $"{method}\\n{path}\\n{ts}\\n{nonce}\\n{bodySha}";
            return new()
            {
                ["X-App-Key"] = AppKey,
                ["X-Timestamp"] = ts,
                ["X-Nonce"] = nonce,
                ["X-Signature"] = HmacSha256B64(payload, SignSecret),
                ["X-Device-Id"] = nonce,
            };
        }

        static string Sha256Hex(string data) => BitConverter.ToString(SHA256.HashData(Encoding.UTF8.GetBytes(data))).Replace("-", "").ToLower();

        static string HmacSha256B64(string data, string secret)
        {
            using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret));
            return Convert.ToBase64String(hmac.ComputeHash(Encoding.UTF8.GetBytes(data)));
        }

        static string RsaEncryptOaep(string plaintext, string pubKeyB64)
        {
            var pub = RSA.Create();
            pub.ImportSubjectPublicKeyInfo(Convert.FromBase64String(pubKeyB64), out _);
            var cipher = pub.Encrypt(Encoding.UTF8.GetBytes(plaintext), RSAEncryptionPadding.OaepSHA256);
            return Convert.ToBase64String(cipher);
        }
    }
}
`
}

/* ============================ C++ (Windows) ============================ */
function generateCpp(p: Required<SdkCodeTemplateParams>): string {
  return `// ${headerComment(p, 'C++')}
// 依赖: OpenSSL (libcrypto) + libcurl
// 编译(MSVC): cl /EHsc main.cpp /I<openssl_include> /link libcrypto.lib libcurl.lib
// 完整 SDK（含设备指纹/心跳守护线程）见 /sdk/cpp/jicek/

#include <openssl/sha.h>
#include <openssl/hmac.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/err.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <curl/curl.h>
#include <nlohmann/json.hpp>
#include <iostream>
#include <string>
#include <sstream>
#include <chrono>
#include <random>
#include <iomanip>

// ============ 配置（自动填入，请补充 SIGN_SECRET） ============
static const std::string SERVER_URL = "${p.serverUrl}";
static const std::string APP_KEY = "${p.appKey}";
static const std::string SIGN_SECRET = "在此填入签名密钥明文";
static const std::string RSA_PUBLIC_KEY = "${p.rsaPublicKey}";  // Base64 DER SPKI

static std::string sha256Hex(const std::string& data) {
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256(reinterpret_cast<const unsigned char*>(data.data()), data.size(), hash);
    std::ostringstream oss;
    for (auto b : hash) oss << std::hex << std::setw(2) << std::setfill('0') << (int)b;
    return oss.str();
}

static std::string base64Encode(const unsigned char* data, size_t len) {
    BIO* b64 = BIO_new(BIO_f_base64()); BIO* mem = BIO_new(BIO_s_mem());
    b64 = BIO_push(b64, mem);
    BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    BIO_write(b64, data, (int)len); BIO_flush(b64);
    BUF_MEM* ptr; BIO_get_mem_ptr(b64, &ptr);
    std::string out(ptr->data, ptr->length);
    BIO_free_all(b64);
    return out;
}

static std::string hmacSha256B64(const std::string& data, const std::string& secret) {
    unsigned char* result = HMAC(EVP_sha256(),
        reinterpret_cast<const unsigned char*>(secret.data()), (int)secret.size(),
        reinterpret_cast<const unsigned char*>(data.data()), data.size(), nullptr, nullptr);
    return base64Encode(result, 32);
}

static std::string rsaEncryptOaep(const std::string& plaintext, const std::string& pubKeyB64) {
    // Base64 解码
    BIO* b64 = BIO_new(BIO_f_base64()); BIO* mem = BIO_new_mem_buf(pubKeyB64.data(), (int)pubKeyB64.size());
    mem = BIO_push(b64, mem); BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    std::string der(4096, '\\0');
    int derLen = BIO_read(mem, &der[0], (int)der.size());
    der.resize(derLen); BIO_free_all(mem);

    const unsigned char* p = reinterpret_cast<const unsigned char*>(der.data());
    EVP_PKEY* pub = d2i_PUBKEY(nullptr, &p, derLen);
    EVP_PKEY_CTX* ctx = EVP_PKEY_CTX_new(pub, nullptr);
    EVP_PKEY_encrypt_init(ctx);
    EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING);
    EVP_PKEY_CTX_set_rsa_oaep_md(ctx, EVP_sha256());
    EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, EVP_sha256());

    size_t outLen = 0;
    EVP_PKEY_encrypt(ctx, nullptr, &outLen,
        reinterpret_cast<const unsigned char*>(plaintext.data()), plaintext.size());
    std::string cipher(outLen, '\\0');
    EVP_PKEY_encrypt(ctx, reinterpret_cast<unsigned char*>(&cipher[0]), &outLen,
        reinterpret_cast<const unsigned char*>(plaintext.data()), plaintext.size());
    cipher.resize(outLen);
    EVP_PKEY_CTX_free(ctx); EVP_PKEY_free(pub);
    return base64Encode(reinterpret_cast<const unsigned char*>(cipher.data()), cipher.size());
}

static size_t writeCb(void* ptr, size_t sz, size_t nmemb, void* userdata) {
    ((std::string*)userdata)->append((char*)ptr, sz * nmemb); return sz * nmemb;
}

static nlohmann::json httpRequest(const std::string& method, const std::string& path,
                                   const std::string& body, const std::vector<std::pair<std::string,std::string>>& extra = {}) {
    auto now = std::chrono::system_clock::now();
    auto ts = std::to_string(std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()).count());
    // 生成 nonce（简化版 UUID）
    std::random_device rd; std::mt19937 gen(rd());
    std::stringstream nonce; nonce << std::hex << rd() << rd() << rd() << rd();
    std::string bodySha = body.empty() ? "" : sha256Hex(body);
    std::string payload = method + "\\n" + path + "\\n" + ts + "\\n" + nonce.str() + "\\n" + bodySha;

    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, ("X-App-Key: " + APP_KEY).c_str());
    headers = curl_slist_append(headers, ("X-Timestamp: " + ts).c_str());
    headers = curl_slist_append(headers, ("X-Nonce: " + nonce.str()).c_str());
    headers = curl_slist_append(headers, ("X-Signature: " + hmacSha256B64(payload, SIGN_SECRET)).c_str());
    headers = curl_slist_append(headers, ("X-Device-Id: " + nonce.str()).c_str());
    headers = curl_slist_append(headers, "Content-Type: application/json; charset=UTF-8");
    for (auto& [k, v] : extra) headers = curl_slist_append(headers, (k + ": " + v).c_str());

    CURL* curl = curl_easy_init();
    std::string resp;
    curl_easy_setopt(curl, CURLOPT_URL, (SERVER_URL + path).c_str());
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, method.c_str());
    if (!body.empty()) curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str());
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeCb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &resp);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);
    curl_easy_perform(curl);
    curl_slist_free_all(headers); curl_easy_cleanup(curl);

    auto root = nlohmann::json::parse(resp);
    if (root["code"].get<int>() != 200) throw std::runtime_error("[" + std::to_string(root["code"].get<int>()) + "] " + root["msg"].get<std::string>());
    return root.contains("data") ? root["data"] : root;
}

int main() {
    curl_global_init(CURL_GLOBAL_DEFAULT);
    std::cout << "=== ${p.softwareName} 接入测试 ===" << std::endl;

    // 1. 卡密登录
    std::string cardCipher = rsaEncryptOaep("在此填入测试卡密", RSA_PUBLIC_KEY);
    auto loginResult = httpRequest("POST", "/api/sdk/card/login",
        R"({"deviceName":"cpp-client","clientVersion":"1.0.0"})", {{"X-Card-Cipher", cardCipher}});
    std::cout << "登录成功: sessionId=" << loginResult["sessionId"] << std::endl;

    // 2. 公告
    auto ann = httpRequest("GET", "/api/sdk/announcement", "");
    std::cout << "公告: " << ann.dump() << std::endl;

    // 3. 更新检查
    auto upd = httpRequest("GET", "/api/sdk/update/check?clientVersion=1.0.0&channel=stable", "");
    std::cout << "更新检查: " << upd.dump() << std::endl;

    curl_global_cleanup();
    return 0;
}
`
}

/* ============================ Go ============================ */
function generateGo(p: Required<SdkCodeTemplateParams>): string {
  return `// ${headerComment(p, 'Go')}
// 用法: go run main.go
// 完整 SDK（含设备指纹/心跳 goroutine）见 /sdk/go/jicek/

package main

import (
    "bytes"
    "crypto"
    "crypto/hmac"
    "crypto/rand"
    "crypto/rsa"
    "crypto/sha256"
    "crypto/x509"
    "encoding/base64"
    "encoding/hex"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "strconv"
    "time"

    "github.com/google/uuid"
)

// ============ 配置（自动填入，请补充 SignSecret） ============
const (
    serverURL    = "${p.serverUrl}"
    appKey       = "${p.appKey}"
    signSecret   = "在此填入签名密钥明文"
    rsaPublicKey = "${p.rsaPublicKey}" // Base64 DER SPKI
)

func sha256Hex(data []byte) string {
    h := sha256.Sum256(data)
    return hex.EncodeToString(h[:])
}

func hmacSha256B64(data string, secret string) string {
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(data))
    return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

func rsaEncryptOaep(plaintext string, pubKeyB64 string) (string, error) {
    der, err := base64.StdEncoding.DecodeString(pubKeyB64)
    if err != nil { return "", err }
    pub, err := x509.ParsePKIXPublicKey(der)
    if err != nil { return "", err }
    cipher, err := rsa.EncryptOAEP(sha256.New(), rand.Reader, pub.(*rsa.PublicKey), []byte(plaintext), nil)
    if err != nil { return "", err }
    return base64.StdEncoding.EncodeToString(cipher), nil
}

func buildSignedHeaders(method, path, body string) http.Header {
    ts := strconv.FormatInt(time.Now().UnixMilli(), 10)
    nonce := uuid.NewString()
    bodySha := ""
    if body != "" { bodySha = sha256Hex([]byte(body)) }
    payload := method + "\\n" + path + "\\n" + ts + "\\n" + nonce + "\\n" + bodySha
    return http.Header{
        "X-App-Key":   {appKey},
        "X-Timestamp": {ts},
        "X-Nonce":     {nonce},
        "X-Signature": {hmacSha256B64(payload, signSecret)},
        "X-Device-Id": {nonce},
        "Content-Type": {"application/json; charset=UTF-8"},
    }
}

func httpRequest(method, path, body string, extra map[string]string) (map[string]interface{}, error) {
    req, _ := http.NewRequest(method, serverURL+path, bytes.NewBufferString(body))
    req.Header = buildSignedHeaders(method, path, body)
    for k, v := range extra { req.Header.Set(k, v) }

    resp, err := http.DefaultClient.Do(req)
    if err != nil { return nil, err }
    defer resp.Body.Close()
    data, _ := io.ReadAll(resp.Body)

    var root map[string]interface{}
    if err := json.Unmarshal(data, &root); err != nil { return nil, err }
    if code, _ := root["code"].(float64); int(code) != 200 {
        return nil, fmt.Errorf("[%v] %v", root["code"], root["msg"])
    }
    if d, ok := root["data"]; ok { return d.(map[string]interface{}), nil }
    return root, nil
}

func login(cardKey string) (map[string]interface{}, error) {
    cipher, err := rsaEncryptOaep(cardKey, rsaPublicKey)
    if err != nil { return nil, err }
    body, _ := json.Marshal(map[string]string{"deviceName": "go-client", "clientVersion": "1.0.0"})
    return httpRequest("POST", "/api/sdk/card/login", string(body), map[string]string{"X-Card-Cipher": cipher})
}

func heartbeat(sessionID string) (map[string]interface{}, error) {
    body, _ := json.Marshal(map[string]interface{}{"sessionId": sessionID, "timestamp": time.Now().UnixMilli()})
    return httpRequest("POST", "/api/sdk/device/heartbeat", string(body), nil)
}

func getAnnouncement() (map[string]interface{}, error) {
    return httpRequest("GET", "/api/sdk/announcement", "", nil)
}

func checkUpdate(version string) (map[string]interface{}, error) {
    return httpRequest("GET", "/api/sdk/update/check?clientVersion="+version+"&channel=stable", "", nil)
}

func main() {
    fmt.Println("=== ${p.softwareName} 接入测试 ===")
    // 1. 卡密登录
    result, err := login("在此填入测试卡密")
    if err != nil { panic(err) }
    fmt.Printf("登录成功: sessionId=%v\\n", result["sessionId"])

    // 2. 公告
    ann, _ := getAnnouncement()
    fmt.Printf("公告: %v\\n", ann)

    // 3. 更新检查
    upd, _ := checkUpdate("1.0.0")
    fmt.Printf("更新检查: %v\\n", upd)
}
`
}

/* ============================ Java ============================ */
function generateJava(p: Required<SdkCodeTemplateParams>): string {
  return `// ${headerComment(p, 'Java')}
// 用法: javac JicekQuickStart.java && java JicekQuickStart
// 完整 SDK（Builder 模式 + 设备指纹 + 心跳守护线程）见 /sdk/java/
// 依赖: JDK 17+（无需三方库，RSA/HTTP 用 javax.crypto + java.net.http）

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class JicekQuickStart {

    // ============ 配置（自动填入，请补充 SIGN_SECRET） ============
    static final String SERVER_URL = "${p.serverUrl}";
    static final String APP_KEY = "${p.appKey}";
    static final String SIGN_SECRET = "在此填入签名密钥明文";
    static final String RSA_PUBLIC_KEY = "${p.rsaPublicKey}"; // Base64 DER SPKI

    static final HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();

    public static void main(String[] args) throws Exception {
        System.out.println("=== ${p.softwareName} 接入测试 ===");
        // 1. 卡密登录
        Map<String, Object> loginResult = login("在此填入测试卡密");
        System.out.println("登录成功: sessionId=" + loginResult.get("sessionId"));
        String sid = (String) loginResult.get("sessionId");

        // 2. 心跳
        Map<String, Object> hb = heartbeat(sid);
        System.out.println("心跳成功: 下次间隔=" + hb.get("nextInterval") + "秒");

        // 3. 公告
        Map<String, Object> ann = get("/api/sdk/announcement");
        System.out.println("公告: " + ann);

        // 4. 更新检查
        Map<String, Object> upd = get("/api/sdk/update/check?clientVersion=1.0.0&channel=stable");
        System.out.println("更新检查: " + upd);
    }

    static Map<String, Object> login(String cardKey) throws Exception {
        String cardCipher = rsaEncryptOaep(cardKey, RSA_PUBLIC_KEY);
        String body = "{\\"deviceName\\":\\"java-client\\",\\"clientVersion\\":\\"1.0.0\\"}";
        return postWithHeader("/api/sdk/card/login", body, "X-Card-Cipher", cardCipher);
    }

    static Map<String, Object> heartbeat(String sessionId) throws Exception {
        String body = "{\\"sessionId\\":\\"" + sessionId + "\\",\\"timestamp\\":" + Instant.now().toEpochMilli() + "}";
        return postWithHeader("/api/sdk/device/heartbeat", body);
    }

    static Map<String, Object> get(String path) throws Exception {
        return request("GET", path, "", null);
    }

    static Map<String, Object> postWithHeader(String path, String body, String... extraHeaders) throws Exception {
        Map<String, String> extra = null;
        if (extraHeaders.length > 0) {
            extra = new HashMap<>();
            for (int i = 0; i < extraHeaders.length; i += 2) extra.put(extraHeaders[i], extraHeaders[i + 1]);
        }
        return request("POST", path, body, extra);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> request(String method, String path, String body, Map<String, String> extra) throws Exception {
        String[] signed = buildSignedHeaders(method, path, body);
        HttpRequest.Builder req = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + path))
            .header("X-App-Key", APP_KEY).header("X-Timestamp", signed[0]).header("X-Nonce", signed[1])
            .header("X-Signature", signed[2]).header("X-Device-Id", signed[1]);
        if (extra != null) for (var e : extra.entrySet()) req.header(e.getKey(), e.getValue());
        if (method.equals("GET")) req.GET();
        else req.POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json; charset=UTF-8");

        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        // 简易 JSON 解析（生产环境建议用 Jackson/Gson）
        String json = resp.body();
        Map<String, Object> root = parseSimpleJson(json);
        if (!Integer.valueOf(200).equals(root.get("code")))
            throw new RuntimeException("[" + root.get("code") + "] " + root.get("msg"));
        return root.containsKey("data") ? (Map<String, Object>) root.get("data") : root;
    }

    static String[] buildSignedHeaders(String method, String path, String body) throws Exception {
        String ts = String.valueOf(Instant.now().toEpochMilli());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodySha = body.isEmpty() ? "" : sha256Hex(body);
        String payload = method + "\\n" + path + "\\n" + ts + "\\n" + nonce + "\\n" + bodySha;
        return new String[]{ ts, nonce, hmacSha256B64(payload, SIGN_SECRET) };
    }

    static String sha256Hex(String data) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static String hmacSha256B64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    static String rsaEncryptOaep(String plaintext, String pubKeyB64) throws Exception {
        byte[] der = Base64.getDecoder().decode(pubKeyB64);
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    /** 极简 JSON 解析（仅适用于平台响应格式） */
    static Map<String, Object> parseSimpleJson(String json) {
        // 简化实现：实际建议引入 Jackson
        Map<String, Object> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1, json.length() - 1);
        // 此处省略完整解析逻辑，建议用 Jackson/Gson 替换
        return map;
    }
}
`
}

/* ============================ Node.js ============================ */
function generateNodejs(p: Required<SdkCodeTemplateParams>): string {
  return `// ${headerComment(p, 'Node.js')}
// 用法: node jicek_quick_start.js
// 零三方依赖，仅用 Node 内置模块

const crypto = require('crypto');
const http = require('http');
const https = require('https');

// ============ 配置（自动填入，请补充 SIGN_SECRET） ============
const SERVER_URL = "${p.serverUrl}";
const APP_KEY = "${p.appKey}";
const SIGN_SECRET = "在此填入签名密钥明文";
const RSA_PUBLIC_KEY = "${p.rsaPublicKey}"; // Base64 DER SPKI

function sha256Hex(data) {
  return crypto.createHash('sha256').update(data, 'utf8').digest('hex');
}

function hmacSha256B64(data, secret) {
  return crypto.createHmac('sha256', secret).update(data, 'utf8').digest('base64');
}

function rsaEncryptOaep(plaintext, pubKeyB64) {
  const pub = crypto.createPublicKey({ key: Buffer.from(pubKeyB64, 'base64'), format: 'der', type: 'spki' });
  const cipher = crypto.publicEncrypt(
    { key: pub, padding: crypto.constants.RSA_PKCS1_OAEP_PADDING, oaepHash: 'sha256' },
    Buffer.from(plaintext, 'utf8')
  );
  return cipher.toString('base64');
}

function buildSignedHeaders(method, path, body) {
  const ts = Date.now().toString();
  const nonce = crypto.randomUUID().replace(/-/g, '');
  const bodySha = body ? sha256Hex(body) : '';
  const payload = \`\${method}\\n\${path}\\n\${ts}\\n\${nonce}\\n\${bodySha}\`;
  return {
    'X-App-Key': APP_KEY,
    'X-Timestamp': ts,
    'X-Nonce': nonce,
    'X-Signature': hmacSha256B64(payload, SIGN_SECRET),
    'X-Device-Id': nonce,
    'Content-Type': 'application/json; charset=UTF-8',
  };
}

function httpRequest(method, path, body, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(SERVER_URL + path);
    const headers = { ...buildSignedHeaders(method, path, body), ...extraHeaders };
    const lib = url.protocol === 'https:' ? https : http;
    const req = lib.request(url, { method, headers }, (resp) => {
      let data = '';
      resp.on('data', (chunk) => (data += chunk));
      resp.on('end', () => {
        try {
          const root = JSON.parse(data);
          if (root.code !== 200) return reject(new Error(\`[\${root.code}] \${root.msg}\`));
          resolve(root.data || root);
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

async function login(cardKey) {
  const cardCipher = rsaEncryptOaep(cardKey, RSA_PUBLIC_KEY);
  const body = JSON.stringify({ deviceName: 'nodejs-client', clientVersion: '1.0.0' });
  return httpRequest('POST', '/api/sdk/card/login', body, { 'X-Card-Cipher': cardCipher });
}

async function heartbeat(sessionId) {
  const body = JSON.stringify({ sessionId, timestamp: Date.now() });
  return httpRequest('POST', '/api/sdk/device/heartbeat', body);
}

async function getAnnouncement() {
  return httpRequest('GET', '/api/sdk/announcement', '');
}

async function checkUpdate(version) {
  return httpRequest('GET', \`/api/sdk/update/check?clientVersion=\${version}&channel=stable\`, '');
}

(async () => {
  console.log('=== ${p.softwareName} 接入测试 ===');
  // 1. 卡密登录
  const result = await login('在此填入测试卡密');
  console.log('登录成功: sessionId=' + result.sessionId);

  // 2. 心跳
  const hb = await heartbeat(result.sessionId);
  console.log('心跳成功: 下次间隔=' + hb.nextInterval + '秒');

  // 3. 公告
  const ann = await getAnnouncement();
  console.log('公告:', ann);

  // 4. 更新检查
  const upd = await checkUpdate('1.0.0');
  console.log('更新检查:', upd);
})().catch(console.error);
`
}

/* ============================ Lua ============================ */
function generateLua(p: Required<SdkCodeTemplateParams>): string {
  return `-- ${headerComment(p, 'Lua')}
-- 依赖: luaossl + LuaSocket（或回退到 curl + openssl 命令行）
-- 用法: lua jicek_quick_start.lua
-- 完整 SDK（含设备指纹/心跳）见 /sdk/lua/jicek.lua

local json = require("dkjson")  -- 或 cjson
local http = require("socket.http")
local https = require("ssl.https")
local openssl_hmac = require("openssl.hmac")
local openssl_digest = require("openssl.digest")
local openssl_base64 = require("openssl.base64")
local pkey = require("openssl.pkey")

-- ============ 配置（自动填入，请补充 SIGNSecret） ============
local SERVER_URL = "${p.serverUrl}"
local APP_KEY = "${p.appKey}"
local SIGN_SECRET = "在此填入签名密钥明文"
-- Lua SDK 接受 PEM 格式公钥，需将 Base64 DER 转换为 PEM
local RSA_PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\\n"
  .. "${p.rsaPublicKey}".gsub("(.{64})", "%1\\n")
  .. "\\n-----END PUBLIC KEY-----"

local function sha256_hex(data)
  return openssl_digest.new("sha256"):final(data)
end

local function hmac_sha256_b64(data, secret)
  return openssl_base64.encode(openssl_hmac.new(secret, "sha256"):final(data))
end

local function rsa_encrypt_oaep(plaintext, pem)
  local pub = pkey.new({ public = pem })
  return openssl_base64.encode(pub:encrypt(plaintext, "oaep", { md = "sha256", mgf1 = "sha256" }))
end

local function uuid_no_dash()
  local tmpl = "xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx"
  return string.gsub(tmpl, "[xy]", function(c)
    local v = (c == "x") and math.random(0, 0xf) or math.random(8, 0xb)
    return string.format("%x", v)
  end)
end

local function build_signed_headers(method, path, body)
  local ts = tostring(os.time() * 1000)
  local nonce = uuid_no_dash()
  local body_sha = (body and #body > 0) and sha256_hex(body) or ""
  local payload = string.format("%s\\n%s\\n%s\\n%s\\n%s", method, path, ts, nonce, body_sha)
  return {
    ["X-App-Key"] = APP_KEY,
    ["X-Timestamp"] = ts,
    ["X-Nonce"] = nonce,
    ["X-Signature"] = hmac_sha256_b64(payload, SIGN_SECRET),
    ["X-Device-Id"] = nonce,
    ["Content-Type"] = "application/json; charset=UTF-8",
  }
end

local function http_request(method, path, body, extra)
  local lib = SERVER_URL:find("^https") and https or http
  local headers = build_signed_headers(method, path, body)
  if extra then for k, v in pairs(extra) do headers[k] = v end end
  local url = SERVER_URL .. path
  local resp = {}
  local _, status = lib.request {
    url = url, method = method, headers = headers,
    source = body and (function() return body end) or nil,
    sink = function(chunk) table.insert(resp, chunk or "") return true end,
  }
  local text = table.concat(resp)
  local root = json.decode(text)
  if root.code ~= 200 then error("[" .. root.code .. "] " .. root.msg) end
  return root.data or root
end

local function login(card_key)
  local cipher = rsa_encrypt_oaep(card_key, RSA_PUBLIC_KEY_PEM)
  local body = json.encode({ deviceName = "lua-client", clientVersion = "1.0.0" })
  return http_request("POST", "/api/sdk/card/login", body, { ["X-Card-Cipher"] = cipher })
end

local function heartbeat(session_id)
  local body = json.encode({ sessionId = session_id, timestamp = os.time() * 1000 })
  return http_request("POST", "/api/sdk/device/heartbeat", body)
end

local function get_announcement()
  return http_request("GET", "/api/sdk/announcement", "")
end

-- 使用示例
print("=== ${p.softwareName} 接入测试 ===")
local result = login("在此填入测试卡密")
print("登录成功: sessionId=" .. (result.sessionId or ""))
local ann = get_announcement()
print("公告:", json.encode(ann))
`
}

/* ============================ Shell ============================ */
function generateShell(p: Required<SdkCodeTemplateParams>): string {
  return `#!/bin/bash
# ${headerComment(p, 'Shell')}
# 依赖: curl + openssl + jq
# 用法: bash jicek_quick_start.sh
# 完整 SDK（含设备指纹/心跳）见 /sdk/shell/jicek.sh

set -euo pipefail

# ============ 配置（自动填入，请补充 SIGN_SECRET） ============
SERVER_URL="${p.serverUrl}"
APP_KEY="${p.appKey}"
SIGN_SECRET="在此填入签名密钥明文"
# Shell SDK 接受 PEM 格式公钥
RSA_PUBLIC_KEY_PEM=$(echo "${p.rsaPublicKey}" | fold -w 64 | sed '1i-----BEGIN PUBLIC KEY-----' | sed '$a-----END PUBLIC KEY-----')

sha256_hex() { printf '%s' "$1" | openssl dgst -sha256 -hex | awk '{print $2}'; }
hmac_sha256_b64() { printf '%s' "$1" | openssl dgst -sha256 -hmac "$2" -binary | openssl base64 -A; }
uuid_no_dash() { cat /proc/sys/kernel/random/uuid | tr -d '-'; }
now_ms() { date +%s%3N; }

# RSA-OAEP/SHA256 加密卡密（输出 Base64）
rsa_encrypt_oaep() {
  local plain="$1" tmp_pem
  tmp_pem=$(mktemp)
  echo "$RSA_PUBLIC_KEY_PEM" > "$tmp_pem"
  local cipher
  cipher=$(printf '%s' "$plain" | openssl pkeyutl -encrypt -inkey "$tmp_pem" -pubin \\
    -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256 2>/dev/null | openssl base64 -A)
  rm -f "$tmp_pem"
  echo "$cipher"
}

# 构造签名并发起 HTTP 请求
# 参数: method path body [extra_header_name extra_header_value]
http_request() {
  local method="$1" path="$2" body="\${3:-}" extra_name="\${4:-}" extra_val="\${5:-}"
  local ts nonce body_sha payload sig
  ts=$(now_ms); nonce=$(uuid_no_dash)
  body_sha=$([ -n "$body" ] && sha256_hex "$body" || echo "")
  payload=$(printf '%s\\n%s\\n%s\\n%s\\n%s' "$method" "$path" "$ts" "$nonce" "$body_sha")
  sig=$(hmac_sha256_b64 "$payload" "$SIGN_SECRET")

  local headers=(-H "X-App-Key: $APP_KEY" -H "X-Timestamp: $ts" -H "X-Nonce: $nonce"
    -H "X-Signature: $sig" -H "X-Device-Id: $nonce" -H "Content-Type: application/json; charset=UTF-8")
  [ -n "$extra_name" ] && headers+=(-H "$extra_name: $extra_val")

  local resp
  if [ "$method" = "GET" ]; then
    resp=$(curl -sS -m 10 -X GET "$SERVER_URL$path" "\${headers[@]}")
  else
    resp=$(curl -sS -m 10 -X POST "$SERVER_URL$path" "\${headers[@]}" --data-binary "$body")
  fi

  local code
  code=$(echo "$resp" | jq -r '.code')
  if [ "$code" != "200" ]; then
    echo "请求失败: $resp" >&2; exit 1
  fi
  echo "$resp" | jq -c '.data // .'
}

echo "=== ${p.softwareName} 接入测试 ==="

# 1. 卡密登录
CARD_CIPHER=$(rsa_encrypt_oaep "在此填入测试卡密")
LOGIN_RESULT=$(http_request "POST" "/api/sdk/card/login" \\
  '{"deviceName":"shell-client","clientVersion":"1.0.0"}' "X-Card-Cipher" "$CARD_CIPHER")
SESSION_ID=$(echo "$LOGIN_RESULT" | jq -r '.sessionId')
echo "登录成功: sessionId=$SESSION_ID"

# 2. 心跳
HB=$(http_request "POST" "/api/sdk/device/heartbeat" \\
  "{\"sessionId\":\"$SESSION_ID\",\"timestamp\":$(now_ms)}")
echo "心跳成功: $HB"

# 3. 公告
ANN=$(http_request "GET" "/api/sdk/announcement")
echo "公告: $ANN"

# 4. 更新检查
UPD=$(http_request "GET" "/api/sdk/update/check?clientVersion=1.0.0&channel=stable")
echo "更新检查: $UPD"
`
}

/* ============================ 易语言 EPL ============================ */
function generateEpl(p: Required<SdkCodeTemplateParams>): string {
  return `' ${headerComment(p, '易语言 EPL')}
' 依赖: 精易模块 v10.0+（提供 HMAC/RSA/HTTP/JSON）
' 用法: 将以下代码复制到易语言 IDE，补充精易模块引用
' 完整 SDK 规范见 /sdk/epl/jicek_epl_spec.md
'
' 【注意】易语言代码为伪代码展示，实际需在易语言 IDE 中按语法编写。
' 本模板展示调用流程，具体子程序实现见 jicek_epl_spec.md。

.版本 2

.程序集 程序集1

.子程序 _启动子程序, 整数型, , 本子程序在程序首次运行时最先执行
    .局部变量 初始化结果, 逻辑型
    .局部变量 登录结果, 逻辑型
    .局部变量 会话ID, 文本型
    .局部变量 到期时间, 文本型
    .局部变量 剩余次数, 整数型
    .局部变量 功能列表, 文本型
    .局部变量 公告内容, 文本型
    .局部变量 更新信息, 文本型

    ' ============ 配置（自动填入，请补充签名密钥） ============
    ' 服务器地址
    ' ${p.serverUrl}
    ' AppKey
    ' ${p.appKey}
    ' 签名密钥（请填入明文）
    ' SIGN_SECRET = 在此填入签名密钥明文
    ' RSA 公钥（PEM 格式）
    ' ${p.rsaPublicKey}

    ' 1. 初始化 SDK
    初始化结果 ＝ 极策k_初始化 (“${p.serverUrl}”, “${p.appKey}”, “在此填入签名密钥明文”, “${p.rsaPublicKey}”, 10000)
    .如果 (初始化结果 ＝ 假)
        标准输出 (, “SDK 初始化失败” ＋ #换行符)
        返回 (1)
    .否则
    .如果结束

    标准输出 (, “=== ${p.softwareName} 接入测试 ===” ＋ #换行符)

    ' 2. 卡密登录（通过引用参数输出结果）
    登录结果 ＝ 极策k_验证卡密 (“在此填入测试卡密”, 会话ID, 到期时间, 剩余次数, 功能列表)
    .如果 (登录结果 ＝ 假)
        标准输出 (, “卡密登录失败” ＋ #换行符)
        返回 (1)
    .否则
    .如果结束
    标准输出 (, “登录成功: 会话ID=” ＋ 会话ID ＋ “, 到期=” ＋ 到期时间 ＋ #换行符)

    ' 3. 启动心跳（后台守护，回调通过重写子程序实现）
    极策k_启动心跳 ()

    ' 4. 拉取公告
    公告内容 ＝ 极策k_获取公告 ()
    标准输出 (, “公告: ” ＋ 公告内容 ＋ #换行符)

    ' 5. 检查更新
    更新信息 ＝ 极策k_检查更新 (“1.0.0”, “stable”)
    标准输出 (, “更新检查: ” ＋ 更新信息 ＋ #换行符)

    ' 6. 退出登录（停止心跳）
    极策k_退出登录 ()
    返回 (0)


' ============ 以下为需重写的回调子程序（可选） ============

.子程序 极策k_心跳成功回调
    .参数 结果, 文本型
    标准输出 (, “心跳成功: ” ＋ 结果 ＋ #换行符)

.子程序 极策k_心跳失败回调
    .参数 失败次数, 整数型
    标准输出 (, “心跳失败第 ” ＋ 到文本 (失败次数) ＋ “ 次” ＋ #换行符)

.子程序 极策k_心跳断开回调
    标准输出 (, “心跳已断开（连续失败超限）” ＋ #换行符)

.子程序 极策k_设备封禁回调
    标准输出 (, “设备已被封禁” ＋ #换行符)


' ============ 以下为 SDK 核心子程序声明（实现见 jicek_epl_spec.md） ============
' 极策k_初始化 (服务器地址, AppKey, 签名密钥, RSA公钥PEM, 超时ms) → 逻辑型
' 极策k_验证卡密 (卡密, 会话ID, 到期时间, 剩余次数, 功能列表) → 逻辑型
' 极策k_启动心跳 () → 逻辑型
' 极策k_退出登录 ()
' 极策k_获取公告 () → 文本型
' 极策k_检查更新 (客户端版本, 通道) → 文本型
'
' 签名算法（精易模块实现）：
'   payload = method + #换行符 + path + #换行符 + 时间戳 + #换行符 + 随机串 + #换行符 + body的SHA256
'   signature = HMAC_SHA256(payload, 签名密钥) → Base64
'
' 请求头：
'   X-App-Key / X-Timestamp / X-Nonce / X-Signature / X-Device-Id
'   卡密登录额外: X-Card-Cipher（RSA-OAEP/SHA256 加密的卡密 Base64）
`
}
