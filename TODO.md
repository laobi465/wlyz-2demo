# 待完成文档

## P0（紧急 - 安全/资金相关）

### [已完成] 设备指纹采集与绑定 ✅
- 优先级：P1
- 完成版本：v0.3.0
- 完成项：
  - [x] 5 维 SHA-256 融合（CPU/主板/硬盘/网卡/BIOS）
  - [x] VM/容器补充维度融合（VM UUID / 容器 ID）
  - [x] RSA 加密传输 + 服务端独立计算指纹（防篡改）
  - [x] 常量时间比对（防时序攻击）
  - [x] 设备绑定 + 换机（@Transactional 同事务）
  - [x] 16 位换机码（SecureRandom，24h 有效）
  - [x] 心跳保活（动态间隔 5-300s + nonce 防重放 + HMAC 签名）
  - [x] 设备封禁/解封 + 超时置离线
- 备注：v0.3.1 待接入 CardKeyService.useCard 完整流程 + Sa-Token 鉴权 + software 表读取签名密钥/心跳间隔

### [已完成] 交接文档生成 ✅
- 优先级：P0
- 完成版本：v0.2.1
- 完成项：
  - [x] README.md 重写为完整 GitHub 介绍
  - [x] PROMPT.md 下一个 AI 接手指南
  - [x] 后端核心文件顶部注释补全（CardKeyMapper / CardTypeMapper）
  - [x] 前端核心文件顶部注释补全（11 个 Vue/TS 文件）

### [已完成] 卡密加密存储与传输 ✅
- 优先级：P0
- 完成版本：v0.2.0
- 完成项：
  - [x] AES-256-GCM 加密入库实现（AesCryptoService）
  - [x] RSA-2048-OAEP 传输加密实现（RsaCryptoService）
  - [x] 卡密明文仅展示一次机制（生成响应 + 卡密列表脱敏）
  - [ ] 国密 SM2/SM4 可选实现（v0.3.0）

### [已完成] 支付适配层 V1 实现 ✅
- 优先级：P0
- 完成版本：v0.2.0
- 完成项：
  - [x] PayAdapterV1Impl（MD5 签名 + form-urlencoded）
  - [x] PayNotifyController（异步回调验签 + 幂等，GET/POST 兼容）
  - [x] PayOrderStateMachineService（5 状态不可逆流转）
  - [x] PayConfigService（商户密钥 AES 加密存储）
  - [x] 退款接口对接 + 管理员二次确认弹窗
  - [x] PayOrderService（订单分页查询 + 超时关闭）

### [已完成] 异步回调幂等机制 ✅
- 优先级：P0
- 完成版本：v0.2.0
- 完成项：
  - [x] Redis 分布式锁（Redisson，按订单号）
  - [x] 订单状态机校验（仅 status=0 才处理）
  - [x] 返回纯字符串 success（无 BOM、无空格）
  - [x] 重复回调日志记录（INFO 级别）

### [已完成] 资金一致性事务 ✅
- 优先级：P0
- 完成版本：v0.2.0
- 完成项：
  - [x] PaymentTransactionService（@Transactional）
  - [x] 订单状态更新与卡密发放同事务
  - [x] 退款时关联卡密同步失效（status=3）

## P1（高）

### [已完成] 客户端 SDK 8 语言实现 ✅
- 优先级：P1
- 完成版本：v0.3.1
- 完成项：
  - [x] Java SDK（12 文件，零第三方依赖，自研 JSON 解析器）
  - [x] Python SDK（stdlib + cryptography）
  - [x] Node.js SDK（crypto + https，零依赖）
  - [x] Go SDK（stdlib，零依赖）
  - [x] C# SDK（BCL，.NET 6+）
  - [x] C++ SDK（OpenSSL + libcurl）
  - [x] Lua SDK（luaossl/luasocket 可选，回退 openssl/curl CLI）
  - [x] Shell SDK（bash 4+ + curl + openssl，jq 可选）
  - [x] 易语言模块（精易模块原生方案 + C++ DLL 方案）
