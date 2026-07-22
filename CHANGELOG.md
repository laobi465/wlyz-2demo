# 更新日志

## [0.3.1] - 2026-07-22

### [新增] 8 语言客户端 SDK 完整实现
统一契约规范见 [sdk/README.md](sdk/README.md)，所有 SDK 实现完全一致的接口语义（签名 / 心跳 / 卡密验证 / 设备指纹采集）。

| 语言 | 目录 | 关键依赖 | 文件数 |
|---|---|---|---|
| Java | `sdk/java/` | JDK 17+ HttpClient + 自研 JSON 解析器（零第三方依赖） | 12 |
| Python | `sdk/python/` | stdlib + cryptography（RSA） | 3 |
| Node.js | `sdk/nodejs/` | crypto + https（零依赖） | 3 |
| Go | `sdk/go/` | stdlib（零依赖） | 4 |
| C# | `sdk/csharp/` | BCL（.NET 6+） | 5 |
| C++ | `sdk/cpp/` | OpenSSL + libcurl | 5 |
| Lua | `sdk/lua/` | luaossl/luasocket 可选，回退 openssl/curl CLI | 3 |
| Shell | `sdk/shell/` | bash 4+ + curl + openssl（jq 可选） | 3 |
| 易语言 | `sdk/epl/` | 精易模块（原生方案）/ jicek.dll（DLL 方案） | 3 |

核心特性：
- **统一签名**：`METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256` → HMAC-SHA256 → Base64
- **5 维设备指纹**：CPU/主板/硬盘/网卡/BIOS 各自 SHA-256 → 拼接 → SHA-256，VM/容器补充维度
- **RSA-2048-OAEP**：卡密 + 5 维哈希 JSON 加密传输
- **动态心跳**：服务端控制 5-300s 间隔 + 客户端指数退避（1/2/4/8/max 30s）+ 5 次失败断开
- **跨平台指纹采集**：Windows (wmic) / Linux (/proc + dmidecode) / macOS (sysctl + ioreg)

## [0.3.0] - 2026-07-21

### [新增] 设备指纹采集与绑定模块
- Device 实体 + DeviceMapper + 4 个 DTO（Fingerprint/Bind/Unbind/Heartbeat）
- DeviceFingerprintService：5 维 SHA-256 融合（CPU+主板+硬盘+网卡+BIOS）+ VM/容器补充维度 + RSA 解密独立计算 + 常量时间比对
- DeviceService：绑定/换机/封禁/解封/分页查询，SecureRandom 生成 16 位换机码，@Transactional 保证绑定+卡密发放原子性
- DeviceHeartbeatService：动态间隔 5-300s（服务端控制）+ 时间戳±300s 容差 + nonce Redis 防重放 + HMAC-SHA256 签名 + 超时 3×interval 置离线
- DevDeviceController + SdkDeviceController
- HmacSignService 扩展 signWithSecret/verify(3 参) 支持多租户每软件独立密钥
- ResultCode 新增 8 个设备模块错误码（3009-3016）
- JicekConstants 新增设备状态/指纹维度/心跳间隔/换机码长度常量
- jicek_device 表补 11 个字段 + 3 个索引

## [0.2.1] - 2026-07-21

### [新增] 交接文档
- PROMPT.md：下一个 AI 接手指南（阅读顺序/项目快照/已完成模块/待办/编码铁律/架构要点/API/常见陷阱/验证清单）
- README.md 补充 API 概览表、数据库表、角色权限、部署说明

### [新增] 文件顶部注释补全
- 后端：CardKeyMapper / CardTypeMapper 补 Javadoc
- 前端：main.ts / vite.config.ts / DevLayout / StatusTag / AmountInput / ConfirmDialog / dashboard / card-key-gen / card-key-list / pay-config / pay-order 共 11 个文件补顶部注释（HTML 注释格式，含作者/日期/用途/接口/安全说明）

### [修改] 文档联动
- PROJECT.md：模块结构同步
- SPEC.md：补充交接文档规范
- TODO.md：标记交接文档已完成

## [0.1.0] - 2026-07-21

### [新增] 项目立项
- 确定项目名称：极策k网络验证
- 确定技术栈：RuoYi-Vue-Plus 5.4.0 + Vue3 + Element Plus + MySQL 8 + Redis 7
- 确定部署模式：SaaS + 私有部署双模式

