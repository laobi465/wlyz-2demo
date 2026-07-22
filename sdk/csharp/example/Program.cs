// 极策k C# SDK 使用示例
// 作者: 极策k  日期: 2026-07-21
using System;
using System.Threading.Tasks;
using Jicek.Sdk;

class Program
{
    static async Task Main()
    {
        var client = new JicekClient(new JicekConfig
        {
            ServerUrl = Environment.GetEnvironmentVariable("JICEK_SERVER_URL") ?? "http://127.0.0.1:8080",
            AppKey = Environment.GetEnvironmentVariable("JICEK_APP_KEY") ?? "",
            SignSecret = Environment.GetEnvironmentVariable("JICEK_SIGN_SECRET") ?? "",
            RsaPublicKey = Environment.GetEnvironmentVariable("JICEK_RSA_PUBLIC_KEY") ?? "",
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
            var result = await client.VerifyCard(Environment.GetEnvironmentVariable("JICEK_CARD_KEY") ?? "");
            Console.WriteLine($"验证成功，到期: {result.ExpireTime}, 剩余: {result.RemainCount}");
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
    }
}
