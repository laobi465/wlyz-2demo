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

### [进行中] 前端补全
- 优先级：P1
- 预计版本：v0.5.0（v0.4.1 已完成卡类/设备/Dashboard 图表）
- 子项：
  - [ ] 软件管理页面（待后端 DevSoftwareController 实现）
  - [x] 卡类管理页面 ✅ v0.4.1（CRUD + 4 种卡类型联动表单 + Decimal.js 金额格式化）
  - [ ] 用户管理页面（待后端 DevUserController 实现）
  - [x] 设备管理页面 ✅ v0.4.1（分页 + 详情弹窗 + 封禁/解封 + 指纹脱敏 + 状态组合）
  - [x] 代理管理页面（v0.4.0 已完成：代理列表 + 提现审核）
  - [~] 数据统计图表（ECharts） ✅ v0.4.1 部分完成（Dashboard 卡密状态饼图 + 今日收支柱状图）；待实现：验证量趋势图 / 设备在线热力图 / 收入多维统计 / 防破解事件统计
  - [ ] H5 终端用户页面（待后端 H5 Controller 实现）

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
- 技术约束：依据铁律 06（防幻觉），仅实现后端 Controller 已存在的页面，软件/用户/H5 未实现

## P2（中）

### [待开始] 云函数远程执行
- 优先级：P2
- 预计版本：v0.5.0
- 子项：
  - [ ] 沙箱隔离（Lua/LuaJIT 或 GraalVM）
  - [ ] 执行超时限制
  - [ ] 资源配额（CPU/内存）
  - [ ] 审计日志
- 备注：抗破解终极方案，沙箱安全是难点

### [已完成] UI 设计规范 ✅
- 优先级：P2
- 完成版本：v0.1.0（文档）/ v0.2.0（前端骨架）
- 状态：规范已定 + 前端骨架已实现

### [待开始] 数据统计与可视化
- 优先级：P2
- 预计版本：v0.3.0
- 子项：
  - [ ] 验证量趋势图（按小时/天/月）
  - [ ] 设备在线热力图
  - [ ] 收入统计（按通道/卡类/代理分维度）
  - [ ] 防破解事件统计

## P3（低）

### [待开始] GitHub 自动更新部署
- 优先级：P3
- 当前版本：v0.1.0
- 状态：方案已设计，待实现

### [待开始] 工单系统
- 优先级：P3
- 预计版本：v0.6.0
- 备注：可考虑复用 RuoYi-Vue-Plus 工作流

### [待开始] 多语言国际化
- 优先级：P3
- 预计版本：v0.7.0
- 备注：先支持中文，后续扩展英文