- 统一契约规范：[sdk/README.md](sdk/README.md)
- 备注：v0.3.2 待接入真实服务端联调测试 + 加壳工具推荐文档

### [已完成] 多级代理 + 分润 + 提现 ✅
- 优先级：P1
- 完成版本：v0.4.0
- 完成项：
  - [x] 代理树形结构存储（parent_id + level + max_sub_level，isDescendant 防环）
  - [x] 分润比例配置（commission_rate DECIMAL(5,2)，0-100）
  - [x] 向上链式分润（直推 type=1 + 父级链 type=2，最多 10 层，同事务原子更新）
  - [x] 分润撤销（退款触发，余额不足保护）
  - [x] 代理充值 / 扣余额（开发者充值 + 制卡扣款）
  - [x] 提现申请工作流（简单状态机：0待审核→1已通过→3已打款 / 0→2已拒绝 / 1→4已失败）
  - [x] 提现审核 + 资金流原子操作（balance↔frozenBalance↔totalWithdraw）
  - [x] 前端代理管理页 + 提现审核页 + API/路由/菜单集成
- 备注：未引入 WarmFlow，采用简单状态机模式（对标 PayOrderStateMachineService）；待接入 PaymentTransactionService 在支付成功回调触发 grantCommission

### [已完成] 前端补全 ✅
- 优先级：P1
- 状态：全部完成
- 子项：
  - [x] 软件管理页面 ✅ v0.8.0（CRUD + 密钥轮换 + 关联校验）
  - [x] 卡类管理页面 ✅ v0.4.1（CRUD + 4 种类型联动表单）
  - [x] 终端用户管理页面 ✅ v0.14.0（CRUD + 封禁 + 重置密码，views/dev/end-user/）
  - [x] 设备管理页面 ✅ v0.4.1（分页 + 详情 + 封禁/解封 + 指纹脱敏）
  - [x] 代理管理页面 ✅ v0.4.0（代理列表 + 提现审核）
  - [x] 数据统计图表 ✅ v0.4.1 + v0.4.3（Dashboard 饼图/柱状图 + 数据统计页 4 Tab）
  - [x] H5 终端用户页面 ✅ v0.13.0（7 页：登录/我的卡密/公告/代理注册/H5 购卡/订单）

### [已完成] 前端补全 - 第一批 ✅
- 优先级：P1
- 完成版本：v0.4.1
- 完成项：
  - [x] 卡类管理页（views/dev/card-type/index.vue）：CRUD + 类型联动表单 + 表单校验
  - [x] 设备管理页（views/dev/device/index.vue）：分页查询 + 详情弹窗 + 封禁/解封 + 指纹脱敏
  - [x] Dashboard ECharts 集成：卡密状态分布饼图 + 今日收支柱状图
  - [x] StatusTag.vue 扩展：新增 device 类型，支持 4 种状态语义
  - [x] deviceApi 新增（page/get/ban/unban），含 current→page 参数映射
  - [x] 路由新增 /card-type + /device；侧边栏新增卡类管理 + 用户管理子菜单
- 技术约束：依据铁律 06（防幻觉），仅实现后端 Controller 已存在的页面；H5 已于 v0.13.0 实现，终端用户管理页已于 v0.14.0 实现

## P2（中）

### [已完成] 云函数远程执行 ✅
- 优先级：P2
- 完成版本：v0.4.2
- 完成项：
  - [x] 沙箱隔离（LuaJ 3.0.6 纯 Java 实现 Lua 5.4 子集，全局表裁剪禁用 os/io/loadfile/dofile/require/debug/package/load）
  - [x] 执行超时限制（独立 jicek-lua-sandbox 线程池 + Future.get(timeoutMs) + cancel(true) 强制中断）
  - [x] 资源配额（内存上限 memoryLimitKb + 输入上限 maxInputKb + 输出上限 maxOutputKb 硬截断）
  - [x] 审计日志（jicek_cloud_function_log 表，仅 INSERT + SELECT，禁 UPDATE/DELETE）
  - [x] 云函数 CRUD + 测试执行 + 执行日志查询（后端 DevCloudFunctionController + 前端双 Tab 页面）
  - [x] 前端路由 /cloud-func + 侧边栏「云端数据」子菜单集成
