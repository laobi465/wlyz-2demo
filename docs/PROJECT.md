# 极策k网络验证 - 项目文档

## 1. 项目概述

### 1.1 目标
面向开发者的多租户卡密验证 SaaS 平台，提供软件授权、设备绑定、云变量、防破解、内置发卡商城一体化解决方案。

### 1.2 背景
对标护卫盾、科御网络验证等闭源 SaaS 产品，差异化点：
- 国产开源技术栈（RuoYi-Vue-Plus），可私有部署
- 真正多租户 SaaS 架构
- 8 语言客户端 SDK 全覆盖
- 资金合规：彩虹易支付不经手资金，规避二清风险

### 1.3 适用范围
- 软件作者、工具开发者、小型团队（自用型）
- 为其他开发者提供卡密 SaaS 服务的平台型运营商

## 2. 架构总览

### 2.1 技术栈
| 层级 | 选型 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.4.6 |
| 权限认证 | Sa-Token | 1.42.0 |
| ORM | MyBatis-Plus | 3.5.12 |
| 工作流 | WarmFlow | 1.7.2 |
| 任务调度 | SnailJob | (RuoYi 集成版) |
| 缓存 | Redisson | 7.x |
| 数据库 | MySQL | 8.0.42 |
| 前端 | Vue3 + TS + Element Plus | EP 2.9.8 |
| Web 容器 | Undertow | (XNIO) |
| 文件存储 | MinIO | 最新版 |
| 支付网关 | 彩虹易支付（独立部署） | V1 接口 |

