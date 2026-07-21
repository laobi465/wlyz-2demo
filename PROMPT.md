# PROMPT.md - 极策k网络验证 下一个 AI 接手指南

> 本文档专为下一个接手本项目的 AI 编写。请务必先读完本文件，再开始任何代码修改。
> 项目遵循 `web-project-flow` skill 的铁律三件套（04 禁硬编码 / 06 防幻觉 / 13 严格遵循项目文档规范），违反任一即重写。

## 0. 阅读顺序（强制）

接手本项目时，**必须按以下顺序读完**，再动手：

1. 本文件（PROMPT.md）
2. [README.md](README.md) - 项目总览
3. [docs/PROJECT.md](docs/PROJECT.md) - 架构/模块/数据流/数据库表
4. [docs/SPEC.md](docs/SPEC.md) - 代码/接口/安全规范
5. [docs/UI-DESIGN.md](docs/UI-DESIGN.md) - UI 设计规范
6. [CHANGELOG.md](CHANGELOG.md) - 已完成版本历史
7. [TODO.md](TODO.md) - 待办任务（P0 已完成，P1 进行中）
8. [jicek-license/src/main/resources/sql/jicek_init.sql](jicek-license/src/main/resources/sql/jicek_init.sql) - 数据库 DDL
9. [jicek-license/src/main/resources/application.yml](jicek-license/src/main/resources/application.yml) - 配置项

**未读完上述文档前，禁止写任何业务代码**（铁律 13 第一步）。

## 1. 项目快照

| 项 | 值 |
|---|---|
| 项目名 | 极策k网络验证 |
| 当前版本 | v0.2.0-SNAPSHOT |
| 仓库 | https://github.com/laobi465/wlyz-2demo |
| 技术栈 | Spring Boot 3.4.6 + MyBatis-Plus 3.5.12 + Redisson + Vue3 + TS + Element Plus 2.9.8 |
| 部署 | Docker（普通 / 宝塔面板） |
| 支付 | 彩虹易支付 V1（独立部署，仅 V1，无 V2） |
| 加密 | AES-256-GCM + RSA-2048-OAEP + HMAC-SHA256（可选国密 SM2/SM4） |

## 2. 已完成模块（v0.2.0）

### 后端（jicek-license）

| 模块 | 路径 | 状态 |
|---|---|---|
| 启动类 | `com.jicek.license.JicekLicenseApplication` | ✅ |
| 通用层 | `common/` (R / ResultCode / ServiceException / GlobalExceptionHandler / JicekConstants) | ✅ |
| 配置层 | `config/` (JicekProperties / MybatisPlusConfig / CorsConfig) | ✅ |
| 加密层 | `crypto/` (AesCryptoService / RsaCryptoService / HmacSignService / Md5SignService / CryptoConfiguration) | ✅ |
| 卡密模块 | `card/` (entity / mapper / dto / generator / service / controller) | ✅ |
| 支付模块 | `pay/` (entity / mapper / dto / adapter / service / controller) | ✅ |
| 资金事务 | `transaction/PaymentTransactionService` | ✅ |
| 控制台 | `dashboard/controller/DevDashboardController` | ✅ |

### 前端（jicek-ui）

| 模块 | 路径 | 状态 |
|---|---|---|
| 入口 | `src/main.ts` + `src/App.vue` | ✅ |
| 路由 | `src/router/index.ts` | ✅ |
| API 客户端 | `src/api/request.ts` + `src/api/index.ts` | ✅ |
| 全局样式 | `src/styles/jicek.scss` | ✅ |
| 布局 | `src/layout/DevLayout.vue` | ✅ |
| 公共组件 | `src/components/jicek/` (StatusTag / AmountInput / ConfirmDialog) | ✅ |
| 控制台页 | `src/views/dev/dashboard/` | ✅ |
| 卡密生成页 | `src/views/dev/card-key-gen/` | ✅ |
| 卡密查询页 | `src/views/dev/card-key-list/` | ✅ |
| 支付配置页 | `src/views/dev/pay-config/` | ✅ |
| 资金流水页 | `src/views/dev/pay-order/` | ✅ |

