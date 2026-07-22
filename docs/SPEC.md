# 极策k网络验证 - 规范文档

## 1. 代码规范

### 1.1 命名
- 包名：`com.jicek.license.{module}`
- 类名：UpperCamelCase，如 `CardKeyService`
- 方法名：lowerCamelCase，如 `generateCardKey`
- 常量：UPPER_SNAKE_CASE，如 `MAX_HEARTBEAT_INTERVAL`
- 数据库表名：`jicek_` 前缀 + 下划线，如 `jicek_card_key`
- 数据库字段：下划线，如 `out_trade_no`

### 1.2 分层
- Controller 只做参数校验和转发，禁写业务逻辑
- Service 实现业务，事务边界在 Service 层
- Mapper 只做数据访问，禁写 SQL 之外的逻辑

### 1.3 注释
- 类注释：作者 + 创建日期 + 用途
- 公共方法：必须写 @param @return @throws
- 复杂逻辑：行内注释说明 why，不说明 what

## 2. 架构规范

### 2.1 分层原则
```
controller → service → mapper → db
              ↓
           crypto/pay/heartbeat (基础设施)
```

### 2.2 模块边界
- `jicek-license` 是核心模块，所有业务在此
- 支付、加密、设备指纹为独立子包，可未来拆为独立模块
- SDK 接口 (`controller/sdk`) 必须独立鉴权，不依赖 Sa-Token 的后台登录态

## 3. 接口规范

### 3.1 统一响应体（沿用 RuoYi-Vue-Plus）
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

### 3.2 错误码（沿用 RuoYi 框架 + 极策k扩展）
| 范围 | 含义 |
|---|---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无权限 |
| 500 | 系统错误 |
| 1001-1999 | 极策k 卡密模块 |
| 2001-2999 | 极策k 支付模块 |
| 3001-3999 | 极策k 设备/心跳模块 |
| 4001-4999 | 极策k 代理/分润/提现模块 |
| 5001-5999 | 极策k 云函数模块 |

#### 极策k 错误码明细

**卡密模块 (1001-1999)**
| 错误码 | 含义 |
|---|---|
| 1001 | 卡密不存在 |
| 1002 | 卡密已使用 |
| 1003 | 卡密已封禁 |
| 1004 | 卡密已过期 |
| 1005 | 卡密已退款 |
| 1006 | 设备数超限 |
| 1007 | 卡密签名错误 |
| 1008 | 卡密解密失败 |
| 1009 | 卡类不存在 |
| 1010 | 卡密生成失败 |

**支付模块 (2001-2999)**
| 错误码 | 含义 |
|---|---|
| 2001 | 支付配置不存在 |
| 2002 | 商户密钥错误 |
| 2003 | 订单不存在 |
| 2004 | 订单状态非法 |
| 2005 | 签名验证失败 |
| 2006 | 重复回调 |
| 2007 | 退款失败 |
| 2008 | 支付通道未启用 |
| 2009 | 金额不匹配 |
| 2010 | 订单超时 |

**设备/心跳模块 (3001-3999)**
| 错误码 | 含义 |
|---|---|
| 3001 | 设备指纹采集失败 |
| 3002 | 设备已封禁 |
| 3003 | IP 已被封禁 |
| 3004 | 并发会话超限 |
| 3005 | 心跳超时 |
| 3006 | 设备数超限 |
| 3007 | 频率超限 |
| 3008 | 防爆破触发 |

**云函数模块 (5001-5999)**
| 错误码 | 含义 |
|---|---|
| 5001 | 云函数不存在 |
| 5002 | 云函数已禁用 |
| 5003 | 代码体积超限 |
| 5004 | 执行超时 |
| 5005 | 运行时错误 |
| 5006 | 输入超限 |
| 5007 | 输出超限 |
| 5008 | 编译失败 |
| 5009 | 函数名已存在 |
| 5010 | 内存超限 |
| 5011 | 并发锁获取失败 |
| 5012 | 参数校验失败 |

### 3.3 SDK 接口签名（HMAC-SHA256 + RSA 混合）
```
请求头：
  X-App-Key: {appKey}
  X-Timestamp: {13位时间戳}
  X-Nonce: {UUID}
  X-Signature: {HMAC-SHA256 签名}
  X-Device-Id: {设备指纹哈希}
  X-Card-Cipher: {RSA 加密的卡密密文}  # 仅卡密接口

签名原文：
  METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256

校验规则：
  1. 时间戳 ±300s 内有效
  2. Nonce 5分钟内不可重复（Redis 缓存）
  3. 签名匹配
  4. 设备指纹未被封禁
```

## 4. 提交规范

### 4.1 Commit Message
```
<type>(<scope>): <subject>

type: feat|fix|docs|style|refactor|test|chore|security
scope: license|pay|crypto|sdk|ui|deploy
```

示例：
```
feat(pay): 实现彩虹易支付V1适配器
fix(crypto): 修复AES-GCM nonce重复问题
security(pay): 异步回调增加幂等校验
docs: 更新PROJECT.md数据库表设计
```

