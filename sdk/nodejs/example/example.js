/**
 * 极策k Node.js SDK 使用示例
 * 作者: 极策k  日期: 2026-07-21
 */
'use strict';

const { JicekClient, JicekException } = require('./jicek');

async function main() {
  // 配置（从环境变量读取，禁硬编码）
  const client = new JicekClient({
    serverUrl: process.env.JICEK_SERVER_URL || 'http://127.0.0.1:8080',
    appKey: process.env.JICEK_APP_KEY,
    signSecret: process.env.JICEK_SIGN_SECRET,
    rsaPublicKey: process.env.JICEK_RSA_PUBLIC_KEY,
  });

  client.setHeartbeatCallback({
    onSuccess: (r) => console.log(`[心跳] 成功，下次间隔: ${r.nextInterval}s`),
    onFailure: (e) => console.error(`[心跳] 失败: ${e.code} ${e.msg}`),
    onDisconnect: () => console.error('[心跳] 断开，请重新验证'),
    onDeviceBanned: () => console.error('[安全] 设备已封禁'),
  });

  try {
    const result = await client.verifyCard(process.env.JICEK_CARD_KEY);
    console.log('验证成功');
    console.log('  到期时间:', result.expireTime);
    console.log('  剩余次数:', result.remainCount);
    console.log('  session:', result.sessionId);

    client.startHeartbeat();
    await new Promise(r => setTimeout(r, 60000));
  } catch (e) {
    if (e instanceof JicekException) {
      console.error(`验证失败: ${e.code} ${e.msg}`);
    } else {
      throw e;
    }
  } finally {
    await client.logout();
  }
}

main().catch(console.error);
