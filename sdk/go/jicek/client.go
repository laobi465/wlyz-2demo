// 极策k网络验证 Go SDK
// 作者: 极策k  日期: 2026-07-21
//
// 零第三方依赖，仅用 Go 标准库。
// 支持 Go 1.21+，跨平台（Windows/Linux/macOS）。
//
// 三件套：
// 1. 卡密验证（VerifyCard）
// 2. 心跳保活（Heartbeat / StartHeartbeat）
// 3. 设备绑定/换机（BindDevice / UnbindDevice）
package jicek

import (
	"bytes"
	"crypto"
	"crypto/hmac"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"net/http"
	"os/exec"
	"runtime"
	"strings"
	"sync"
	"time"
)

// JicekError SDK 异常
type JicekError struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
}

func (e *JicekError) Error() string {
	return fmt.Sprintf("[%d] %s", e.Code, e.Msg)
}

// Config SDK 配置
type Config struct {
	ServerURL    string
	AppKey       string
	SignSecret   string
	RsaPublicKey string // Base64 编码的 DER 公钥（SPKI 格式）
	Timeout      time.Duration
}

// HeartbeatResult 心跳结果
type HeartbeatResult struct {
	NextInterval int   `json:"nextInterval"`
	ServerTime   int64 `json:"serverTime"`
}

// VerifyResult 卡密验证结果
type VerifyResult struct {
	Valid       bool     `json:"valid"`
	SessionID   string   `json:"sessionId"`
	ExpireTime  string   `json:"expireTime"`
	RemainCount *int     `json:"remainCount,omitempty"`
	Features    []string `json:"features,omitempty"`
	Msg         string   `json:"msg,omitempty"`
}

// FingerprintResult 设备指纹采集结果
type FingerprintResult struct {
	Fingerprint     string `json:"fingerprint"`
	EncryptedDetail string `json:"encryptedDetail"`
	IsVm            int    `json:"isVm"`
	VmExtra         string `json:"vmExtra"`
	OsType          string `json:"osType"`
	OsVersion       string `json:"osVersion"`
	DeviceName      string `json:"deviceName"`
	ClientVersion   string `json:"clientVersion"`
}

// HeartbeatCallback 心跳回调
type HeartbeatCallback struct {
	OnSuccess      func(*HeartbeatResult)
	OnFailure      func(*JicekError)
	OnDisconnect   func()
	OnDeviceBanned func()
}

// Client SDK 主类
type Client struct {
	config            *Config
	httpClient        *http.Client
	sessionID         string
	heartbeatInterval int
	heartbeatStop     chan struct{}
	heartbeatWG       sync.WaitGroup
	callback          HeartbeatCallback
	failCount         int
	mu                sync.Mutex
}

// NewClient 创建客户端
func NewClient(cfg *Config) *Client {
	if cfg.Timeout == 0 {
		cfg.Timeout = 10 * time.Second
	}
	return &Client{
		config:            cfg,
		httpClient:        &http.Client{Timeout: cfg.Timeout},
		heartbeatInterval: 60,
	}
}

// SetHeartbeatCallback 设置心跳回调
func (c *Client) SetHeartbeatCallback(cb HeartbeatCallback) {
	c.callback = cb
}

// VerifyCard 卡密验证
func (c *Client) VerifyCard(cardKey string) (*VerifyResult, error) {
	fp := collectFingerprint(c.config.RsaPublicKey)
	cardCipher, err := rsaEncryptOAEP(cardKey, c.config.RsaPublicKey)
	if err != nil {
		return nil, &JicekError{Code: 500, Msg: "RSA 加密失败: " + err.Error()}
	}

	body := map[string]interface{}{
		"fingerprint":     fp.Fingerprint,
		"encryptedDetail": fp.EncryptedDetail,
		"cardCipher":      cardCipher,
		"deviceName":      fp.DeviceName,
		"osType":          fp.OsType,
		"osVersion":       fp.OsVersion,
		"clientVersion":   fp.ClientVersion,
		"isVm":            fp.IsVm,
		"vmExtra":         fp.VmExtra,
	}
	data, err := c.post("/api/sdk/card/verify", body)
	if err != nil {
		return nil, err
	}
	result := &VerifyResult{}
	if raw, ok := data["sessionId"]; ok {
		if s, ok := raw.(string); ok {
			result.SessionID = s
			c.sessionID = s
		}
	}
	if raw, ok := data["expireTime"]; ok {
		if s, ok := raw.(string); ok {
			result.ExpireTime = s
		}
	}
	if raw, ok := data["remainCount"]; ok {
		if n, ok := raw.(float64); ok {
			v := int(n)
			result.RemainCount = &v
		}
	}
	result.Valid = true
	return result, nil
}

