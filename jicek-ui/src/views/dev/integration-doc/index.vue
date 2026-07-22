<!--
  极策k 对接文档页（v0.12.0）
  作者: 极策k  日期: 2026-07-22

  供开发者自行接入 SDK 时参考的对接文档：
   - 接入流程
   - 凭证说明（appKey / signSecret / rsaPublicKey）
   - 请求头规范与签名算法
   - RSA 卡密加密
   - SDK API 列表
   - 错误码表
   - 9 种语言 SDK 索引

  注：开发者也可在「软件管理」点「接入代码」一键生成已填入凭证的代码模板。
-->
<template>
  <div class="jicek-page integration-doc">
    <el-card>
      <template #header>
        <span class="jicek-card-title">SDK 对接文档</span>
        <el-text type="info" style="margin-left: 12px">开发者自行接入参考</el-text>
      </template>

      <!-- 目录 -->
      <el-anchor :container="scrollContainer" direction="horizontal" style="margin-bottom: 16px">
        <el-anchor-link href="#flow">接入流程</el-anchor-link>
        <el-anchor-link href="#credential">凭证说明</el-anchor-link>
        <el-anchor-link href="#header">请求头规范</el-anchor-link>
        <el-anchor-link href="#sign">签名算法</el-anchor-link>
        <el-anchor-link href="#rsa">RSA 加密</el-anchor-link>
        <el-anchor-link href="#api">API 列表</el-anchor-link>
        <el-anchor-link href="#error">错误码</el-anchor-link>
        <el-anchor-link href="#sdk">SDK 索引</el-anchor-link>
      </el-anchor>

      <div ref="scrollContainer" class="doc-content">
        <!-- 1. 接入流程 -->
        <h3 id="flow">一、接入流程</h3>
        <el-steps direction="vertical" :active="4" process-status="success">
          <el-step title="创建软件" description="在「软件管理」新建软件，系统自动生成 appKey + signSecret + RSA 密钥对。创建时明文仅展示一次，请立即保存。" />
          <el-step title="获取凭证" description="appKey 与 RSA 公钥可在软件详情随时查看；signSecret 仅在创建/轮换时返回明文，丢失需轮换重置。" />
          <el-step title="选择 SDK 语言" description="支持 Python / C# / C++ / Go / Java / Node.js / Lua / Shell / 易语言 共 9 种语言，可一键生成接入代码。" />
          <el-step title="集成调用" description="将 SDK 集成到您的软件，实现卡密登录、心跳保活、公告拉取、更新检查等核心功能。" />
        </el-steps>
        <el-alert type="info" :closable="false" style="margin-top: 12px">
          <template #title>
            快速接入：在「软件管理」点击「接入代码」按钮，选择语言即可一键生成已填入凭证的代码模板。
          </template>
        </el-alert>

        <!-- 2. 凭证说明 -->
        <h3 id="credential">二、凭证说明</h3>
        <el-table :data="credentials" border stripe>
          <el-table-column prop="name" label="凭证" width="160" />
          <el-table-column prop="desc" label="说明" />
          <el-table-column prop="security" label="安全级别" width="120">
            <template #default="{ row }">
              <el-tag :type="row.securityType" size="small">{{ row.security }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="storage" label="存储方式" width="140" />
        </el-table>

        <!-- 3. 请求头规范 -->
        <h3 id="header">三、请求头规范</h3>
        <p>所有 <code>/api/sdk/**</code> 接口请求必须携带以下请求头，由 <code>SdkAuthFilter</code> 统一校验：</p>
        <el-table :data="headers" border stripe>
          <el-table-column prop="name" label="请求头" width="180" />
          <el-table-column prop="desc" label="说明" />
          <el-table-column prop="required" label="必填" width="80">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-alert type="warning" :closable="false" style="margin-top: 8px">
          <template #title>
            卡密登录接口额外需 <code>X-Card-Cipher</code> 头，值为 RSA-2048-OAEP/SHA256 加密后的卡密 Base64 字符串。
          </template>
        </el-alert>

        <!-- 4. 签名算法 -->
        <h3 id="sign">四、签名算法</h3>
        <el-card shadow="never" class="code-card">
          <pre>// 1. 构造签名原文（5 行，以 \n 分隔）
payload = METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256

// 各字段说明：
//   METHOD       HTTP 方法（大写），如 POST / GET
//   PATH         请求路径（不含 query string），如 /api/sdk/card/login
//   TIMESTAMP    13 位毫秒时间戳，与服务端时差不得超过 ±300 秒
//   NONCE        唯一随机串（建议 UUID v4 去横线，32 位十六进制）
//   BODY_SHA256  请求体的 SHA-256 十六进制摘要（GET 请求或无 body 时为空字符串）

// 2. 计算 HMAC-SHA256 签名
signature = Base64( HMAC-SHA256(payload, signSecret) )

// 3. 将签名放入请求头
X-Signature = signature</pre>
        </el-card>
        <el-alert type="warning" :closable="false" style="margin-top: 8px">
          <template #title>
            防重放：每个 <code>X-Nonce</code> 在 5 分钟内仅可使用一次（Redis 原子去重）。请确保每次请求生成新的 nonce。
          </template>
        </el-alert>

        <!-- 5. RSA 加密 -->
        <h3 id="rsa">五、RSA 卡密加密</h3>
        <p>卡密登录时，卡密明文不得直接传输，须先经 RSA-2048-OAEP 加密后放入 <code>X-Card-Cipher</code> 头：</p>
        <el-table :data="rsaParams" border stripe>
          <el-table-column prop="name" label="参数" width="160" />
          <el-table-column prop="value" label="值" />
        </el-table>
        <el-card shadow="never" class="code-card">
          <pre>// 加密流程
cipher = RSA_OAEP_Encrypt(plaintext=卡密明文, publicKey=RSA公钥, hash=SHA256, mgf=MGF1-SHA256)
X-Card-Cipher = Base64(cipher)

// 各语言实现：
//   Python:   cryptography 库 padding.OAEP(MGF1(SHA256), SHA256, None)
//   C#:       RSA.Encrypt(plaintext, RSAEncryptionPadding.OaepSHA256)
//   Go:       rsa.EncryptOAEP(sha256.New(), rand.Reader, pub, plain, nil)
//   Java:     Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
//   Node.js:  crypto.publicEncrypt({padding: RSA_PKCS1_OAEP_PADDING, oaepHash: 'sha256'})
//   C++:      EVP_PKEY_CTX + RSA_PKCS1_OAEP_PADDING + EVP_sha256
//   Lua:      openssl.pkey:encrypt(plain, "oaep", {md="sha256", mgf1="sha256"})
//   Shell:    openssl pkeyutl -encrypt -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256</pre>
        </el-card>

        <!-- 6. API 列表 -->
        <h3 id="api">六、SDK API 列表</h3>
        <el-table :data="apiList" border stripe>
          <el-table-column prop="method" label="方法" width="80" />
          <el-table-column prop="path" label="路径" width="220" />
          <el-table-column prop="desc" label="说明" />
          <el-table-column prop="auth" label="鉴权" width="100">
            <template #default="{ row }">
              <el-tag :type="row.auth ? 'success' : 'info'" size="small">{{ row.auth ? '签名' : '免签' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 7. 错误码 -->
        <h3 id="error">七、错误码</h3>
        <el-table :data="errorCodes" border stripe>
          <el-table-column prop="code" label="错误码" width="100" />
          <el-table-column prop="msg" label="含义" />
          <el-table-column prop="category" label="类别" width="120" />
        </el-table>

        <!-- 8. SDK 索引 -->
        <h3 id="sdk">八、SDK 语言索引</h3>
        <el-table :data="sdkList" border stripe>
          <el-table-column prop="lang" label="语言" width="140" />
          <el-table-column prop="path" label="SDK 目录" width="180" />
          <el-table-column prop="deps" label="依赖" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goCodeGen(row.lang)">一键生成代码</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-alert type="info" :closable="false" style="margin-top: 12px">
          <template #title>
            一键生成代码会自动填入您软件的 appKey 与 RSA 公钥，signSecret 需手动填入（脱敏存储）。
          </template>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const scrollContainer = ref<HTMLElement>()

/* 跳转到软件管理页使用代码生成器 */
function goCodeGen(_lang: string) {
  router.push('/software')
}

/* 凭证说明 */
const credentials = [
  { name: 'appKey', desc: '软件唯一标识，SDK 请求头 X-App-Key 使用', security: '公开', securityType: 'success' as const, storage: '明文存储' },
  { name: 'signSecret', desc: 'HMAC-SHA256 签名密钥，用于请求签名', security: '机密', securityType: 'danger' as const, storage: 'AES 加密存储' },
  { name: 'rsaPublicKey', desc: 'RSA-2048 公钥，客户端加密卡密用', security: '公开', securityType: 'success' as const, storage: '明文存储' },
  { name: 'rsaPrivateKey', desc: 'RSA-2048 私钥，服务端解密卡密用', security: '机密', securityType: 'danger' as const, storage: 'AES 加密存储' }
]

/* 请求头 */
const headers = [
  { name: 'X-App-Key', desc: '软件 AppKey', required: true },
  { name: 'X-Timestamp', desc: '13 位毫秒时间戳，时差 ±300 秒内有效', required: true },
  { name: 'X-Nonce', desc: '唯一随机串（UUID v4 去横线），5 分钟内防重放', required: true },
  { name: 'X-Signature', desc: 'HMAC-SHA256 签名 Base64', required: true },
  { name: 'X-Device-Id', desc: '设备标识（设备指纹或 nonce）', required: true },
  { name: 'Content-Type', desc: '固定 application/json; charset=UTF-8', required: true },
  { name: 'X-Card-Cipher', desc: 'RSA 加密的卡密 Base64（仅卡密登录接口）', required: false }
]

/* RSA 参数 */
const rsaParams = [
  { name: '算法', value: 'RSA-2048' },
  { name: '填充模式', value: 'OAEP (Optimal Asymmetric Encryption Padding)' },
  { name: 'OAEP Hash', value: 'SHA-256' },
  { name: 'MGF1 Hash', value: 'SHA-256' },
  { name: '公钥格式', value: 'Base64 DER SPKI（部分语言接受 PEM）' },
  { name: '密文编码', value: 'Base64' }
]

/* API 列表 */
const apiList = [
  { method: 'POST', path: '/api/sdk/card/login', desc: '卡密登录（X-Card-Cipher 头携带 RSA 加密卡密）', auth: true },
  { method: 'POST', path: '/api/sdk/device/heartbeat', desc: '心跳保活（动态间隔，维持会话）', auth: true },
  { method: 'POST', path: '/api/sdk/device/bind', desc: '设备绑定（首次登录自动绑定或手动绑定）', auth: true },
  { method: 'POST', path: '/api/sdk/device/unbind', desc: '设备解绑（换机码解绑）', auth: true },
  { method: 'GET', path: '/api/sdk/announcement', desc: '拉取远程公告（已发布状态）', auth: true },
  { method: 'GET', path: '/api/sdk/update/check', desc: '检查软件更新（按版本+通道匹配）', auth: true }
]

/* 错误码 */
const errorCodes = [
  { code: 200, msg: '成功', category: '通用' },
  { code: 3100, msg: 'AppKey 不存在', category: 'SDK 鉴权' },
  { code: 3101, msg: '时间戳过期（±300 秒外）', category: 'SDK 鉴权' },
  { code: 3102, msg: 'Nonce 防重放拦截', category: 'SDK 鉴权' },
  { code: 3103, msg: '签名校验失败', category: 'SDK 鉴权' },
  { code: 3104, msg: '软件已禁用', category: 'SDK 鉴权' },
  { code: 3105, msg: 'RSA 卡密解密失败', category: 'SDK 鉴权' },
  { code: 3106, msg: '卡密无效或已封禁', category: 'SDK 业务' },
  { code: 3107, msg: '卡密已过期', category: 'SDK 业务' },
  { code: 3108, msg: '设备并发超限', category: 'SDK 业务' },
  { code: 3109, msg: '设备已封禁', category: 'SDK 业务' },
  { code: 3110, msg: '会话无效或已过期', category: 'SDK 业务' },
  { code: 1021, msg: '公告不存在', category: '公告模块' },
  { code: 1025, msg: '公告状态非法', category: '公告模块' },
  { code: 1031, msg: '更新包不存在', category: '更新模块' },
  { code: 1035, msg: '更新包状态非法', category: '更新模块' }
]

/* SDK 索引 */
const sdkList = [
  { lang: 'Python', path: '/sdk/python', deps: 'cryptography 库' },
  { lang: 'C# / .NET', path: '/sdk/csharp', deps: '.NET 8+ / .NET Framework 4.8+' },
  { lang: 'C++ (Windows)', path: '/sdk/cpp', deps: 'OpenSSL + libcurl' },
  { lang: 'Go', path: '/sdk/go', deps: 'Go 1.21+，零三方依赖' },
  { lang: 'Java', path: '/sdk/java', deps: 'JDK 17+' },
  { lang: 'Node.js', path: '/sdk/nodejs', deps: 'Node 18+，零三方依赖' },
  { lang: 'Lua', path: '/sdk/lua', deps: 'luaossl + LuaSocket' },
  { lang: 'Shell', path: '/sdk/shell', deps: 'curl + openssl + jq' },
  { lang: '易语言', path: '/sdk/epl', deps: '精易模块 v10.0+' }
]
</script>

<style scoped lang="scss">
.integration-doc {
  .doc-content {
    max-height: calc(100vh - 220px);
    overflow-y: auto;
    padding-right: 8px;
  }
  h3 {
    margin: 24px 0 12px;
    padding-bottom: 8px;
    border-bottom: 2px solid var(--jicek-primary);
    color: var(--jicek-text-primary);
  }
  p {
    color: var(--jicek-text-secondary);
    line-height: 1.8;
  }
  code {
    background: var(--jicek-bg-secondary, #f5f7fa);
    color: var(--jicek-primary);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
    font-size: 13px;
  }
  .code-card {
    background: #1e1e1e;
    :deep(pre) {
      color: #d4d4d4;
      font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
      font-size: 13px;
      line-height: 1.7;
      margin: 0;
      white-space: pre-wrap;
    }
  }
}
</style>
