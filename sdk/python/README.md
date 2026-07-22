# 极策k Python SDK

面向 Python 3.9+ 应用的卡密验证 SDK。

## 依赖

- Python 3.9+
- `cryptography`（RSA-OAEP 加密）
- 其余使用标准库（`hashlib`, `hmac`, `uuid`, `json`, `urllib`, `subprocess`）

```bash
pip install cryptography
```

## 快速开始

```python
from jicek import JicekClient, JicekException

client = JicekClient(
    server_url="https://verify.example.com",
    app_key="your-app-key",
    sign_secret="your-sign-secret",
    rsa_public_key="BASE64_RSA_PUBLIC_KEY",
)

try:
    result = client.verify_card("JC-XXXX-XXXX-XXXX")
    print("到期时间:", result["expireTime"])
    print("剩余次数:", result.get("remainCount"))

    client.start_heartbeat()  # 后台守护线程

    import time
    time.sleep(60)
finally:
    client.logout()
```

## 接口

| 方法 | 说明 |
|---|---|
| `verify_card(card_key)` | 卡密验证 |
| `bind_device(card_key)` | 设备绑定 |
| `unbind_device(bind_code)` | 换机 |
| `heartbeat()` | 单次心跳 |
| `start_heartbeat()` | 启动后台心跳线程 |
| `stop_heartbeat()` | 停止心跳 |
| `logout()` | 退出登录 |

## 作者

极策k  2026-07-21