// Heartbeat 单次心跳
func (c *Client) Heartbeat() (*HeartbeatResult, error) {
	fp := collectFingerprint(c.config.RsaPublicKey)
	timestamp := time.Now().UnixMilli()
	nonce := genUUID()

	body := map[string]interface{}{
		"tenantId":    0,
		"softwareId":  0,
		"fingerprint": fp.Fingerprint,
		"timestamp":   timestamp,
		"nonce":       nonce,
	}
	jsonBody, _ := json.Marshal(body)
	headers := c.buildSignedHeaders("POST", "/api/sdk/device/heartbeat", string(jsonBody), fp.Fingerprint)
	headers["X-Sign-Secret"] = c.config.SignSecret
	headers["X-Heartbeat-Interval"] = fmt.Sprintf("%d", c.heartbeatInterval)

	respBody, err := c.httpRequest("POST", "/api/sdk/device/heartbeat", string(jsonBody), headers)
	if err != nil {
		return nil, err
	}
	data, err := parseResponse(respBody)
	if err != nil {
		return nil, err
	}
	result := &HeartbeatResult{}
	if v, ok := data["nextInterval"].(float64); ok {
		result.NextInterval = int(v)
		c.mu.Lock()
		c.heartbeatInterval = result.NextInterval
		c.mu.Unlock()
	} else {
		result.NextInterval = 60
	}
	if v, ok := data["serverTime"].(float64); ok {
		result.ServerTime = int64(v)
	}
	return result, nil
}

// StartHeartbeat 启动后台心跳
func (c *Client) StartHeartbeat() {
	c.heartbeatStop = make(chan struct{})
	c.heartbeatWG.Add(1)
	go func() {
		defer c.heartbeatWG.Done()
		c.heartbeatLoop()
	}()
}

// StopHeartbeat 停止心跳
func (c *Client) StopHeartbeat() {
	if c.heartbeatStop != nil {
		close(c.heartbeatStop)
		c.heartbeatWG.Wait()
		c.heartbeatStop = nil
	}
}

// Logout 退出登录
func (c *Client) Logout() {
	if c.sessionID != "" {
		body := map[string]interface{}{"sessionId": c.sessionID}
		_, _ = c.post("/api/sdk/auth/logout", body)
	}
	c.StopHeartbeat()
	c.sessionID = ""
}

/* ============ 内部方法 ============ */

func (c *Client) heartbeatLoop() {
	for {
		c.mu.Lock()
		interval := c.heartbeatInterval
		c.mu.Unlock()

		select {
		case <-c.heartbeatStop:
			return
		case <-time.After(time.Duration(interval) * time.Second):
		}

		_, err := c.Heartbeat()
		if err == nil {
			c.failCount = 0
			if c.callback.OnSuccess != nil {
				c.callback.OnSuccess(&HeartbeatResult{NextInterval: c.heartbeatInterval})
			}
			continue
		}

		je, ok := err.(*JicekError)
		if !ok {
			je = &JicekError{Code: 500, Msg: err.Error()}
		}
		c.failCount++

		if je.Code == 3002 {
			if c.callback.OnDeviceBanned != nil {
				c.callback.OnDeviceBanned()
			}
			return
		}
		if c.callback.OnFailure != nil {
			c.callback.OnFailure(je)
		}
		if c.failCount >= 5 {
			if c.callback.OnDisconnect != nil {
				c.callback.OnDisconnect()
			}
			return
		}

		// 指数退避
		backoff := 1 << uint(c.failCount)
		if backoff > 30 {
			backoff = 30
		}
		select {
		case <-c.heartbeatStop:
			return
		case <-time.After(time.Duration(backoff) * time.Second):
		}
	}
}