### 2.2 核心模块依赖
```
极策k网络验证 (RuoYi-Vue-Plus)
├── ruoyi-admin          # 启动模块
├── ruoyi-common-*       # 通用模块（多租户、安全、Redis 等）
├── ruoyi-modules
│   ├── system          # 系统管理（RuoYi 自带）
│   ├── workflow        # 工作流（RuoYi 自带）
│   └── jicek-license   # ★ 卡密验证核心模块（新增，v0.2.0 已实现）
│       ├── common      # 通用：R/ResultCode/ServiceException/常量
│       ├── config      # 配置：JicekProperties/MybatisPlusConfig/CorsConfig
│       ├── crypto      # ★ 加密层（已实现，v0.16.0 加 SmCryptoService 国密可选）
│       │   ├── AesCryptoService     # AES-256-GCM
│       │   ├── RsaCryptoService     # RSA-2048-OAEP
│       │   ├── HmacSignService      # HMAC-SHA256
│       │   ├── Md5SignService       # MD5（V1 兼容）+ SHA-256
│       │   ├── SmCryptoService      # ★ SM2/SM4/SM3 国密可选（v0.16.0，@ConditionalOnProperty 默认关闭）
│       │   └── CryptoConfiguration  # Bean 配置
│       ├── card        # 卡密模块（已实现）
│       │   ├── entity  # CardType / CardKey
│       │   ├── mapper  # CardTypeMapper / CardKeyMapper
│       │   ├── dto     # 生成请求/响应 DTO
│       │   ├── generator # CardKeyGenerator (SecureRandom)
│       │   ├── service # CardKeyService
│       │   └── controller # DevCardKeyController / DevCardTypeController
│       ├── pay         # ★ 支付适配层（已实现）
│       │   ├── entity  # PayConfig / PayOrder
│       │   ├── mapper  # PayConfigMapper / PayOrderMapper
│       │   ├── dto     # 请求/响应/回调 DTO
│       │   ├── adapter # PayAdapter 接口 + PayAdapterV1Impl
│       │   ├── service # PayConfigService / PayOrderStateMachineService
│       │   │           # PayNotifyService / PayOrderService
│       │   └── controller # PayNotifyController / DevPayController
│       ├── transaction # ★ 资金一致性事务（已实现）
│       │   └── PaymentTransactionService
│       ├── dashboard   # 控制台（已实现）
│       │   └── controller # DevDashboardController
│       ├── device      # 设备指纹（待实现 v0.3.0）
│       ├── heartbeat   # 心跳保活（待实现 v0.3.0）
│       ├── cloudfunc   # ★ 云函数模块（v0.4.2 新增，LuaJ 沙箱远程执行）
│       │   ├── entity  # CloudFunction / CloudFunctionLog
│       │   ├── mapper  # CloudFunctionMapper / CloudFunctionLogMapper（审计表禁 UPDATE/DELETE）
│       │   ├── dto     # CloudFunctionSaveDTO / CloudFunctionInvokeDTO / CloudFunctionInvokeResult
│       │   ├── sandbox # LuaSandboxService（LuaJ 3.0.6 沙箱引擎，全局表裁剪 + 超时中断 + 输出截断）
│       │   ├── service # CloudFunctionService
│       │   └── controller # DevCloudFunctionController
│       ├── stats      # ★ 数据统计模块（v0.4.3 新增，4 子项多维分析）
│       │   ├── dto     # VerifyTrendDTO / DeviceHeatmapDTO / IncomeStatsDTO / AntiCrackStatsDTO
│       │   ├── service # StatsService（内存分桶聚合，基于现有业务表）
│       │   └── controller # DevStatsController
│       ├── deploy     # ★ 部署模块（v0.5.0 新增，GitHub Webhook 自动更新）
│       │   ├── entity  # DeployLog
│       │   ├── mapper  # DeployLogMapper（审计表禁 UPDATE/DELETE）
│       │   ├── dto     # WebhookResultDTO / ManualDeployDTO / DeployStatusDTO
│       │   ├── service # DeployService（备份→拉代码→构建→重启→健康检查→失败回滚，HMAC-SHA256 验签 + Redisson 锁 + 异步 daemon 线程）
│       │   └── controller # DevDeployController
│       ├── ticket     # ★ 工单模块（v0.6.0 新增，双向工单；v0.15.0 加 AdminTicketController）
│       │   ├── entity  # Ticket / TicketReply
│       │   ├── mapper  # TicketMapper / TicketReplyMapper（回复表禁 UPDATE/DELETE）
│       │   ├── dto     # TicketCreateDTO / TicketReplyDTO / TicketDetailDTO / AdminTicketReplyDTO（v0.15.0）
│       │   ├── service # TicketService（CRUD + 状态机 + 分类 + adminPage/adminGet/adminReply/adminClose，类型字段由 Controller 设定防越权）
│       │   └── controller # H5TicketController + DevTicketController + AdminTicketController（v0.15.0，/api/admin/ticket 4 接口 @AuthRequired(role=2)）
│       ├── auth       # ★ 鉴权模块（v0.7.0 新增，JWT + @AuthRequired 渐进式；v0.15.0 加 AdminDevUserController + DevUserService）
│       │   ├── entity  # DevUser / AdminUser
│       │   ├── mapper  # DevUserMapper / AdminUserMapper
│       │   ├── dto     # LoginDTO / LoginResultDTO / ChangePasswordDTO / UserInfoDTO / DevUserDetailDTO / DevUserResetPasswordDTO（v0.15.0）
│       │   ├── service # JwtService（HMAC-SHA256） + AuthService（登录/当前用户/改密） + DevUserService（v0.15.0，page/get/ban/unban/resetPassword）
│       │   ├── interceptor # AuthContext（ThreadLocal） + @AuthRequired 注解 + JwtAuthInterceptor
│       │   └── controller # AuthController（/api/auth/* 4 接口） + AdminDevUserController（v0.15.0，/api/admin/dev-user 5 接口 @AuthRequired(role=2)）
│       ├── software   # ★ 软件模块（v0.8.0 新增，CRUD + 密钥生成/轮换 + 关联校验）
│       │   ├── entity  # Software
│       │   ├── mapper  # SoftwareMapper
│       │   ├── dto     # SoftwareSaveDTO / SoftwareDetailDTO / SoftwareCreateResultDTO
│       │   ├── service # SoftwareService（CRUD + 密钥生成 + 轮换 + 关联校验）
│       │   └── controller # DevSoftwareController（/api/dev/software/* 7 接口）
│       ├── sdk         # ★ SDK 模块（v0.9.0 新增，终端用户通过 SDK 在开发者软件内接入；v0.16.0 加 SdkCloudFunctionController）
│       │   ├── auth    # SdkAuthFilter（签名鉴权） + SoftwareContext（ThreadLocal） + CachedBodyHttpServletRequest
│       │   ├── dto     # SdkLoginRequestDTO / SdkLoginResultDTO / SdkCloudFunctionInvokeDTO（v0.16.0）
│       │   ├── service # SdkAuthService（卡密登录）
│       │   └── controller # SdkLoginController（/api/sdk/card/login） + SdkCloudFunctionController（v0.16.0，/api/sdk/cloud-function/invoke）
│       ├── announcement # ★ 公告模块（v0.10.0 新增，开发者按软件/版本下发，SDK 拉取展示）
│       │   ├── entity  # Announcement
│       │   ├── mapper  # AnnouncementMapper
│       │   ├── dto     # AnnouncementSaveDTO / AnnouncementDetailDTO / SdkAnnouncementDTO
│       │   ├── service # AnnouncementService（CRUD + 发布/下线状态机 + SDK 拉取 + 版本范围匹配）
│       │   └── controller # DevAnnouncementController（/api/dev/announcement/* 7 接口） + SdkAnnouncementController（/api/sdk/announcement）
│       ├── update      # ★ 更新包模块（v0.11.0 新增，多格式 exe/sh/win/lua/zip/7z，SDK 检查更新）
│       │   ├── entity  # UpdatePackage
│       │   ├── mapper  # UpdatePackageMapper
│       │   ├── dto     # UpdatePackageSaveDTO / UpdatePackageDetailDTO / SdkUpdateCheckResultDTO / UploadResultDTO
│       │   ├── service # UpdatePackageService（文件上传 + CRUD + 发布/下线 + SDK 检查更新 + SHA-256 + 路径穿越防御）
│       │   └── controller # DevUpdatePackageController（/api/dev/update-package/* 8 接口） + SdkUpdateController（/api/sdk/update/check）
│       ├── h5          # ★ H5 模块（v0.13.0 新增，终端用户验证界面，X-H5-Token 鉴权 + Redisson 会话）
│       │   ├── entity  # H5Session
│       │   ├── mapper  # H5SessionMapper
│       │   ├── dto     # H5LoginRequestDTO / H5LoginResultDTO / H5CardDetailDTO
│       │   ├── auth    # H5AuthContext(ThreadLocal) + H5AuthInterceptor(X-H5-Token 头)
│       │   ├── service # H5AuthService(login/my-card/logout，复用 Md5SignService.sha256Hex 校验卡密)
│       │   └── controller # H5AuthController + H5AnnouncementController + H5AgentController
│       ├── shop       # ★ 内嵌卡网模块（v0.13.0 新增，店铺 + 商品 + H5 下单）
│       │   ├── entity  # Shop / ShopProduct
│       │   ├── mapper  # ShopMapper / ShopProductMapper
│       │   ├── dto     # ShopSaveDTO / ShopProductSaveDTO / ShopDetailDTO / ShopProductDTO / H5ShopViewDTO / H5CreateOrderDTO / H5CreateOrderResultDTO
│       │   ├── service # ShopService（店铺 CRUD + 商品 CRUD + H5 下单写 jicek_pay_order）
│       │   └── controller # DevShopController（/api/dev/shop/* 11 接口） + H5ShopController（/api/h5/shop/info 公开 + /api/h5/shop/order 需 X-H5-Token）
│       ├── enduser    # ★ 终端用户账号模块（v0.14.0 新增，H5 账号密码登录复用 H5Session）
│       │   ├── entity  # EndUser
│       │   ├── mapper  # EndUserMapper
│       │   ├── dto     # EndUserSaveDTO / H5EndUserLoginDTO / EndUserDetailDTO
│       │   ├── service # EndUserService（CRUD + ban/unban + reset-password + 账号登录自建 H5Session）
│       │   └── controller # DevEndUserController（/api/dev/end-user/* 8 接口 JWT） + H5EndUserController（/api/h5/end-user/login 公开）
│       └── sdk-gen     # ★ SDK 代码生成器（v0.12.0 前端实现，9 语言模板，无后端）
└── jicek-ui            # ★ 前端（v0.2.0 已实现骨架，v0.4.1 补全卡类/设备/Dashboard 图表，v0.4.2 新增云函数，v0.4.3 新增数据统计，v0.5.0 新增部署管理，v0.6.0 新增工单管理，v0.7.0 新增鉴权框架，v0.8.0 新增软件管理，v0.10.0 新增公告管理，v0.12.0 新增 SDK 代码生成 + 对接文档，v0.13.0 新增 H5 + 内嵌卡网，v0.14.0 新增终端用户管理 + 多语言国际化，v0.15.0 新增管理员后台 + i18n 全量）
    ├── src/api         # API 客户端 + 接口定义（authApi/softwareApi/dashboardApi/cardKeyApi/cardTypeApi/payApi/agentApi/withdrawApi/deviceApi/cloudFuncApi/statsApi/deployApi/ticketApi/announcementApi/h5Api/shopApi v0.13.0 + endUserApi v0.14.0 + admin.ts v0.15.0 独立 adminAxios 实例 jicek_admin_token 隔离）
    ├── src/components/jicek # 公共组件（StatusTag 4 类型/AmountInput/ConfirmDialog）
    ├── src/components/LangSwitch.vue # ★ 多语言切换组件（v0.14.0，顶栏下拉 + localStorage 持久化）
    ├── src/i18n        # ★ 多语言国际化（v0.14.0 起，v0.15.0 全量改造 17 dev 页面 + 语言包扩展 16 新模块，所有用户可见文案中英文切换）
    ├── src/layout      # DevLayout (220px 侧栏 + 60px 顶栏) + AdminLayout（v0.15.0 管理员后台布局）
    ├── src/router      # 路由配置（11 个页面路由 + /h5/* 7 个 public 子路由 v0.13.0 + /shop 后台路由 + /end-user v0.14.0 + /admin/* 守卫 v0.15.0 校验 jicek_admin_token）
    ├── src/styles      # jicek.scss (CSS 变量系统)
    ├── src/utils       # ★ sdk-code-templates.ts（v0.12.0，9 语言代码模板生成器）
    ├── src/views/dev   # 开发者页面（v0.15.0 全量 i18n 改造）
        ├── dashboard   # 控制台（v0.4.1 集成 ECharts 饼图 + 柱状图）
        ├── card-key-gen # 卡密生成
        ├── card-key-list # 卡密查询
        ├── card-type   # ★ 卡类管理（v0.4.1 新增，CRUD + 4 种类型联动表单）
        ├── device      # ★ 设备管理（v0.4.1 新增，分页 + 详情 + 封禁/解封 + 指纹脱敏）
        ├── pay-config  # 支付配置
        ├── pay-order   # 资金流水
        ├── agent       # 代理管理（v0.4.0）
        ├── withdraw    # 提现审核（v0.4.0）
        ├── cloud-func  # ★ 云函数管理（v0.4.2 新增，双 Tab：函数列表 + 执行日志）
        ├── stats       # ★ 数据统计（v0.4.3 新增，4 Tab：验证趋势/设备热力图/收入统计/防破解事件）
        ├── deploy      # ★ 部署管理（v0.5.0 新增，3 状态卡片 + 手动触发 + 审计日志 + 状态轮询）
        ├── ticket      # ★ 工单管理（v0.6.0 新增，双 Tab：收件箱 + 已提交 + 详情对话流）
        ├── login       # ★ 登录页（v0.7.0 新增，租户ID+用户名+密码 + 表单校验）
        ├── software    # ★ 软件管理（v0.8.0，CRUD + 密钥展示弹窗 + 轮换二次确认）+ v0.12.0 SdkCodeGenDialog.vue（接入代码生成弹窗）
        ├── integration-doc # ★ 对接文档页（v0.12.0 新增，接入流程 + 签名算法 + RSA + API + 错误码 + SDK 索引）
        ├── shop        # ★ 内嵌卡网管理（v0.13.0 新增，店铺 + 商品双层弹窗）
        └── end-user    # ★ 终端用户管理（v0.14.0 新增，CRUD + 封禁 + 重置密码）
    ├── src/views/h5    # ★ H5 终端用户页（v0.13.0 新增，7 页：H5Layout + login + my-card + announcement + agent/register + shop + shop/order）
    └── src/views/admin # ★ 管理员后台页（v0.15.0 新增，jicek_admin_token 隔离 + adminAxios 独立实例）
        ├── login       # 管理员登录（用户名+密码，无租户ID）
        ├── ticket      # 工单管理（筛选+表格+详情弹窗+回复+关闭）
        └── dev-user    # 开发者管理（筛选+表格+封禁/解封+重置密码）
```