- 备注：抗破解终极方案；SDK 调用走 SdkCloudFunctionController 待后续版本实现（复用同一 Service）；资源配额仅内存/IO，CPU 配额未实现（LuaJ 纯 Java 难以精确限制 CPU）

### [已完成] 远程公告 ✅
- 优先级：P1
- 完成版本：v0.10.0
- 完成项：
  - [x] 公告表 jicek_announcement（15 字段 + 2 索引）
  - [x] 开发者后台 CRUD + 发布/下线状态机（草稿→已发布→已下线，不可逆）
  - [x] SDK 拉取已发布公告（GET /api/sdk/announcement，SdkAuthFilter 鉴权）
  - [x] 客户端版本范围匹配（minVersion/maxVersion，语义化版本比较）
  - [x] 排序：pinned DESC + sortOrder DESC + publishTime DESC
  - [x] viewCount 拉取次数累加
  - [x] 前端公告管理页 + 路由 + 菜单

### [已完成] 自动更新模块 ✅
- 优先级：P1
- 完成版本：v0.11.0
- 完成项：
  - [x] 更新包表 jicek_update_package（19 字段 + 2 索引）
  - [x] 文件上传（multipart + SHA-256 + 路径穿越防御 + UUID 文件名）
  - [x] 开发者后台 CRUD + 发布/下线状态机（草稿→已发布→已下线，不可逆）
  - [x] 多格式支持（exe/sh/win/lua/zip/7z）
  - [x] 双通道（1稳定版 2内测版，SDK 按通道拉取）
  - [x] 强制更新（forceUpdate=1 旧版拒绝运行）
  - [x] 客户端版本范围匹配（minClientVersion/maxClientVersion）
  - [x] SDK 检查更新（GET /api/sdk/update/check，返回 hasUpdate/forceUpdate/downloadUrl/sha256）
  - [x] Storage 配置类（storage.root / downloadBaseUrl / updateSubDir，环境变量注入）
  - [x] 前端更新包管理页（上传进度条 + CRUD + 发布/下线 + 查看 + 删除）+ 路由 + 菜单

### [已完成] SDK 代码生成器 + 对接文档 ✅
- 优先级：P2
- 完成版本：v0.12.0
- 完成项：
  - [x] 9 语言代码模板生成器（Python/C#/C++/Go/Java/Node.js/Lua/Shell/易语言）
  - [x] 代码生成弹窗（语言 Tab + 复制 + 自动填入 appKey/RSA公钥）
  - [x] 软件管理页新增「接入代码」按钮
  - [x] 对接文档页（接入流程 + 凭证 + 请求头 + 签名算法 + RSA + API + 错误码 + SDK 索引）
  - [x] 路由 /integration-doc + 系统设置子菜单
- 备注：signSecret 脱敏无法自动填入，模板留占位符；覆盖 card/login + heartbeat + announcement + update/check 四接口

### [已完成] UI 设计规范 ✅
- 优先级：P2
- 完成版本：v0.1.0（文档）/ v0.2.0（前端骨架）
- 状态：规范已定 + 前端骨架已实现

### [已完成] 数据统计与可视化 ✅
- 优先级：P2
- 完成版本：v0.4.3
- 子项：
  - [x] 验证量趋势图（折线图，按小时/天/月，卡密激活 + 新增设备双线）
  - [x] 设备在线热力图（ECharts heatmap，近 7 天 × 24 小时，基于 last_heartbeat 聚合）
  - [x] 收入统计（按通道/卡类/代理分维度，柱状图+折线图双 Y 轴 + 明细表格 + 占比进度条）
  - [x] 防破解事件统计（封禁设备/卡密/IP 汇总 + 按天趋势折线图）
- 备注：
  - 数据源全部基于现有业务表聚合，无独立统计表（铁律 06）
  - 代理维度因 PayOrder 暂无 agent_id 字段，前端显示 alert 提示「待扩展」，待后续 PayOrder 扩展后补全
  - Dashboard 页面（v0.4.1）保留作为今日汇总快照，数据统计页（v0.4.3）作为多维分析入口，二者职责互补