## 3. 待办任务（按优先级）

详见 [TODO.md](TODO.md)，摘要：

### P1（高，v0.3.0 计划）
- **8 语言客户端 SDK**：Java / C# / Python / Go / Node.js / C++ / 易语言 / Lua / Shell
  - 每个 SDK 必须有三件套：签名（HMAC-SHA256）+ 心跳（动态间隔）+ 卡密验证
- **设备指纹采集与绑定**：CPU + 主板 + 硬盘 + 网卡 + BIOS 哈希融合
  - 注意防伪造，需考虑虚拟机/容器场景
- **前端补全**：
  - 软件管理页面、卡类管理页面、用户管理页面、设备管理页面、代理管理页面
  - ECharts 数据统计图表
  - H5 终端用户页面（购卡/续费/换机/在线设备）

### P1（高，v0.4.0 计划）
- **多级代理 + 分润**：
  - 代理树形结构存储
  - 分润比例配置
  - 制卡扣余额（事务）
  - 提现申请工作流（WarmFlow）
  - 提现审核 + 二次确认

### P2（中）
- 云函数远程执行（沙箱：Lua/LuaJIT 或 GraalVM）
- 数据统计与可视化（验证量趋势、设备热力图、收入多维统计）

### P3（低）
- GitHub 自动更新部署（Webhook + 自动重启 + 管理员弹窗）
- 工单系统
- 多语言国际化

## 4. 编码铁律（HARD，违反即重写）

### 4.1 铁律 04 - 禁硬编码

- 禁止硬编码任何密钥、商户号、域名、IP、超时参数、倍率
- 全部走 `JicekProperties` 环境变量注入或数据库配置项
- 禁止假数据、占位符（`// TODO`、`pass`、`Lorem Ipsum`、`your_api_key_here`）
- 测试数据必须标注 `// 仅本地测试模拟`

### 4.2 铁律 06 - 防幻觉

- 不确定说"不知道"或标注「待核实」
- 禁止虚构接口、表、字段、返回结构
- 资料缺失时直接列出"需要用户补充的资料清单"
- 回答末尾说明可信度

### 4.3 铁律 13 - 严格遵循项目文档规范

- 写代码前必须读完 README / 接口文档 / 数据库结构 / 编码规范
- 命名/分层/注释/异常格式/响应体完全对标现有源码
- 错误码沿用 `ResultCode` 枚举，禁止自创数字状态码
- 新增功能兼容旧逻辑，改动稳定代码前必须告知风险
- 完成代码后主动自检合规校验清单

### 4.4 铁律 09 - 文档联动

任何变更必须同步更新四份核心文档：
- `CHANGELOG.md` - 语义化版本号 + 分类条目
- `docs/PROJECT.md` - 架构/模块/功能清单
- `docs/SPEC.md` - 代码/接口/安全规范
- `TODO.md` - P0/P1/P2/P3 任务清单

## 5. 架构要点

### 5.1 分层

```
Controller (REST) → Service (业务) → Mapper (MyBatis-Plus) → MySQL
                       ↓
                  CryptoService (AES/RSA/HMAC/MD5)
                       ↓
                  Redisson (分布式锁/缓存)
```

### 5.2 资金安全核心（不可破坏）

**`PaymentTransactionService` 是资金安全的核心，修改前必须告知用户风险：**

```java
@Transactional(rollbackFor = Exception.class)
public void processPaymentSuccess(PayOrder order, PayNotifyDTO notify) {
    // 1. 订单状态流转：0 → 1（必须先流转，幂等检查在此）
    // 2. 生成 N 张卡密并加密入库（N = order.quantity）
    // 3. 任意一步失败，整个事务回滚
}
```

- 订单状态流转与卡密发放必须在**同一事务**内
- 退款时关联卡密必须同步失效（status=3）
- 禁止伪异步（不能用 `@Async` 跳过事务边界）
- 卡密发放必须真实写入数据库，不能只写日志

### 5.3 异步回调幂等（不可破坏）

**`PayNotifyService.handleNotify` 流程顺序不可调换：**

