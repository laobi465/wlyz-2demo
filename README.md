# 极策k网络验证

> 面向开发者的多租户卡密验证 SaaS 平台 · 基于 RuoYi-Vue-Plus 技术栈 · 国产开源可私有部署

[![Version](https://img.shields.io/badge/version-0.17.0-blue)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-Proprietary-red)](#license)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-一键部署-blue)](#部署教程)

## 项目简介

极策k网络验证是一款面向软件开发者的卡密验证 SaaS 平台，对标护卫盾、科御网络验证等闭源产品，差异化优势：

- **国产开源技术栈**：基于 RuoYi-Vue-Plus，可私有部署、可二次开发
- **真正多租户 SaaS**：MyBatis-Plus 租户隔离，开发者独立数据空间
- **资金合规**：彩虹易支付独立部署，平台不经手资金，规避二清风险
- **9 语言 SDK 全覆盖**：Java / C# / Python / Go / Node.js / C++ / 易语言 / Lua / Shell
- **多端接入**：SDK 接入（HMAC-SHA256 签名）+ H5 验证界面（明文卡密）+ 终端用户账号体系
- **最前沿加密**：AES-256-GCM + RSA-2048-OAEP + HMAC-SHA256（可选国密 SM2/SM4/SM3）

## 核心功能

### 后端模块（jicek-license）

| 模块 | 说明 |
|---|---|
| 卡密管理 | 卡类/卡密 CRUD + 批量生成 + 状态机（未使用/已使用/已封禁/已退款/已过期）|
| 软件管理 | 多软件 CRUD + AppKey/SignSecret/RSA 密钥生成与轮换 |
| SDK 鉴权 | HMAC-SHA256 签名 + RSA 加密卡密 + Nonce 防重放 + 时间戳容差 |
| H5 验证界面 | 卡密登录 + 我的卡密 + 公告 + 代理注册邀请码 + 内嵌卡网系统 |
| 终端用户账号 | 独立账号体系（与卡密登录并存），H5 账号密码登录 |
| 设备指纹 | 5 维 SHA-256 融合（CPU+主板+硬盘+网卡+BIOS）+ 换机码 + 心跳保活 |
| 多级代理 | 树形代理 + 链式分润（直推+父级链最多 10 层）+ 提现状态机 |
| 云函数 | LuaJ 3.0.6 沙箱 + 审计日志不可篡改 + SDK 端调用 |
| 数据统计 | 验证量趋势 + 设备热力 + 收入统计（通道/卡类/代理维度）+ 防破解事件 |
| 公告系统 | CRUD + 发布/下线状态机 + 版本范围匹配 |
| 自动更新包 | 文件上传 + 多格式（exe/sh/win/lua/zip/7z）+ 发布/下线 |
| 工单系统 | 单向工单（开发者→管理员）+ 管理员端处理 |
| 部署管理 | GitHub Webhook 自动更新 + 备份回滚 + 健康检查 |
| 管理员后台 | 工单处理 + 开发者账号管理（封禁/解封/重置密码）|

### 前端模块（jicek-ui）

| 模块 | 说明 |
|---|---|
| 开发者后台 | Dashboard + 软件管理 + 卡类/卡密 + 设备 + 代理 + 提现 + 支付配置 + 资金流水 + 云函数 + 统计 + 工单 + 部署 + 更新包 + 公告 + 卡网 + 终端用户 |
| 管理员后台 | 工单管理 + 开发者账号管理（独立 AdminLayout + 独立 token 隔离）|
| H5 验证界面 | 卡密登录 + 我的卡密 + 公告 + 代理注册 + 卡网购卡（移动端布局 375px）|
| 多语言国际化 | vue-i18n 中英文切换（所有用户可见文案）|
| SDK 代码生成器 | 9 语言代码模板一键生成 + 对接文档页 |

### SDK（9 语言）

| 语言 | 路径 |
|---|---|
| Java | sdk/java/ |
| Python | sdk/python/ |
| Node.js | sdk/nodejs/ |
| Go | sdk/go/ |
| C# | sdk/csharp/ |
| C++ | sdk/cpp/ |
| Lua | sdk/lua/ |
| Shell | sdk/shell/ |
| 易语言 | sdk/e/ |

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4.6 + Java 17 + MyBatis-Plus 3.5.12 + Redisson 3.45.1 + JJWT 0.12.6 + LuaJ 3.0.6 + Hutool 5.8.34 + BouncyCastle 1.79 |
| 前端 | Vue 3.4 + Vite + TypeScript + Element Plus + Pinia + Vue Router 4 + vue-i18n 9 + ECharts |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 部署 | Docker + Docker Compose v2 + 宝塔面板 |

## 部署教程

### 方式一：一键安装脚本（推荐）

适用于全新 CentOS/Ubuntu/Debian 服务器，自动完成宝塔面板 + Docker + 项目部署。

**SSH 一键安装**（推荐，脚本会自动 clone 项目到 `/opt/jicek`）：

```bash
curl -fsSL https://raw.githubusercontent.com/laobi465/wlyz-2demo/trae/agent-iEYsbS/install.sh | bash
```

**或下载后执行**：

```bash
curl -fsSL https://raw.githubusercontent.com/laobi465/wlyz-2demo/trae/agent-iEYsbS/install.sh -o install.sh
chmod +x install.sh
./install.sh
```

脚本自动完成：
1. 检测操作系统环境（支持 CentOS/RHEL/Rocky/AlmaLinux/Ubuntu/Debian）
2. 检测并安装宝塔面板（未安装则拉取官方脚本）
3. 检测并安装 Docker + Docker Compose v2
4. 端口冲突检测 + 自动递增（8080/3306/6379/80/8888/888，实查 ss/netstat，项目端口冲突自动 +1 找空闲端口）
5. 自动生成加密密钥（AES-256/RSA-2048/HMAC/JWT/MySQL密码，运行时随机生成）
6. Docker Compose 构建并启动 4 服务（mysql/redis/app/ui）
7. 输出配置信息到 `/root/jicek-deploy-info.txt`（权限 600）

**安装完成后**：
- 前端 UI：`http://服务器IP`
- 后端 API：`http://服务器IP:8080`
- 宝塔面板：`http://服务器IP:8888`
- 配置信息：`/root/jicek-deploy-info.txt`（含所有密钥，请妥善保管）

**默认账号**：
- 开发者：`dev / dev@123`
- 管理员：`admin / admin@123`
- 首次登录后请立即修改密码

**端口冲突自动处理**：
项目端口（8080/3306/6379/80）冲突时脚本自动 +1 递增寻找空闲端口（最多 100 次），无需手动干预。宝塔端口（8888/888）由宝塔面板管理，冲突时仅提示。也可通过环境变量预设端口：
```bash
APP_PORT=8081 MYSQL_PORT=3307 REDIS_PORT=6380 UI_PORT=81 ./install.sh
```

### 方式二：手动 Docker 部署

已有 Docker 环境的服务器可手动部署：

```bash
# 1. 克隆项目
git clone https://github.com/laobi465/wlyz-2demo.git
cd wlyz-2demo

# 2. 生成加密密钥（必须，铁律 04）
# MySQL 密码
MYSQL_ROOT_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)
MYSQL_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)
# AES-256 密钥
JICEK_AES_KEY=$(head -c 32 /dev/urandom | base64)
# HMAC 密钥
JICEK_HMAC_KEY=$(head -c 32 /dev/urandom | base64)
# JWT 密钥
JICEK_JWT_SECRET=$(head -c 48 /dev/urandom | base64)
# RSA-2048 密钥对
openssl genrsa -out private.pem 2048
JICEK_RSA_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in private.pem | base64 -w 0)
JICEK_RSA_PUBLIC_KEY=$(openssl rsa -in private.pem -pubout | base64 -w 0)

# 3. 写入 .env 文件
cat > .env <<EOF
APP_PORT=8080
MYSQL_PORT=3306
REDIS_PORT=6379
UI_PORT=80
MYSQL_DATABASE=jicek
MYSQL_USERNAME=jicek
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD=$MYSQL_PASSWORD
REDIS_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)
JICEK_AES_KEY=$JICEK_AES_KEY
JICEK_RSA_PRIVATE_KEY=$JICEK_RSA_PRIVATE_KEY
JICEK_RSA_PUBLIC_KEY=$JICEK_RSA_PUBLIC_KEY
JICEK_HMAC_KEY=$JICEK_HMAC_KEY
JICEK_JWT_SECRET=$JICEK_JWT_SECRET
JICEK_SM_ENABLED=false
JICEK_DEPLOY_ENABLED=false
JAVA_OPTS=-Xms256m -Xmx512m
EOF
chmod 600 .env

# 4. 构建并启动
docker compose build
docker compose up -d

# 5. 查看状态
docker compose ps
```

### Docker 服务管理

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f                    # 全部日志
docker compose logs -f jicek-app          # 仅后端日志
docker compose logs -f jicek-ui           # 仅前端日志

# 重启服务
docker compose restart                    # 重启全部
docker compose restart jicek-app          # 仅重启后端

# 停止/启动
docker compose down                       # 停止并移除容器
docker compose up -d                      # 启动（不重新构建）
docker compose up -d --build              # 重新构建并启动（代码更新后）

# 数据库备份
docker exec jicek-mysql mysqldump -u root -p<MYSQL_ROOT_PASSWORD> jicek > backup.sql

# 查看资源占用
docker stats jicek-mysql jicek-redis jicek-app jicek-ui
```

### 环境变量说明

| 变量 | 必填 | 说明 |
|---|---|---|
| MYSQL_ROOT_PASSWORD | 是 | MySQL root 密码（自动生成）|
| MYSQL_PASSWORD | 是 | MySQL 业务用户密码（自动生成）|
| REDIS_PASSWORD | 是 | Redis 密码（自动生成）|
| JICEK_AES_KEY | 是 | AES-256 主密钥 Base64（32 字节）|
| JICEK_RSA_PRIVATE_KEY | 是 | RSA-2048 私钥 PKCS#8 Base64 |
| JICEK_RSA_PUBLIC_KEY | 是 | RSA-2048 公钥 X.509 Base64 |
| JICEK_HMAC_KEY | 是 | HMAC-SHA256 签名密钥 Base64 |
| JICEK_JWT_SECRET | 是 | JWT 签名密钥（≥32 字节）|
| JICEK_SM_ENABLED | 否 | 国密开关，默认 false |
| JICEK_SM4_KEY | 否 | SM4 密钥（国密启用时必填）|
| JICEK_SM2_PRIVATE_KEY | 否 | SM2 私钥（国密启用时必填）|
| JICEK_DEPLOY_ENABLED | 否 | GitHub 自动部署开关，默认 false |
| GITHUB_WEBHOOK_SECRET | 否 | GitHub Webhook 密钥（部署启用时必填）|
| APP_PORT | 否 | 后端端口，默认 8080 |
| UI_PORT | 否 | 前端端口，默认 80 |
| MYSQL_PORT | 否 | MySQL 端口，默认 3306 |
| REDIS_PORT | 否 | Redis 端口，默认 6379 |

### 安全注意事项

1. `.env` 文件包含所有密钥，权限已设为 600，禁止提交到 git
2. 生产环境建议在宝塔面板配置 HTTPS（免费 SSL 证书）
3. MySQL/Redis 端口建议不对外开放（仅容器内通信）
4. 定期备份 MySQL 数据
5. `/root/jicek-deploy-info.txt` 包含所有敏感信息，请妥善保管

## License

Proprietary - 详见 LICENSE
