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
| 当前版本 | v0.16.0 |
| 仓库 | https://github.com/laobi465/wlyz-2demo |
| 技术栈 | Spring Boot 3.4.6 + MyBatis-Plus 3.5.12 + Redisson + Vue3 + TS + Element Plus 2.9.8 |
| 部署 | Docker（普通 / 宝塔面板）+ GitHub Webhook 自动更新 |
| 支付 | 彩虹易支付 V1（独立部署，仅 V1，无 V2） |
| 加密 | AES-256-GCM + RSA-2048-OAEP + HMAC-SHA256（可选国密 SM2/SM4） |
| SDK | 8 语言（Java/Python/Node.js/Go/C#/C++/Lua/Shell + 易语言），统一契约见 sdk/README.md |

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
| 设备模块 | `device/` (entity/mapper/dto/fingerprint/service/controller) | ✅ v0.3.0 |
| 客户端 SDK | `sdk/` (java/python/nodejs/go/csharp/cpp/lua/shell/epl) | ✅ v0.3.1 |
| 代理模块 | `agent/` (entity/mapper/dto/service/controller) | ✅ v0.4.0 |
| 云函数模块 | `cloudfunc/` (entity/mapper/dto/sandbox/service/controller) | ✅ v0.4.2 |
| 数据统计模块 | `stats/` (dto/service/controller) | ✅ v0.4.3 |
| 部署模块 | `deploy/` (entity/mapper/dto/service/controller) | ✅ v0.5.0 |
| 工单模块 | `ticket/` (entity/mapper/dto/service/controller) | ✅ v0.6.1（单向：开发者→管理员） |
| 鉴权模块 | `auth/` (entity/mapper/dto/service/interceptor/controller) | ✅ v0.7.0（JWT + @AuthRequired 渐进式） |
| 软件模块 | `software/` (entity/mapper/dto/service/controller) | ✅ v0.8.0（CRUD + 密钥生成/轮换 + 关联校验 + @AuthRequired） |
| SDK 模块 | `sdk/` (auth/dto/service/controller) | ✅ v0.9.0（SdkAuthFilter 签名鉴权 + 卡密登录） |
| 公告模块 | `announcement/` (entity/mapper/dto/service/controller) | ✅ v0.10.0（CRUD + 发布/下线状态机 + SDK 拉取 + 版本范围匹配） |
| 更新包模块 | `update/` (entity/mapper/dto/service/controller) | ✅ v0.11.0（文件上传 + CRUD + 发布/下线 + SDK 检查更新 + 多格式 exe/sh/win/lua/zip/7z） |
| h5/ | `h5/` (entity/mapper/dto/auth/service/controller) | ✅ v0.13.0（H5 终端用户验证界面，X-H5-Token 鉴权 + Redisson 会话） |
| shop/ | `shop/` (entity/mapper/dto/service/controller) | ✅ v0.13.0（内嵌卡网：店铺 + 商品 + H5 下单） |
| agent/ 扩展 | `agent/` (util/InviteCodeGenerator + dto/AgentRegisterDTO) | ✅ v0.13.0（invite_code/invited_by 字段 + H5 公开注册接口 + 邀请码生成器） |
| enduser/ | `enduser/` (entity/mapper/dto/service/controller) | ✅ v0.14.0（终端用户账号体系，H5 账号密码登录复用 H5Session） |
| ticket/controller/AdminTicketController | `ticket/controller/AdminTicketController` | ✅ v0.15.0（管理员工单处理：page/get/reply/close，@AuthRequired(role=2)） |
| auth/controller/AdminDevUserController + auth/service/DevUserService | `auth/controller/AdminDevUserController` + `auth/service/DevUserService` | ✅ v0.15.0（管理员租户管理：page/get/ban/unban/reset-password，@AuthRequired(role=2)） |
| sdk/controller/SdkCloudFunctionController | `sdk/controller/SdkCloudFunctionController` | ✅ v0.16.0（SDK 端云函数调用，POST /api/sdk/cloud-function/invoke，invokeSource="sdk"，SdkAuthFilter 鉴权） |
| crypto/SmCryptoService | `crypto/SmCryptoService` | ✅ v0.16.0（国密 SM2/SM4/SM3 可选实现，@ConditionalOnProperty 默认关闭，不影响现有 AES/RSA） |

### 前端（jicek-ui）