### 4.2 分支策略
- `main` 生产分支
- `dev` 开发分支
- `feat/*` 功能分支
- `fix/*` 修复分支
- `security/*` 安全修复分支（高优先级）

## 5. 测试规范
- 单元测试覆盖率 ≥ 70%（核心模块 ≥ 90%）
- 支付/卡密/加密模块必须有集成测试
- SDK 必须有跨语言对接示例测试

## 6. 安全规范

### 6.1 输入校验
- 所有入参使用 `@Validated` 校验
- 字符串入参做 XSS 过滤
- 金额字段强制 BigDecimal

### 6.2 权限控制
- 管理员/开发者/代理/用户四套独立鉴权
- 数据隔离：MyBatis-Plus 多租户插件
- 敏感操作二次确认 + 操作审计

### 6.3 敏感信息
- 商户密钥：AES-256-GCM 加密入库
- 卡密明文：仅在生成时返回一次，库中只存哈希 + 密文
- RSA 私钥：环境变量注入，不入库不入 git
- API 签名密钥：每软件独立，支持轮换

### 6.4 资金安全（重点）
- 订单状态机不可逆（0→1 后不可回 0）
- 异步回调必须验签 + 幂等（订单号 + Redis 锁）
- 退款必须管理员二次确认 + 短信/邮件验证码
- 资金流水永不删除，只允许审计查询
- 卡密发放与支付成功必须事务一致（同一事务内：更新订单 → 生成卡密绑定关系）
- 退款后卡密立即失效（状态置为 3，所有 session 立即踢出）

### 6.5 卡密安全（重点）
- 卡密生成后立即 AES-256-GCM 加密入库
- 卡密明文展示仅一次（生成响应 + 用户首次查询）
- 卡密传输必须 RSA-2048 加密（客户端公钥加密，服务端私钥解密）
- 卡密查询接口频率限制（同 IP 每分钟 ≤ 10 次）
- 卡密封禁后所有相关 session 立即失效
- 卡密生成采用密码学安全随机数（SecureRandom），禁用 Math.random()

### 6.6 加密算法规范
| 用途 | 算法 | 密钥长度 | 模式 |
|---|---|---|---|
| 卡密入库加密 | AES | 256 | GCM |
| 卡密传输加密 | RSA | 2048 | OAEP |
| 接口签名 | HMAC-SHA256 | 256 | - |
| 易支付签名 | MD5 | 128 | - |
| 设备指纹 | SHA-256 | 256 | - |
| 卡密哈希索引 | SHA-256 | 256 | - |
| 国密可选-对称 | SM4 | 128 | CBC |
| 国密可选-非对称 | SM2 | 256 | - |
| 国密可选-摘要 | SM3 | 256 | - |

### 6.7 防破解措施
- 设备指纹绑定（CPU+主板+硬盘+网卡+BIOS 五维哈希）
- IP 限制（单 IP 并发会话数）
- 并发会话数限制（防一号多开）
- 防爆破（连续失败 N 次封禁 IP/设备）
- 防抓包（HTTPS + 一次一密封包 + 时间戳防重放）
- 加密通信（RSA+AES 混合加密）
- 云变量（核心参数云端化，客户端无敏感配置）
- 云函数（关键算法服务端执行，客户端只调用）

### 6.8 云函数沙箱安全（重点，v0.4.2）

云函数是抗破解的终极方案——关键算法在服务端 LuaJ 沙箱执行，客户端只调用。沙箱安全是核心难点，采用三层防护：

**第一层：全局表裁剪（禁用危险函数）**
- 禁用 `os` / `io` / `loadfile` / `dofile` / `require` / `debug` / `package` / `load`，全部设为 `LuaValue.NIL`
- 仅保留 `BaseLib` / `MathLib` / `StringLib` / `TableLib`
- 禁用 `load` 禁止动态编译字符串，所有 Lua 代码必须在源码顶层定义
- `LuaC.install(globals)` 必须在 BaseLib 加载之前，确保 `globals.load()` 能编译用户代码

**第二层：超时强制中断**
- 独立 `jicek-lua-sandbox` daemon 线程池（4 核心/16 最大/64 队列/CallerRunsPolicy），与业务线程池隔离
- `Future.get(timeoutMs)` 控制执行时长，超时后 `future.cancel(true)` 强制中断线程
- 超时上限 `CF_MAX_TIMEOUT_MS`(30000ms)，默认 `CF_DEFAULT_TIMEOUT_MS`(3000ms)，禁固定值（铁律 04）
- 线程必须为 daemon，防止 JVM 退出受阻

**第三层：输出大小硬截断**
- 输入大小：DTO `@Size(max=262144)` + Service 层二次校验实际字节数 ≤ `maxInputKb * 1024`
- 输出大小：`luaValueToJson()` 序列化后超过 `maxOutputKb * 1024` 直接截断
- 错误信息：`truncateError()` 截断至 `CF_ERROR_MSG_MAX_BYTES`(4KB)
- 绝对上限 `CF_ABSOLUTE_IO_KB`(256KB)，单函数配置不可超过此值

