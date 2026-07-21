# 更新日志

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

## 待发布版本（开发中）

### [未发布] v0.3.0
- 8 语言客户端 SDK（Java/C#/Python/Go/Node/C++/易语言/Lua/Shell）
- 设备指纹采集与绑定
- 数据统计图表（ECharts）