## P3（低）

### [已完成] GitHub 自动更新部署 ✅
- 优先级：P3
- 完成版本：v0.5.0
- 完成项：
  - [x] GitHub Webhook 自动触发（HMAC-SHA256 签名验证 + 常量时间比较防时序攻击）
  - [x] 管理员后台手动触发（二次确认 + ConfirmDialog）
  - [x] 部署编排：备份 → git pull → mvn build → npm build → 重启 → 健康检查 → 失败自动回滚
  - [x] Redisson 分布式锁防并发（jicek:deploy:lock，5 分钟自动释放）
  - [x] 重启模式分发：docker（docker restart）/ btpanel（宝塔 API HTTP 调用）/ none（跳过）
  - [x] 健康检查轮询（/actuator/health，超时 60s，间隔 3s）
  - [x] 部署审计日志（jicek_deploy_log 表，仅 INSERT + SELECT + 受控更新 status，禁 UPDATE/DELETE）
  - [x] Webhook 异步执行（daemon 线程 jicek-deploy-{logId}，避免 GitHub 超时）
  - [x] 前端部署管理页（3 状态卡片 + 手动触发 + 日志表格 + 状态轮询）+ 路由 /deploy + 侧边栏「系统设置」子菜单
  - [x] 5 份核心文档同步（CHANGELOG/TODO/PROMPT/PROJECT/SPEC）
- 备注：
  - 部署功能默认关闭（jicek.deploy.enabled=false），需显式开启
  - Webhook Secret / 项目根目录 / 重启模式等全部走环境变量注入（铁律 04）
  - 错误码范围 7001-7010，新增 18 个部署常量
  - 后续可扩展：DB 迁移步骤（当前仅代码构建）、Slack/钉钉通知、灰度发布

### [已完成] 工单系统 ✅
- 优先级：P3
- 完成版本：v0.6.0，v0.6.1 简化为单向（开发者→管理员）
- 完成项：
  - [x] 单向工单（开发者→管理员，v0.6.1 取消终端用户→开发者方向）
  - [x] 基础 CRUD + 状态机（0待处理→1处理中→2已回复→3已关闭）
  - [x] 工单分类（1换机申请 2充值问题 3卡密问题 4其他）
  - [x] DevTicketController（提交/查询/详情/补充回复 4 接口）
  - [x] 前端工单管理页（列表 + 新建工单 + 详情对话流 + 补充回复）
  - [x] 回复审计表（jicek_ticket_reply，仅 INSERT+SELECT）
  - [x] 5 份核心文档同步
- 备注：
  - 管理员端 Controller（处理开发者提交的工单，replierType=3）待管理员后台框架就绪后补全
  - 后续可扩展：附件上传（需 MinIO）、优先级、SLA 超时提醒

### [已完成] 鉴权框架 ✅
- 优先级：P1
- 完成版本：v0.7.0
- 完成项：
  - [x] JWT 鉴权（JJWT 0.12.6 替代原 SPEC.md 描述的 Sa-Token，HMAC-SHA256 签名）
  - [x] 双角色体系：开发者 ROLE_DEV=1（带 tenantId）+ 管理员 ROLE_ADMIN=2（无 tenantId）
  - [x] BCrypt 密码哈希（cost=10）+ 登录失败统一返回防枚举
  - [x] @AuthRequired 注解 + JwtAuthInterceptor 渐进式鉴权（未标注放行，兼容现有裸传参数接口）
  - [x] AuthContext ThreadLocal 持有当前用户身份，afterCompletion 强制清理防串号
  - [x] 4 接口：dev/login + admin/login + me + change-password
  - [x] 前端登录页 + router 守卫 + 拦截器自动注入 token + DevLayout 退出/改密
  - [x] 11 个鉴权错误码 9001-9011 + 5 份核心文档同步