### 2.3 数据流
```
[客户端 SDK] ─HTTPS─→ [验证 API] ─→ [Redis 缓存]
                              ↓
                       [MySQL 业务库]
                              ↓
                       [心跳/云变量/卡密]

[用户 H5] ──→ [购卡] ──→ [订单生成] ──→ [彩虹易支付]
                                          ↓ 异步回调
                                    [验签+幂等+发卡]
```

#### H5 购卡数据流（v0.13.0 新增）
```
[H5 终端用户]
   │
   │ 1. GET /api/h5/shop/info?path=xxx（公开，查 jicek_shop + jicek_shop_product）
   ▼
[店铺列表 + 商品列表]
   │
   │ 2. POST /api/h5/shop/order（X-H5-Token，写 jicek_pay_order status=0）
   ▼
[彩虹易支付 V1 跳转]
   │
   │ 3. 异步回调 /pay/notify/{tenantId}
   ▼
[PayNotifyService：Redisson 锁 + MD5 验签 + 幂等]
   │
   ▼
[PaymentTransactionService.processPaymentSuccess]
   │  ├─ 订单 status 0→1
   │  └─ 卡密发放（按 quantity 生成 N 张 jicek_card_key，AES-256-GCM 加密入库）
   ▼
[返回纯字符串 success]
```