| 模块 | 路径 | 状态 |
|---|---|---|
| 入口 | `src/main.ts` + `src/App.vue` | ✅ |
| 路由 | `src/router/index.ts` | ✅ |
| API 客户端 | `src/api/request.ts` + `src/api/index.ts` | ✅ |
| 全局样式 | `src/styles/jicek.scss` | ✅ |
| 布局 | `src/layout/DevLayout.vue` | ✅ |
| 公共组件 | `src/components/jicek/` (StatusTag / AmountInput / ConfirmDialog) | ✅ |
| 控制台页 | `src/views/dev/dashboard/` | ✅ v0.4.1 集成 ECharts |
| 卡密生成页 | `src/views/dev/card-key-gen/` | ✅ |
| 卡密查询页 | `src/views/dev/card-key-list/` | ✅ |
| 卡类管理页 | `src/views/dev/card-type/` | ✅ v0.4.1 |
| 设备管理页 | `src/views/dev/device/` | ✅ v0.4.1 |
| 支付配置页 | `src/views/dev/pay-config/` | ✅ |
| 资金流水页 | `src/views/dev/pay-order/` | ✅ |
| 代理管理页 | `src/views/dev/agent/` | ✅ v0.4.0 |
| 提现审核页 | `src/views/dev/withdraw/` | ✅ v0.4.0 |
| 云函数管理页 | `src/views/dev/cloud-func/` | ✅ v0.4.2 |
| 数据统计页 | `src/views/dev/stats/` | ✅ v0.4.3 |
| 部署管理页 | `src/views/dev/deploy/` | ✅ v0.5.0 |
| 工单管理页 | `src/views/dev/ticket/` | ✅ v0.6.0 |
| 登录页 | `src/views/dev/login/` | ✅ v0.7.0（租户ID+用户名+密码 + 表单校验） |
| 路由守卫 | `src/router/index.ts` beforeEach | ✅ v0.7.0（无 token 跳 /login） |
| 拦截器鉴权 | `src/api/request.ts` | ✅ v0.7.0（自动注入 Bearer + 401/9001/9002/9003 跳登录） |
| 布局鉴权 | `src/layout/DevLayout.vue` | ✅ v0.7.0（用户昵称头像 + 退出 + 修改密码弹窗） |
| 软件管理页 | `src/views/dev/software/` | ✅ v0.8.0（CRUD + 密钥展示弹窗 + 轮换二次确认） |
| 公告管理页 | `src/views/dev/announcement/` | ✅ v0.10.0（CRUD + 发布/下线状态机 + 软件下拉筛选 + 只读查看） |
| 更新包管理页 | `src/views/dev/update-package/` | ✅ v0.11.0（文件上传进度 + CRUD + 发布/下线 + SHA-256 展示 + 强制更新开关） |
| SDK 代码生成弹窗 | `src/views/dev/software/SdkCodeGenDialog.vue` | ✅ v0.12.0（9 语言一键生成 + 自动填入 appKey/RSA公钥 + 复制） |
| 对接文档页 | `src/views/dev/integration-doc/` | ✅ v0.12.0（接入流程 + 签名算法 + RSA + API + 错误码 + SDK 索引） |
| H5 终端用户页（7 个） | `src/views/h5/*` | ✅ v0.13.0（H5Layout + login + my-card + announcement + agent/register + shop + shop/order） |
| 内嵌卡网管理页 | `src/views/dev/shop/` | ✅ v0.13.0（店铺 CRUD + 商品双层弹窗 + 状态开关） |
| 终端用户管理页 | `src/views/dev/end-user/` | ✅ v0.14.0（CRUD + 封禁 + 重置密码） |
| 多语言国际化 | `src/i18n/` + `src/components/LangSwitch.vue` | ✅ v0.14.0（vue-i18n 9.x 中英文，渐进式改造） |
| 管理员后台 | `src/views/admin/` | ✅ v0.15.0（AdminLayout + login + ticket + dev-user 4 页，jicek_admin_token 隔离 + adminAxios 独立实例） |
| 多语言国际化全量 | `src/i18n/` | ✅ v0.15.0（17 个 dev 页面全量 i18n + 16 个新语言包模块，所有用户可见文案支持中英文切换） |

## 3. 待办任务（按优先级）

详见 [TODO.md](TODO.md)，摘要：

### P1（高，v0.3.1 已完成 ✅）
- **8 语言客户端 SDK**：Java / Python / Node.js / Go / C# / C++ / Lua / Shell + 易语言
  - 统一契约规范：[sdk/README.md](sdk/README.md)
  - 所有 SDK 实现完全一致的接口语义：签名（HMAC-SHA256）+ 心跳（动态间隔+指数退避）+ 卡密验证 + 5 维设备指纹采集
- **设备指纹采集与绑定**：✅ 已完成 v0.3.0

### P1（高，v0.4.0 已完成 ✅）
- **多级代理 + 分润 + 提现**：
  - 代理树形结构存储（parent_id + level + isDescendant 防环）
  - 向上链式分润（直推 type=1 + 父级链 type=2，最多 10 层，同事务原子）
  - 分润撤销（退款触发，余额不足保护）
  - 提现审核状态机（0待审核→1已通过→3已打款 / 0→2已拒绝 / 1→4已失败）
  - 前端代理管理页 + 提现审核页 + API/路由/菜单集成
  - 技术决策：未引入 WarmFlow，采用简单状态机；密码用 Hutool BCrypt

### P1（高，v0.4.1 已完成 ✅）
- **前端补全 - 第一批**（依据铁律 06，仅实现后端 Controller 已存在的页面）：
  - 卡类管理页：CRUD + 4 种卡类型联动表单 + Decimal.js 金额格式化
  - 设备管理页：分页 + 详情弹窗 + 封禁/解封 + 指纹脱敏 + 设备状态组合
  - Dashboard ECharts 集成：卡密状态分布饼图 + 今日收支柱状图
  - StatusTag 扩展：新增 device 类型，现支持 4 种状态语义（order/card/withdraw/device）
  - deviceApi 新增（page/get/ban/unban），含 current→page 参数映射
  - 路由 /card-type + /device；侧边栏卡密管理子菜单 + 用户管理子菜单

### P1（高，v0.5.0 计划）
- **SDK 联调**：接入真实服务端联调测试 + 加壳工具推荐文档（VMProtect/Themida/Enigma）
- **CardKeyService.useCard 完整流程**：DeviceService.bindDevice 接入卡密校验 + Sa-Token 鉴权 + software 表读取签名密钥/心跳间隔
- **分润接入支付回调**：PaymentTransactionService 支付成功时触发 CommissionService.grantCommission
- **代理制卡扣余额**：AgentService.deductBalance 接入代理制卡流程
- **前端补全 - 剩余项**：
  - 软件管理页面（待后端 DevSoftwareController）
  - 用户管理页面（待后端 DevUserController）
  - ~~数据统计扩展图表~~ ✅ v0.4.3 已完成（4 Tab：验证量趋势/设备热力图/收入统计/防破解事件）
  - H5 终端用户页面（购卡/续费/换机/在线设备，待后端 H5 Controller）

### P2（中，v0.4.2 已完成 ✅）
- **云函数远程执行**（抗破解终极方案）：
  - LuaJ 3.0.6 沙箱引擎（纯 Java 实现 Lua 5.4 子集，全局表裁剪 + 超时中断 + 输出截断三层防护）
  - 审计日志不可篡改（jicek_cloud_function_log 仅 INSERT + SELECT）
  - 前端双 Tab 页面（函数列表 + 执行日志）+ 路由/菜单集成
  - SDK 调用走 SdkCloudFunctionController 待后续版本实现（复用同一 Service）

### P2（中，v0.4.3 已完成 ✅）
- **数据统计与可视化**：
  - 4 Tab 页面（验证量趋势折线图 / 设备在线热力图 / 收入统计柱状图+表格 / 防破解事件折线图）
  - 数据源全部基于现有业务表聚合，无独立统计表（铁律 06）
  - 代理维度因 PayOrder 暂无 agent_id 字段，前端 alert 提示「待扩展」
  - Dashboard 页面（v0.4.1）保留为今日汇总快照，数据统计页（v0.4.3）为多维分析入口

