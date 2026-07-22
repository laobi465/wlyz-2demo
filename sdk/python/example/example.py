"""极策k Python SDK 使用示例
作者: 极策k  日期: 2026-07-21
"""
import os
import time

from jicek import JicekClient, JicekException


class HeartbeatCallbacks:
    @staticmethod
    def on_success(result):
        print(f"[心跳] 成功，下次间隔: {result.get('nextInterval')}s")

    @staticmethod
    def on_failure(e: JicekException):
        print(f"[心跳] 失败: {e.code} {e.msg}")

    @staticmethod
    def on_disconnect():
        print("[心跳] 断开，请重新验证")

    @staticmethod
    def on_device_banned():
        print("[安全] 设备已封禁")


def main():
    # 配置（从环境变量读取，禁硬编码）
    server_url = os.environ.get("JICEK_SERVER_URL", "http://127.0.0.1:8080")
    app_key = os.environ.get("JICEK_APP_KEY", "")
    sign_secret = os.environ.get("JICEK_SIGN_SECRET", "")
    rsa_public_key = os.environ.get("JICEK_RSA_PUBLIC_KEY", "")
    card_key = os.environ.get("JICEK_CARD_KEY", "")

    client = JicekClient(
        server_url=server_url,
        app_key=app_key,
        sign_secret=sign_secret,
        rsa_public_key=rsa_public_key,
    )
    client.set_heartbeat_callback(HeartbeatCallbacks)

    try:
        result = client.verify_card(card_key)
        print("验证成功")
        print("  到期时间:", result.get("expireTime"))
        print("  剩余次数:", result.get("remainCount"))
        print("  session:", result.get("sessionId"))

        client.start_heartbeat()
        time.sleep(60)
    except JicekException as e:
        print(f"验证失败: {e.code} {e.msg}")
    finally:
        client.logout()


if __name__ == "__main__":
    main()
