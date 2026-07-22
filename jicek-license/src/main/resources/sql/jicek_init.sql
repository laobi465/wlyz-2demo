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
  device_fingerprint VARCHAR(128) NOT NULL COMMENT '设备指纹（5维SHA-256融合）',
  device_info     TEXT         COMMENT '设备详情JSON（AES加密）',
  device_name     VARCHAR(128) COMMENT '设备名称（客户端自报）',
  os_type         VARCHAR(32)  COMMENT '操作系统类型：windows/linux/macos/android/ios',
  os_version      VARCHAR(64)  COMMENT '操作系统版本',
  client_version  VARCHAR(32)  COMMENT '客户端版本',
  is_vm           TINYINT      DEFAULT 0 COMMENT '是否虚拟机：0否 1是',
  vm_extra        VARCHAR(255) COMMENT 'VM/容器补充维度（VM UUID/容器ID）',
  bind_ip         VARCHAR(45)  COMMENT '首次绑定IP',
  bind_code       VARCHAR(32)  COMMENT '换机码（绑定时生成）',
  last_heartbeat  DATETIME,
  online_status   TINYINT      DEFAULT 0 COMMENT '0离线 1在线',
  status          TINYINT      DEFAULT 0 COMMENT '0正常 1封禁',
  bind_time       DATETIME     COMMENT '绑定时间',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_fp (tenant_id, software_id, device_fingerprint),
  KEY idx_user (user_id),
  KEY idx_software_status (tenant_id, software_id, status),
  KEY idx_bind_code (bind_code),
  KEY idx_heartbeat (online_status, last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='设备';

-- ============================================================
-- 7. 代理表
-- ============================================================
DROP TABLE IF EXISTS jicek_agent;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理';

-- ============================================================
-- 7.1 代理套餐表（代理可售的软件/卡类 + 制卡价）
-- ============================================================
DROP TABLE IF EXISTS jicek_agent_package;
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
  UNIQUE KEY uk_agent_card (tenant_id, agent_id, card_type_id),
  KEY idx_agent (tenant_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理套餐（可售卡类+制卡价）';

-- ============================================================
-- 7.2 分润流水表（不可变，仅审计查询）
-- ============================================================
DROP TABLE IF EXISTS jicek_commission;
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
  KEY idx_order (order_id),
  KEY idx_source (source_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分润流水（不可删除，仅审计）';

-- ============================================================
-- 7.3 提现申请表（状态机：0待审核 → 1已通过 → 2已拒绝 / 3已打款）
-- ============================================================
DROP TABLE IF EXISTS jicek_withdraw;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

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
-- 10. 云函数表（远程执行 Lua 代码，抗破解终极方案）
-- ============================================================
DROP TABLE IF EXISTS jicek_cloud_function;
CREATE TABLE jicek_cloud_function (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  software_id     BIGINT       NOT NULL COMMENT '所属软件',
  name            VARCHAR(64)  NOT NULL COMMENT '函数名（同一软件下唯一）',
  description     VARCHAR(255) COMMENT '描述',
  code            MEDIUMTEXT   NOT NULL COMMENT 'Lua 源代码（最长 64KB）',
  runtime         VARCHAR(16)  DEFAULT 'lua' COMMENT '运行时（当前仅 lua）',
  timeout_ms      INT          DEFAULT 3000 COMMENT '执行超时（毫秒），上限 30000',
  memory_limit_kb INT          DEFAULT 8192 COMMENT '内存上限（KB，提示用，实际靠 JVM 限制）',
  max_input_kb    INT          DEFAULT 32 COMMENT '输入大小上限（KB）',
  max_output_kb   INT          DEFAULT 32 COMMENT '输出大小上限（KB）',
  enabled         TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
  version         INT          DEFAULT 1 COMMENT '版本号（每次保存自增）',
  invoke_count    BIGINT       DEFAULT 0 COMMENT '累计调用次数',
  last_invoke_time DATETIME    COMMENT '最后调用时间',
  last_invoke_ip  VARCHAR(45),
  create_by       BIGINT       COMMENT '创建人（开发者用户ID）',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_sw_name (tenant_id, software_id, name),
  KEY idx_software (tenant_id, software_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云函数定义';

-- ============================================================
-- 11. 云函数执行日志表（审计，每次执行一行）
-- ============================================================
DROP TABLE IF EXISTS jicek_cloud_function_log;
CREATE TABLE jicek_cloud_function_log (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  function_id     BIGINT       NOT NULL,
  function_name   VARCHAR(64)  NOT NULL COMMENT '冗余：函数名（便于审计）',
  software_id     BIGINT       NOT NULL COMMENT '冗余：所属软件',
  invoke_source   VARCHAR(16)  NOT NULL COMMENT 'dev（开发者测试）/ sdk（客户端调用）',
  caller_ip       VARCHAR(45),
  input_size      INT          NOT NULL COMMENT '输入字节数',
  output_size     INT          NOT NULL COMMENT '输出字节数',
  duration_ms     INT          NOT NULL COMMENT '实际执行耗时（毫秒）',
  status          TINYINT      NOT NULL COMMENT '0成功 1编译失败 2运行时错误 3超时 4内存超限 5输入超限 6输出超限',
  error_message   TEXT         COMMENT '失败时记录错误信息（截断 4KB）',
  create_time     DATETIME     NOT NULL,
  KEY idx_func (tenant_id, function_id, create_time),
  KEY idx_software (tenant_id, software_id, create_time),
  KEY idx_status (tenant_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云函数执行日志';

-- ============================================================
-- 10. 部署审计日志表（v0.6.0 新增，审计表，仅 INSERT + SELECT，禁 UPDATE/DELETE）
-- ============================================================
DROP TABLE IF EXISTS jicek_deploy_log;
CREATE TABLE jicek_deploy_log (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  trigger_source  VARCHAR(10)  NOT NULL COMMENT 'webhook(自动) / manual(手动)',
  commit_hash     VARCHAR(40)  COMMENT 'Git commit SHA',
  branch          VARCHAR(50)  DEFAULT 'main',
  status          TINYINT      NOT NULL COMMENT '0进行中 1成功 2失败 3已回滚',
  duration_ms     INT          COMMENT '总耗时毫秒',
  operator_ip     VARCHAR(64),
  error_message   VARCHAR(4096) COMMENT '错误信息（截断至 4KB）',
  create_time     DATETIME     NOT NULL,
  KEY idx_status (tenant_id, status, create_time),
  KEY idx_source (tenant_id, trigger_source, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部署审计日志（禁 UPDATE/DELETE）';

-- ============================================================
-- 13. 工单表（v0.6.1 单向工单：开发者→管理员）
-- ============================================================
DROP TABLE IF EXISTS jicek_ticket;
CREATE TABLE jicek_ticket (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
  ticket_no       VARCHAR(32)  NOT NULL COMMENT '工单号（TK+时间戳+随机，前端展示用）',
  title           VARCHAR(128) NOT NULL COMMENT '标题',
  content         TEXT         NOT NULL COMMENT '问题描述',
  category        TINYINT      NOT NULL COMMENT '1换机申请 2充值问题 3卡密问题 4其他',
  target          TINYINT      NOT NULL COMMENT '目标：2管理员（v0.6.1 简化为单向，恒为2）',
  status          TINYINT      DEFAULT 0 COMMENT '0待处理 1处理中 2已回复 3已关闭',
  creator_type    TINYINT      NOT NULL COMMENT '创建者类型：2开发者（v0.6.1 简化为单向，恒为2）',
  creator_id      BIGINT       NOT NULL COMMENT '创建者ID（开发者用户ID）',
  creator_name    VARCHAR(64)  COMMENT '创建者名称',
  software_id     BIGINT       COMMENT '关联软件（可选）',
  device_id       BIGINT       COMMENT '关联设备（换机申请时用）',
  handler_id      BIGINT       COMMENT '处理人ID（管理员）',
  handler_time    DATETIME     COMMENT '首次处理时间',
  close_time      DATETIME     COMMENT '关闭时间',
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_tenant_status (tenant_id, status, create_time),
  KEY idx_creator (creator_type, creator_id, create_time),
  KEY idx_handler (handler_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表（单向：开发者→管理员）';

-- ============================================================
-- 14. 工单回复表（对话记录，审计表，仅 INSERT + SELECT，禁 UPDATE/DELETE）
-- ============================================================
DROP TABLE IF EXISTS jicek_ticket_reply;
CREATE TABLE jicek_ticket_reply (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  ticket_id       BIGINT       NOT NULL COMMENT '关联工单ID',
  replier_type    TINYINT      NOT NULL COMMENT '回复者类型：2开发者 3管理员',
  replier_id      BIGINT       NOT NULL COMMENT '回复者ID',
  replier_name    VARCHAR(64)  COMMENT '回复者名称',
  content         TEXT         NOT NULL COMMENT '回复内容',
  create_time     DATETIME     NOT NULL,
  KEY idx_ticket (tenant_id, ticket_id, create_time),
  KEY idx_replier (replier_type, replier_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单回复（审计，禁 UPDATE/DELETE）';

-- ============================================================
-- 15. 开发者用户表（v0.7.0 鉴权框架，租户账号）
-- ============================================================
DROP TABLE IF EXISTS jicek_dev_user;
CREATE TABLE jicek_dev_user (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL COMMENT '租户ID（开发者隔离）',
  username        VARCHAR(64)  NOT NULL COMMENT '登录用户名',
  password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname        VARCHAR(64)  COMMENT '昵称',
  email           VARCHAR(128) COMMENT '邮箱',
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  last_login_time DATETIME     COMMENT '最后登录时间',
  last_login_ip   VARCHAR(45)  COMMENT '最后登录 IP',
  remark          VARCHAR(255),
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_tenant_username (tenant_id, username),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开发者用户（租户账号）';

-- ============================================================
-- 16. 管理员用户表（v0.7.0 鉴权框架，超管账号）
-- ============================================================
DROP TABLE IF EXISTS jicek_admin_user;
CREATE TABLE jicek_admin_user (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(64)  NOT NULL COMMENT '登录用户名',
  password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname        VARCHAR(64),
  role            TINYINT      DEFAULT 1 COMMENT '1超级管理员 2运营',
  status          TINYINT      DEFAULT 1 COMMENT '0封禁 1正常',
  last_login_time DATETIME,
  last_login_ip   VARCHAR(45),
  create_time     DATETIME     NOT NULL,
  update_time     DATETIME     NOT NULL,
  UNIQUE KEY uk_username (username),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户';

-- ============================================================
-- 初始化超管账号（v0.7.0）
-- 默认账号：admin / admin@123  （BCrypt cost=10）
-- 首次登录后必须修改密码（铁律 04，生产环境强制环境变量覆盖）
-- BCrypt('admin@123', 10) = $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ============================================================
INSERT INTO jicek_admin_user (username, password_hash, nickname, role, status, create_time, update_time)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', 1, 1, NOW(), NOW());

-- 初始化默认开发者账号（租户ID=1）
-- 默认账号：dev / dev@123  （BCrypt cost=10）
-- BCrypt('dev@123', 10) = $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu.
INSERT INTO jicek_dev_user (tenant_id, username, password_hash, nickname, status, create_time, update_time)
VALUES (1, 'dev', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu.', '默认开发者', 1, NOW(), NOW());

-- ============================================================
-- 完成
-- ============================================================
SELECT 'jicek database initialized successfully' AS message;