**审计铁律（不可篡改）**
- `jicek_cloud_function_log` 表仅允许 INSERT + SELECT，Service 层禁 UPDATE/DELETE
- 每次执行（成功/失败）必须落审计日志，含 invokeSource/callerIp/inputSize/outputSize/durationMs/status/errorMessage
- 审计日志写入失败不应阻断主流程（invoke 已返回结果给客户端），但需记录 ERROR 日志
- 执行状态码：0成功 / 1编译失败 / 2运行时错误 / 3超时 / 4内存超限 / 5输入超限 / 6输出超限

**输入注入契约**
- 通过 `jicek.input` 全局变量传入字符串（非 JSON 对象，由 Lua 代码自行解析）
- Lua 代码 `return` 返回值由 `luaValueToJson()` 递归序列化为 JSON
- table 自动判断数组 vs 对象：key 为 1..n 连续正整数则为数组，否则为对象

**异常映射（错误码 → 审计状态码）**
| 异常 | ResultCode | 审计 status |
|---|---|---|
| LuaError（编译期） | CF_COMPILE_FAIL(5008) | 1 |
| LuaError（运行期） | CF_RUNTIME_ERROR(5005) | 2 |
| TimeoutException | CF_TIMEOUT(5004) | 3 |
| OutOfMemoryError | CF_MEMORY_LIMIT(5010) | 4 |
| 输入超限 | CF_INPUT_TOO_LARGE(5006) | 5 |
| 输出超限 | CF_OUTPUT_TOO_LARGE(5007) | 6 |

## 7. 部署规范

### 7.1 环境变量（不入 git）
```bash
# 数据库
MYSQL_HOST=
MYSQL_PORT=3306
MYSQL_DATABASE=jicek
MYSQL_USERNAME=
MYSQL_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=6379
REDIS_PASSWORD=

# 加密密钥（必须通过环境变量注入）
JICEK_AES_KEY=          # AES-256 主密钥
JICEK_RSA_PRIVATE_KEY=  # RSA 私钥
JICEK_HMAC_KEY=         # HMAC 签名主密钥

# GitHub Webhook
GITHUB_WEBHOOK_SECRET=

# 宝塔面板（可选）
BTPANEL_API_URL=
BTPANEL_API_KEY=
```

### 7.2 Docker 部署
```yaml
# docker-compose.yml 核心
services:
  jicek-app:
    image: jicek/license:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=jicek-mysql
      - REDIS_HOST=jicek-redis
      - GITHUB_WEBHOOK_SECRET=${GITHUB_WEBHOOK_SECRET}
      - JICEK_AES_KEY=${JICEK_AES_KEY}
      - JICEK_RSA_PRIVATE_KEY=${JICEK_RSA_PRIVATE_KEY}
    depends_on:
      - jicek-mysql
      - jicek-redis
```

### 7.3 配置文件
- `application.yml`：通用配置（可入 git）
- `application-prod.yml`：生产配置（不入 git，仅环境变量覆盖）
- 所有敏感信息必须走环境变量，禁硬编码（铁律 04）

## 8. UI 规范

### 8.1 禁用项
- 禁用 emoji / 表情符号 / 图标化装饰
- 禁用毛玻璃 / backdrop-filter / 玻璃拟态
- 禁用暗黑风格
- 禁用大渐变 / 炫光 / 霓虹 / 3D 凸起
- 禁用 Lorem Ipsum / "示例文字" / "TODO" 占位文案

### 8.2 色彩
| 用途 | 色值 |
|---|---|
| 主背景 | #FFFFFF |
| 次背景 | #F7F8FA |
| 主色 | #1A4D8F |
| 成功 | #2E7D5B |
| 警告 | #B8860B |
| 危险 | #B23A3A |
| 主文字 | #1F2937 |
| 次文字 | #6B7280 |
| 边框 | #E5E7EB |

### 8.3 组件
- 按钮：圆角 6px，扁平 + 轻微阴影
- 卡片：白底 + 1px 边框，圆角 8px
- 表格：行高 48px，斑马纹
- 导航：左侧 220px，选中态左 3px 边框

## 9. 文档规范

### 9.1 文件位置
- CHANGELOG.md → 根目录
- TODO.md → 根目录
- README.md → 根目录（GitHub 介绍）
- PROMPT.md → 根目录（下一个 AI 接手指南）
- PROJECT.md → docs/
- SPEC.md → docs/
- UI-DESIGN.md → docs/
- 接口文档 → docs/api/
- 部署文档 → docs/deploy/

### 9.2 更新时机
- 每次提交必须更新 CHANGELOG.md
- 功能变更必须同步更新 PROJECT.md
- 规范变更必须同步更新 SPEC.md
- 任务完成必须勾选 TODO.md
- 交接时必须生成/更新 README.md + PROMPT.md

### 9.3 版本规范
- 遵循 SemVer：MAJOR.MINOR.PATCH
- 0.x.x：开发阶段，API 不稳定
- 1.0.0：首个正式版
