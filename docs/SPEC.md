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
- SDK 接口 (`controller/sdk`) 必须独立鉴权（HMAC-SHA256 签名），不依赖 JWT 后台登录态
- 后台鉴权采用 JJWT 0.12.6（HMAC-SHA256），替代原 SPEC 描述但实际未引入的 Sa-Token；密钥环境变量 `JICEK_JWT_SECRET` 注入（≥ 32 字节）

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
| 1001-1999 | 极策k 卡密/软件模块 |
| 2001-2999 | 极策k 支付模块 |
| 3001-3999 | 极策k 设备/心跳/SDK 鉴权模块 |
| 4001-4999 | 极策k 代理/分润/提现模块 |
| 5001-5999 | 极策k 云函数模块 |
| 6001-6999 | 极策k 数据统计模块 |
| 7001-7999 | 极策k 部署模块 |
| 8001-8999 | 极策k 工单模块 |
| 9001-9999 | 极策k 鉴权模块 |

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

**数据统计模块 (6001-6999)**
| 错误码 | 含义 |
|---|---|
| 6001 | 统计粒度非法（仅支持 hour/day/month） |
| 6002 | 统计维度非法（仅支持 channel/cardType/agent） |
| 6003 | 统计时间范围超过最大限制（90 天） |
| 6004 | 统计参数非法 |

**部署模块 (7001-7999)**
| 错误码 | 含义 |
|---|---|
| 7001 | 部署锁获取失败（已有部署进行中） |
| 7002 | Webhook 签名验证失败 |
| 7003 | Webhook 事件类型非法（仅 push） |
| 7004 | Webhook Secret 未配置 |
| 7005 | git pull 失败 |
| 7006 | 构建（mvn/npm）失败 |
| 7007 | 重启失败（容器名缺失 / 宝塔 API 错误） |
| 7008 | 健康检查失败（超时未恢复） |
| 7009 | 回滚失败 |
| 7010 | 部署参数非法（功能未启用 / tenantId 缺失等） |

**工单模块 (8001-8999)**
| 错误码 | 含义 |
|---|---|
| 8001 | 工单不存在 |
| 8002 | 工单状态非法，无法操作 |
| 8003 | 工单已关闭，无法回复 |
| 8004 | 无权操作该工单 |
| 8005 | 工单目标非法（仅支持 1开发者 2管理员） |
| 8006 | 工单分类非法 |
| 8007 | 工单内容超过长度限制 |
| 8008 | 回复内容不能为空 |
| 8009 | 创建者类型非法 |
| 8010 | 工单参数非法 |

**软件模块 (1012-1019)**
| 错误码 | 含义 |
|---|---|
| 1012 | 软件不存在 |
| 1013 | 同租户下软件名称已存在 |
| 1014 | 软件下存在卡类，无法删除 |
| 1015 | 软件下存在设备，无法删除 |
| 1016 | 软件下存在云函数，无法删除 |
| 1017 | 软件已禁用 |
| 1018 | 软件参数非法 |
| 1019 | 无权操作该软件（租户隔离） |

**公告模块 (1021-1029)**
| 错误码 | 含义 |
|---|---|
| 1021 | 公告不存在 |
| 1022 | 公告标题不能为空 |
| 1023 | 公告内容不能为空 |
| 1024 | 公告所属软件无效或无权操作 |
| 1025 | 公告状态非法 |
| 1026 | 公告类型非法 |
| 1027 | 公告已发布，不能重复发布 |
| 1028 | 公告已下线 |
| 1029 | 公告未发布，不能下线 |

**鉴权模块 (9001-9999)**
| 错误码 | 含义 |
|---|---|
| 9001 | Token 缺失（未登录） |
| 9002 | Token 无效（被篡改/过期/密钥未配置） |
| 9003 | Token 角色不匹配（@AuthRequired(role) 校验失败） |
| 9004 | 用户不存在 |
| 9005 | 密码错误（用户不存在也返回此码，防枚举） |
| 9006 | 用户已被封禁（status=0） |
| 9007 | 用户已存在（注册/创建时重复） |
| 9008 | 原密码错误（修改密码时） |
| 9009 | 新密码长度不足（< 8 位） |
| 9010 | 角色非法 |
| 9011 | 无权限（@AuthRequired(role) 校验失败时的业务语义） |

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
- 管理员/开发者/代理/用户四套独立鉴权（v0.7.0 已实现开发者 + 管理员 JWT 鉴权）
- 数据隔离：MyBatis-Plus 多租户插件
- 敏感操作二次确认 + 操作审计

### 6.2.1 JWT 鉴权框架（v0.7.0）