1. 加 Redisson 分布式锁（按 outTradeNo）
2. 解析回调数据
3. 幂等检查（订单 status != 0 直接返回 success）
4. 获取支付配置
5. MD5 验签
6. 校验 trade=TRADE_SUCCESS
7. 调用 `PaymentTransactionService.processPaymentSuccess`
8. 返回纯字符串 `success`（无 BOM、无空格、无 JSON 包裹）

### 5.4 卡密安全（不可破坏）

- 生成用 `SecureRandom`，禁用 `Math.random`
- 入库用 AES-256-GCM 加密，存 `card_cipher` 字段
- 查询索引用 SHA-256 哈希，存 `card_hash` 字段
- 明文仅在生成响应中展示一次，列表/查询接口必须脱敏
- IV 每次随机，不复用

### 5.5 多租户隔离

- MyBatis-Plus `TenantLineInnerInterceptor` 全局拦截
- 所有业务表必须有 `tenant_id` 字段
- Controller 层接收 `tenantId` 参数（后续接入 Sa-Token 后从 token 提取）

## 6. 关键配置项

### 6.1 环境变量（敏感，禁硬编码）

| 变量名 | 用途 |
|---|---|
| `JICEK_AES_KEY` | AES-256 主密钥（Base64，32 字节） |
| `JICEK_RSA_PRIVATE_KEY` | RSA 私钥（Base64，PKCS#8） |
| `JICEK_RSA_PUBLIC_KEY` | RSA 公钥（Base64，X.509） |
| `JICEK_HMAC_KEY` | HMAC-SHA256 密钥（Base64，32 字节） |
| `MYSQL_HOST` / `MYSQL_PASSWORD` | 数据库 |
| `REDIS_HOST` / `REDIS_PASSWORD` | Redis |

### 6.2 业务配置（application.yml）

- `jicek.pay.notify-url-prefix` - 异步回调 URL 前缀
- `jicek.pay.notify.lock-prefix` - 回调锁前缀
- `jicek.pay.notify.lock-timeout` - 锁超时秒数
- `jicek.pay.order.timeout-minutes` - 订单超时分钟数

## 7. API 路由表

### 7.1 开发者 API（`/api/dev/*`）

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 控制台 | GET | `/api/dev/dashboard/summary` | 汇总数据（需传 tenantId） |
| 卡密 | POST | `/api/dev/card/generate` | 批量生成（最多 1000 张） |
| 卡密 | GET | `/api/dev/card/query` | 按卡号查询 |
| 卡密 | POST | `/api/dev/card/ban` | 封禁 |
| 卡密 | POST | `/api/dev/card/refund` | 退款并失效 |
| 卡类 | GET | `/api/dev/card-type/page` | 分页查询 |
| 卡类 | POST | `/api/dev/card-type` | 新建 |
| 卡类 | PUT | `/api/dev/card-type/{id}` | 更新 |
| 卡类 | DELETE | `/api/dev/card-type/{id}` | 删除 |
| 支付 | GET | `/api/dev/pay/config/{tenantId}` | 获取配置 |
| 支付 | POST | `/api/dev/pay/config` | 保存配置 |
| 支付 | POST | `/api/dev/pay/create` | 发起支付 |
| 支付 | GET | `/api/dev/pay/order/page` | 订单分页 |
| 支付 | POST | `/api/dev/pay/refund` | 退款 |

### 7.2 公开回调

| 方法 | 路径 | 返回 |
|---|---|---|
| GET / POST | `/pay/notify/{tenantId}` | 纯字符串 `success` |

## 8. 数据库表（核心）

| 表名 | 关键字段 | 说明 |
|---|---|---|
| `jicek_pay_config` | `merchant_key`（AES 加密存储） | 支付配置 |
| `jicek_pay_order` | `status` (0/1/2/3/4) | 支付订单（5 状态机） |
| `jicek_card_type` | `type` (1/2/3/4) | 卡类（时长/次数/功能/永久） |
| `jicek_card_key` | `card_cipher`（AES） + `card_hash`（SHA-256） | 卡密（加密存储） |
| `jicek_software` | `app_key` + `sign_secret`（AES） | 软件 |
| `jicek_device` | `device_fingerprint` | 设备（指纹哈希） |
| `jicek_agent` | `parent_id` + `balance` | 代理（多级树形） |