## 3. 功能清单（已设计，待实现）

### 3.1 核心验证
- [ ] 卡密验证（时长/次数/功能/永久 四种模式）
- [ ] 账号验证（注册、登录、续费）
- [ ] 设备指纹绑定（CPU+主板+硬盘+网卡+BIOS）
- [ ] 心跳保活（动态间隔 5-300s）
- [ ] 在线/离线状态管理

### 3.2 卡密管理
- [x] 卡类管理（4 种类型 + 定价 + 绑定策略）✅ v0.4.1（前端 CRUD + 类型联动表单）
- [x] 批量生成（自定义前缀、字符集、长度）✅ v0.2.0
- [x] 加密存储（AES-256-GCM）✅ v0.2.0
- [x] 查询/封禁/解封/退款 ✅ v0.2.0

### 3.3 支付系统
- [ ] 彩虹易支付 V1 适配器（MD5 签名）
- [ ] 支付通道管理（支付宝/微信/QQ/银联）
- [ ] 订单状态机（5 状态流转）
- [ ] 异步回调验签 + 幂等
- [ ] 资金流水审计

### 3.4 代理体系
- [x] 多级代理（树形结构，parent_id + level + isDescendant 防环）✅ v0.4.0
- [x] 分润比例配置（commission_rate DECIMAL(5,2)，0-100）✅ v0.4.0
- [x] 向上链式分润（直推 type=1 + 父级链 type=2，最多 10 层，同事务原子）✅ v0.4.0
- [x] 分润撤销（退款触发，余额不足保护）✅ v0.4.0
- [x] 提现审核状态机（简单状态机，未引入 WarmFlow）✅ v0.4.0
- [x] 代理制卡扣余额 ✅ v0.15.0（CardKeyService.batchGenerate 代理制卡分支调用 AgentService.deductBalance，先扣款再生成卡密，同事务）
- [x] 分润接入支付回调 ✅ v0.15.0（PayNotifyService 支付成功事务提交后调 CommissionService.grantCommission，分润独立事务 + try-catch，分润失败不回滚卡密；jicek_commission 加 uk_order_agent 幂等索引）

### 3.5 云端数据
- [ ] 云变量（key/value + 签名加密）
- [x] 云函数（远程执行，抗破解终极方案）✅ v0.4.2（LuaJ 3.0.6 沙箱 + 全局表裁剪 + 超时中断 + 输出截断 + 审计日志不可篡改）
- [x] 远程公告（按软件/版本下发）✅ v0.10.0（CRUD + 发布/下线状态机 + SDK 拉取 + 版本范围匹配）

### 3.6 客户端 SDK
- [x] Java SDK ✅ v0.3.1
- [x] C# SDK ✅ v0.3.1
- [x] Python SDK ✅ v0.3.1
- [x] Go SDK ✅ v0.3.1
- [x] Node.js SDK ✅ v0.3.1
- [x] C++ SDK ✅ v0.3.1
- [x] 易语言模块 ✅ v0.3.1
- [x] Lua SDK ✅ v0.3.1
- [x] Shell SDK ✅ v0.3.1

### 3.7 安全防护
- [ ] IP 黑名单 / 设备黑名单
- [ ] 频率限制（Redisson 分布式限流）
- [ ] 防爆破（连续失败 N 次封禁 IP）
- [ ] 一次一密封包（防重放）

### 3.8 v0.12.0 + v0.13.0 新增功能（已完成）
- [x] SDK 代码生成器 ✅ v0.12.0（前端纯实现，9 语言模板一键生成 + 对接文档页）
- [x] 自动更新模块 ✅ v0.11.0（更新包上传 + 多格式 + 强制更新 + SDK 检查更新）
- [x] H5 终端用户界面 ✅ v0.13.0（卡密登录 + 我的卡密 + 公告 + 7 页前端）
- [x] 代理邀请码注册 ✅ v0.13.0（8 位 SecureRandom + 继承邀请人配置）
- [x] 内嵌卡网系统 ✅ v0.13.0（店铺 + 商品 + H5 下单写 jicek_pay_order）

### 3.9 v0.14.0 新增功能（已完成）
- [x] 终端用户账号体系 ✅ v0.14.0（jicek_end_user 表 + 后台 8 接口 CRUD/封禁/重置密码 + H5 账号密码登录复用 H5Session）
- [x] 多语言国际化 ✅ v0.14.0（vue-i18n 9.x 中英文 + LangSwitch 组件 + 渐进式改造）

### 3.10 v0.15.0 新增功能（已完成）
- [x] 代理制卡扣余额 ✅ v0.15.0（CardKeyService.batchGenerate 代理制卡分支调 AgentService.deductBalance，先扣款再生成卡密同事务；CardKeyGenRequestDTO 加 agentId）
- [x] 分润接入支付回调 ✅ v0.15.0（PayOrder 加 agentId + jicek_pay_order 加 idx_agent；PayNotifyService 支付成功后调 CommissionService.grantCommission，分润独立事务 + try-catch，分润失败不回滚卡密；jicek_commission 加 uk_order_agent 幂等）
- [x] 管理员端工单处理 + 租户管理 ✅ v0.15.0（AdminTicketController 4 接口 + AdminDevUserController 5 接口，@AuthRequired(role=2)；DevUserService 新建；前端 AdminLayout + 管理员登录/工单/开发者管理 3 页 + adminAxios 独立实例 + jicek_admin_token 隔离）
- [x] 多语言国际化全量 ✅ v0.15.0（17 个 dev 页面全量 i18n 改造 + 语言包扩展 16 个新模块，所有用户可见文案支持中英文切换）

