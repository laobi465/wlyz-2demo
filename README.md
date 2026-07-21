# 极策k网络验证

> 面向开发者的多租户卡密验证 SaaS 平台

## 项目状态

- 版本：v0.2.0-SNAPSHOT（开发中）
- 文档版本：v0.1.0

## 技术栈

- 后端：Spring Boot 3.4.6 + MyBatis-Plus 3.5.12 + Redisson
- 数据库：MySQL 8.0.42
- 加密：AES-256-GCM + RSA-2048 + HMAC-SHA256（可选国密 SM2/SM4）
- 支付：彩虹易支付 V1（独立部署，HTTP 对接）

## 模块说明

- `jicek-license/` - 卡密验证核心模块（P0 已完成）

## 文档

- [CHANGELOG.md](CHANGELOG.md) - 更新日志
- [TODO.md](TODO.md) - 任务清单
- [docs/PROJECT.md](docs/PROJECT.md) - 项目文档
- [docs/SPEC.md](docs/SPEC.md) - 规范文档
- [docs/UI-DESIGN.md](docs/UI-DESIGN.md) - UI 设计规范

## 快速开始

```bash
# 1. 初始化数据库
mysql -u root -p < jicek-license/src/main/resources/sql/jicek_init.sql

# 2. 配置环境变量（敏感信息禁硬编码）
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

## 安全规范

- 所有敏感密钥必须通过环境变量注入，禁硬编码
- 卡密 AES-256-GCM 加密入库，明文仅展示一次
- 资金与卡密发放同事务，杜绝已支付未发卡
- 异步回调 HMAC 验签 + Redis 分布式锁幂等

## License

Proprietary - 极策k
