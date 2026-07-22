# 极策k Go SDK

面向 Go 1.21+ 应用的卡密验证 SDK。

## 依赖

- Go 1.21+
- 零第三方依赖（仅用标准库 `crypto/*`, `net/http`, `os/exec`, `runtime`）

## 安装

```bash
# 复制 sdk/go/jicek/ 到你的项目
```

## 快速开始

```go
package main

import (
	"log"
	"os"
	"time"

	"yourmod/jicek"
)

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
	log.Printf("到期时间: %s, 剩余次数: %d", result.ExpireTime, result.RemainCount)

	client.StartHeartbeat()
	time.Sleep(60 * time.Second)
	client.Logout()
}

func getenv(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
```

## 作者

极策k  2026-07-21