### 3.11 v0.16.0 新增功能（已完成，三项遗留补全）
- [x] 收入统计代理维度 ✅ v0.16.0（StatsService 新增 groupByAgent() 按 PayOrder.agentId 分组 + AgentMapper 预加载代理名；前端 stats 页移除 dimension='agent' 的 alert 提示，代理维度正常展示）
- [x] SDK 云函数调用接口 ✅ v0.16.0（新建 SdkCloudFunctionController，POST /api/sdk/cloud-function/invoke，SdkAuthFilter 鉴权，invokeSource="sdk"；CloudFunctionService 新增 findBySoftwareAndName 三元查询；SdkCloudFunctionInvokeDTO 含 functionName + input）
- [x] 国密 SM2/SM4 可选实现 ✅ v0.16.0（新建 SmCryptoService：SM4-CBC 对称 + SM2 非对称 + SM3 摘要；@ConditionalOnProperty(name="jicek.crypto.sm.enabled", havingValue="true") 默认关闭；密钥环境变量 JICEK_SM4_KEY/JICEK_SM2_PRIVATE_KEY 注入；不影响现有 AES-256-GCM / RSA-2048-OAEP）

## 4. 角色权限体系

### 4.1 管理员（超级管理员）
- 全局租户管理（开发者注册审核、套餐分配、封禁）
- 系统配置（加密算法开关、全局签名密钥轮换）
- 支付通道管理（启用/禁用 支付宝/微信/QQ/银联）
- 全局审计日志（资金流水、卡密生成、敏感操作）
- 系统更新面板（GitHub Webhook 自动更新）
- 公告管理（全站公告下发）

### 4.2 开发者（租户）
- 控制台（仪表盘：今日验证量/在线设备/收入/卡密销量）
- 软件管理（AppKey、签名密钥、版本号、心跳间隔）
- 卡密管理（卡类、生成、查询、封禁、退款）
- 用户管理（终端用户、设备绑定）
- 代理管理（多级代理、分润、提现审核）
- 支付配置（通道选择、商户凭证、订单流水）
- 云端数据（云变量、云函数、远程公告）
- 数据统计 ✅ v0.4.3（验证量趋势、设备在线热力图、收入多维统计、防破解事件，4 Tab + ECharts）
- 安全中心（IP/设备黑名单、风控规则、密钥轮换）

### 4.3 代理
- 控制台（销量/分润/余额/待提现）
- 卡密管理（仅自己制卡、查询、封禁）
- 用户管理（仅自己发展的用户）
- 资金管理（充值、提现、流水明细）
- 下级代理（开发者授权时）

### 4.4 终端用户（H5）
- 登录（卡密/账号密码）
- 我的卡密（到期时间、剩余次数、绑定设备）
- 续费/购卡（H5 唤起支付，无接口选择权）
- 换机申请（需换机码或开发者审核）
- 在线设备（自助踢下线）
- 公告中心
- 工单（联系开发者）

## 5. 数据库表设计（核心，资金/卡密相关重点）

### 5.1 支付配置表
```sql
CREATE TABLE jicek_pay_config (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
  gateway_url     VARCHAR(255) NOT NULL COMMENT '易支付网关地址',
  pid             BIGINT       NOT NULL COMMENT '商户ID',
  merchant_key    VARCHAR(512) NOT NULL COMMENT '商户密钥(加密存储)',
  notify_url      VARCHAR(255) COMMENT '异步通知地址(系统生成)',
  return_url      VARCHAR(255) COMMENT '同步跳转地址',
  enabled_channels VARCHAR(100) COMMENT '启用的支付通道JSON: alipay,wxpay,qqpay,unionpay',
  enabled         TINYINT      DEFAULT 1,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_tenant (tenant_id)
) COMMENT='支付配置';
```

### 5.2 支付订单表
```sql
CREATE TABLE jicek_pay_order (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  out_trade_no    VARCHAR(64)  NOT NULL COMMENT '商户订单号',
  trade_no        VARCHAR(64)  COMMENT '易支付流水号',
  card_type_id    BIGINT       COMMENT '购买的卡类ID',
  quantity        INT          DEFAULT 1,
  amount          DECIMAL(10,2) NOT NULL COMMENT '金额',
  pay_type        VARCHAR(20)  COMMENT 'alipay/wxpay/qqpay/unionpay',
  status          TINYINT      DEFAULT 0 COMMENT '0待支付/1已支付/2失败/3已退款/4已关闭',
  user_ip         VARCHAR(45)  COMMENT '用户支付IP',
  device          VARCHAR(20)  COMMENT 'pc/mobile',
  param           VARCHAR(255) COMMENT '业务扩展参数',
  pay_time        DATETIME     COMMENT '支付完成时间',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_trade (tenant_id, out_trade_no),
  KEY idx_status (status, create_time)
) COMMENT='支付订单';
```

### 5.3 卡类表
```sql
CREATE TABLE jicek_card_type (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL COMMENT '所属软件',
  name            VARCHAR(64)  NOT NULL COMMENT '卡类名称',
  type            TINYINT      NOT NULL COMMENT '1时长卡 2次数卡 3功能卡 4永久卡',
  duration        INT          COMMENT '时长(秒), 时长卡用',
  count           INT          COMMENT '次数, 次数卡用',
  features        VARCHAR(255) COMMENT '功能列表JSON, 功能卡用',
  price           DECIMAL(10,2) NOT NULL,
  bind_strategy   TINYINT      DEFAULT 0 COMMENT '0不绑定 1首次登录绑定 2指定N台',
  max_devices     INT          DEFAULT 1,
  enabled         TINYINT      DEFAULT 1,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  KEY idx_software (tenant_id, software_id)
) COMMENT='卡类';
```

### 5.4 卡密表
```sql
CREATE TABLE jicek_card_key (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  card_type_id    BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL,
  card_no         VARCHAR(64)  NOT NULL COMMENT '卡号',
  card_cipher     VARCHAR(512) NOT NULL COMMENT 'AES-256-GCM 加密后的卡密',
  card_hash       VARCHAR(64)  NOT NULL COMMENT '卡密SHA-256哈希(查询用)',
  status          TINYINT      DEFAULT 0 COMMENT '0未使用 1已使用 2已封禁 3已退款 4已过期',
  bound_user_id   BIGINT       COMMENT '绑定的用户ID',
  bound_devices   VARCHAR(500) COMMENT '绑定设备JSON',
  expire_time     DATETIME     COMMENT '到期时间',
  first_use_time  DATETIME,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_card (tenant_id, card_no),
  KEY idx_hash (card_hash),
  KEY idx_status (tenant_id, status)
) COMMENT='卡密';
```

