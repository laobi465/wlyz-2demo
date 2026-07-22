# 极策k Node.js SDK

面向 Node.js 18+ 应用的卡密验证 SDK。

## 依赖

- Node.js 18+（使用内置 `crypto`, `https`, `child_process`）
- 零第三方依赖

## 安装

```bash
# 直接复制 sdk/nodejs/jicek/ 到你的项目
```

## 快速开始

```javascript
const { JicekClient } = require('./jicek');

const client = new JicekClient({
  serverUrl: 'https://verify.example.com',
  appKey: 'your-app-key',
  signSecret: 'your-sign-secret',
  rsaPublicKey: 'BASE64_RSA_PUBLIC_KEY',
});

client.setHeartbeatCallback({
  onSuccess: (r) => console.log('[心跳] 成功', r.nextInterval + 's'),
  onFailure: (e) => console.error('[心跳] 失败', e.code, e.msg),
  onDisconnect: () => console.error('[心跳] 断开'),
  onDeviceBanned: () => console.error('[安全] 设备已封禁'),
});

(async () => {
  try {
    const result = await client.verifyCard('JC-XXXX-XXXX-XXXX');
    console.log('到期时间:', result.expireTime);
    console.log('剩余次数:', result.remainCount);

    client.startHeartbeat();
    await new Promise(r => setTimeout(r, 60000));
  } catch (e) {
    console.error('验证失败:', e.code, e.msg);
  } finally {
    client.logout();
  }
})();
```

## 作者

极策k  2026-07-21
