# 更新日志

## [0.8.0] - 2026-07-22

### [新增] 软件管理模块（卡密/设备/云函数的父实体，接入鉴权框架）

软件表 `jicek_software` 早已存在但后端无模块，本次补全垂直切片。所有接口 `@AuthRequired(role=ROLE_DEV)`，tenantId 从 AuthContext 获取，前端禁传（防越权）。

- **后端**：`software/` 模块（entity/mapper/dto/service/controller）
  - `Software` entity + `SoftwareMapper`
  - 3 DTO：`SoftwareSaveDTO`（JSR-303 校验）/ `SoftwareDetailDTO`（signSecret 脱敏，无 rsaPrivateKey）/ `SoftwareCreateResultDTO`（明文一次性返回）
  - `SoftwareService`：create/update/page/get/delete/regenerateSignSecret/regenerateRsaKey
  - `DevSoftwareController`：7 接口，类级 `@AuthRequired(role=ROLE_DEV)`
- **密钥自动生成**：
  - appKey：SecureRandom 32 字符（大写字母+数字），全局唯一查重（最多 5 次重试）
  - signSecret：SecureRandom 32 字节 → Base64，AES-256-GCM 加密存储
  - RSA-2048 密钥对：公钥明文 + 私钥 AES-256-GCM 加密存储
- **密钥轮换**：`POST /{id}/regenerate-sign-secret` + `POST /{id}/regenerate-rsa-key`，返回新明文（仅此一次）
- **删除关联校验**：关联卡类/设备/云函数时拒绝删除（错误码 1014/1015/1016）
- **安全铁律**：signSecret 查询返回脱敏（前 4 字符 + ****）；rsaPrivateKey 永不返回；所有操作校验 `software.tenantId == AuthContext.currentTenantId()`
- **错误码**：1012-1019 共 8 个（SOFTWARE_NOT_FOUND/NAME_EXISTS/HAS_CARD_TYPE/HAS_DEVICE/HAS_CLOUD_FUNC/DISABLED/PARAM_INVALID/PERMISSION_DENIED）
- **前端**：
  - `softwareApi` 7 方法
  - `views/dev/software/index.vue` 软件管理页（列表 + 创建/编辑弹窗 + 密钥展示弹窗 + 轮换二次确认 + 复制按钮）
  - 密钥展示弹窗：`show-close=false` + `close-on-click-modal=false` + 警告「仅此一次展示」
  - 路由 `/software` + DevLayout 菜单「软件管理」入口

## [0.7.0] - 2026-07-22

### [新增] 鉴权框架（JWT + BCrypt + @AuthRequired 渐进式鉴权）

替代 SPEC.md 2.1 节原描述的 Sa-Token（实际未引入依赖），改用 JJWT 0.12.6 轻量标准库；双角色（开发者 ROLE_DEV=1 / 管理员 ROLE_ADMIN=2）+ ThreadLocal 上下文 + 注解式鉴权，兼容现有裸传参数接口。