### P2（中，待开始）

### P3（低，v0.5.0 已完成 ✅）
- **GitHub 自动更新部署**：
  - Webhook 自动触发（HMAC-SHA256 验签 + 常量时间比较）+ 管理员后台手动触发
  - 部署编排：备份 → git pull → mvn build → npm build → 重启 → 健康检查 → 失败回滚
  - Redisson 分布式锁 + daemon 线程异步执行 + 审计日志（仅 INSERT + SELECT）
  - 重启模式分发：docker / btpanel / none
  - 前端部署管理页（3 状态卡片 + 手动触发 + 日志表格 + 状态轮询）+ 路由 /deploy + 侧边栏「系统设置」子菜单
### P3（低，v0.6.0 已完成工单系统 ✅）
- **工单系统**：双向工单（终端用户→开发者 + 开发者→管理员）+ 状态机 + 分类 + 双 Controller + 前端双 Tab 页面。H5 前端 + 管理员端 Controller 待对应框架就绪后补全
- ✅ 多语言国际化（v0.14.0 已完成，vue-i18n 9.x 中英文 + 渐进式改造）

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
| 代理 | POST | `/api/dev/agent` | 创建代理 |
| 代理 | PUT | `/api/dev/agent` | 更新代理 |
| 代理 | GET | `/api/dev/agent/page` | 代理分页（扁平） |
| 代理 | GET | `/api/dev/agent/tree` | 代理树形（多级） |
| 代理 | GET | `/api/dev/agent/{tenantId}/{agentId}` | 代理详情 |
| 代理 | POST | `/api/dev/agent/ban` | 封禁代理 |
| 代理 | POST | `/api/dev/agent/unban` | 解封代理 |
| 代理 | POST | `/api/dev/agent/recharge` | 代理充值 |
| 代理 | GET | `/api/dev/agent/commission/page` | 分润流水分页 |
| 提现 | POST | `/api/dev/withdraw/apply` | 提现申请 |
| 提现 | POST | `/api/dev/withdraw/audit` | 审核（approve/reject/payout/fail） |
| 提现 | GET | `/api/dev/withdraw/page` | 提现分页 |
| 提现 | GET | `/api/dev/withdraw/{tenantId}/{withdrawId}` | 提现详情 |
| 提现 | GET | `/api/dev/withdraw/pending-amount` | 待审核总额 |
| 设备 | GET | `/api/dev/device/page` | 设备分页（参数：tenantId/softwareId/status/onlineStatus/page/size） |
| 设备 | GET | `/api/dev/device/{tenantId}/{deviceId}` | 设备详情（含完整指纹） |
| 设备 | POST | `/api/dev/device/ban` | 封禁设备（params: tenantId/deviceId） |
| 设备 | POST | `/api/dev/device/unban` | 解封设备（params: tenantId/deviceId） |
| 云函数 | POST | `/api/dev/cloud-func` | 新建/更新云函数 |
| 云函数 | GET | `/api/dev/cloud-func/page` | 分页（参数：tenantId/softwareId/name/enabled/current/size） |
| 云函数 | GET | `/api/dev/cloud-func/{tenantId}/{functionId}` | 云函数详情 |
| 云函数 | DELETE | `/api/dev/cloud-func/{tenantId}/{functionId}` | 删除云函数 |
| 云函数 | POST | `/api/dev/cloud-func/toggle-enabled` | 启用/禁用（params: tenantId/functionId/enabled） |
| 云函数 | POST | `/api/dev/cloud-func/invoke` | 测试执行（body: tenantId/softwareId/functionId/input） |
| 云函数 | GET | `/api/dev/cloud-func/log/page` | 执行日志分页（参数：tenantId/functionId/softwareId/status/invokeSource/current/size） |
| 数据统计 | GET | `/api/dev/stats/verify-trend` | 验证量趋势（参数：tenantId/softwareId/granularity=hour\|day\|month/days） |
| 数据统计 | GET | `/api/dev/stats/device-heatmap` | 设备在线热力图（参数：tenantId/softwareId/days，默认 7） |
| 数据统计 | GET | `/api/dev/stats/income` | 收入统计（参数：tenantId/softwareId/dimension=channel\|cardType\|agent/days） |
| 数据统计 | GET | `/api/dev/stats/anti-crack` | 防破解事件（参数：tenantId/softwareId/days） |
| 部署 | POST | `/api/dev/deploy/webhook` | GitHub Webhook 入口（HMAC-SHA256 验签，立即返回 accepted，异步执行） |
| 部署 | POST | `/api/dev/deploy/manual` | 手动触发部署（body: tenantId/branch） |
| 部署 | GET | `/api/dev/deploy/status` | 当前状态（enabled/deploying/lastDeploy） |
| 部署 | GET | `/api/dev/deploy/log/page` | 部署审计日志分页（参数：tenantId/status/triggerSource/current/size） |
| 工单 | POST | `/api/dev/ticket/submit` | Dev 向管理员提交工单（target=2, creatorType=2，body: tenantId/title/content/category） |
| 工单 | GET | `/api/dev/ticket/submit/page` | Dev 提交工单分页（参数：tenantId/devUserId/category/status） |
| 工单 | GET | `/api/dev/ticket/submit/{tenantId}/{ticketId}` | Dev 工单详情（含回复列表） |
| 工单 | POST | `/api/dev/ticket/submit/reply` | Dev 补充回复（replierType=2，状态→处理中） |

### 7.2 鉴权 API（`/api/auth/*`，全部免鉴权除了 /me 与 /change-password）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/dev/login` | 开发者登录（body: tenantId/username/password，返回 token/userId/role/tenantId/username/nickname） |
| POST | `/api/auth/admin/login` | 管理员登录（body: username/password，无 tenantId） |
| GET | `/api/auth/me` | 获取当前登录用户信息（@AuthRequired） |
| POST | `/api/auth/change-password` | 修改密码（@AuthRequired，body: oldPassword/newPassword，新密码 ≥ 8 位） |