完整 DDL 见 `jicek_init.sql`。

## 9. 前端规范

### 9.1 风格

- 现代简约（详见 `docs/UI-DESIGN.md`）
- 主色：极策蓝 `#1A4D8F`
- 禁用：emoji / 毛玻璃 / 暗黑风格 / 夸张渐变
- 布局：220px 左侧导航 + 60px 顶栏 + 主内容区

### 9.2 公共组件

| 组件 | 用途 |
|---|---|
| `StatusTag.vue` | 状态标签（订单/卡密两种模式） |
| `AmountInput.vue` | 金额输入（decimal.js 精度） |
| `ConfirmDialog.vue` | 二次确认弹窗（资金/卡密操作） |

### 9.3 API 调用约定

```typescript
import { dashboardApi, cardKeyApi, cardTypeApi, payApi } from '@/api'

// 统一响应：{ code, msg, data }
// 拦截器自动剥 data，失败自动 ElMessage.error
const data = await dashboardApi.summary(tenantId)
```

## 10. 开发流程

### 10.1 新增功能

1. 读 `TODO.md` 确认任务优先级
2. 读 `docs/SPEC.md` 确认接口/规范
3. 读 `jicek_init.sql` 确认表结构（如需新表，先改 DDL）
4. 写代码（后端：entity → mapper → service → controller；前端：api → view）
5. 自检合规清单（铁律 13 第七条）
6. 更新四份核心文档
7. 提交（conventional commit）+ 推送

### 10.2 修改资金/卡密相关代码

**必须先告知用户风险，经用户确认后再改。** 涉及文件：
- `PaymentTransactionService.java`
- `PayNotifyService.java`
- `PayOrderStateMachineService.java`
- `CardKeyService.java`
- `CardKeyGenerator.java`

### 10.3 Commit 规范

```
<type>(<scope>): <subject>

<body>
```

- type: feat / fix / docs / style / refactor / test / chore
- scope: 模块名（如 pay / card / ui / docs）
- 示例：`feat(pay): 新增退款接口` / `fix(card): 修复卡密生成并发问题`

## 11. 常见陷阱

1. **彩虹易支付 V1 签名**：MD5，参数按 ASCII 升序拼接，最后拼 `商户密钥`，全大写。**不是** RSA-SHA256（那是幻觉，V1 只有 MD5）。
2. **回调返回值**：必须纯字符串 `success`，不能是 JSON `{"code":200}`，不能有 BOM/空格/换行。
3. **金额精度**：后端 `BigDecimal`，前端 `decimal.js`，禁用 `number` 直接运算。
4. **卡密明文**：仅生成响应中返回，其他接口必须脱敏（如 `JC-****-****-****`）。
5. **多租户**：所有查询必须带 `tenant_id`，不能跨租户查询。
6. **设备指纹**：必须多维哈希融合，单维度易伪造（v0.3.0 实现）。
7. **心跳间隔**：动态 5-300s，禁固定值（v0.3.0 实现）。

## 12. 验证清单

写完代码后，逐条自检：

- [ ] 是否读完项目文档？（铁律 13 第一步）
- [ ] 是否有硬编码密钥/域名/超时？（铁律 04）
- [ ] 是否有假数据/占位符？（铁律 04）
- [ ] 是否虚构接口/表/字段？（铁律 06）
- [ ] 命名/分层/注释是否对标现有源码？（铁律 13 第四条）
- [ ] 错误码是否沿用 `ResultCode` 枚举？（铁律 13 第六条）
- [ ] 资金/卡密相关改动是否告知用户风险？（铁律 13 第五条）
- [ ] 四份核心文档是否同步更新？（铁律 09）
- [ ] UI 是否符合现代简约规范？（铁律 03）
- [ ] 敏感字段是否加密存储？（卡密 AES、商户密钥 AES）

---

**最后提醒**：本项目是资金相关系统，任何改动都要谨慎。不确定就说"待核实"，不要脑补。改动前先备份，改动后先自检。
