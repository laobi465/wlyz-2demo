# 极策k C++ SDK

面向 C++17 应用的卡密验证 SDK（header-only 风格 + 单一实现文件）。

## 依赖

- C++17+
- OpenSSL 1.1.1+ 或 3.x（用于 HMAC-SHA256 / RSA-OAEP / SHA-256）
- libcurl（HTTP 通信）

Debian/Ubuntu 安装：

```bash
sudo apt install libssl-dev libcurl4-openssl-dev
```

## 编译

```bash
g++ -std=c++17 example/main.cpp jicek/JicekClient.cpp \
    -lcrypto -lcurl -lpthread -o example
```

## 快速开始

```cpp
#include "jicek/JicekClient.h"
#include <iostream>
#include <thread>

int main() {
    jicek::JicekClient client({
        .serverUrl = "https://verify.example.com",
        .appKey = "your-app-key",
        .signSecret = "your-sign-secret",
        .rsaPublicKey = "BASE64_RSA_PUBLIC_KEY",
    });

    client.setHeartbeatCallback({
        [](const jicek::HeartbeatResult& r) { std::cout << "[心跳] 成功 " << r.nextInterval << "s\n"; },
        [](const jicek::JicekException& e) { std::cerr << "[心跳] 失败 " << e.code << " " << e.msg << "\n"; },
        []() { std::cerr << "[心跳] 断开\n"; },
        []() { std::cerr << "[安全] 设备已封禁\n"; },
    });

    try {
        auto result = client.verifyCard("JC-XXXX-XXXX-XXXX");
        std::cout << "到期: " << result.expireTime << "\n";
        client.startHeartbeat();
        std::this_thread::sleep_for(std::chrono::seconds(60));
    } catch (const jicek::JicekException& e) {
        std::cerr << "验证失败: " << e.code << " " << e.msg << "\n";
    }
    client.logout();
    return 0;
}
```

## 作者

极策k  2026-07-21