### 7.2.1 软件 API（`/api/dev/software/*`，全部 @AuthRequired(role=ROLE_DEV)，tenantId 从 AuthContext 获取）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/dev/software` | 创建软件（自动生成 appKey/signSecret/RSA，返回明文仅此一次） |
| PUT | `/api/dev/software` | 更新软件（仅非敏感字段：name/version/minVersion/heartbeatInterval/maxConcurrent/enabled） |
| GET | `/api/dev/software/page` | 分页查询（params: current/size/name/enabled） |
| GET | `/api/dev/software/{id}` | 详情（signSecret 脱敏，无 rsaPrivateKey） |
| DELETE | `/api/dev/software/{id}` | 删除（关联卡类/设备/云函数时拒绝） |
| POST | `/api/dev/software/{id}/regenerate-sign-secret` | 轮换签名密钥（返回新明文仅此一次） |
| POST | `/api/dev/software/{id}/regenerate-rsa-key` | 轮换 RSA 密钥对（返回新公钥+私钥明文仅此一次） |

### 7.2.2 SDK API（`/api/sdk/**`，全部由 SdkAuthFilter 签名鉴权，无公开接口）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/sdk/card/login` | 卡密登录（X-Card-Cipher 头传 RSA 加密卡密，返回卡类信息+软件配置） |
| GET | `/api/sdk/announcement` | 拉取已发布公告（params: clientVersion 可选，返回最多 20 条） |
| GET | `/api/sdk/update/check` | 检查更新（params: clientVersion 必填 + channel 可选，返回 hasUpdate/forceUpdate/downloadUrl/sha256） |
| POST | `/api/sdk/device/bind` | 设备绑定（旧接口，过渡期仍需 X-Sign-Secret 头） |
| POST | `/api/sdk/device/unbind` | 设备换机（旧接口） |
| POST | `/api/sdk/device/heartbeat` | 设备心跳（旧接口，过渡期仍需 X-Sign-Secret 头） |

SDK 请求头规范（所有 `/api/sdk/**` 必填）：
- `X-App-Key`：软件 AppKey（开发者后台创建软件时生成）
- `X-Timestamp`：13 位毫秒时间戳（±300s 容差）
- `X-Nonce`：UUID v4（5 分钟内不可重复，Redis 原子防重放）
- `X-Signature`：HMAC-SHA256 签名 Base64（签名原文 = METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256）
- `X-Card-Cipher`：RSA-2048-OAEP 加密的卡密密文（仅卡密接口）

### 7.3 公开回调

| 方法 | 路径 | 返回 |
|---|---|---|
| GET / POST | `/pay/notify/{tenantId}` | 纯字符串 `success` |

### 7.4 H5 API（`/api/h5/**`，X-H5-Token 鉴权独立于 JWT，v0.13.0）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/h5/auth/login` | 卡密登录（公开，appKey + cardKey），返回 X-H5-Token |
| GET | `/api/h5/auth/my-card` | 我的卡密（需 X-H5-Token，按 cardType 渲染） |
| POST | `/api/h5/auth/logout` | 退出登录（需 X-H5-Token，清 Redis+DB） |
| GET | `/api/h5/announcement` | H5 公告列表（需 X-H5-Token） |
| POST | `/api/h5/agent/register` | 代理邀请码注册（公开，appKey + inviteCode） |
| GET | `/api/h5/shop/info?path=xxx` | H5 店铺信息（公开，路径模式查询） |
| POST | `/api/h5/shop/order` | H5 下单（需 X-H5-Token，写 jicek_pay_order status=0） |

### 7.5 Dev Shop API（`/api/dev/shop/**`，@AuthRequired JWT，v0.13.0）

11 接口：店铺 CRUD（save/page/get/update/delete）+ 店铺开关（toggle-status）+ 商品 CRUD（product-save/product-page/product-get/product-update/product-delete）+ 重新生成邀请码（POST `/api/dev/agent/{tenantId}/{agentId}/regenerate-invite-code`）。

### 7.6 Dev End User API（`/api/dev/end-user/**`，@AuthRequired JWT，v0.14.0）

8 接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/dev/end-user` | 创建终端用户（tenantId + softwareId + username 三元唯一 + BCrypt 密码哈希） |
| PUT | `/api/dev/end-user` | 更新终端用户（密码可空表示不改） |
| DELETE | `/api/dev/end-user/{id}` | 删除终端用户 |
| GET | `/api/dev/end-user/page` | 分页查询 |
| GET | `/api/dev/end-user/{id}` | 详情 |
| POST | `/api/dev/end-user/{id}/ban` | 封禁 |
| POST | `/api/dev/end-user/{id}/unban` | 解封 |
| POST | `/api/dev/end-user/reset-password` | 重置密码 |

### 7.7 H5 End User API（v0.14.0）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/h5/end-user/login` | 终端用户账号密码登录（公开，appKey + username + password），返回 X-H5-Token |

### 7.8 Admin Ticket API（`/api/admin/ticket/**`，@AuthRequired(role=2)，v0.15.0）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/ticket/page` | 分页查询所有租户工单（params: current/size/tenantId/category/status） |
| GET | `/api/admin/ticket/{id}` | 工单详情（含回复列表） |
| POST | `/api/admin/ticket/{id}/reply` | 管理员回复工单（replierType=2，状态→已回复） |
| POST | `/api/admin/ticket/{id}/close` | 关闭工单（状态→已关闭） |

### 7.9 Admin Dev User API（`/api/admin/dev-user/**`，@AuthRequired(role=2)，v0.15.0）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/dev-user/page` | 分页查询所有开发者账号（params: current/size/tenantId/username/status） |
| GET | `/api/admin/dev-user/{id}` | 开发者详情 |
| POST | `/api/admin/dev-user/{id}/ban` | 封禁开发者（status=0） |
| POST | `/api/admin/dev-user/{id}/unban` | 解封开发者（status=1） |
| POST | `/api/admin/dev-user/reset-password` | 重置密码（BCrypt 哈希存储） |

