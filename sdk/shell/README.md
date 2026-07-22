# 极策k网络验证 Shell SDK

> 作者: 极策k  日期: 2026-07-21

适用于 Linux/macOS 服务器、CI/CD 脚本、运维自动化场景的 Bash SDK。

## 特性

- **零运行时依赖**：仅需 `bash 4+` / `curl` / `openssl` + 标准 Unix 工具
- **jq 可选**：缺失 `jq` 时自动回退到内置 sed/grep 解析器
- **跨平台**：Linux / macOS / Windows (Git Bash / MSYS2)
- **后台心跳**：通过 `&` 在子 shell 中运行，进程 PID 暴露给主脚本

## 依赖

### 必需
- bash 4.0+
- curl
- openssl
- 标准工具：grep / sed / awk / cat / mktemp / hostname / uname

### 可选
- `jq`：更可靠的 JSON 解析（强烈推荐安装）
- `dmidecode`：Linux 主板/BIOS 序列号采集（需 root）
- `uuidgen`：UUID 生成（缺失时回退到 `/proc/sys/kernel/random/uuid`）

## 文件结构

```
sdk/shell/
├── README.md
├── jicek.sh               # 主模块（source 使用）
└── example/
    └── example.sh         # 使用示例
```

## 快速开始

```bash
#!/usr/bin/env bash
source /path/to/jicek.sh

# 读取 RSA 公钥
RSA_PUB=$(<./rsa_pub.pem)

# 初始化
jicek_init "https://api.jicek.example.com" "ak_xxx" "sk_xxx" "$RSA_PUB"

# 卡密验证
jicek_verify_card "JK-DEMO-XXXX-XXXX-XXXX"
echo "sessionId=$JICEK_SESSION_ID"

# 启动后台心跳
jicek_start_heartbeat

# 业务逻辑...
sleep 60

# 退出
jicek_logout
```

## 核心 API

### `jicek_init <server_url> <app_key> <sign_secret> <rsa_pub_pem> [timeout]`

初始化客户端。所有参数必填（除 timeout 默认 10s）。

### `jicek_verify_card <card_key>`

卡密验证。成功后 `JICEK_SESSION_ID` 被设置，响应 data 输出到 stdout。
失败时调用 `jicek_throw <code> <msg>` 退出。

### `jicek_heartbeat`

单次心跳。成功后 `JICEK_HEARTBEAT_INTERVAL` 更新为服务端下发的间隔。

### `jicek_start_heartbeat` / `jicek_stop_heartbeat`

启动/停止后台心跳。`JICEK_HEARTBEAT_PID` 保存心跳进程 PID。

### `jicek_logout`

退出登录，销毁 session，停止心跳。

## 全局状态变量

| 变量 | 说明 |
|---|---|
| `JICEK_SERVER_URL` | 服务端地址 |
| `JICEK_APP_KEY` | 软件 AppKey |
| `JICEK_SESSION_ID` | 当前会话 ID（验证成功后设置） |
| `JICEK_HEARTBEAT_INTERVAL` | 当前心跳间隔（秒） |
| `JICEK_HEARTBEAT_PID` | 心跳进程 PID |
| `JICEK_FAIL_COUNT` | 心跳连续失败次数 |
| `JICEK_TIMEOUT` | HTTP 超时（秒） |

## 心跳回调（可重写）

```bash
# 在 source jicek.sh 后重写以下函数：
jicek_on_heartbeat_success() {
    echo "心跳成功"
}
jicek_on_heartbeat_failure() {
    echo "心跳失败" >&2
}
jicek_on_heartbeat_disconnect() {
    echo "心跳断开（5 次失败）" >&2
    # 可在此触发告警/重启逻辑
}
jicek_on_device_banned() {
    echo "设备被封禁" >&2
}
```

## 错误处理

`jicek_throw <code> <msg>` 会输出错误到 stderr 并以错误码退出。
若希望捕获错误而非退出，可在调用前用 `set +e`：

```bash
set +e
jicek_verify_card "xxx"
rc=$?
set -e
if [[ $rc -ne 0 ]]; then
    echo "验证失败，错误码: $rc"
fi
```

## 加密说明

| 算法 | 实现 |
|---|---|
| HMAC-SHA256 | `openssl dgst -sha256 -hmac` |
| SHA-256 | `openssl dgst -sha256 -hex` |
| RSA-2048-OAEP | `openssl pkeyutl -encrypt -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256` |
| Base64 | `openssl base64 -A` |

## License

MIT