- 备注：
  - 密钥通过环境变量 JICEK_JWT_SECRET 注入（至少 32 字节），未配置时 warn 但不阻止启动
  - 渐进式鉴权设计：现有接口未加 @AuthRequired 仍可访问，新接口从 AuthContext 取身份
  - 后续可扩展：管理员端 Controller（处理开发者工单 + 租户管理）、@AuthRequired(role=2) 限制管理员接口、代理/用户角色

### [已完成] v0.13.0 新增功能 ✅
- 优先级：P1
- 完成版本：v0.13.0
- 完成项：
  - [x] H5 验证界面：`h5/` 后端模块（H5Session + UUID token 24h + DB/Redis 双写 + H5AuthInterceptor）+ `views/h5/` 7 页前端
  - [x] 代理邀请码注册：Agent 表加 invite_code/invited_by 字段 + 8 位 SecureRandom 邀请码 + 公开注册接口继承邀请人配置
  - [x] 内嵌卡网系统：`shop/` 后端模块（店铺 + 商品，可覆盖卡类售价）+ DevShopController 11 接口 + H5 公开查询 + H5 需 token 下单
- 备注：详见 [CHANGELOG.md](CHANGELOG.md) v0.13.0 条目；5 份核心文档已同步

### [已完成] v0.14.0 新增功能 ✅
- 优先级：P1（终端用户账号体系）+ P3（多语言国际化）
- 完成版本：v0.14.0
- 完成项：
  - [x] 终端用户账号体系：`enduser/` 后端模块（jicek_end_user 表 + 三元唯一 + BCrypt）+ DevEndUserController 8 接口（CRUD + ban/unban + reset-password）+ H5 公开登录接口复用 H5Session + 错误码 1053-1057
  - [x] 多语言国际化：vue-i18n 9.x（Composition API）+ 中英文语言包（六模块）+ LangSwitch 顶栏组件 + 渐进式改造（登录页 + DevLayout + 终端用户页全量 i18n）
- 备注：详见 [CHANGELOG.md](CHANGELOG.md) v0.14.0 条目；5 份核心文档已同步

### [已完成] 多语言国际化 ✅
- 优先级：P3
- 完成版本：v0.14.0
- 备注：vue-i18n 9.x 中英文 + 渐进式改造，详见 v0.14.0 新增功能条目

### [已完成] v0.15.0 新增功能 ✅
- 优先级：P1
- 完成版本：v0.15.0
- 完成项：
  - [x] 代理制卡扣余额：CardKeyService.batchGenerate 代理制卡分支调用 AgentService.deductBalance（先扣款再生成卡密，同事务）；CardKeyGenRequestDTO 加 agentId 字段
  - [x] 分润接入支付回调：PayOrder 加 agentId 字段 + jicek_pay_order 表加 idx_agent 索引；PayNotifyService 支付成功事务提交后调 CommissionService.grantCommission（分润独立事务 + try-catch，分润失败不回滚卡密）；jicek_commission 表加 uk_order_agent(out_trade_no, agent_id) 幂等索引
  - [x] 管理员端 Controller：AdminTicketController（/api/admin/ticket 4 接口 page/get/reply/close）+ AdminDevUserController（/api/admin/dev-user 5 接口 page/get/ban/unban/reset-password），@AuthRequired(role=JicekConstants.ROLE_ADMIN)；DevUserService 新建；前端 AdminLayout + 管理员登录/工单/开发者管理 3 页 + adminAxios 独立实例 + jicek_admin_token 隔离
  - [x] 多语言国际化全量：17 个 dev 页面全量 i18n 改造 + 语言包扩展 16 个新模块（dashboard/software/cardType/cardKey/device/agent/withdraw/payConfig/payOrder/cloudFunc/stats/ticket/deploy/updatePackage/announcement/shop + admin），所有用户可见文案支持中英文切换
- 备注：详见 [CHANGELOG.md](CHANGELOG.md) v0.15.0 条目；7 份核心文档已同步（CHANGELOG/README/PROMPT/PROJECT/SPEC/UI-DESIGN/TODO）

## 待实现清单

全部完成，无待实现项。核心功能与扩展项已全部完成，后续按需迭代。