### 7.10 SDK Cloud Function API（`/api/sdk/cloud-function/**`，SdkAuthFilter 鉴权，v0.16.0）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/sdk/cloud-function/invoke` | SDK 端调用云函数（body: functionName + input，invokeSource="sdk"，复用 CloudFunctionService.invoke，softwareId 从 SoftwareContext 获取） |

## 8. 数据库表（核心）

| 表名 | 关键字段 | 说明 |
|---|---|---|
| `jicek_pay_config` | `merchant_key`（AES 加密存储） | 支付配置 |
| `jicek_pay_order` | `status` (0/1/2/3/4) | 支付订单（5 状态机） |
| `jicek_card_type` | `type` (1/2/3/4) | 卡类（时长/次数/功能/永久） |
| `jicek_card_key` | `card_cipher`（AES） + `card_hash`（SHA-256） | 卡密（加密存储） |
| `jicek_software` | `app_key` + `sign_secret`（AES） | 软件 |
| `jicek_device` | `device_fingerprint` | 设备（指纹哈希） |
| `jicek_agent` | `parent_id` + `level` + `balance` + `frozen_balance` + `commission_rate` + `invite_code`(v0.13.0) + `invited_by`(v0.13.0) | 代理（多级树形 + 余额 + 邀请码） |
| `jicek_agent_package` | `agent_id` + `card_type_id` + `agent_price` | 代理可售卡类 + 代理价 |
| `jicek_commission` | `agent_id` + `order_id` + `commission_rate`(快照) + `type`(1/2) + `status`(0/1) | 分润流水（不可变） |
| `jicek_withdraw` | `amount` + `fee` + `actual_amount` + `status`(0-4) | 提现申请（5 状态机） |
| `jicek_cloud_function` | `code`(MEDIUMTEXT) + `timeout_ms` + `enabled` + `version` + `invoke_count` | 云函数（UNIQUE: tenant_id+software_id+name） |
| `jicek_cloud_function_log` | `status`(0-6) + `invoke_source` + `caller_ip` + `duration_ms` | 云函数执行审计日志（仅 INSERT + SELECT） |
| `jicek_deploy_log` | `trigger_source`(webhook/manual) + `status`(0-3) + `commit_hash` + `duration_ms` | 部署审计日志（仅 INSERT + SELECT + 受控更新 status，禁 UPDATE/DELETE） |
| `jicek_ticket` | `ticket_no` + `category`(1-4) + `target`(1开发者2管理员) + `status`(0-3) + `creator_type`(1用户2开发者) | 工单主表（受控 UPDATE status/handlerId/closeTime） |
| `jicek_ticket_reply` | `replier_type`(1用户2开发者3管理员) + `content` | 工单回复审计表（仅 INSERT + SELECT，禁 UPDATE/DELETE） |
| `jicek_h5_session` | `h5_token`(UUID) + `cardKeyId` + `tenantId` + `softwareId` + `expireTime` | H5 会话（v0.13.0，Redis 缓存加速 + DB 持久化） |
| `jicek_shop` | `tenantId` + `softwareId` + `name` + `path`(uk_tenant_path 唯一) + `status` | 内嵌卡网店铺（v0.13.0） |
| `jicek_shop_product` | `shopId` + `cardTypeId`(唯一) + `price`(覆盖卡类售价) + `sortOrder` + `status` | 卡网商品（v0.13.0） |
| `jicek_end_user` | `tenantId` + `softwareId` + `username`(uk_tenant_software_username 三元唯一) + `passwordHash`(BCrypt) + `nickname` + `email` + `phone` + `status` | 终端用户（v0.14.0，与 jicek_dev_user 结构类似但绑定 softwareId） |

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
| `StatusTag.vue` | 状态标签（4 种类型：order 订单 / card 卡密 / withdraw 提现 / device 设备） |
| `AmountInput.vue` | 金额输入（decimal.js 精度） |
| `ConfirmDialog.vue` | 二次确认弹窗（资金/卡密操作） |

> 设备状态约定（caller 需组合 status + onlineStatus 后传入）：0=在线 / 1=离线 / 2=封禁

### 9.3 API 调用约定

