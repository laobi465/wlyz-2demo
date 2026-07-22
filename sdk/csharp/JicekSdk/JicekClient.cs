// 极策k网络验证 C# SDK
// 作者: 极策k  日期: 2026-07-21
//
// 零第三方依赖，仅用 .NET BCL（System.Security.Cryptography, System.Net.Http）
// 支持 .NET 8+ / .NET Framework 4.8+
//
// 三件套：
// 1. 卡密验证（VerifyCard）
// 2. 心跳保活（Heartbeat / StartHeartbeat）
// 3. 设备绑定/换机（BindDevice / UnbindDevice）

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Jicek.Sdk
{
    /// <summary>SDK 异常</summary>
    public class JicekException : Exception
    {
        public int Code { get; }
        public string Msg { get; }
        public JicekException(int code, string msg) : base($"[{code}] {msg}")
        {
            Code = code;
            Msg = msg;
        }
    }

    /// <summary>SDK 配置</summary>
    public class JicekConfig
    {
        public string ServerUrl { get; set; } = "";
        public string AppKey { get; set; } = "";
        public string SignSecret { get; set; } = "";
        public string RsaPublicKey { get; set; } = ""; // Base64 SPKI
        public int TimeoutMs { get; set; } = 10000;
    }

    public class VerifyResult
    {
        public bool Valid { get; set; }
        public string SessionId { get; set; } = "";
        public string ExpireTime { get; set; } = "";
        public int? RemainCount { get; set; }
        public List<string> Features { get; set; } = new();
        public string Msg { get; set; } = "";
    }

    public class HeartbeatResult
    {
        public int NextInterval { get; set; }
        public long ServerTime { get; set; }
    }

    public class HeartbeatCallback
    {
        public Action<HeartbeatResult>? OnSuccess;
        public Action<JicekException>? OnFailure;
        public Action? OnDisconnect;
        public Action? OnDeviceBanned;
    }

    /// <summary>SDK 主类</summary>
    public class JicekClient : IDisposable
    {
        private readonly JicekConfig _config;
        private readonly HttpClient _httpClient;
        private readonly FingerprintCollector _fpCollector = new();
        private string? _sessionId;
        private int _heartbeatInterval = 60;
        private CancellationTokenSource? _heartbeatCts;
        private int _failCount;

        public HeartbeatCallback? HeartbeatCallback { get; set; }

        public JicekClient(JicekConfig config)
        {
            if (string.IsNullOrEmpty(config.ServerUrl)) throw new ArgumentException("ServerUrl 必填");
            if (string.IsNullOrEmpty(config.AppKey)) throw new ArgumentException("AppKey 必填");
            if (string.IsNullOrEmpty(config.SignSecret)) throw new ArgumentException("SignSecret 必填");
            if (string.IsNullOrEmpty(config.RsaPublicKey)) throw new ArgumentException("RsaPublicKey 必填");

            _config = config;
            _httpClient = new HttpClient { Timeout = TimeSpan.FromMilliseconds(config.TimeoutMs) };
        }

        public async Task<VerifyResult> VerifyCard(string cardKey)
        {
            var fp = _fpCollector.Collect(_config.RsaPublicKey);
            var cardCipher = CryptoUtil.RsaEncryptOaep(cardKey, _config.RsaPublicKey);

            var body = new Dictionary<string, object?>
            {
                ["fingerprint"] = fp.Fingerprint,
                ["encryptedDetail"] = fp.EncryptedDetail,
                ["cardCipher"] = cardCipher,
                ["deviceName"] = fp.DeviceName,
                ["osType"] = fp.OsType,
                ["osVersion"] = fp.OsVersion,
                ["clientVersion"] = fp.ClientVersion,
                ["isVm"] = fp.IsVm,
                ["vmExtra"] = fp.VmExtra,
            };
            var data = await PostAsync("/api/sdk/card/verify", body);
            var result = new VerifyResult { Valid = true };
            if (data.TryGetValue("sessionId", out var sid) && sid is JsonElement jeSid)
            {
                result.SessionId = jeSid.GetString() ?? "";
                _sessionId = result.SessionId;
            }
            if (data.TryGetValue("expireTime", out var et) && et is JsonElement jeEt)
                result.ExpireTime = jeEt.GetString() ?? "";
            if (data.TryGetValue("remainCount", out var rc) && rc is JsonElement jeRc && jeRc.ValueKind == JsonValueKind.Number)
                result.RemainCount = jeRc.GetInt32();
            return result;
        }

        public async Task<HeartbeatResult> Heartbeat()
        {
            var fp = _fpCollector.Collect(_config.RsaPublicKey);
            var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var nonce = Guid.NewGuid().ToString("N");

            var body = new Dictionary<string, object?>
            {
                ["tenantId"] = 0,
                ["softwareId"] = 0,
                ["fingerprint"] = fp.Fingerprint,
                ["timestamp"] = timestamp,
                ["nonce"] = nonce,
            };
            var jsonBody = JsonSerializer.Serialize(body);
            var headers = BuildSignedHeaders("POST", "/api/sdk/device/heartbeat", jsonBody, fp.Fingerprint);
            headers["X-Sign-Secret"] = _config.SignSecret;
            headers["X-Heartbeat-Interval"] = _heartbeatInterval.ToString();

            var respBody = await HttpRequestAsync("POST", "/api/sdk/device/heartbeat", jsonBody, headers);
            var data = ParseResponse(respBody);
            var result = new HeartbeatResult { NextInterval = 60 };
            if (data.TryGetValue("nextInterval", out var ni) && ni is JsonElement jeNi && jeNi.ValueKind == JsonValueKind.Number)
            {
                result.NextInterval = jeNi.GetInt32();
                _heartbeatInterval = result.NextInterval;
            }
            if (data.TryGetValue("serverTime", out var st) && st is JsonElement jeSt && jeSt.ValueKind == JsonValueKind.Number)
                result.ServerTime = jeSt.GetInt64();
            return result;
        }

        public void StartHeartbeat()
        {
            if (_heartbeatCts != null) return;
            _heartbeatCts = new CancellationTokenSource();
            var token = _heartbeatCts.Token;
            Task.Run(() => HeartbeatLoop(token), token);
        }

        public void StopHeartbeat()
        {
            _heartbeatCts?.Cancel();
            _heartbeatCts = null;
        }

        public async Task LogoutAsync()
        {
            if (_sessionId != null)
            {
                try { await PostAsync("/api/sdk/auth/logout", new { sessionId = _sessionId }); }
                catch { /* 忽略退出失败 */ }
            }
            StopHeartbeat();
            _sessionId = null;
        }

        public void Logout() => LogoutAsync().GetAwaiter().GetResult();

        public void Dispose()
        {
            StopHeartbeat();
            _httpClient.Dispose();
        }

        /* ============ 内部方法 ============ */

        private async Task HeartbeatLoop(CancellationToken token)
        {
            while (!token.IsCancellationRequested)
            {
                try
                {
                    var r = await Heartbeat();
                    _failCount = 0;
                    HeartbeatCallback?.OnSuccess?.Invoke(r);
                }
                catch (JicekException e)
                {
                    _failCount++;
                    if (e.Code == 3002) { HeartbeatCallback?.OnDeviceBanned?.Invoke(); StopHeartbeat(); return; }
                    HeartbeatCallback?.OnFailure?.Invoke(e);
                    if (_failCount >= 5) { HeartbeatCallback?.OnDisconnect?.Invoke(); StopHeartbeat(); return; }
                    int backoff = Math.Min(1 << _failCount, 30);
                    try { await Task.Delay(backoff * 1000, token); } catch { return; }
                    continue;
                }
                try { await Task.Delay(_heartbeatInterval * 1000, token); } catch { return; }
            }
        }

        private async Task<Dictionary<string, object?>> PostAsync(string path, object body)
        {
            var jsonBody = JsonSerializer.Serialize(body);
            var fp = _fpCollector.Collect(_config.RsaPublicKey);
            var headers = BuildSignedHeaders("POST", path, jsonBody, fp.Fingerprint);
            var respBody = await HttpRequestAsync("POST", path, jsonBody, headers);
            return ParseResponse(respBody);
        }

        private Dictionary<string, string> BuildSignedHeaders(string method, string path, string body, string deviceId)
        {
            var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds().ToString();
            var nonce = Guid.NewGuid().ToString("N");
            var bodySha = string.IsNullOrEmpty(body) ? "" : CryptoUtil.Sha256Hex(body);
            var payload = $"{method}\n{path}\n{timestamp}\n{nonce}\n{bodySha}";
            var signature = CryptoUtil.HmacSha256Base64(payload, _config.SignSecret);
            return new Dictionary<string, string>
            {
                ["X-App-Key"] = _config.AppKey,
                ["X-Timestamp"] = timestamp,
                ["X-Nonce"] = nonce,
                ["X-Signature"] = signature,
                ["X-Device-Id"] = deviceId,
                ["Content-Type"] = "application/json; charset=UTF-8",
            };
        }

        private async Task<string> HttpRequestAsync(string method, string path, string body, Dictionary<string, string> headers)
        {
            var url = _config.ServerUrl.TrimEnd('/') + path;
            using var req = new HttpRequestMessage(new HttpMethod(method), url);
            foreach (var kv in headers) req.Headers.TryAddWithoutValidation(kv.Key, kv.Value);
            if (!string.IsNullOrEmpty(body))
                req.Content = new StringContent(body, Encoding.UTF8, "application/json");

            using var resp = await _httpClient.SendAsync(req);
            var respBody = await resp.Content.ReadAsStringAsync();
            if (!resp.IsSuccessStatusCode)
                throw new JicekException((int)resp.StatusCode, $"HTTP 请求失败: {respBody}");
            return respBody;
        }

        private static Dictionary<string, object?> ParseResponse(string respText)
        {
            using var doc = JsonDocument.Parse(respText);
            var root = doc.RootElement;
            int code = root.TryGetProperty("code", out var c) && c.ValueKind == JsonValueKind.Number ? c.GetInt32() : 0;
            if (code != 200)
            {
                var msg = root.TryGetProperty("msg", out var m) ? m.GetString() ?? "" : "未知错误";
                throw new JicekException(code, msg);
            }
            var result = new Dictionary<string, object?>();
            if (root.TryGetProperty("data", out var d) && d.ValueKind == JsonValueKind.Object)
            {
                foreach (var p in d.EnumerateObject()) result[p.Name] = p.Value;
            }
            else
            {
                foreach (var p in root.EnumerateObject()) result[p.Name] = p.Value;
            }
            return result;
        }
    }
}
