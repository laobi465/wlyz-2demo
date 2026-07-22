# 极策k C# SDK

面向 .NET 8 / .NET Framework 4.8+ 应用的卡密验证 SDK。

## 依赖

- .NET 8+（或 .NET Framework 4.8+，需 `System.Net.Http`）
- 零第三方 NuGet 依赖（仅用 BCL 内置 `System.Security.Cryptography`）

## 安装

将 `sdk/csharp/JicekSdk/*.cs` 加入项目即可。

## 快速开始

```csharp
using Jicek.Sdk;

var client = new JicekClient(new JicekConfig
{
    ServerUrl = "https://verify.example.com",
    AppKey = "your-app-key",
    SignSecret = "your-sign-secret",
    RsaPublicKey = "BASE64_RSA_PUBLIC_KEY",
});

client.HeartbeatCallback = new HeartbeatCallback
{
    OnSuccess = r => Console.WriteLine($"[心跳] 成功 {r.NextInterval}s"),
    OnFailure = e => Console.Error.WriteLine($"[心跳] 失败 {e.Code} {e.Msg}"),
    OnDisconnect = () => Console.Error.WriteLine("[心跳] 断开"),
    OnDeviceBanned = () => Console.Error.WriteLine("[安全] 设备已封禁"),
};

try
{
    var result = await client.VerifyCard("JC-XXXX-XXXX-XXXX");
    Console.WriteLine($"到期: {result.ExpireTime}, 剩余: {result.RemainCount}");
    client.StartHeartbeat();
    await Task.Delay(60_000);
}
catch (JicekException e)
{
    Console.Error.WriteLine($"验证失败: {e.Code} {e.Msg}");
}
finally
{
    client.Logout();
}
```

## 作者

极策k  2026-07-21