```typescript
import {
  dashboardApi, cardKeyApi, cardTypeApi, payApi,
  agentApi, withdrawApi, deviceApi, cloudFuncApi, statsApi, deployApi
} from '@/api'

// 统一响应：{ code, msg, data }
// 拦截器自动剥 data，失败自动 ElMessage.error
const data = await dashboardApi.summary(tenantId)

// 分页参数映射：前端 current/size → 后端 page/size（device 接口用 page/size，card-type/cloud-func 用 current/size，差异在 API 层屏蔽）
const deviceList = await deviceApi.page({ tenantId: 1, current: 1, size: 20 })

// 部署状态轮询：deploying=true 时每 5s 刷新，完成后停止
const status = await deployApi.status()
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
8. **代理分润**：向上链式遍历父级（直推 type=1 + 父级 type=2），分润比例是**快照**（下单时锁定，不随后续修改变化）；退款时撤销（status=0）并扣回余额。
9. **提现状态机**：不可逆（0→1→3 / 0→2 / 1→4），资金流必须同事务（balance↔frozenBalance↔totalWithdraw），禁伪异步。
10. **分润撤销余额不足**：余额 < 撤销金额时，余额清零 + 累计收益扣减差额，余额永不为负。
11. **代理密码**：使用 Hutool `cn.hutool.crypto.digest.BCrypt`（spring-security-crypto 未引入依赖）。
12. **设备状态组合**：Device 实体含两个状态字段（status 0/1 + onlineStatus 0/1），UI 层须通过 `deviceTagStatus()` 合并为 StatusTag 的 device 类型值（封禁→2 / 在线→0 / 离线→1），不可直接展示两个字段。
13. **设备指纹脱敏**：列表展示仅前 16 字符 + `****`，详情弹窗可展示完整指纹（审计用），符合「敏感信息最小暴露」原则。
14. **分页参数命名差异**：DevCardTypeController 使用 `current`/`size`，DevDeviceController 使用 `page`/`size`。前端统一使用 `current`/`size`，在 API 层做映射（如 `page: params.current || 1`），不可让调用方关心差异。
15. **ECharts 生命周期**：必须 `onBeforeUnmount` 调用 `chart.dispose()` 释放实例；异步数据驱动渲染须用 `watch(data, () => nextTick(() => render()))` 确保 DOM 已就绪；窗口 resize 须监听并调用 `chart.resize()`。
16. **页面实现范围**（铁律 06）：仅实现后端 Controller 已存在的页面。若 UI-DESIGN.md 列出但后端无 Controller，**禁止虚构接口**，应标注「待后端 XXController 实现后再补」。
17. **云函数沙箱安全**（v0.4.2）：LuaJ 全局表必须裁剪（禁 os/io/loadfile/dofile/require/debug/package/load，全设为 `LuaValue.NIL`）；`LuaC.install(globals)` 必须在 BaseLib 之前，否则 `globals.load()` 无法编译用户代码；超时控制用 `Future.get(timeoutMs)` + `future.cancel(true)` 强制中断，禁用固定 sleep 轮询。
18. **云函数审计不可篡改**：`jicek_cloud_function_log` 表仅允许 INSERT + SELECT，Service 层禁 UPDATE/DELETE，确保执行历史完整可追溯。审计日志写入失败不应阻断主流程（invoke 已返回结果）。
19. **云函数输入注入契约**：通过 `jicek.input` 全局变量传入字符串，Lua 代码 `return` 返回值由 `luaValueToJson()` 递归序列化为 JSON（table 自动判断数组 vs 对象，key 为 1..n 连续正整数则为数组）。输入/输出大小在 Service 层二次校验（DTO 校验 + 实际字节数校验），超限直接截断或拒绝。
20. **云函数线程池隔离**：独立 `jicek-lua-sandbox` daemon 线程池（4 核心/16 最大/64 队列/CallerRunsPolicy）与业务线程池隔离，避免沙箱执行阻塞主业务。线程名必须为 daemon，防止 JVM 退出受阻。
21. **数据统计不新建表**（v0.4.3）：所有统计基于现有业务表（CardKey/Device/PayOrder）内存聚合，禁虚构统计表（铁律 06）。时间分组使用 Java Stream + `DateTimeFormatter` 分桶，与现有 DevDashboardController 风格一致。
22. **统计时间标签连续补 0**：`StatsService.buildTimeLabels()` 必须生成完整日期序列，无数据时段补 0，避免 ECharts X 轴跳跃。粒度切换时 hour 固定查近 1 天（24 点）、day/month 用 `STATS_DEFAULT_RANGE_DAYS`(7) 默认值，禁字面量（铁律 04）。
23. **统计范围上限**：`STATS_MAX_RANGE_DAYS`(90) 天硬上限，超限抛 `STATS_RANGE_EXCEED`(6003)，防止全表扫描。热力图固定 `STATS_HEATMAP_DAYS`(7) 天避免维度爆炸。
24. **统计代理维度预留**：PayOrder 当前无 `agent_id` 字段，`groupByAgent()` 返回空列表 + 前端 alert 提示「待扩展」，禁虚构字段（铁律 06）。待 PayOrder 扩展 agent_id 后再实现。
25. **ECharts 生命周期**：多 Tab 页面每个 Tab 独立 chart 实例，`onBeforeUnmount` 必须 dispose 全部图表，Tab 切换后 `setTimeout(resize, 50)` 避免尺寸未初始化。`watch` 全局筛选（如 softwareId）触发 `reloadAll` 并行加载。
26. **Webhook 验签常量时间比较**（v0.5.0）：`MessageDigest.isEqual` 比较期望签名与接收签名，禁用 `String.equals`（时序攻击风险）。签名格式 `sha256=<hex>`，前缀 `DEPLOY_WEBHOOK_SIGNATURE_PREFIX` 校验后再截取。
27. **部署功能默认关闭**（v0.5.0）：`jicek.deploy.enabled=false` 是默认值，开发环境禁触发真实部署。生产开启需显式设置 `JICEK_DEPLOY_ENABLED=true`，否则 manual 接口返回 403/参数错误，webhook 接口直接忽略。
28. **部署异步执行**（v0.5.0）：Webhook 立即返回 `accepted(deployLogId, message)`，部署在 daemon 线程 `jicek-deploy-{logId}` 中异步执行。GitHub Webhook 默认 10s 超时，同步执行会超时重试导致重复触发。前端通过 `deployApi.status()` 轮询（5s 间隔），完成后停止。
29. **部署 Redisson 锁防并发**（v0.5.0）：`jicek:deploy:lock` Redisson 分布式锁，5 分钟自动释放防死锁。Webhook 与 manual 共用同一锁，获取失败抛 `DEPLOY_LOCK_FAIL`(7001)。
30. **部署审计不可篡改**（v0.5.0）：`jicek_deploy_log` 表仅允许 INSERT + SELECT + 受控更新 status（0→1/2/3），禁 UPDATE 其他字段 / DELETE 任意记录。审计失败不阻断主流程，但记录 ERROR 日志。
31. **部署外部命令执行**（v0.5.0）：禁用 `Runtime.exec`（参数拼接易 shell 注入），统一用 `ProcessBuilder` 参数化执行 git/mvn/npm/docker 命令。`redirectErrorStream(true)` 合并 stderr 到 stdout 便于日志收集。
32. **部署重启模式分发**（v0.5.0）：`restart-mode` 三选一 — `docker`（`docker restart {container}`）/ `btpanel`（HTTP 调用宝塔 API）/ `none`（跳过重启，仅构建）。模式错误或容器名缺失抛 `DEPLOY_RESTART_FAIL`(7007)。
33. **部署回滚机制**（v0.5.0）：备份 jar + dist 到 `.jicek-backup/{timestamp}/`，保留最近 3 个（`DEPLOY_BACKUP_KEEP_COUNT`）。任一步骤失败（git pull / build / restart / healthCheck）触发 `rollback()`：还原最近备份 → restart → 标记 status=3(ROLLED_BACK)。
34. **部署健康检查**（v0.5.0）：轮询 `{health-check-base-url}/actuator/health`，超时 60s（`DEPLOY_HEALTH_CHECK_TIMEOUT_SECONDS`），间隔 3s（`DEPLOY_HEALTH_CHECK_INTERVAL_SECONDS`）。超时未恢复抛 `DEPLOY_HEALTH_CHECK_FAIL`(7008) 并触发回滚。
35. **部署 StatusTag 不扩展**（v0.5.0）：StatusTag 组件仅支持 order/card/withdraw/device 四类业务状态，部署状态（0-3）用 `el-tag` + `deployTagType()` / `deployStatusText()` 函数直接渲染，保持组件纯净性，避免为单一场景污染公共组件。
36. **工单类型字段由 Controller 设定**（v0.6.1 单向）：creatorType / target / replierType 三个字段由 Controller 固定设定，前端不传这些字段（防越权提单）。Dev 端固定 target=2管理员 + creatorType=2开发者 + replierType=2开发者。
37. **工单状态机受控流转**（v0.6.1 单向）：0待处理→1处理中→2已回复→3已关闭。开发者补充回复→状态变「处理中」（提醒管理员有新信息），管理员回复→状态变「已回复」（待管理员 Controller 实现）。任意状态可关闭，已关闭禁回复（抛 TICKET_ALREADY_CLOSED 8003）。
38. **工单回复表审计不可变**（v0.6.1）：`jicek_ticket_reply` 仅 INSERT + SELECT，禁 UPDATE/DELETE。工单主表 `jicek_ticket` 仅受控 UPDATE（status/handlerId/handlerTime/closeTime/updateTime），其余字段不可变。
39. **JWT 密钥环境变量注入**（v0.7.0）：`JICEK_JWT_SECRET` 至少 32 字节，通过 `JicekProperties.Auth.jwtSecret` 注入。`JwtService.init()` 检测到密钥 < 32 字节时 warn 但**不抛异常**（允许应用启动），运行期调用鉴权接口才抛 `AUTH_TOKEN_INVALID`(9002)。HMAC-SHA256 签名，禁用弱算法 HS256+短密钥组合。
40. **ThreadLocal 必须清理**（v0.7.0）：`JwtAuthInterceptor.afterCompletion` **必须**调用 `AuthContext.clear()`，否则线程池复用导致用户身份串号（A 用户请求残留 B 用户身份）。无论业务是否抛异常都需执行，故用 afterCompletion 而非 postHandle。
41. **渐进式鉴权**（v0.7.0）：未标注 `@AuthRequired` 的接口**放行**（兼容现有裸传 tenantId 参数的接口），新接口可从 `AuthContext.current()` 获取身份。注解查找规则：方法级优先，类级兜底。`@AuthRequired(role=2)` 限制仅管理员可访问（role 默认 0 = 任意已登录用户）。
42. **登录失败防枚举**（v0.7.0）：用户不存在和密码错误统一返回 `AUTH_PASSWORD_ERROR`(9005)，不区分「用户不存在」与「密码错误」，避免攻击者通过差异响应枚举有效用户名。
43. **JWT claims 不可信**（v0.7.0）：JWT 仅证明「未被篡改」不证明「用户仍有效」。`/api/auth/me` 与所有 @AuthRequired 接口如需保证用户当前状态，应从数据库查询 `jicek_dev_user`/`jicek_admin_user` 校验 status=1（AuthService.currentUser 已实现，新接口从 AuthContext 取身份后视需要二次查库）。
44. **密钥未配置的容错**（v0.7.0）：生产环境必须配置 `JICEK_JWT_SECRET`；开发环境若未配置，应用可启动但所有鉴权接口返回 9002，便于本地开发快速发现问题而不阻塞启动。
45. **前端 token 失效跳转防重复**（v0.7.0）：`clearAuthAndRedirect()` 检查 `window.location.pathname` 是否已为 `/login`，避免在登录页因 401 响应触发死循环跳转。localStorage key：`jicek_token`（token）+ `jicek_user`（JSON 序列化的用户信息）。
46. **软件密钥明文仅此一次**（v0.8.0）：`SoftwareCreateResultDTO` 含 signSecret + rsaPrivateKey 明文，**仅在创建/轮换接口返回**。查询接口（`get`/`page`）返回 `SoftwareDetailDTO`，signSecret 脱敏（前 4 字符 + ****），rsaPrivateKey 永不返回。前端密钥展示弹窗 `show-close=false` + `close-on-click-modal=false`，强制用户点击「我已保存」。
47. **软件 tenantId 从 AuthContext 获取**（v0.8.0）：`SoftwareService.requireCurrentTenantId()` 从 ThreadLocal 取租户身份，前端禁传 tenantId（防越权）。`requireOwnedSoftware(id, tenantId)` 校验 `software.tenantId == AuthContext.currentTenantId()`，不匹配抛 `SOFTWARE_PERMISSION_DENIED`(1019)。
48. **软件密钥入库必须 AES 加密**（v0.8.0）：`signSecret` 和 `rsaPrivateKey` 入库前必须 `aesCryptoService.encrypt()`，查询时 `decrypt()` 后脱敏展示。明文禁入库（铁律 04 禁硬编码扩展：禁明文存储敏感密钥）。`appKey` 和 `rsaPublicKey` 可明文存储（客户端可见）。
49. **软件删除关联校验**（v0.8.0）：`SoftwareService.delete()` 删除前必须校验关联卡类/设备/云函数，存在则抛 `SOFTWARE_HAS_CARD_TYPE`(1014) / `SOFTWARE_HAS_DEVICE`(1015) / `SOFTWARE_HAS_CLOUD_FUNC`(1016)。防止删除软件后子实体成为孤儿数据。
50. **appKey 全局唯一查重**（v0.8.0）：`generateUniqueAppKey()` 生成后查 `jicek_software.app_key` 是否冲突，最多重试 5 次。虽 32 字符随机冲突概率极低（36^32 ≈ 2^165），但铁律要求健壮性。冲突超限抛 `FAIL`。
51. **密钥轮换影响范围**（v0.8.0）：轮换 signSecret 后所有客户端 SDK 需更新配置（HMAC-SHA256 签名失效）；轮换 RSA 密钥对后所有客户端加密的卡密将无法解密（需重新发放或更新客户端）。前端轮换操作必须二次确认 + 警告提示。
52. **SDK 鉴权用 Filter 不用 Interceptor**（v0.9.0）：`SdkAuthFilter` 需在 Filter 阶段读取 body 计算 SHA-256 用于签名校验，HandlerInterceptor 的 preHandle 阶段 body 尚未解析。用 `CachedBodyHttpServletRequest` 包装请求体使其可重复读，解决 Filter 读取后 `@RequestBody` 无法再读的问题。仅 `/api/sdk/**` 路径包装，不影响其他请求。
53. **SoftwareContext ThreadLocal 必须清理**（v0.9.0）：`SdkAuthFilter.doFilterInternal` 的 `finally` 块**必须** `SoftwareContext.clear()`，否则 Tomcat 线程池复用导致软件身份串号。与 AuthContext（后台用户）同理，但 SoftwareContext 持有的是 Software 实体而非用户身份。
54. **nonce 防重放用 Redis 原子操作**（v0.9.0）：`redissonClient.getBucket(key).trySet("1", Duration.ofMinutes(5))` 是原子的 setIfAbsent + TTL，禁用「先查再写」两步操作（并发场景下两请求同时查到不存在，同时写入）。trySet 返回 false 表示 nonce 已存在（重放攻击）。
55. **卡密禁明文查库**（v0.9.0）：SDK 卡密登录通过 `cardHash = SHA-256(卡密明文)` 查 `jicek_card_key.card_hash` 索引，禁用 `WHERE card_no = ?` 明文查询（防 SQL 注入泄露卡密）。卡密明文仅在 RSA 解密后内存中短暂存在，永不日志输出。
56. **SDK 每软件独立密钥验签**（v0.9.0）：`SdkAuthFilter` 从 `software.sign_secret`（AES 解密后）获取该软件独立的签名密钥，传入 `HmacSignService.verify(data, signature, secretKey)` 验签。不用全局 HmacSignService 单例（其用全局 hmacKey），而是用按软件的 signSecret。
57. **过渡期：SdkDeviceController 旧接口**（v0.9.0）：现有 `/api/sdk/device/heartbeat` 仍从 `@RequestHeader("X-Sign-Secret")` 取明文 signSecret 做二次校验（旧代码遗留）。SdkAuthFilter 已用 software.signSecret 完成验签，X-Sign-Secret 头冗余且不安全。后续版本统一改为从 SoftwareContext 获取，移除明文密钥头。
58. **H5 鉴权独立于 JWT**（v0.13.0）：`/api/h5/**` 走 `X-H5-Token` 头（UUID，24h，DB+Redis 双写），H5AuthInterceptor 拦截，公开接口（login/agent-register/shop-info）需在 `WebMvcConfig.excludePathPatterns` 显式放行。`H5AuthContext` ThreadLocal 必须在 `afterCompletion` 清理，与 AuthContext/SoftwareContext 同理防串号。
59. **H5 卡密校验复用 SDK 同源算法**（v0.13.0）：`H5AuthService.login()` 用 `Md5SignService.sha256Hex(cardKey)` 计算哈希后查 `jicek_card_key.card_hash` 索引，与 `SdkAuthService` 同源。但 H5 用明文卡密传输（依赖 HTTPS），SDK 用 RSA-2048-OAEP 加密传输。
60. **代理邀请码注册继承规则**（v0.13.0）：新代理 `parentId=inviter.id` / `level=inviter.level+1` / `maxSubLevel=inviter.maxSubLevel-1` / `commissionRate=inviter.commissionRate`（继承）。邀请码 8 位 SecureRandom，字符集 `INVITE_CODE_CHARSET` 去易混淆字符 I/O/0/1。注册接口 `POST /api/h5/agent/register` 公开，无需 JWT 也无需 X-H5-Token。
61. **终端用户账号登录复用 H5Session**（v0.14.0）：H5Session 表 `user_id` 字段（可空）与 `card_key_id`（可空）互斥——卡密登录填 cardKeyId/userId=null，账号登录填 userId/cardKeyId=null。EndUserService 自行生成 H5Session（不修改 H5AuthService），login 流程：appKey→查 Software→查 EndUser by (tenantId, softwareId, username)→BCrypt.checkpw 校验→生成 UUID token→存 H5Session。
62. **多语言国际化渐进式改造**（v0.14.0）：vue-i18n 9.x Composition API 模式（legacy: false），useI18n() 在 setup 获取 t 函数。语言包按模块组织（common/lang/topbar/menu/login/endUser），新增页面应优先用 t() 而非硬编码中文。LangSwitch 切换后 location.reload() 同步 Element Plus 语言包（因 main.ts 仅初始化时按 localStorage 决定 EP locale）。localStorage key: `jicek_locale`。
63. **管理员 token 独立于开发者 token**（v0.15.0）：管理员登录存 `jicek_admin_token`，开发者存 `jicek_token`，前端 `adminAxios` 独立实例注入 admin token，路由守卫按 `/admin/*` 前缀分流。后端 `@AuthRequired(role=2)` 限制管理员接口，`JwtAuthInterceptor` 拦截 `/api/admin/**`。
64. **分润幂等与事务边界**（v0.15.0）：`PayNotifyService` 支付成功事务（订单状态+卡密发放）提交后，再调 `CommissionService.grantCommission`，分润独立事务 + try-catch。分润失败不回滚卡密（已发放不可逆）。`jicek_commission` 表 `uk_order_agent(out_trade_no, agent_id)` 唯一索引保证幂等，重复回调短路返回。代理制卡扣余额在制卡事务内先扣款再生成卡密，余额不足事务回滚。
65. **国密 SM2/SM4 可选实现默认关闭**（v0.16.0）：`SmCryptoService` 用 `@ConditionalOnProperty(name="jicek.crypto.sm.enabled", havingValue="true")` 控制，默认 false 不启用。密钥通过 `JICEK_SM4_KEY`（16字节 hex）/ `JICEK_SM2_PRIVATE_KEY` 环境变量注入，未配置时 warn 不阻止启动。SM4 用 CBC 模式对标 SPEC 6.6，SM2 公钥由私钥派生无需单独配置。不影响现有 AES-256-GCM / RSA-2048-OAEP。

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