### 5.5 软件表
```sql
CREATE TABLE jicek_software (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  name            VARCHAR(64)  NOT NULL,
  app_key         VARCHAR(64)  NOT NULL COMMENT '应用Key',
  sign_secret     VARCHAR(512) NOT NULL COMMENT '签名密钥(加密存储)',
  rsa_public_key  TEXT         COMMENT 'RSA公钥(客户端用)',
  rsa_private_key TEXT         COMMENT 'RSA私钥(加密存储)',
  version         VARCHAR(20)  COMMENT '当前版本',
  min_version     VARCHAR(20)  COMMENT '最低支持版本',
  heartbeat_interval INT       DEFAULT 60 COMMENT '心跳间隔(秒)',
  max_concurrent  INT          DEFAULT 1 COMMENT '最大并发会话',
  enabled         TINYINT      DEFAULT 1,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_appkey (app_key)
) COMMENT='软件';
```

### 5.6 设备表
```sql
CREATE TABLE jicek_device (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL,
  user_id         BIGINT       COMMENT '绑定用户',
  device_fingerprint VARCHAR(128) NOT NULL COMMENT '设备指纹哈希',
  device_info     TEXT         COMMENT '设备详情JSON',
  last_heartbeat  DATETIME,
  online_status   TINYINT      DEFAULT 0,
  status          TINYINT      DEFAULT 0 COMMENT '0正常 1封禁',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_fp (tenant_id, software_id, device_fingerprint),
  KEY idx_user (user_id)
) COMMENT='设备';
```

### 5.7 代理表
```sql
CREATE TABLE jicek_agent (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '所属开发者',
  parent_id       BIGINT       DEFAULT 0 COMMENT '上级代理ID, 0为顶级',
  username        VARCHAR(64)  NOT NULL,
  password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密',
  real_name       VARCHAR(64)  COMMENT '真实姓名（提现校验）',
  contact         VARCHAR(128) COMMENT '联系方式（QQ/微信/手机）',
  balance         DECIMAL(10,2) DEFAULT 0 COMMENT '可用余额',
  frozen_balance  DECIMAL(10,2) DEFAULT 0 COMMENT '冻结余额（提现中）',
  total_earnings  DECIMAL(10,2) DEFAULT 0 COMMENT '累计收益',
  total_withdraw  DECIMAL(10,2) DEFAULT 0 COMMENT '累计已提现',
  commission_rate DECIMAL(5,2)  DEFAULT 0 COMMENT '分润比例（0-100，百分比）',
  max_sub_level   INT          DEFAULT 0 COMMENT '允许发展的下级层级数，0为不可发展下级',
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  level           INT          DEFAULT 1 COMMENT '代理级别（1=顶级代理）',
  last_login_time DATETIME,
  last_login_ip   VARCHAR(45),
  remark          VARCHAR(255),
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_parent (tenant_id, parent_id),
  KEY idx_tenant (tenant_id)
) COMMENT='代理';
```

### 5.8 代理套餐表
```sql
CREATE TABLE jicek_agent_package (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  agent_id        BIGINT       NOT NULL COMMENT '所属代理（0表示所有代理默认套餐）',
  software_id     BIGINT       NOT NULL,
  card_type_id    BIGINT       NOT NULL,
  agent_price     DECIMAL(10,2) NOT NULL COMMENT '代理制卡价（≤ 卡类零售价）',
  enabled         TINYINT      DEFAULT 1,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_agent_card (tenant_id, agent_id, card_type_id)
) COMMENT='代理套餐（可售卡类+制卡价）';
```

### 5.9 分润流水表（不可变，仅审计查询）
```sql
CREATE TABLE jicek_commission (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  agent_id        BIGINT       NOT NULL COMMENT '受益代理',
  order_id        BIGINT       NOT NULL COMMENT '关联支付订单',
  out_trade_no    VARCHAR(64)  NOT NULL COMMENT '订单号（冗余，便于查询）',
  source_agent_id BIGINT       COMMENT '来源代理（下级制卡/销售），null 表示终端用户购买',
  card_type_id    BIGINT,
  order_amount    DECIMAL(10,2) NOT NULL COMMENT '订单原金额',
  commission_rate DECIMAL(5,2)  NOT NULL COMMENT '分润比例快照',
  commission_amount DECIMAL(10,2) NOT NULL COMMENT '分润金额',
  type            TINYINT      NOT NULL COMMENT '1直接销售 2下级分润 3制卡差价',
  status          TINYINT      DEFAULT 1 COMMENT '0已撤销（退款连带） 1有效',
  create_time     DATETIME     NOT NULL,
  KEY idx_agent (tenant_id, agent_id, create_time),
  KEY idx_order (order_id)
) COMMENT='分润流水（不可删除，仅审计）';
```

### 5.10 提现申请表（状态机：0待审核→1已通过→3已打款 / 0→2已拒绝 / 1→4已失败）
```sql
CREATE TABLE jicek_withdraw (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  agent_id        BIGINT       NOT NULL,
  amount          DECIMAL(10,2) NOT NULL COMMENT '申请提现金额',
  fee             DECIMAL(10,2) DEFAULT 0 COMMENT '手续费',
  actual_amount   DECIMAL(10,2) NOT NULL COMMENT '实际到账金额',
  pay_type        VARCHAR(20)  NOT NULL COMMENT 'alipay/wxpay/bank',
  pay_account     VARCHAR(128) NOT NULL COMMENT '收款账号',
  pay_name        VARCHAR(64)  COMMENT '收款人姓名',
  status          TINYINT      DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝 3已打款 4已失败',
  audit_by        BIGINT       COMMENT '审核人（开发者用户ID）',
  audit_time      DATETIME,
  audit_remark    VARCHAR(255),
  trade_no        VARCHAR(64)  COMMENT '打款流水号',
  fail_reason     VARCHAR(255),
  apply_time      DATETIME     NOT NULL,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  KEY idx_agent (tenant_id, agent_id, status),
  KEY idx_status (tenant_id, status, create_time)
) COMMENT='提现申请';
```

