-- ============================================================
-- 极策k网络验证 - 数据库初始化脚本 v0.2.0
-- 作者: 极策k  日期: 2026-07-21
-- 数据库: MySQL 8.0.42+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS jicek DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jicek;

-- ============================================================
-- 1. 支付配置表（每个租户一行）
-- ============================================================
DROP TABLE IF EXISTS jicek_pay_config;
CREATE TABLE jicek_pay_config (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
  gateway_url     VARCHAR(255) NOT NULL COMMENT '易支付网关地址',
  pid             BIGINT       NOT NULL COMMENT '商户ID',
  merchant_key    VARCHAR(512) NOT NULL COMMENT '商户密钥(AES-256-GCM 加密存储)',
  notify_url      VARCHAR(255) COMMENT '异步通知地址(系统生成)',
  return_url      VARCHAR(255) COMMENT '同步跳转地址',
  enabled_channels VARCHAR(100) COMMENT '启用的支付通道JSON: alipay,wxpay,qqpay,unionpay',
  enabled         TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付配置';

-- ============================================================
-- 2. 支付订单表
-- ============================================================
DROP TABLE IF EXISTS jicek_pay_order;
CREATE TABLE jicek_pay_order (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  out_trade_no    VARCHAR(64)  NOT NULL COMMENT '商户订单号',
  trade_no        VARCHAR(64)  COMMENT '易支付流水号',
  card_type_id    BIGINT       COMMENT '购买的卡类ID',
  quantity        INT          DEFAULT 1,
  amount          DECIMAL(10,2) NOT NULL COMMENT '金额',
  pay_type        VARCHAR(20)  COMMENT 'alipay/wxpay/qqpay/unionpay',
  status          TINYINT      DEFAULT 0 COMMENT '0待支付 1已支付 2失败 3已退款 4已关闭',
  user_ip         VARCHAR(45)  COMMENT '用户支付IP',
  device          VARCHAR(20)  COMMENT 'pc/mobile',
  param           VARCHAR(255) COMMENT '业务扩展参数',
  pay_time        DATETIME     COMMENT '支付完成时间',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_trade (tenant_id, out_trade_no),
  KEY idx_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';

-- ============================================================
-- 3. 卡类表
-- ============================================================
DROP TABLE IF EXISTS jicek_card_type;
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
  bind_strategy   TINYINT      DEFAULT 0 COMMENT '0不绑定 1首次登录 2指定N台',
  max_devices     INT          DEFAULT 1,
  enabled         TINYINT      DEFAULT 1,
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  KEY idx_software (tenant_id, software_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡类';

-- ============================================================
-- 4. 卡密表（核心，加密存储）
-- ============================================================
DROP TABLE IF EXISTS jicek_card_key;
CREATE TABLE jicek_card_key (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  card_type_id    BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL,
  card_no         VARCHAR(64)  NOT NULL COMMENT '卡号',
  card_cipher     VARCHAR(512) NOT NULL COMMENT 'AES-256-GCM 加密后的卡密',
  card_hash       VARCHAR(64)  NOT NULL COMMENT '卡密 SHA-256 哈希(查询用)',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡密';

-- ============================================================
-- 5. 软件表
-- ============================================================
DROP TABLE IF EXISTS jicek_software;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件';

-- ============================================================
-- 6. 设备表
-- ============================================================
DROP TABLE IF EXISTS jicek_device;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备';

-- ============================================================
-- 7. 代理表
-- ============================================================
DROP TABLE IF EXISTS jicek_agent;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理';

-- ============================================================
-- 8. 更新记录表（/bdeploy 用）
-- ============================================================
DROP TABLE IF EXISTS jicek_update_log;
CREATE TABLE jicek_update_log (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  version_from    VARCHAR(64)  COMMENT '更新前版本(commit hash)',
  version_to      VARCHAR(64)  COMMENT '更新后版本',
  trigger_type    VARCHAR(20)  COMMENT 'webhook/manual/rollback',
  trigger_by      BIGINT       COMMENT '触发人用户ID',
  trigger_ip      VARCHAR(45),
  status          VARCHAR(20)  COMMENT 'running/success/failed/rolled_back',
  start_time      DATETIME,
  end_time        DATETIME,
  log_content     LONGTEXT     COMMENT '完整日志',
  error_message   TEXT,
  create_time     DATETIME     NOT NULL,
  KEY idx_status (status),
  KEY idx_create (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统更新记录';

-- ============================================================
-- 9. 审计日志表
-- ============================================================
DROP TABLE IF EXISTS jicek_update_audit;
CREATE TABLE jicek_update_audit (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  update_log_id   BIGINT       NOT NULL,
  operator_id     BIGINT       NOT NULL,
  operator_name   VARCHAR(64),
  operation       VARCHAR(50)  COMMENT '触发更新/批准/回滚/查看日志',
  operation_ip    VARCHAR(45),
  operation_time  DATETIME     NOT NULL,
  detail          TEXT,
  KEY idx_update (update_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='更新操作审计';

-- ============================================================
-- 完成
-- ============================================================
SELECT 'jicek database initialized successfully' AS message;
