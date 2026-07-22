# 极策k网络验证 客户端 SDK 契约规范

> 本文件定义所有语言 SDK 的统一接口契约。8 语言 SDK 必须实现完全一致的接口语义。
> 作者: 极策k  日期: 2026-07-21

## 1. 通信协议

- 传输：HTTPS（生产强制，开发可 HTTP）
- 编码：UTF-8
- 请求体：JSON（`Content-Type: application/json`）
- 响应体：`{ "code": 200, "msg": "...", "data": {...} }`
- 成功：`code == 200`；其他均为失败

## 2. 请求头（所有 SDK 接口必须）

| Header | 说明 | 必填 |
|---|---|---|
| `X-App-Key` | 软件 AppKey（开发者后台创建软件时生成） | 是 |
| `X-Timestamp` | 13 位毫秒时间戳 | 是 |
| `X-Nonce` | UUID v4，5 分钟内不可重复 | 是 |
| `X-Signature` | HMAC-SHA256 签名（Base64） | 是 |
| `X-Device-Id` | 设备指纹（64 字符 SHA-256） | 是 |
| `X-Card-Cipher` | RSA-2048-OAEP 加密的卡密密文（Base64） | 仅卡密接口 |

## 3. 签名算法

签名原文（按顺序用 `\n` 连接）：

```
METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256
```

- `METHOD`：HTTP 方法大写（GET/POST）
- `PATH`：请求路径（含 query string，不含 host）
- `TIMESTAMP`：13 位毫秒时间戳（与 X-Timestamp 一致）
- `NONCE`：UUID（与 X-Nonce 一致）
- `BODY_SHA256`：请求体 JSON 的 SHA-256 小写十六进制；GET 请求为空字符串

签名值：`Base64( HMAC-SHA256( signSecret, 签名原文 ) )`

服务端校验：
1. 时间戳 ±300s 内有效
2. Nonce 5 分钟内不可重复（Redis 缓存）
3. 签名匹配（常量时间比较）
4. 设备指纹未被封禁

## 4. 设备指纹采集

5 维原始数据（客户端采集后单独 SHA-256）：

| 维度 | 来源 | 说明 |
|---|---|---|
| `cpu` | CPU 序列号 / CPUID | Windows: wmic / Linux: /proc/cpuinfo / macOS: sysctl |
| `mainboard` | 主板序列号 | Windows: wmic baseboard / Linux: dmidecode / macOS: ioreg |
| `disk` | 系统盘序列号 | Windows: wmic diskdrive / Linux: /sys/block / macOS: diskutil |
| `mac` | 主网卡 MAC 地址 | 排除虚拟网卡、loopback |
| `bios` | BIOS UUID / 序列号 | Windows: wmic bios / Linux: dmidecode / macOS: ioreg |

客户端处理流程：
1. 采集 5 维原始字符串
2. 对每维单独 SHA-256（64 字符小写十六进制），得到 5 维哈希
3. 拼接 5 维哈希 → SHA-256 → 最终指纹 `fingerprint`（64 字符）
4. 5 维哈希 JSON `{"cpu":"...","mainboard":"...","disk":"...","mac":"...","bios":"..."}` → RSA 公钥加密 → `encryptedDetail`（Base64）

VM/容器场景（`isVm=1`）：
- 客户端额外采集 VM UUID（dmidecode -s system-uuid）或容器 ID（/proc/self/cgroup）
- 最终指纹 = `SHA-256(5维哈希拼接 + vmExtra)`

## 5. 加密算法

| 用途 | 算法 | 参数 |
|---|---|---|
| 卡密传输 | RSA-2048-OAEP | SHA-256 MGF1，输出 Base64 |
| 请求签名 | HMAC-SHA256 | 密钥=软件 signSecret，输出 Base64 |
| 指纹计算 | SHA-256 | 输出 64 字符小写十六进制 |

## 6. SDK 核心接口（8 语言统一语义）

```text
class JicekClient:
    # 构造
    constructor(appKey, signSecret, rsaPublicKey, serverUrl)

    # 卡密验证（核心）
    verifyCard(cardKey) -> VerifyResult
        # 1. 采集/读取设备指纹
        # 2. RSA 加密卡密
        # 3. POST /api/sdk/card/verify
        # 返回：{ valid, expireTime, remainCount, features, sessionId }

    # 心跳保活
    heartbeat() -> HeartbeatResult
        # POST /api/sdk/device/heartbeat
        # 返回：{ nextInterval, serverTime }
        # SDK 内部启动定时器，按 nextInterval 动态调整

    # 设备绑定
    bindDevice(cardKey) -> BindResult
        # POST /api/sdk/device/bind
        # 返回：{ bindCode }

    # 设备换机
    unbindDevice(bindCode) -> UnbindResult
        # POST /api/sdk/device/unbind
        # 返回：{ newBindCode }

    # 退出登录
    logout()
        # 通知服务端销毁 session
```

## 7. 心跳机制

- 首次心跳间隔：60s（默认）
- 后续间隔：由服务端 `nextInterval` 控制（5-300s）
- 超时阈值：3 × interval（服务端判定，客户端无需关心）
- 失败重试：指数退避（1s, 2s, 4s, 8s, 最大 30s），连续 5 次失败触发 `onDisconnect` 回调
- 心跳线程：守护线程，禁阻塞主线程

## 8. 错误码

SDK 内部统一抛出 `JicekException`，包含 `code` 和 `msg`：

| code | 含义 | SDK 处理 |
|---|---|---|
| 200 | 成功 | - |
| 401 | 未登录 | 触发 onAuthFailed |
| 1001 | 卡密不存在 | 抛出 |
| 1002 | 卡密已使用 | 抛出 |
| 1003 | 卡密已封禁 | 抛出 |
| 1004 | 卡密已过期 | 抛出 |
| 3002 | 设备已封禁 | 触发 onDeviceBanned |
| 3005 | 心跳超时 | 触发 onDisconnect |
| 3007 | 频率超限 | 退避重试 |

## 9. 文件结构

每个语言 SDK 目录结构：

```
sdk/{lang}/
├── README.md          # 使用说明 + 示例
├── src/               # 源码
│   ├── JicekClient    # 主类
│   ├── Crypto         # 加密辅助
│   ├── Fingerprint    # 指纹采集
│   ├── HttpClient     # HTTP 通信
│   └── Exception      # 异常定义
└── example/           # 示例代码
```