### 5.11 云函数表（v0.4.2 新增）
```sql
CREATE TABLE jicek_cloud_function (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL,
  name            VARCHAR(64)  NOT NULL COMMENT '函数名（字母开头，字母数字下划线，最长64）',
  description     VARCHAR(255) COMMENT '描述',
  code            MEDIUMTEXT   NOT NULL COMMENT 'Lua 代码（最大 64KB）',
  runtime         VARCHAR(20)  DEFAULT 'lua' COMMENT '运行时（当前仅 lua）',
  timeout_ms      INT          NOT NULL COMMENT '超时毫秒（100-30000）',
  memory_limit_kb INT          NOT NULL COMMENT '内存上限 KB',
  max_input_kb    INT          NOT NULL COMMENT '输入上限 KB',
  max_output_kb   INT          NOT NULL COMMENT '输出上限 KB',
  enabled         TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
  version         INT          DEFAULT 1 COMMENT '版本号（每次更新自增）',
  invoke_count    BIGINT       DEFAULT 0 COMMENT '累计调用次数',
  last_invoke_time DATETIME    COMMENT '最后调用时间',
  last_invoke_ip  VARCHAR(64)  COMMENT '最后调用 IP',
  create_by       BIGINT,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_sw_name (tenant_id, software_id, name)
) COMMENT='云函数';
```

### 5.12 云函数执行日志表（v0.4.2 新增，审计表，仅 INSERT + SELECT，禁 UPDATE/DELETE）
```sql
CREATE TABLE jicek_cloud_function_log (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  function_id     BIGINT       NOT NULL,
  function_name   VARCHAR(64)  NOT NULL COMMENT '函数名快照',
  software_id     BIGINT       NOT NULL,
  invoke_source   VARCHAR(10)  NOT NULL COMMENT 'dev(开发者测试) / sdk(客户端调用)',
  caller_ip       VARCHAR(64),
  input_size      INT          NOT NULL COMMENT '输入字节数',
  output_size     INT          NOT NULL COMMENT '输出字节数',
  duration_ms     INT          NOT NULL COMMENT '执行耗时毫秒',
  status          TINYINT      NOT NULL COMMENT '0成功 1编译失败 2运行时错误 3超时 4内存超限 5输入超限 6输出超限',
  error_message   VARCHAR(4096) COMMENT '错误信息（截断至 4KB）',
  create_time     DATETIME     NOT NULL,
  KEY idx_func (tenant_id, function_id, create_time),
  KEY idx_software (tenant_id, software_id, create_time),
  KEY idx_status (tenant_id, status, create_time)
) COMMENT='云函数执行日志（审计，禁 UPDATE/DELETE）';
```

### 5.13 部署审计日志表（v0.5.0 新增，审计表，仅 INSERT + SELECT + 受控更新 status，禁 UPDATE 其他字段 / DELETE）
```sql
CREATE TABLE jicek_deploy_log (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
  trigger_source  VARCHAR(10)  NOT NULL COMMENT 'webhook / manual',
  commit_hash     VARCHAR(64)  COMMENT '触发部署的 commit hash',
  branch          VARCHAR(64)  NOT NULL DEFAULT 'main' COMMENT '部署分支',
  status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0进行中 1成功 2失败 3已回滚',
  duration_ms     BIGINT       COMMENT '部署耗时毫秒',
  operator_ip     VARCHAR(64)  COMMENT '触发者 IP（webhook 为 GitHub IP，manual 为操作者 IP）',
  error_message   VARCHAR(4096) COMMENT '错误信息（截断至 4KB）',
  create_time     DATETIME     NOT NULL,
  KEY idx_status (status, create_time),
  KEY idx_source (trigger_source, create_time)
) COMMENT='部署审计日志（审计，禁 UPDATE/DELETE）';
```

### 5.14 工单表（v0.6.0 新增，双向工单：终端用户→开发者 / 开发者→管理员）
```sql
CREATE TABLE jicek_ticket (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  ticket_no       VARCHAR(32)  NOT NULL COMMENT '工单号（TK+时间戳+随机）',
  title           VARCHAR(128) NOT NULL,
  content         TEXT         NOT NULL,
  category        TINYINT      NOT NULL COMMENT '1换机申请 2充值问题 3卡密问题 4其他',
  target          TINYINT      NOT NULL COMMENT '1开发者 2管理员',
  status          TINYINT      DEFAULT 0 COMMENT '0待处理 1处理中 2已回复 3已关闭',
  creator_type    TINYINT      NOT NULL COMMENT '1终端用户 2开发者',
  creator_id      BIGINT       NOT NULL,
  creator_name    VARCHAR(64),
  software_id     BIGINT,
  device_id       BIGINT,
  handler_id      BIGINT,
  handler_time    DATETIME,
  close_time      DATETIME,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_tenant_status (tenant_id, status, create_time),
  KEY idx_tenant_target (tenant_id, target, status),
  KEY idx_creator (creator_type, creator_id, create_time),
  KEY idx_handler (handler_id, status)
) COMMENT='工单主表';

CREATE TABLE jicek_ticket_reply (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  ticket_id       BIGINT       NOT NULL,
  replier_type    TINYINT      NOT NULL COMMENT '1用户 2开发者 3管理员',
  replier_id      BIGINT       NOT NULL,
  replier_name    VARCHAR(64),
  content         TEXT         NOT NULL,
  create_time     DATETIME     NOT NULL,
  KEY idx_ticket (tenant_id, ticket_id, create_time),
  KEY idx_replier (replier_type, replier_id, create_time)
) COMMENT='工单回复（审计，禁 UPDATE/DELETE）';
```

