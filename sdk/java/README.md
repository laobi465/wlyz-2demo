# 极策k Java SDK

面向 Java 17+ 应用的卡密验证 SDK。

## 依赖

- JDK 17+
- 无第三方依赖（仅用 JDK 内置 `java.net.http.HttpClient`）

## 快速开始

```java
import com.jicek.sdk.JicekClient;
import com.jicek.sdk.model.VerifyResult;

JicekClient client = JicekClient.builder()
    .serverUrl("https://verify.example.com")
    .appKey("your-app-key")
    .signSecret("your-sign-secret")
    .rsaPublicKey("BASE64_RSA_PUBLIC_KEY")
    .build();

// 卡密验证
VerifyResult result = client.verifyCard("JC-XXXX-XXXX-XXXX");
if (result.isValid()) {
    System.out.println("到期时间: " + result.getExpireTime());
    System.out.println("剩余次数: " + result.getRemainCount());
}

// 心跳保活（守护线程自动运行）
client.startHeartbeat();

// 退出
client.logout();
client.close();
```

## 接口

| 方法 | 说明 |
|---|---|
| `verifyCard(cardKey)` | 卡密验证，返回到期时间/剩余次数/功能列表 |
| `bindDevice(cardKey)` | 设备绑定，返回换机码 |
| `unbindDevice(bindCode)` | 换机，返回新换机码 |
| `heartbeat()` | 单次心跳 |
| `startHeartbeat()` | 启动守护线程自动心跳 |
| `logout()` | 退出登录 |
| `close()` | 释放资源 |

## 设备指纹采集

SDK 自动采集 5 维指纹（CPU/主板/硬盘/网卡/BIOS）。VM/容器场景自动追加补充维度。

## 作者

极策k  2026-07-21