### [新增] 支付模块设计
- 确认采用方案 B：独立部署彩虹易支付，RuoYi-Vue-Plus 通过 HTTP 对接
- 确认只实现 V1 接口（MD5 签名 + form-urlencoded + GET 异步回调）
- 删除 V2 适配器设计（用户澄清无独立 V2 官方文档）
- 支付通道由管理员后台勾选（支付宝/微信/QQ/银联），终端用户无选择权
- 异步通知验签 + 幂等检查 + 返回字符串 success
- 订单状态机：0待支付→1已支付→2失败→3已退款→4已关闭

### [新增] 安全设计
- 卡密传输加密：RSA-2048 + AES-256-GCM（可选国密 SM2+SM4）
- 商户密钥加密存储：使用 RuoYi-Vue-Plus EncryptUtils
- 设备指纹：CPU + 主板 + 硬盘 + 网卡 + BIOS 哈希
- 心跳保活：动态间隔（默认 60s，可配置 5-300s）

### [新增] 角色体系设计
- 管理员：全局租户管理、支付通道授权、系统更新、审计日志
- 开发者（租户）：软件/卡密/用户/代理/支付/云数据/统计全功能
- 代理：制卡、查询、分润、提现、下级代理（开发者授权时）
- 终端用户：H5 续费/换机/在线设备管理

### [新增] 文档体系
- 创建 CHANGELOG.md / PROJECT.md / SPEC.md / TODO.md 初始模板

### [新增] 自动更新系统设计
- GitHub Webhook 自动触发（HMAC-SHA256 签名验证）
- 双重启策略适配器（Docker + 宝塔面板）
- 完整流程：备份 → git pull → 依赖 → DB迁移 → 清缓存 → 健康检查 → 重启
- 失败自动回滚 + 审计日志

## [0.2.0] - 2026-07-21

### [新增] 后端核心模块完整实现
- 加密层：AES-256-GCM / RSA-2048-OAEP / HMAC-SHA256 / MD5（V1 兼容）
- 支付适配层：彩虹易支付 V1 完整实现（支付/查询/退款）
- 订单状态机：5 状态不可逆流转 + 金额一致性校验
- 异步回调：Redisson 分布式锁 + MD5 验签 + 幂等 + 返回 success
- 卡密服务：SecureRandom 生成 + AES-256-GCM 加密入库 + 明文仅展示一次
- 资金一致性事务：@Transactional 保证「订单流转 + 卡密发放」原子性

### [新增] 后端 Controller
- DevDashboardController：控制台汇总数据
- DevCardKeyController：卡密生成/查询/封禁/退款
- DevCardTypeController：卡类 CRUD
- DevPayController：支付配置/发起/订单查询/退款
- PayNotifyController：异步回调（GET/POST 双兼容）

### [新增] 前端 Vue3 完整骨架
- Vite + TypeScript + Element Plus 2.9.8
- 全局样式：极策蓝 #1A4D8F 主色 + CSS 变量系统
- 路由：5 个核心页面
- API 客户端：axios 拦截器 + 统一响应处理
- 公共组件：StatusTag/AmountInput/ConfirmDialog
- Layout：220px 左侧导航 + 60px 顶栏 + 主内容区

### [新增] 前端核心页面
- 控制台：今日收入/订单/退款/净收入/卡密状态分布
- 卡密生成：批量生成 + 明文一次性展示弹窗
- 卡密查询：按卡号查询 + 封禁/退款操作
- 支付配置：网关/商户/通道选择 + 加密选项
- 资金流水：分页表格 + 退款确认弹窗

### [新增] 配置类
- MybatisPlusConfig：分页插件（最大 500 条）
- CorsConfig：开发环境跨域
- JicekProperties：所有敏感字段环境变量注入

## [0.4.0] - 2026-07-22

### [新增] 多级代理 + 分润 + 提现模块

完整实现多级代理体系（树形结构 + 向上链式分润 + 提现审核状态机），覆盖后端全层 + 前端页面 + 数据库 DDL。

#### 数据库
- `jicek_agent` 表扩展至 18 字段：新增 `real_name` / `contact` / `frozen_balance` / `total_withdraw` / `commission_rate`(DECIMAL(5,2)) / `max_sub_level` / `last_login_time` / `last_login_ip` / `remark` + `UNIQUE KEY uk_tenant_username`
- 新增 `jicek_agent_package` 表：代理可售卡类 + 代理价（agent_id=0 表示全代理默认）
- 新增 `jicek_commission` 表：分润流水不可变快照（agent_id / order_id / source_agent_id / order_amount / commission_rate 快照 / commission_amount / type 1直推2下级3差级 / status 0已撤销1有效）
- 新增 `jicek_withdraw` 表：提现申请（amount / fee / actual_amount / pay_type / pay_account / pay_name / status 0-4 / audit_by / audit_time / trade_no / fail_reason）

