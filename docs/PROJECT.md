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
│       ├── crypto      # ★ 加密层（已实现）
│       │   ├── AesCryptoService     # AES-256-GCM
│       │   ├── RsaCryptoService     # RSA-2048-OAEP
│       │   ├── HmacSignService      # HMAC-SHA256
│       │   ├── Md5SignService       # MD5（V1 兼容）+ SHA-256
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
│       └── sdk-gen     # SDK 代码生成器（待实现 v0.3.0）
└── jicek-ui            # ★ 前端（v0.2.0 已实现骨架）
    ├── src/api         # API 客户端 + 接口定义
    ├── src/components/jicek # 公共组件（StatusTag/AmountInput/ConfirmDialog）
    ├── src/layout      # DevLayout (220px 侧栏 + 60px 顶栏)
    ├── src/router      # 路由配置
    ├── src/styles      # jicek.scss (CSS 变量系统)
    └── src/views/dev   # 开发者页面
        ├── dashboard   # 控制台
        ├── card-key-gen # 卡密生成
        ├── card-key-list # 卡密查询
        ├── pay-config  # 支付配置
        └── pay-order   # 资金流水
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

## 3. 功能清单（已设计，待实现）

### 3.1 核心验证
- [ ] 卡密验证（时长/次数/功能/永久 四种模式）
- [ ] 账号验证（注册、登录、续费）
- [ ] 设备指纹绑定（CPU+主板+硬盘+网卡+BIOS）
- [ ] 心跳保活（动态间隔 5-300s）
- [ ] 在线/离线状态管理

### 3.2 卡密管理
- [ ] 卡类管理（4 种类型 + 定价 + 绑定策略）
- [ ] 批量生成（自定义前缀、字符集、长度）
- [ ] 加密存储（AES-256-GCM）
- [ ] 查询/封禁/解封/退款

### 3.3 支付系统
- [ ] 彩虹易支付 V1 适配器（MD5 签名）
- [ ] 支付通道管理（支付宝/微信/QQ/银联）
- [ ] 订单状态机（5 状态流转）
- [ ] 异步回调验签 + 幂等
- [ ] 资金流水审计

### 3.4 代理体系
- [ ] 多级代理（树形结构）
- [ ] 分润比例配置
- [ ] 代理制卡（扣余额）
- [ ] 提现审核工作流（WarmFlow）

### 3.5 云端数据
- [ ] 云变量（key/value + 签名加密）
- [ ] 云函数（远程执行，抗破解终极方案）
- [ ] 远程公告（按软件/版本下发）

### 3.6 客户端 SDK
- [ ] Java SDK
- [ ] C# SDK
- [ ] Python SDK
- [ ] Go SDK
- [ ] Node.js SDK
- [ ] C++ SDK
- [ ] 易语言模块
- [ ] Lua SDK
- [ ] Shell SDK

### 3.7 安全防护
- [ ] IP 黑名单 / 设备黑名单
- [ ] 频率限制（Redisson 分布式限流）
- [ ] 防爆破（连续失败 N 次封禁 IP）
- [ ] 一次一密封包（防重放）

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
- 数据统计（验证量、设备、收入、防破解事件）
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
  password_hash   VARCHAR(128) NOT NULL,
  balance         DECIMAL(10,2) DEFAULT 0 COMMENT '余额',
  total_earnings  DECIMAL(10,2) DEFAULT 0 COMMENT '累计收益',
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  level           INT          DEFAULT 1 COMMENT '代理级别',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  KEY idx_parent (tenant_id, parent_id),
  KEY idx_tenant (tenant_id)
) COMMENT='代理';
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

## 7. 自动更新系统

### 7.1 触发方式
- GitHub Webhook 自动触发（push 到 main 分支）
- 管理员后台手动触发

### 7.2 更新流程
```
备份 → git pull → 依赖安装 → DB迁移 → 清缓存 → 健康检查 → 重启 → 健康检查 → 完成
                                                          ↓ 失败
                                                       自动回滚
```

### 7.3 重启策略
- Docker 模式：docker-compose restart 或 docker restart
- 宝塔模式：通过宝塔 API 重启容器

### 7.4 安全
- Webhook 签名验证（HMAC-SHA256 + Secret）
- 更新分布式锁（防并发）
- 完整审计日志
- 失败自动回滚

## 8. 目录结构
（见 2.2 节）
