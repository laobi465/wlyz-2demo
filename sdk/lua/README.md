# 极策k网络验证 Lua SDK

> 作者: 极策k  日期: 2026-07-21

适用于游戏脚本、嵌入式设备、自动化脚本场景的 Lua 客户端 SDK。

## 特性

- **零硬依赖**：自动探测 `luaossl` / `LuaSocket`，缺失时回退到 `openssl` / `curl` 命令行
- **跨平台**：Windows / Linux / macOS 指纹采集
- **单文件**：`jicek.lua` 一个文件即可集成
- **心跳回调**：Lua 无原生线程，提供 `heartbeat_tick()` 由宿主循环推进

## 依赖

### 必需
- Lua 5.2+ 或 LuaJIT 2.0+

### 可选（推荐安装以提升性能）
- `luaossl`：原生加密（避免命令行开销）
- `luasocket` + `luasec`：原生 HTTP/HTTPS

### 系统命令回退（缺失上述库时自动使用）
- `openssl`：HMAC-SHA256 / SHA-256 / RSA-OAEP
- `curl`：HTTP 请求
- `wmic`（Windows）/ `dmidecode`（Linux）/ `ioreg`（macOS）：指纹采集

## 文件结构

```
sdk/lua/
├── README.md
├── jicek.lua              # 主模块
└── example/
    └── example.lua        # 使用示例
```

## 快速开始

```lua
local jicek = require("jicek")

local client = jicek.new_client({
    server_url = "https://api.jicek.example.com",
    app_key = "ak_xxx",
    sign_secret = "sk_xxx",
    rsa_public_key = [[
-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----
]],
    timeout = 10,
})

-- 卡密验证
local ok, result = pcall(function()
    return client:verify_card("JK-DEMO-XXXX-XXXX-XXXX")
end)

if ok then
    print("验证成功，sessionId: " .. tostring(result.sessionId))

    -- 启动心跳（宿主循环推进）
    client:start_heartbeat()
    client:set_heartbeat_callback({
        on_success = function() print("心跳成功") end,
        on_disconnect = function() print("心跳断开") end,
    })

    -- 主循环
    while running do
        client:heartbeat_tick()
        -- ...业务逻辑...
    end

    client:logout()
else
    print("验证失败: " .. tostring(result))
end
```

## 核心 API

### `jicek.new_client(config)` → JicekClient

| 字段 | 类型 | 说明 |
|---|---|---|
| server_url | string | 服务端地址 |
| app_key | string | 软件 AppKey |
| sign_secret | string | HMAC 签名密钥 |
| rsa_public_key | string | RSA 公钥（PEM 格式） |
| timeout | int | HTTP 超时秒数（默认 10） |

### `client:verify_card(card_key)` → table

卡密验证，返回 `{ sessionId, expireTime, remainCount, features }`。

### `client:heartbeat()` → table

单次心跳，返回 `{ nextInterval, serverTime }`。一般由 `heartbeat_tick` 自动调度。

### `client:start_heartbeat()` / `stop_heartbeat()`

启动/停止心跳。Lua 无原生线程，需宿主循环调用 `heartbeat_tick()`。

### `client:heartbeat_tick()` → bool

心跳推进器。在宿主主循环中按需调用（如每秒一次），到达间隔会触发一次心跳。返回 `true` 表示本次触发了心跳。

### `client:logout()`

退出登录，销毁 session。

### `client:set_heartbeat_callback(cb)`

设置回调表：
- `on_success()`：心跳成功
- `on_failure(err)`：心跳失败
- `on_disconnect()`：连续 5 次失败，断开
- `on_device_banned()`：设备被封禁

## 异常

所有错误通过 `error(jicek.new_exception(code, msg))` 抛出，使用 `pcall` 捕获：

```lua
local ok, err = pcall(function() client:verify_card("xxx") end)
if not ok then
    if type(err) == "table" and err.code then
        print(string.format("错误码: %d, 消息: %s", err.code, err.msg))
    end
end
```

## 加密后端说明

| 后端 | 优先级 | 加密 | HTTP | 备注 |
|---|---|---|---|---|
| luaossl + luasocket | 高 | ✅ 原生 | ✅ 原生 | 性能最佳 |
| openssl CLI + curl CLI | 低 | ✅ 命令行 | ✅ 命令行 | 兼容性最好 |

首次调用时自动探测并缓存后端类型，后续调用不再重复探测。

## License

MIT