#### 后端 - 实体/Mapper/DTO
- 4 实体：`Agent` / `AgentPackage` / `Commission` / `Withdraw`（金额字段全 BigDecimal）
- 4 Mapper：均继承 `BaseMapper<T>`
- 5 DTO：`AgentSaveDTO`（含校验注解）/ `AgentTreeNode` / `WithdrawApplyDTO` / `WithdrawAuditDTO` / `AgentFinanceSummary`

#### 后端 - Service
- `AgentService`：创建（用户名唯一 + 父级校验 + 层级计算 + BCrypt 密码）/ 更新（含 isDescendant 防环）/ 封禁解封 / 充值 / 扣余额 / 分页 / 树形构建（一次查询 + Map 分组递归）
- `CommissionService`：
  - `grantCommission`：直推代理分润（type=1）+ 向上遍历父级链分润（type=2，最多 AGENT_MAX_LEVEL=10 层），同 @Transactional 原子更新余额+累计收益
  - `revokeCommission`：退款时撤销订单所有有效分润（status=0），余额不足保护（余额清零 + 累计收益扣减差额，余额永不负）
- `WithdrawService`：
  - 申请：金额 ≥ WITHDRAW_MIN_AMOUNT(10) + 代理状态校验 + 余额校验 + balance→frozenBalance（同事务）
  - 审核状态机（不可逆）：0待审核→1已通过 / 0→2已拒绝(冻结→余额) / 1→3已打款(冻结→总提现) / 1→4已失败(冻结→余额)
  - 资金流铁律 06：所有资金变动同事务，禁伪异步

#### 后端 - Controller
- `DevAgentController`（`/api/dev/agent`）：CRUD + 树形 + 封禁/解封 + 充值 + 分润流水分页
- `DevWithdrawController`（`/api/dev/withdraw`）：申请 + 审核(approve/reject/payout/fail) + 分页 + 详情 + 待审核总额

#### 后端 - 常量/错误码
- `JicekConstants` 新增：代理状态 / 分润类型 / 分润状态 / 提现状态 / WITHDRAW_MIN_AMOUNT / WITHDRAW_FEE_RATE / AGENT_MAX_LEVEL / Redis 锁 key
- `ResultCode` 新增 17 个错误码（4001-4017）

#### 前端
- `StatusTag.vue` 扩展：支持 `withdraw` 类型（5 状态色映射）
- `jicek.scss` 新增 `.jicek-tag-info` 样式类
- 代理管理页（`views/dev/agent/index.vue`）：树形展示 + 扁平分页 + 创建/编辑/封禁/解封/充值，Decimal.js 金额格式化
- 提现审核页（`views/dev/withdraw/index.vue`）：状态筛选 + 汇总 + 4 动作审核弹窗，资金操作用 ElNotification 持久提示
- API 定义：新增 `agentApi`（9 方法）+ `withdrawApi`（5 方法），含 current→page 参数映射
- 路由：新增 `/agent` + `/withdraw`
- 侧边栏：新增「代理管理」子菜单（代理列表 + 提现审核）

#### 技术决策
- 密码哈希：使用 `cn.hutool.crypto.digest.BCrypt`（Hutool 已在依赖，spring-security-crypto 未引入）
- 提现工作流：采用简单状态机（对标 PayOrderStateMachineService 模式），未引入 WarmFlow（保持依赖精简）
- 分润撤销余额不足保护：余额 < 撤销金额时，余额清零 + 累计收益扣减差额，确保余额永不为负

## [0.4.1] - 2026-07-22

### [新增] 前端补全（卡类/设备/Dashboard 图表）

依据 UI-DESIGN.md 6.2 节页面清单 + 铁律 06（仅实现后端 Controller 已存在的页面），完成 3 个前端页面 + ECharts 数据可视化，并对 StatusTag/API/路由/菜单进行配套扩展。

#### 新增页面