### 5.15 鉴权用户表（v0.7.0 新增，双角色独立表）
```sql
-- 开发者用户表（带 tenantId，多租户隔离）
CREATE TABLE jicek_dev_user (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '所属租户',
  username        VARCHAR(64)  NOT NULL,
  password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希（cost=10）',
  nickname        VARCHAR(64),
  email           VARCHAR(128),
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  last_login_time DATETIME,
  last_login_ip   VARCHAR(45),
  login_count     INT          DEFAULT 0,
  remark          VARCHAR(255),
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_status (status)
) COMMENT='开发者用户';

-- 管理员用户表（无 tenantId，全局超管/运营）
CREATE TABLE jicek_admin_user (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(64)  NOT NULL,
  password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希（cost=10）',
  nickname        VARCHAR(64),
  role            TINYINT      DEFAULT 1 COMMENT '1超管 2运营',
  email           VARCHAR(128),
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  last_login_time DATETIME,
  last_login_ip   VARCHAR(45),
  login_count     INT          DEFAULT 0,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_username (username),
  KEY idx_status (status)
) COMMENT='管理员用户';

-- 初始化账号：
-- admin / BCrypt('admin@123')  超管
-- dev   / BCrypt('dev@123')    默认开发者 tenant_id=1
```

## 6. 使用指南（待实现后补全）

### 6.1 部署要求
- JDK 17 或 21
- MySQL 8.0.42+
- Redis 7.2.8+
- PHP 8.0+（彩虹易支付独立部署）
- Docker（普通 Docker 或宝塔面板内 Docker）

### 6.2 快速启动
（待代码完成后补全）

## 7. 自动更新系统（v0.5.0 已实现 ✅）

### 7.1 触发方式
- GitHub Webhook 自动触发（push 到 main 分支，HMAC-SHA256 验签）
- 管理员后台手动触发（ConfirmDialog 二次确认）

### 7.2 更新流程
```
备份(jar+dist → .jicek-backup/{ts}/) → git pull → mvn build → npm build → 重启 → 健康检查 → 标记成功
                                                                        ↓ 失败
                                                                    还原备份 → 重启 → 标记已回滚
```

### 7.3 重启策略（restart-mode 配置）
- `docker` 模式：`docker restart {container}`
- `btpanel` 模式：HTTP 调用宝塔 API 重启容器
- `none` 模式：跳过重启（仅构建）

### 7.4 安全
- Webhook 签名验证（HMAC-SHA256 + `MessageDigest.isEqual` 常量时间比较防时序攻击）
- Redisson 分布式锁 `jicek:deploy:lock`（防并发，5 分钟自动释放防死锁）
- 完整审计日志（`jicek_deploy_log` 表，仅 INSERT + SELECT + 受控更新 status）
- 失败自动回滚（备份保留 3 个，`DEPLOY_BACKUP_KEEP_COUNT`）
- 外部命令执行用 `ProcessBuilder` 参数化（禁 `Runtime.exec` 防 shell 注入）
- Webhook 异步执行（daemon 线程 `jicek-deploy-{logId}`，立即返回 accepted 避免 GitHub 超时）

### 7.5 健康检查
- 轮询 `{health-check-base-url}/actuator/health`
- 超时 60s（`DEPLOY_HEALTH_CHECK_TIMEOUT_SECONDS`），间隔 3s（`DEPLOY_HEALTH_CHECK_INTERVAL_SECONDS`）
- 超时未恢复触发回滚

### 7.6 配置（application.yml + 环境变量）
| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `jicek.deploy.enabled` | `JICEK_DEPLOY_ENABLED` | false | 部署功能开关（默认关闭，防开发环境误触发） |
| `jicek.deploy.webhook-secret` | `GITHUB_WEBHOOK_SECRET` | (空) | Webhook 签名密钥 |
| `jicek.deploy.project-root` | `JICEK_DEPLOY_PROJECT_ROOT` | /workspace | 项目根目录 |
| `jicek.deploy.restart-mode` | `JICEK_DEPLOY_RESTART_MODE` | none | 重启模式：docker/btpanel/none |
| `jicek.deploy.docker-container` | `JICEK_DEPLOY_DOCKER_CONTAINER` | jicek-app | Docker 容器名 |
| `jicek.deploy.btpanel-api-url` | `BTPANEL_API_URL` | (空) | 宝塔 API URL |
| `jicek.deploy.btpanel-api-key` | `BTPANEL_API_KEY` | (空) | 宝塔 API Key |
| `jicek.deploy.health-check-base-url` | `JICEK_DEPLOY_HEALTH_URL` | http://127.0.0.1:8080 | 健康检查 URL |

## 8. 目录结构
（见 2.2 节）

## 9. 交接文档

### 9.1 README.md
项目对外的 GitHub 介绍页，包含：项目简介、版本进度、技术栈、模块结构、快速开始、API 概览、数据库表、角色权限、部署、安全规范。

### 9.2 PROMPT.md
给下一个 AI 的接手指南，包含：
- 强制阅读顺序（README → PROJECT → SPEC → UI-DESIGN → CHANGELOG → TODO → SQL → yml）
- 项目快照（版本/仓库/技术栈）
- 已完成模块清单
- 待办任务（P1/P2/P3）
- 编码铁律三件套（04/06/13）
- 架构要点（资金安全/异步回调幂等/卡密安全/多租户）
- 关键配置项
- API 路由表
- 前端规范
- 开发流程
- 常见陷阱（V1 签名/回调返回值/金额精度/卡密明文/多租户）
- 验证清单（10 项自检）

### 9.3 文件顶部注释规范
- Java 文件：Javadoc 格式 `/** ... */`，置于类声明前，含「作者/日期/用途/安全说明」
- Vue 文件：HTML 注释格式 `<!-- ... -->`，置于 `<template>` 前，含「作者/日期/功能/接口/安全」
- TS/SCSS 文件：块注释格式 `/** ... */`，置于文件首行