- **签名算法**：HMAC-SHA256（JJWT 0.12.6），密钥环境变量 `JICEK_JWT_SECRET`（≥ 32 字节），未配置时 warn 但不阻止启动
- **JWT claims**：`uid` / `role`（1开发者 2管理员）/ `tenantId`（管理员为 null）/ `username` / `iss`=jicek-license / `iat` / `exp`（默认 24h）
- **传输**：HTTP 头 `Authorization: Bearer {token}`
- **密码哈希**：BCrypt（Hutool `cn.hutool.crypto.digest.BCrypt`，cost=10）
- **鉴权方式**：`@AuthRequired` 注解（方法级优先，类级兜底；`role()` 默认 0=任意已登录用户，`role=2` 限制管理员）；未标注的接口**放行**（渐进式兼容现有裸传参数接口）
- **上下文**：`AuthContext` ThreadLocal 持有当前用户身份，`JwtAuthInterceptor.afterCompletion` **必须** `AuthContext.clear()` 防线程池串号
- **拦截路径**：`/api/dev/**` + `/api/admin/**`；排除 `/api/auth/**` + `/api/sdk/**` + `/api/h5/**` + `/api/pay/notify/**` + `/api/deploy/webhook` + `/actuator/**`
- **防枚举**：登录失败（用户不存在 + 密码错误）统一返回 `AUTH_PASSWORD_ERROR`(9005)
- **默认账号**：admin/admin@123（超管）、dev/dev@123（tenantId=1）

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

### 6.9 部署安全（重点，v0.5.0）

GitHub Webhook 自动更新是高风险操作（执行 git pull / 构建 / 重启），安全设计必须严格：

**第一层：Webhook 签名验证**
- GitHub 发送 `X-Hub-Signature-256: sha256=<hex>` 头
- 服务端用 `GITHUB_WEBHOOK_SECRET` 对 body 做 HMAC-SHA256，hex 编码后与 header 比较
- 比较必须用 `MessageDigest.isEqual`（常量时间），禁用 `String.equals`（时序攻击风险）
- 签名前缀 `sha256=` 校验通过后再截取 hex 部分（`DEPLOY_WEBHOOK_SIGNATURE_PREFIX`）
- Secret 未配置直接抛 `DEPLOY_SECRET_NOT_CONFIGURED`(7004)，禁空 Secret 通过

**第二层：分布式锁防并发**
- Redisson 锁 `jicek:deploy:lock`，5 分钟自动释放防死锁（`DEPLOY_LOCK_TIMEOUT_SECONDS`）
- Webhook 与 manual 共用同一锁，获取失败抛 `DEPLOY_LOCK_FAIL`(7001)
- 锁防止 GitHub 重试（10s 超时）+ 手动触发并发执行

**第三层：异步执行 + 审计**
- Webhook 立即返回 `accepted(deployLogId, message)`，部署在 daemon 线程 `jicek-deploy-{logId}` 异步执行
- daemon 线程防 JVM 退出受阻
- `jicek_deploy_log` 表仅 INSERT + SELECT + 受控更新 status（0→1/2/3），禁 UPDATE 其他字段 / DELETE 任意记录
- 审计失败不阻断主流程，但记录 ERROR 日志

**第四层：备份 + 回滚**
- 部署前备份 jar + dist 到 `.jicek-backup/{timestamp}/`
- 保留最近 `DEPLOY_BACKUP_KEEP_COUNT`(3) 个备份，清理旧备份防磁盘膨胀
- 任一步骤失败（git pull / build / restart / healthCheck）触发 `rollback()`：还原最近备份 → restart → 标记 status=3(ROLLED_BACK)

**第五层：外部命令安全执行**
- 禁用 `Runtime.exec`（参数拼接易 shell 注入）
- 统一用 `ProcessBuilder` 参数化执行 git / mvn / npm / docker 命令
- `redirectErrorStream(true)` 合并 stderr 到 stdout 便于日志收集
- 工作目录显式设置（`directory(rootDir)`）

**配置铁律**
- `jicek.deploy.enabled` 默认 false（防开发环境误触发），生产开启需显式 `JICEK_DEPLOY_ENABLED=true`
- 所有敏感配置（Webhook Secret / 宝塔 API Key）必须走环境变量注入（铁律 04）
- 错误信息截断至 `DEPLOY_ERROR_MSG_MAX_BYTES`(4KB)，防数据库字段溢出
- 健康检查 URL + 项目根目录 + 重启模式 + 容器名全部可配置，禁硬编码

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

# 部署配置（v0.5.0 新增）
JICEK_DEPLOY_ENABLED=false         # 部署功能开关（默认 false，生产开启）
JICEK_DEPLOY_PROJECT_ROOT=/workspace  # 项目根目录
JICEK_DEPLOY_RESTART_MODE=none     # 重启模式：docker/btpanel/none
JICEK_DEPLOY_DOCKER_CONTAINER=jicek-app  # Docker 容器名
JICEK_DEPLOY_HEALTH_URL=http://127.0.0.1:8080  # 健康检查 URL
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