- **数据库**：`jicek_dev_user`（15 字段 + uk_tenant_username）+ `jicek_admin_user`（11 字段 + uk_username，含 role 字段：1超管 2运营）
- **默认账号**：admin/admin@123（超管）、dev/dev@123（tenantId=1）
- **后端**：2 Entity + 2 Mapper + 4 DTO（Login/LoginResult/ChangePassword/UserInfo）+ JwtService（HMAC-SHA256，密钥环境变量 JICEK_JWT_SECRET 至少 32 字节）+ AuthContext（ThreadLocal，afterCompletion 强制清理防串号）+ @AuthRequired 注解（方法级优先，类级兜底；未标注放行，渐进式兼容）+ JwtAuthInterceptor + WebMvcConfig + AuthService + AuthController
- **接口**：POST /api/auth/dev/login、POST /api/auth/admin/login、GET /api/auth/me、POST /api/auth/change-password
- **安全**：BCrypt 密码哈希（cost=10）；登录失败统一返回 AUTH_PASSWORD_ERROR（防用户枚举）；JWT claims 含 uid/role/tenantId/username；密钥未配置时 warn 但不阻止启动
- **错误码**：9001-9011 共 11 个（AUTH_TOKEN_MISSING/AUTH_TOKEN_INVALID/AUTH_TOKEN_WRONG_ROLE/AUTH_USER_NOT_FOUND/AUTH_PASSWORD_ERROR/AUTH_USER_BANNED/AUTH_USER_ALREADY_EXISTS/AUTH_OLD_PASSWORD_ERROR/AUTH_PASSWORD_TOO_SHORT/AUTH_ROLE_INVALID/AUTH_NO_PERMISSION）
- **前端**：request.ts 拦截器自动注入 `Authorization: Bearer {token}` + 401/9001/9002/9003 自动清 token 跳 /login；新增登录页（租户ID+用户名+密码表单）；router beforeEach 守卫（无 token 跳 /login，已登录访问 /login 跳 /dashboard）；DevLayout 顶栏接入用户昵称头像 + 修改密码弹窗 + 退出登录二次确认
- **拦截路径**：/api/dev/** + /api/admin/** 走鉴权；排除 /api/auth/** + /api/sdk/** + /api/h5/** + /api/pay/notify/** + /api/deploy/webhook + /actuator/**

## [0.6.1] - 2026-07-22

### [调整] 工单系统简化为单向（开发者→管理员）

取消终端用户→开发者方向，仅保留开发者→管理员，移除 H5TicketController 与收件箱功能。

- 删除 `H5TicketController`，移除 `DevTicketController` 的 receive 系列接口
- `TicketService.page` 简化签名，强制查询 creatorType=2/target=2 的工单
- 移除终端用户相关常量（TICKET_TARGET_DEV / TICKET_CREATOR_USER / TICKET_REPLIER_USER）
- 前端移除收件箱 Tab，只保留「已提交」+ 新建工单 + 补充回复
- SQL `target`/`creator_type` 字段注释标注恒为 2

## [0.6.0] - 2026-07-22

### [新增] 工单系统（双向工单）

双向工单全层实现：终端用户→开发者（H5）+ 开发者→管理员（Dev），基础 CRUD + 状态机 + 分类。

- **数据库**：`jicek_ticket`（工单主表，4 索引）+ `jicek_ticket_reply`（回复审计表，仅 INSERT+SELECT）
- **后端**：entity/mapper/dto/service/双 Controller（H5TicketController `/api/h5/ticket` + DevTicketController `/api/dev/ticket`）
- **状态机**：0待处理 →[用户回复]→ 1处理中 →[开发者回复]→ 2已回复 →[关闭]→ 3已关闭；任意状态可关闭；已关闭禁回复
- **安全**：creatorType/target/replierType 由 Controller 按入口设定（防越权）；租户隔离；回复表审计不可变
- **分类**：1换机申请 2充值问题 3卡密问题 4其他
- **错误码**：8001-8010（10 个工单错误码）
- **前端**：双 Tab 页面（收件箱 + 已提交）+ 详情对话框 + 回复对话流 + 新建工单表单 + 路由 /ticket + 侧边栏「系统设置 > 工单管理」
- **H5 端**：后端 Controller 已实现，前端页面待 H5 整体框架就绪后补全（铁律 06，不虚构未实现框架）

## [0.5.0] - 2026-07-22

### [新增] GitHub 自动更新部署模块

依据 docs/PROJECT.md 第 7 节自动更新系统设计 + docs/SPEC.md 第 7 节部署规范 + docs/UI-DESIGN.md 6.2 节「系统设置 > 部署管理」+ 铁律三件套（04 禁硬编码 / 06 防幻觉 / 13 严格遵循项目文档规范），完整实现自动部署全层（DDL + entity + mapper + 3 DTO + Service + Controller + 前端页面 + 路由/菜单 + 5 份文档同步）。Webhook 自动触发 + 手动触发双入口，备份→拉代码→构建→重启→健康检查→失败回滚完整编排。

#### 数据库
- `jicek_deploy_log` 表（11 字段）：id / tenant_id / trigger_source(webhook/manual) / commit_hash / branch / status(0-3) / duration_ms / operator_ip / error_message / create_time + 2 索引（status / source）
- 审计铁律：仅 INSERT + SELECT（受控更新 status 0→1/2/3），禁 UPDATE/DELETE

#### 后端 - 实体/Mapper/DTO
- `DeployLog` 实体：@TableName("jicek_deploy_log")，字段对应表结构
- `DeployLogMapper`：extends BaseMapper<DeployLog>，注释明确「审计表，禁 UPDATE/DELETE，仅 INSERT + SELECT」
- `WebhookResultDTO`：含 `accepted(deployLogId, message)` / `ignored(message)` 静态工厂方法
- `ManualDeployDTO`：tenantId @NotNull + branch（可选）
- `DeployStatusDTO`：deploying / lastDeploy / enabled 三字段

#### 后端 - DeployService（核心编排）
- `handleWebhook(request)`：读取 body → HMAC-SHA256 验签 → 校验 push 事件 → 解析 commitHash/branch → 异步触发部署
- `manualDeploy(tenantId, branch, ip)`：手动触发入口
- `triggerDeploy(source, commitHash, branch, ip)`：Redisson 分布式锁 `jicek:deploy:lock`（5 分钟自动释放防死锁）→ 创建审计日志(status=0) → daemon 线程 `jicek-deploy-{logId}` 异步执行
- `executeDeployFlow(branch)`：备份 → gitPull → buildBackend → buildFrontend → restart → healthCheck，任一失败均触发 rollback
- `backupArtifacts(rootDir)`：备份 jar + dist 到 `.jicek-backup/{timestamp}/`，清理旧备份保留 3 个
- `gitPull(rootDir, branch)`：`git fetch origin {branch}` + `git reset --hard origin/{branch}`（确保本地与服务端一致）
- `buildBackend(rootDir)`：`mvn clean package -DskipTests -q`
- `buildFrontend(rootDir)`：`npm ci --silent` + `npm run build --silent`
- `restart()`：按 restartMode 分发（docker → `docker restart` / btpanel → 宝塔 API HTTP 调用 / none → 跳过）
- `healthCheck()`：轮询 `/actuator/health`，超时 60s，间隔 3s
- `rollback()`：还原最近备份 → restart → 标记 status=3
- `verifySignature(body, signature)`：HMAC-SHA256 + `MessageDigest.isEqual` 常量时间比较（防时序攻击）
- `execCommand(workDir, command...)`：ProcessBuilder 执行外部命令（禁 `Runtime.exec` 防注入）

#### 后端 - DevDeployController
- 路由前缀：`/api/dev/deploy`
- 4 接口：POST /webhook（GitHub 调用，立即返回 accepted） / POST /manual（手动触发） / GET /status（当前状态 + 最近部署） / GET /log/page（部署审计日志分页）
- `getClientIp()`：穿透 X-Forwarded-For/X-Real-IP 代理头
- Webhook 立即返回，部署在 daemon 线程中异步执行，避免 GitHub Webhook 超时

#### 后端 - 常量/错误码
- `JicekConstants` 新增 18 个常量：DEPLOY_SOURCE_WEBHOOK/MANUAL + DEPLOY_STATUS_RUNNING/SUCCESS/FAILED/ROLLED_BACK + DEPLOY_DEFAULT_BRANCH + REDIS_KEY_DEPLOY_LOCK + DEPLOY_LOCK_TIMEOUT_SECONDS(300) + DEPLOY_HEALTH_CHECK_TIMEOUT_SECONDS(60) + DEPLOY_HEALTH_CHECK_INTERVAL_SECONDS(3) + DEPLOY_HEALTH_CHECK_PATH + DEPLOY_ERROR_MSG_MAX_BYTES(4KB) + DEPLOY_BACKUP_KEEP_COUNT(3) + DEPLOY_WEBHOOK_SIGNATURE_HEADER/EVENT_HEADER/EVENT_PUSH/SIGNATURE_PREFIX
- `ResultCode` 新增 10 个错误码（7001-7010）：DEPLOY_LOCK_FAIL / DEPLOY_WEBHOOK_SIGN_FAIL / DEPLOY_WEBHOOK_EVENT_INVALID / DEPLOY_SECRET_NOT_CONFIGURED / DEPLOY_GIT_PULL_FAIL / DEPLOY_BUILD_FAIL / DEPLOY_RESTART_FAIL / DEPLOY_HEALTH_CHECK_FAIL / DEPLOY_ROLLBACK_FAIL / DEPLOY_PARAM_INVALID

#### 后端 - 配置
- `JicekProperties` 新增 `Deploy` 内部类：webhookSecret / projectRoot / restartMode(docker/btpanel/none) / dockerContainer / btpanelApiUrl / btpanelApiKey / healthCheckBaseUrl / enabled(默认 false)
- `application.yml` 新增 `jicek.deploy` 配置段，全部走环境变量注入：
  - `GITHUB_WEBHOOK_SECRET`（Webhook 签名密钥）
  - `JICEK_DEPLOY_ENABLED`（默认 false，显式开启）
  - `JICEK_DEPLOY_PROJECT_ROOT`（项目根目录）
  - `JICEK_DEPLOY_RESTART_MODE`（重启模式：docker/btpanel/none）
  - `JICEK_DEPLOY_DOCKER_CONTAINER`（Docker 容器名）
  - `BTPANEL_API_URL` / `BTPANEL_API_KEY`（宝塔 API）
  - `JICEK_DEPLOY_HEALTH_URL`（健康检查 URL）

#### 前端
- **部署管理页**（`views/dev/deploy/index.vue`）：
  - 3 状态卡片：部署功能启用态 / 当前状态（部署中 or 空闲） / 最近部署结果
  - 手动触发部署按钮：ConfirmDialog 二次确认（提示服务可能短暂不可用）
  - 部署审计日志表格：ID / 来源（Webhook or 手动）/ 状态 / 分支 / Commit（前 7 位等宽字体）/ 耗时 / 操作 IP / 时间 / 错误信息
  - 筛选：status（0-3）/ triggerSource（webhook/manual）
  - 状态码映射：0进行中(warning) / 1成功(success) / 2失败(danger) / 3已回滚(info)，使用 `el-tag` + `deployTagType()` / `deployStatusText()` 函数（不扩展 StatusTag 组件，保持其纯净性）
  - 轮询机制：部署进行中时每 5s 刷新 status，完成后停止轮询并刷新日志（onBeforeUnmount 清理定时器）
- API 定义：新增 `deployApi`（3 方法）：status / manual / logPage
- 路由：新增 `/deploy`（name: Deploy, icon: Refresh）
- 侧边栏：新增「系统设置」子菜单（icon: Setting）+ 「部署管理」项

#### 技术决策
- **部署功能默认关闭**：`jicek.deploy.enabled=false`，需显式开启，防止开发环境误触发
- **Webhook 异步执行**：Webhook 立即返回 accepted，部署在 daemon 线程中异步执行，避免 GitHub Webhook 超时（GitHub 默认 10s 超时）
- **HMAC-SHA256 + 常量时间比较**：`MessageDigest.isEqual` 防时序攻击，对标 HmacSignService 既有模式
- **Redisson 分布式锁**：`jicek:deploy:lock` 防并发触发，5 分钟自动释放防死锁
- **ProcessBuilder 替代 Runtime.exec**：参数化执行外部命令，防 shell 注入
- **不扩展 StatusTag 支持 deploy 类型**：保持组件纯净（仅 order/card/withdraw/device 四类业务状态），部署状态用 el-tag 直接渲染
- **审计表与业务表分离**：复用既有 jicek_update_log 设计思路，但新建独立 jicek_deploy_log 表（避免与原 update_log 字段含义冲突），仅 INSERT + SELECT + 受控更新 status
- **备份保留策略**：DEPLOY_BACKUP_KEEP_COUNT(3) 常量控制，清理旧备份防磁盘膨胀
- **错误信息截断**：errorMessage 截断至 DEPLOY_ERROR_MSG_MAX_BYTES(4KB)，防数据库字段溢出

## [0.4.3] - 2026-07-22

### [新增] 数据统计与可视化模块

依据 UI-DESIGN.md 6.2 节「数据统计」4 子项规范 + 铁律三件套（04 禁硬编码 / 06 防幻觉 / 13 严格遵循项目文档规范），完整实现数据统计全层（DTO + Service + Controller + 前端页面 + 路由/菜单）。数据源全部基于现有业务表聚合，无独立统计表（铁律 06：禁止虚构表）。

#### 后端 - DTO（4 个，对应 4 子项）
- `VerifyTrendDTO`：验证量趋势（labels + activateCounts + newDeviceCounts + 汇总数）
- `DeviceHeatmapDTO`：设备热力图（days + hours + points[day,hour,value] + currentOnline + totalDevice）
- `IncomeStatsDTO`：收入统计（dimension + items[{name,key,amount,count}] + totalAmount + totalCount，内部 IncomeItem 静态类）
- `AntiCrackStatsDTO`：防破解事件（bannedDeviceCount + bannedCardCount + bannedIpCount + 时间趋势序列）

#### 后端 - StatsService（核心聚合逻辑）
- `verifyTrend()`：基于 `jicek_card_key.first_use_time`（卡密激活）+ `jicek_device.bind_time`（新增设备），按 hour/day/month 内存分桶，时间标签连续补 0
- `deviceHeatmap()`：基于 `jicek_device.last_heartbeat` 按天×小时聚合，固定近 7 天 × 24 小时网格，points 为 [dayIndex, hour, count] 三元组
- `income()`：基于 `jicek_pay_order`(status=1 已支付) 聚合
  - `groupByChannel()`：按 payType 分组（alipay/wxpay/qqpay/unionpay → 中文名映射，走常量禁字面量）
  - `groupByCardType()`：按 cardTypeId 分组，预加载卡类名避免 N+1
  - `groupByAgent()`：PayOrder 暂无 agent_id 字段，返回空列表 + 前端提示（铁律 06：禁止虚构字段）
- `antiCrack()`：基于 `jicek_device.status=1`(封禁设备) + `jicek_card_key.status=2`(封禁卡密) + `bind_ip` 去重统计封禁 IP 数，按天趋势
- 工具方法：`validateGranularity()` / `validateDimension()` / `sanitizeDays()`(超 90 天抛 STATS_RANGE_EXCEED) / `bucketByTime()` / `buildTimeLabels()`(连续日期序列)

#### 后端 - DevStatsController
- 路由前缀：`/api/dev/stats`
- 4 接口：GET verify-trend / GET device-heatmap / GET income / GET anti-crack
- 全部 `@RequestParam`，tenantId 必填，softwareId 可选，days 可选（默认 7/30）
- 与 DevDashboardController（仅 /summary）分离，单一职责

#### 后端 - 常量/错误码
- `JicekConstants` 新增 11 个常量：STATS_GRANULARITY_HOUR/DAY/MONTH + STATS_DIMENSION_CHANNEL/CARD_TYPE/AGENT + STATS_MAX_RANGE_DAYS(90) + STATS_DEFAULT_RANGE_DAYS(7) + STATS_HEATMAP_DAYS(7) + STATS_HOURS_PER_DAY(24)
- `ResultCode` 新增 4 个错误码（6001-6999）：STATS_GRANULARITY_INVALID(6001) / STATS_DIMENSION_INVALID(6002) / STATS_RANGE_EXCEED(6003) / STATS_PARAM_INVALID(6004)

#### 前端
- **数据统计页**（`views/dev/stats/index.vue`）：
  - 4 Tab 布局：验证量趋势 / 设备在线热力图 / 收入统计 / 防破解事件
  - 全局软件筛选（softwareId）+ 刷新按钮（reloadAll 并行加载 4 接口）
  - **验证量趋势 Tab**：粒度切换（hour/day/month 单选按钮组）+ 天数配置 + 双折线图（卡密激活/新增设备，渐变 areaStyle）+ 汇总卡片
  - **设备热力图 Tab**：ECharts heatmap（x=小时 0-23、y=日期、value=在线设备数）+ visualMap 渐变色（#F0F4FA→#2E7D5B）+ 当前在线/总设备汇总
  - **收入统计 Tab**：维度切换（channel/cardType/agent）+ 天数配置 + 柱状图（金额）+ 折线图（订单数，双 Y 轴）+ 明细表格（名称/订单数/金额/占比进度条）+ 代理维度 alert 提示
  - **防破解事件 Tab**：3 汇总卡片（封禁设备/卡密/IP）+ 双折线图（封禁设备/封禁卡密，渐变 areaStyle）
  - ECharts 生命周期管理：onBeforeUnmount dispose 全部 4 图表 + watch softwareId 触发 reloadAll + Tab 切换后 setTimeout resize
  - 金额展示使用 decimal.js 保证精度（铁律 13）
- API 定义：新增 `statsApi`（4 方法）
- 路由：新增 `/stats`（name: Stats, icon: TrendCharts）
- 侧边栏：新增「数据统计」子菜单（icon: TrendCharts）+ 「数据统计」项

#### 技术决策
- **不新建统计表**：所有统计基于现有业务表（CardKey/Device/PayOrder）聚合，避免数据冗余和同步问题（铁律 06）
- **内存分桶而非 SQL GROUP BY**：与现有代码风格一致（DevDashboardController 也是内存聚合），保持 MyBatis-Plus LambdaQueryWrapper 风格统一
- **时间标签连续补 0**：buildTimeLabels() 生成完整日期序列，无数据时段补 0，避免图表 X 轴跳跃
- **金额 BigDecimal + Decimal.js**：前后端均使用高精度数值类型（铁律 13：金额精度）
- **代理维度预留**：PayOrder 当前无 agent_id 字段，前端显示 alert 提示「待扩展」，禁止虚构字段（铁律 06）
- **热力图固定 7 天**：STATS_HEATMAP_DAYS(7)，避免维度爆炸（7×24=168 点足够展示周期规律）

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

## [0.4.2] - 2026-07-22

### [新增] 云函数远程执行模块（抗破解终极方案）

依据 UI-DESIGN.md 6.2 节「云端数据 > 云函数」+ 铁律三件套（04 禁硬编码 / 06 防幻觉 / 13 严格遵循项目文档规范），完整实现云函数全层（DDL + entity + mapper + dto + service + controller + sandbox + 前端页面 + 路由/菜单）。关键算法在服务端 LuaJ 沙箱执行，客户端只调用，实现抗破解。

#### 数据库
- `jicek_cloud_function` 表：19 字段（id/tenantId/softwareId/name/description/code(MEDIUMTEXT)/runtime/timeoutMs/memoryLimitKb/maxInputKb/maxOutputKb/enabled/version/invokeCount/lastInvokeTime/lastInvokeIp/createBy/createTime/updateTime）+ `UNIQUE KEY uk_sw_name`(tenant_id, software_id, name)
- `jicek_cloud_function_log` 表：14 字段审计日志（含 invokeSource/callerIp/inputSize/outputSize/durationMs/status/errorMessage）+ 3 索引（func/software/status）

#### 后端 - 实体/Mapper/DTO
- 2 实体：`CloudFunction` / `CloudFunctionLog`（@TableName 注解对应表名）
- 2 Mapper：均继承 `BaseMapper<T>`，`CloudFunctionLogMapper` 注释明确「审计表，禁 UPDATE/DELETE，仅 INSERT + SELECT」
- 3 DTO：`CloudFunctionSaveDTO`（name @Pattern `^[a-zA-Z][a-zA-Z0-9_]{0,63}$` + timeoutMs @Min(100)@Max(30000) + 内存/输入/输出上限校验）/ `CloudFunctionInvokeDTO`（tenantId/softwareId/functionId @NotNull + input @Size(max=262144)）/ `CloudFunctionInvokeResult`（含 success()/fail() 静态工厂方法）

#### 后端 - LuaJ 沙箱引擎（`LuaSandboxService`）
- 依赖：`org.luaj:luaj-jse:3.0.6`（纯 Java 实现 Lua 5.4 子集，无原生依赖，Spring Boot 兼容）
- **安全设计三层防护**：
  1. **全局表裁剪**：禁用 os/io/loadfile/dofile/require/debug/package/load（设为 NIL），仅保留 BaseLib/MathLib/StringLib/TableLib
  2. **超时强制中断**：独立 `jicek-lua-sandbox` daemon 线程池（4 核心/16 最大/64 队列/CallerRunsPolicy）+ `Future.get(timeoutMs)` + `future.cancel(true)` 中断
  3. **输出大小硬截断**：结果序列化后超过 `maxOutputKb` 直接截断
- **输入注入契约**：通过 `jicek.input` 全局变量传入字符串，Lua 代码 `return` 返回值递归序列化为 JSON（table 自动判断数组 vs 对象，key 为 1..n 连续正整数则为数组）
- **异常映射**：LuaError→CF_COMPILE_FAIL/CF_RUNTIME_ERROR、TimeoutException→CF_TIMEOUT、OutOfMemoryError→CF_MEMORY_LIMIT
- **编译器安装顺序**：`LuaC.install(globals)` 必须在 BaseLib 之前，确保 globals.load() 能编译用户代码
- `truncateError()`：错误信息截断至 `CF_ERROR_MSG_MAX_BYTES`（4KB）

#### 后端 - Service（`CloudFunctionService`）
- `save()`：代码长度校验（≤CF_CODE_MAX_BYTES 64KB）+ 默认值填充（null 时走 JicekConstants 常量）+ name 唯一性校验 + version 自增
- `page()`/`get()`/`delete()`/`toggleEnabled()`
- `invoke()`：核心调度方法（校验启用态→输入大小二次校验→沙箱执行→落审计日志→更新统计）
- `mapExceptionToStatus()`：错误码→审计状态码映射，确保日志一致性
- `recordSuccessLog()`/`recordFailureLog()`：审计日志写入（仅 INSERT）
- `updateInvokeStats()`：`@Transactional` 独立方法，失败不影响主流程

#### 后端 - Controller（`DevCloudFunctionController`）
- 路由前缀：`/api/dev/cloud-func`
- 接口：POST save / GET page / GET {tenantId}/{functionId} / DELETE / POST toggle-enabled / POST invoke / GET log/page
- `getClientIp()`：穿透 X-Forwarded-For/X-Real-IP 代理头
- 调用来源固定为 `CF_SOURCE_DEV`（SDK 调用走后续 SdkCloudFunctionController，复用同一 Service）

#### 后端 - 常量/错误码
- `JicekConstants` 新增 14 个常量：CF_CODE_MAX_BYTES(64KB) / CF_DEFAULT_TIMEOUT_MS(3000) / CF_MAX_TIMEOUT_MS(30000) / CF_DEFAULT_MEMORY_KB(8192) / CF_DEFAULT_INPUT_KB(32) / CF_DEFAULT_OUTPUT_KB(32) / CF_ABSOLUTE_IO_KB(256) / CF_ERROR_MSG_MAX_BYTES(4KB) / CF_STATUS_*(0-6) / CF_SOURCE_DEV/SDK / CF_RUNTIME_LUA / 2 个 Redis key
- `ResultCode` 新增 12 个错误码（5001-5012）：CF_NOT_FOUND / CF_DISABLED / CF_CODE_TOO_LARGE / CF_TIMEOUT / CF_RUNTIME_ERROR / CF_INPUT_TOO_LARGE / CF_OUTPUT_TOO_LARGE / CF_COMPILE_FAIL / CF_NAME_EXISTS / CF_MEMORY_LIMIT / CF_LOCK_FAIL / CF_PARAM_INVALID

#### 前端
- **云函数管理页**（`views/dev/cloud-func/index.vue`）：
  - 双 Tab 布局：函数列表 + 执行日志
  - 函数列表：筛选（softwareId/name/enabled）+ 表格 + CRUD 操作
  - 编辑弹窗：表单含代码 textarea（等宽字体 `var(--jicek-font-mono)`）+ 字节数实时统计（`codeBytesText` computed）+ 超时/内存/输入/输出上限配置
  - 测试执行弹窗：输入 textarea + 执行结果展示（状态 tag + 耗时 + 输入输出大小 + 格式化 JSON 输出 + 错误信息）
  - 执行日志 Tab：筛选（functionId/softwareId/status/invokeSource）+ 表格
  - 删除二次确认（ConfirmDialog 组件）
  - 状态码映射：0成功/1编译失败/2运行时错误/3超时/4内存超限/5输入超限/6输出超限
- API 定义：新增 `cloudFuncApi`（7 方法），分页参数使用 `current`/`size`（与 card-type 一致）
- 路由：新增 `/cloud-func`（name: CloudFunc, icon: Cpu）
- 侧边栏：新增「云端数据」子菜单（icon: Cpu）+ 「云函数」项

#### 技术决策
- **沙箱引擎选型**：采用 LuaJ 3.0.6 而非 GraalVM/LuaJIT，理由：纯 Java 实现、无原生依赖、与 Spring Boot 兼容、依赖体积小（约 300KB）
- **禁用动态加载**：`load` 设为 NIL，所有 Lua 代码必须在源码顶层定义，禁动态编译字符串，进一步缩小攻击面
- **审计不可篡改**：审计表仅 INSERT + SELECT，Service 层禁 UPDATE/DELETE，确保执行历史完整可追溯
- **线程池隔离**：独立 `jicek-lua-sandbox` 线程池与业务线程池隔离，避免沙箱执行阻塞主业务
- **分页参数一致性**：cloud-func/page 使用 `current`/`size`（与 card-type 一致，与 device 的 `page`/`size` 不同，差异在 API 层屏蔽）

## 待发布版本（开发中）

### [未发布] v0.5.0
- 前端补全剩余项：软件/用户管理（待后端 Controller）+ H5 终端用户页面
- CardKeyService.useCard 完整流程接入 + Sa-Token 鉴权 + software 表读取签名密钥/心跳间隔
- 代理制卡扣余额接入 AgentService.deductBalance
- 分润发放接入 PaymentTransactionService（支付成功回调触发 grantCommission）