func (c *Client) post(path string, body map[string]interface{}) (map[string]interface{}, error) {
	jsonBody, _ := json.Marshal(body)
	fp := collectFingerprint(c.config.RsaPublicKey)
	headers := c.buildSignedHeaders("POST", path, string(jsonBody), fp.Fingerprint)
	respBody, err := c.httpRequest("POST", path, string(jsonBody), headers)
	if err != nil {
		return nil, err
	}
	return parseResponse(respBody)
}

func (c *Client) buildSignedHeaders(method, path, body, deviceID string) map[string]string {
	timestamp := fmt.Sprintf("%d", time.Now().UnixMilli())
	nonce := genUUID()
	bodySha := ""
	if body != "" {
		bodySha = sha256Hex(body)
	}
	payload := method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodySha
	signature := hmacSha256Base64(payload, c.config.SignSecret)
	return map[string]string{
		"X-App-Key":   c.config.AppKey,
		"X-Timestamp": timestamp,
		"X-Nonce":     nonce,
		"X-Signature": signature,
		"X-Device-Id": deviceID,
		"Content-Type": "application/json; charset=UTF-8",
	}
}

func (c *Client) httpRequest(method, path, body string, headers map[string]string) (string, error) {
	url := strings.TrimRight(c.config.ServerURL, "/") + path
	req, err := http.NewRequest(method, url, strings.NewReader(body))
	if err != nil {
		return "", &JicekError{Code: 500, Msg: err.Error()}
	}
	for k, v := range headers {
		req.Header.Set(k, v)
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", &JicekError{Code: 500, Msg: "网络异常: " + err.Error()}
	}
	defer resp.Body.Close()
	respBody, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != 200 {
		return "", &JicekError{Code: resp.StatusCode, Msg: "HTTP 请求失败: " + string(respBody)}
	}
	return string(respBody), nil
}

/* ============ 加密工具 ============ */

func hmacSha256Base64(data, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(data))
	return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

func sha256Hex(input string) string {
	h := sha256.Sum256([]byte(input))
	return hex.EncodeToString(h[:])
}

func rsaEncryptOAEP(plaintext, publicKeyB64 string) (string, error) {
	pubDer, err := base64.StdEncoding.DecodeString(publicKeyB64)
	if err != nil {
		// 尝试 PEM 格式
		block, _ := pem.Decode([]byte(publicKeyB64))
		if block == nil {
			return "", fmt.Errorf("公钥格式错误")
		}
		pubDer = block.Bytes
	}
	pub, err := x509.ParsePKIXPublicKey(pubDer)
	if err != nil {
		// 尝试 PKCS1
		pubP1, err2 := x509.ParsePKCS1PublicKey(pubDer)
		if err2 != nil {
			return "", err
		}
		pub = pubP1
	}
	rsaPub, ok := pub.(*rsa.PublicKey)
	if !ok {
		return "", fmt.Errorf("非 RSA 公钥")
	}
	cipher, err := rsa.EncryptOAEP(sha256.New(), rand.Reader, rsaPub, []byte(plaintext), nil)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(cipher), nil
}

func genUUID() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	return fmt.Sprintf("%x", b)
}

func parseResponse(respText string) (map[string]interface{}, error) {
	var root map[string]interface{}
	if err := json.Unmarshal([]byte(respText), &root); err != nil {
		return nil, &JicekError{Code: 500, Msg: "响应解析失败: " + err.Error()}
	}
	code := 0
	if v, ok := root["code"].(float64); ok {
		code = int(v)
	}
	if code != 200 {
		msg, _ := root["msg"].(string)
		return nil, &JicekError{Code: code, Msg: msg}
	}
	if data, ok := root["data"].(map[string]interface{}); ok {
		return data, nil
	}
	return root, nil
}

// crypto 包占位（防止 import 未使用，rsa.EncryptOAEP 实际使用 crypto.SHA256）
var _ = crypto.SHA256
