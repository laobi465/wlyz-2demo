# 极策k网络验证

> 面向开发者的多租户卡密验证 SaaS 平台 · 基于 RuoYi-Vue-Plus 技术栈 · 国产开源可私有部署

[![Version](https://img.shields.io/badge/version-0.2.0--SNAPSHOT-blue)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-Proprietary-red)](#license)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)](https://vuejs.org/)

## 项目简介

极策k网络验证是一款面向软件开发者的卡密验证 SaaS 平台，对标护卫盾、科御网络验证等闭源产品，差异化优势：

- **国产开源技术栈**：基于 RuoYi-Vue-Plus，可私有部署、可二次开发
- **真正多租户 SaaS**：MyBatis-Plus 租户隔离，开发者独立数据空间
- **资金合规**：彩虹易支付独立部署，平台不经手资金，规避二清风险
- **8 语言 SDK 全覆盖**：Java / C# / Python / Go / Node.js / C++ / 易语言 / Lua / Shell
- **最前沿加密**：AES-256-GCM + RSA-2048-OAEP + HMAC-SHA256（可选国密 SM2/SM4）

## 当前版本（v0.2.0）

### 已完成 ✅

#### 后端核心模块
- **加密层**：AES-256-GCM（存储）/ RSA-2048-OAEP（传输）/ HMAC-SHA256（签名）/ MD5（V1 兼容）
- **卡密模块**：SecureRandom 生成 + AES-256-GCM 加密入库 + SHA-256 哈希索引 + 明文仅展示一次
- **支付适配层**：彩虹易支付 V1 完整实现（支付/查询/退款/异步回调）
- **订单状态机**：5 状态不可逆流转（0待支付→1已支付→2失败→3已退款→4已关闭）
- **异步回调**：Redisson 分布式锁 + MD5 验签 + 幂等 + 返回纯字符串 `success`
- **资金一致性**：`@Transactional` 保证「订单状态流转 + 卡密发放」原子性
- **多租户拦截器**：MyBatis-Plus TenantLineInnerInterceptor
- **RESTful API**：5 个开发者 Controller 全部就位

#### 前端 Vue3 骨架
- Vite 5 + TypeScript 严格模式 + Element Plus 2.9.8 + Pinia
- 全局样式：极策蓝 `#1A4D8F` 主色 + CSS 变量系统（遵循现代简约 UI 规范）
- 路由：5 个核心页面
- API 客户端：axios 拦截器 + 统一响应处理 + decimal.js 金额精度
- 公共组件：StatusTag / AmountInput / ConfirmDialog
- Layout：220px 左侧导航 + 60px 顶栏 + 主内容区
- 5 个核心页面：控制台 / 卡密生成 / 卡密查询 / 支付配置 / 资金流水

### 待实现（v0.3.0+）

- 8 语言客户端 SDK
- 设备指纹采集与绑定（CPU/主板/硬盘/网卡/BIOS）
- 多级代理 + 分润 + 提现工作流（WarmFlow）
- 前端补全（软件/卡类/用户/设备/代理管理 + ECharts + H5）
- 云变量 / 云函数（沙箱）/ 远程公告
- GitHub Webhook 自动更新部署

详见 [TODO.md](TODO.md)。

## 技术栈

| 层级 | 选型 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.4.6 |
| 权限认证 | Sa-Token | 1.42.0 |
| ORM | MyBatis-Plus | 3.5.12 |
| 缓存/锁 | Redisson | 7.x |
| 数据库 | MySQL | 8.0.42 |
| 前端 | Vue3 + TS + Element Plus | EP 2.9.8 |
| 构建工具 | Vite | 5.x |
| 支付网关 | 彩虹易支付（独立部署） | V1 接口 |
| 工作流 | WarmFlow | 1.7.2 |
| 文件存储 | MinIO | 最新版 |

## 模块结构

```
wlyz-2demo/
├── jicek-license/                    # 后端 - 卡密验证核心模块
│   └── src/main/java/com/jicek/license/
│       ├── common/                   # 通用：R / ResultCode / ServiceException
│       ├── config/                   # 配置：JicekProperties / MybatisPlusConfig / CorsConfig
│       ├── crypto/                   # ★ 加密层（5 个服务）
│       │   ├── AesCryptoService      # AES-256-GCM
│       │   ├── RsaCryptoService      # RSA-2048-OAEP
│       │   ├── HmacSignService       # HMAC-SHA256
│       │   ├── Md5SignService        # MD5（V1 兼容）+ SHA-256
│       │   └── CryptoConfiguration   # Bean 配置
│       ├── card/                     # 卡密模块
│       │   ├── entity                # CardType / CardKey
│       │   ├── generator             # CardKeyGenerator (SecureRandom)
│       │   ├── service               # CardKeyService
│       │   └── controller            # DevCardKeyController / DevCardTypeController
│       ├── pay/                      # ★ 支付适配层
│       │   ├── adapter               # PayAdapter + PayAdapterV1Impl
│       │   ├── service               # PayConfigService / PayOrderStateMachineService
│       │   │                         # PayNotifyService / PayOrderService
│       │   └── controller            # PayNotifyController / DevPayController
│       ├── transaction/              # ★ 资金一致性事务
│       │   └── PaymentTransactionService
│       └── dashboard/                # 控制台
│           └── controller            # DevDashboardController
├── jicek-ui/                         # 前端 - Vue3 + TS + Element Plus
│   └── src/
│       ├── api/                      # API 客户端 + 接口定义
│       ├── components/jicek/         # 公共组件（StatusTag/AmountInput/ConfirmDialog）
│       ├── layout/                   # DevLayout (220px 侧栏 + 60px 顶栏)
│       ├── router/                   # 路由配置
│       ├── styles/                   # jicek.scss (CSS 变量系统)
│       └── views/dev/                # 开发者页面
│           ├── dashboard/            # 控制台
│           ├── card-key-gen/         # 卡密生成
│           ├── card-key-list/        # 卡密查询
│           ├── pay-config/           # 支付配置
│           └── pay-order/            # 资金流水
├── docs/                             # 核心文档
│   ├── PROJECT.md                    # 项目文档
│   ├── SPEC.md                       # 规范文档
│   └── UI-DESIGN.md                  # UI 设计规范
├── CHANGELOG.md                      # 更新日志
├── TODO.md                           # 任务清单
└── README.md                         # 本文件
```

## 快速开始

### 环境要求

- JDK 17 或 21
- MySQL 8.0.42+
- Redis 7.2.8+
- Maven 3.9+
- Node.js 18+ / pnpm 8+
- PHP 8.0+（彩虹易支付独立部署）

### 后端启动

```bash
# 1. 初始化数据库
mysql -u root -p < jicek-license/src/main/resources/sql/jicek_init.sql

# 2. 配置环境变量（铁律：敏感信息禁硬编码）
export JICEK_AES_KEY=<Base64 encoded 32-byte key>
export JICEK_RSA_PRIVATE_KEY=<Base64 encoded PKCS#8 RSA private key>
export JICEK_RSA_PUBLIC_KEY=<Base64 encoded X.509 RSA public key>
export JICEK_HMAC_KEY=<Base64 encoded 32-byte key>
export MYSQL_HOST=127.0.0.1
export MYSQL_PASSWORD=<your-password>
export REDIS_HOST=127.0.0.1

# 3. 编译运行
cd jicek-license
mvn spring-boot:run
```

### 前端启动

```bash
cd jicek-ui
pnpm install
pnpm dev   # 默认 http://localhost:5173，自动代理 /api 到 8080
```

### 彩虹易支付部署

独立部署彩虹易支付 V1，配置商户 PID 和密钥后，在开发者后台「支付配置」页面填入网关地址、PID、商户密钥，勾选启用的支付通道（支付宝/微信/QQ/银联）。

## 核心文档

- [CHANGELOG.md](CHANGELOG.md) - 更新日志（语义化版本）
- [TODO.md](TODO.md) - 任务清单（P0/P1/P2/P3 优先级）
- [PROMPT.md](PROMPT.md) - 下一个 AI 接手指南（阅读顺序/架构要点/常见陷阱/验证清单）
- [docs/PROJECT.md](docs/PROJECT.md) - 项目文档（架构/模块/数据流/数据库表）
- [docs/SPEC.md](docs/SPEC.md) - 规范文档（代码/接口/安全规范）
- [docs/UI-DESIGN.md](docs/UI-DESIGN.md) - UI 设计规范（现代简约风格）

## 安全规范（铁律）

1. **禁硬编码**：所有密钥、商户号、域名、超时参数必须通过环境变量或数据库配置项注入
2. **卡密加密存储**：AES-256-GCM 加密入库，SHA-256 哈希用于查询索引，明文仅展示一次
3. **资金一致性**：订单状态流转 + 卡密发放必须同事务（`@Transactional`）
4. **异步回调幂等**：Redisson 分布式锁按订单号 + 状态机校验（仅 status=0 才处理）
5. **签名验证**：HMAC-SHA256（API）+ MD5（V1 兼容），使用 `MessageDigest.isEqual` 防时序攻击
6. **防重放**：HMAC 签名包含 timestamp（±300s 容差）+ nonce（Redis 缓存 5 分钟）
7. **多租户隔离**：MyBatis-Plus TenantLineInnerInterceptor 全局拦截
8. **金额精度**：后端 `BigDecimal` + 前端 `decimal.js`，杜绝浮点误差

## API 概览

### 开发者 API（`/api/dev/*`）

| 模块 | 端点 | 说明 |
|---|---|---|
| 控制台 | `GET /api/dev/dashboard/summary` | 今日收入/订单/退款/净收入/卡密分布 |
| 卡密 | `POST /api/dev/card/generate` | 批量生成卡密（最多 1000 张） |
| 卡密 | `GET /api/dev/card/query` | 按卡号查询卡密详情 |
| 卡密 | `POST /api/dev/card/ban` | 封禁卡密 |
| 卡密 | `POST /api/dev/card/refund` | 退款并失效卡密 |
| 卡类 | `GET /api/dev/card-type/page` | 卡类分页查询 |
| 卡类 | `POST /api/dev/card-type` | 新建卡类 |
| 卡类 | `PUT /api/dev/card-type/{id}` | 更新卡类 |
| 卡类 | `DELETE /api/dev/card-type/{id}` | 删除卡类 |
| 支付 | `GET /api/dev/pay/config/{tenantId}` | 获取支付配置 |
| 支付 | `POST /api/dev/pay/config` | 保存支付配置 |
| 支付 | `POST /api/dev/pay/create` | 发起支付 |
| 支付 | `GET /api/dev/pay/order/page` | 订单分页查询 |
| 支付 | `POST /api/dev/pay/refund` | 退款 |

### 公开回调

| 端点 | 说明 |
|---|---|
| `GET /pay/notify/{tenantId}` | 彩虹易支付异步通知（GET 兼容） |
| `POST /pay/notify/{tenantId}` | 彩虹易支付异步通知（POST） |

返回纯字符串 `success`（无 BOM、无空格、无 JSON 包裹）。

## 数据库表（核心）

| 表名 | 说明 |
|---|---|
| `jicek_pay_config` | 支付配置（商户密钥 AES 加密存储） |
| `jicek_pay_order` | 支付订单（5 状态机） |
| `jicek_card_type` | 卡类（时长/次数/功能/永久 四种） |
| `jicek_card_key` | 卡密（AES-256-GCM 加密 + SHA-256 哈希索引） |
| `jicek_software` | 软件（AppKey + 签名密钥 + RSA 密钥对） |
| `jicek_device` | 设备（指纹哈希 + 在线状态） |
| `jicek_agent` | 代理（多级树形结构 + 余额 + 分润） |

完整 DDL 见 [jicek-license/src/main/resources/sql/jicek_init.sql](jicek-license/src/main/resources/sql/jicek_init.sql)。

## 角色权限

| 角色 | 范围 |
|---|---|
| **管理员** | 全局租户管理、支付通道授权、系统更新、审计日志 |
| **开发者（租户）** | 软件/卡密/用户/代理/支付/云数据/统计全功能 |
| **代理** | 制卡、查询、分润、提现、下级代理（开发者授权时） |
| **终端用户（H5）** | 登录、续费/购卡、换机申请、在线设备管理、公告、工单 |

## 部署

支持两种 Docker 部署方式：
- 普通 Docker（docker-compose）
- 宝塔面板内 Docker

GitHub Webhook 自动更新部署（v0.5.0+ 规划中）：
- 推送 main 分支自动触发
- 备份 → git pull → 依赖安装 → DB 迁移 → 清缓存 → 健康检查 → 重启
- 失败自动回滚

## License

Proprietary - 极策k

Copyright © 2026 极策k. All rights reserved.
