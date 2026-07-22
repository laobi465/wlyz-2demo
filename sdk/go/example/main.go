// 极策k Go SDK 使用示例
// 作者: 极策k  日期: 2026-07-21
package main

import (
	"log"
	"os"
	"time"

	"yourmod/jicek"
)

func getenv(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}

func main() {
	client := jicek.NewClient(&jicek.Config{
		ServerURL:    getenv("JICEK_SERVER_URL", "http://127.0.0.1:8080"),
		AppKey:       os.Getenv("JICEK_APP_KEY"),
		SignSecret:   os.Getenv("JICEK_SIGN_SECRET"),
		RsaPublicKey: os.Getenv("JICEK_RSA_PUBLIC_KEY"),
	})

	client.SetHeartbeatCallback(jicek.HeartbeatCallback{
		OnSuccess:      func(r *jicek.HeartbeatResult) { log.Printf("[心跳] 成功 %ds", r.NextInterval) },
		OnFailure:      func(e *jicek.JicekError) { log.Printf("[心跳] 失败 %d %s", e.Code, e.Msg) },
		OnDisconnect:   func() { log.Println("[心跳] 断开") },
		OnDeviceBanned: func() { log.Println("[安全] 设备已封禁") },
	})

	result, err := client.VerifyCard(os.Getenv("JICEK_CARD_KEY"))
	if err != nil {
		log.Fatalf("验证失败: %v", err)
	}
	log.Printf("验证成功，到期: %s, session: %s", result.ExpireTime, result.SessionID)

	client.StartHeartbeat()
	time.Sleep(60 * time.Second)
	client.Logout()
}