- **卡类管理页**（`views/dev/card-type/index.vue`）：
  - 完整 CRUD：分页列表 + 创建/编辑表单 + 删除二次确认
  - 类型联动表单：4 种卡类型（1时长卡/2次数卡/3功能卡/4永久卡）通过 `v-if` 切换显示对应字段
  - `durationHint` 计算属性：将秒数转为「X天Y小时」人类可读时长
  - `specText()` / `bindStrategyText()` 表格列展示
  - 金额字段使用 Decimal.js 格式化（铁律 13 金额精度）
  - 表单校验规则（必填项 + 数值范围）

- **设备管理页**（`views/dev/device/index.vue`）：
  - 分页查询：支持按 softwareId / status / onlineStatus 筛选
  - 详情弹窗：el-descriptions 2 列布局，含完整指纹（`<code>` 等宽字体展示）
  - 封禁/解封操作：调用 `/api/dev/device/ban` + `/unban`，操作后刷新列表
  - **设备状态组合**：Device 实体含两个状态字段（status 0/1 + onlineStatus 0/1），通过 `deviceTagStatus()` 组合为 StatusTag 的 device 类型值（封禁→2 / 在线→0 / 离线→1）
  - **指纹脱敏**：列表中指纹仅展示前 16 字符 + `****`，详情弹窗展示完整指纹
  - OS 类型以 el-tag 色彩映射展示（windows/linux/macos/android/ios）

- **Dashboard ECharts 集成**（`views/dev/dashboard/index.vue` 改写）：
  - 新增依赖：`echarts@^5.5.0`
  - 卡密状态分布饼图（环形，40%/65% 半径）：未使用（#6B7280 灰）/ 已使用（#2E7D5B 绿）/ 已封禁（#B23A3A 红）
  - 今日收支柱状图：今日收入（#2E7D5B 绿）/ 今日退款（#B23A3A 红）/ 今日净收入（#1A4D8F 主色蓝）
  - 生命周期管理：`ref<HTMLElement>` 容器 + `let chart: ECharts | null` 实例 + `onBeforeUnmount` 销毁 + `window:resize` 监听
  - 数据驱动渲染：`watch(summary, ...)` + `nextTick()` 确保异步数据加载后 DOM 已就绪
  - 空数据保护：饼图数据为 0 时显示「暂无数据」灰色占位
  - 金额 tooltip 使用 Decimal.js：`¥${new Decimal(p.value).toFixed(2)}`

#### 公共组件扩展

- **StatusTag.vue 扩展**：Props.type 新增 `'device'` 类型，配套 `deviceMap` 三态映射（0=在线 success / 1=离线 pending / 2=已封禁 danger），`map` computed 分支扩展支持 4 类型
- 现支持 4 种状态语义：order（订单）/ card（卡密）/ withdraw（提现）/ device（设备）

#### API 定义

- 新增 `deviceApi`（`src/api/index.ts`）：
  - `page` / `get` / `ban` / `unban` 4 方法
  - 分页参数映射：前端 `current`/`size` → 后端 `page`/`size`（DevDeviceController 使用 page/size，DevCardTypeController 使用 current/size，差异在 API 层屏蔽）
- `cardTypeApi` 此前已存在（v0.2.0）

#### 路由 + 菜单

- 路由新增：`/card-type`（CardType，icon: Files）+ `/device`（Device，icon: Monitor）
- DevLayout 侧边栏：
  - 「卡密管理」子菜单新增「卡类管理」项
  - 新增「用户管理」子菜单（icon: Monitor）+ 「设备管理」项

#### 技术决策

- **页面实现范围**：依据铁律 06（防幻觉），仅实现后端 Controller 已存在的页面。软件管理（无 Software Controller）/ 用户管理（无 User Controller）/ H5 终端用户页（无对应 Controller）均未实现，避免虚构接口
- **设备状态组合策略**：Device 实体的 status 和 onlineStatus 字段语义不同（封禁态 vs 在线态），UI 层合并为单一展示态，避免重复标签
- **指纹脱敏策略**：列表展示脱敏（前 16 字符）+ 详情弹窗展示完整（用于审计），符合「敏感信息最小暴露」原则

## 待发布版本（开发中）

### [未发布] v0.5.0
- 前端补全剩余项：软件/用户管理（待后端 Controller）+ H5 终端用户页面
- CardKeyService.useCard 完整流程接入 + Sa-Token 鉴权 + software 表读取签名密钥/心跳间隔
- 代理制卡扣余额接入 AgentService.deductBalance
- 分润发放接入 PaymentTransactionService（支付成功回调触发 grantCommission）
