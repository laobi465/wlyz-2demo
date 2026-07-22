# 极策k网络验证 - UI 设计规范

> 版本：v0.1.0  日期：2026-07-21
> 适用范围：管理员后台 / 开发者后台 / 代理后台 / 终端用户 H5
> 技术栈：Vue3 + TypeScript + Element Plus 2.9.8

## 1. 设计原则

### 1.1 核心理念
- **专业克制**：面向开发者，禁用花哨装饰
- **信息密度优先**：表格、表单、列表为主，留白合理
- **状态可读**：所有资金/卡密状态必须一眼可辨
- **安全提示前置**：敏感操作前必有确认弹窗

### 1.2 严格禁用项（铁律）
1. ❌ 禁用 emoji / 表情符号 / 图标化装饰
2. ❌ 禁用毛玻璃 / `backdrop-filter` / 玻璃拟态
3. ❌ 禁用暗黑风格（深色主题），整体保持明亮
4. ❌ 禁用大渐变 / 炫光 / 霓虹 / 3D 凸起
5. ❌ 禁用 Lorem Ipsum / "示例文字" / "TODO" 占位文案（铁律 04）
6. ❌ 禁用过度动画（仅允许 fade / slide-down 200ms 内）

## 2. 色彩系统

### 2.1 主色板
| 用途 | 色值 | CSS 变量 | 说明 |
|---|---|---|---|
| 主背景 | `#FFFFFF` | `--jicek-bg-primary` | 纯白 |
| 次背景 | `#F7F8FA` | `--jicek-bg-secondary` | 极浅灰，卡片间隙、表格条纹 |
| 主色（极策蓝） | `#1A4D8F` | `--jicek-primary` | 藏蓝，按钮/链接/选中态 |
| 主色悬浮 | `#164080` | `--jicek-primary-hover` | 主色 10% 加深 |
| 主色浅 | `#F0F4FA` | `--jicek-primary-light` | 选中态背景 |
| 成功 | `#2E7D5B` | `--jicek-success` | 墨绿，支付成功、卡密有效 |
| 警告 | `#B8860B` | `--jicek-warning` | 暗金，余额不足、即将到期 |
| 危险 | `#B23A3A` | `--jicek-danger` | 砖红，封禁、退款失败、删除 |
| 信息 | `#3B7EA1` | `--jicek-info` | 中蓝，提示、辅助信息 |

### 2.2 文字色
| 用途 | 色值 | CSS 变量 |
|---|---|---|
| 主文字 | `#1F2937` | `--jicek-text-primary` |
| 次文字 | `#6B7280` | `--jicek-text-secondary` |
| 占位文字 | `#9CA3AF` | `--jicek-text-placeholder` |
| 反白文字 | `#FFFFFF` | `--jicek-text-inverse` |

### 2.3 边框与分割
| 用途 | 色值 | CSS 变量 |
|---|---|---|
| 边框/分割线 | `#E5E7EB` | `--jicek-border` |
| 悬浮边框 | `#D1D5DB` | `--jicek-border-hover` |
| 禁用背景 | `#F3F4F6` | `--jicek-bg-disabled` |

### 2.4 全局 CSS 变量定义
```css
:root {
  /* 主色 */
  --jicek-bg-primary: #FFFFFF;
  --jicek-bg-secondary: #F7F8FA;
  --jicek-primary: #1A4D8F;
  --jicek-primary-hover: #164080;
  --jicek-primary-light: #F0F4FA;
  --jicek-success: #2E7D5B;
  --jicek-warning: #B8860B;
  --jicek-danger: #B23A3A;
  --jicek-info: #3B7EA1;

  /* 文字 */
  --jicek-text-primary: #1F2937;
  --jicek-text-secondary: #6B7280;
  --jicek-text-placeholder: #9CA3AF;
  --jicek-text-inverse: #FFFFFF;

  /* 边框 */
  --jicek-border: #E5E7EB;
  --jicek-border-hover: #D1D5DB;
  --jicek-bg-disabled: #F3F4F6;

  /* 圆角 */
  --jicek-radius-sm: 4px;
  --jicek-radius-md: 6px;
  --jicek-radius-lg: 8px;

  /* 阴影 */
  --jicek-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --jicek-shadow-md: 0 2px 8px rgba(0, 0, 0, 0.06);
  --jicek-shadow-focus: 0 0 0 3px rgba(26, 77, 143, 0.1);

  /* 过渡 */
  --jicek-transition: all 0.2s ease;
}
```

## 3. 字体系统

### 3.1 字体栈
```css
--jicek-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI',
    'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei',
    'Helvetica Neue', Helvetica, Arial, sans-serif;
--jicek-font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas',
    'Courier New', monospace;
```

### 3.2 字号层级
| 用途 | 字号 | 行高 | 字重 | 用途说明 |
|---|---|---|---|---|
| H1 页面标题 | 24px | 32px | 600 | 页面主标题 |
| H2 区块标题 | 20px | 28px | 600 | 卡片标题、区块标题 |
| H3 小标题 | 16px | 24px | 600 | 表单分组、子区块 |
| 正文 | 14px | 22px | 400 | 默认文本 |
| 辅助文字 | 13px | 20px | 400 | 表格次要列、说明 |
| 小字 | 12px | 18px | 400 | 标签、徽章、时间戳 |
| 代码 | 13px | 20px | 400 | 用 mono 字体 |

## 4. 组件规范

### 4.1 按钮
- 圆角：`6px`
- 高度：默认 36px（large 40px / small 32px）
- 内边距：`0 16px`
- 阴影：扁平 + `box-shadow: 0 1px 2px rgba(0,0,0,0.04)`
- 主按钮：`background: #1A4D8F; color: #FFFFFF`
- 次按钮：`background: #FFFFFF; border: 1px solid #E5E7EB; color: #1F2937`
- 危险按钮：`background: #B23A3A; color: #FFFFFF`
- 禁用：`background: #F3F4F6; color: #9CA3AF; cursor: not-allowed`

### 4.2 卡片
- 背景：`#FFFFFF`
- 边框：`1px solid #E5E7EB`
- 圆角：`8px`
- 阴影：禁用大阴影，仅保留 `box-shadow: 0 1px 2px rgba(0,0,0,0.04)`
- 内边距：`16px`（紧凑）/ `24px`（默认）

### 4.3 输入框
- 高度：36px
- 边框：`1px solid #E5E7EB`
- 圆角：`6px`
- 聚焦：`border-color: #1A4D8F; box-shadow: 0 0 0 3px rgba(26,77,143,0.1)`
- 禁用：`background: #F3F4F6; color: #9CA3AF`
- 错误：`border-color: #B23A3A`

### 4.4 表格
- 行高：`48px`
- 表头：`background: #F7F8FA; font-weight: 600`
- 斑马纹：奇数行 `background: #FFFFFF`，偶数行 `background: #F7F8FA`
- 悬浮行：`background: #F0F4FA`
- 边框：仅底部 `1px solid #E5E7EB`

### 4.5 导航
- 左侧导航宽度：`220px`
- 菜单项高度：`40px`
- 选中态：`background: #F0F4FA; border-left: 3px solid #1A4D8F; color: #1A4D8F; font-weight: 600`
- 悬浮态：`background: #F7F8FA; color: #1A4D8F`

### 4.6 标签/徽章
- 圆角：`4px`
- 内边距：`2px 8px`
- 字号：`12px`
- 状态色映射：
  - 待支付 / 未使用：`background: #F7F8FA; color: #6B7280; border: 1px solid #E5E7EB`
  - 已支付 / 有效：`background: rgba(46,125,91,0.1); color: #2E7D5B; border: 1px solid rgba(46,125,91,0.2)`
  - 已退款 / 失败：`background: rgba(178,58,58,0.1); color: #B23A3A; border: 1px solid rgba(178,58,58,0.2)`
  - 已封禁 / 过期：`background: rgba(184,134,11,0.1); color: #B8860B; border: 1px solid rgba(184,134,11,0.2)`

### 4.7 弹窗 Dialog
- 圆角：`8px`
- 阴影：`box-shadow: 0 8px 32px rgba(0,0,0,0.12)`
- 标题高度：`56px`，字号 `16px`，字重 `600`
- 内边距：`24px`
- 确认弹窗必须包含二次确认文案（资金/卡密/封禁操作）

## 5. 布局规范

### 5.1 后台布局
```
┌─────────────────────────────────────────────────────┐
│ 顶部导航条 (60px)                                    │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│ 左侧     │   主内容区                                │
│ 导航     │   padding: 24px                          │
│ 220px    │   background: #F7F8FA                    │
│          │                                          │
│          │                                          │
└──────────┴──────────────────────────────────────────┘
```

### 5.2 H5 移动端布局
- 设计稿宽度：`375px`（iPhone 标准）
- 最大内容宽度：`100%`（全屏适配）
- 底部 Tab 栏高度：`56px`
- 顶部导航高度：`44px`
- 内容区内边距：`16px`

### 5.3 间距系统
基于 `4px` 倍数：
| 用途 | 值 |
|---|---|
| 紧凑间距 | 4px |
| 默认间距 | 8px |
| 区块内间距 | 12px |
| 区块间间距 | 16px |
| 卡片内边距 | 24px |
| 区段间间距 | 32px |

## 6. 三端页面清单

### 6.1 管理员后台（RuoYi-Vue-Plus 原生扩展）
```
系统管理（RuoYi 自带）
├─ 租户管理（开发者注册审核、套餐分配、封禁）
├─ 系统配置（加密算法开关、全局签名密钥轮换）
├─ 支付通道管理（启用/禁用 支付宝/微信/QQ/银联）
├─ 全局审计日志（资金流水、卡密生成、敏感操作）
├─ 系统更新面板（GitHub Webhook 自动更新）
└─ 公告管理（全站公告下发）
```

### 6.2 开发者后台（每个租户一个）
```
控制台（仪表盘：今日验证量/在线设备/收入/卡密销量）
软件管理
├─ 软件列表（AppKey、签名密钥、版本号、心跳间隔）
├─ 软件配置（设备绑定数、IP 限制、并发会话、防爆破策略）
└─ 版本管理与自动更新（上传更新包、最低版本号、强制更新）
卡密管理
├─ 卡类管理（时长卡/次数卡/功能卡/永久卡 + 定价）
├─ 卡密生成（批量、自定义前缀、字符集、长度）
├─ 卡密查询（按卡号/状态/绑定设备/生成时间筛选）
└─ 卡密封禁/解封/退款
用户管理
├─ 终端用户列表（账号、设备、到期时间、最后心跳）
└─ 设备管理（机器码绑定、在线状态、封禁）
代理管理
├─ 代理列表（多级树形结构、分润比例、余额）
├─ 代理套餐（可售软件/卡类、制卡价）
└─ 提现审核（待审核/已通过/已拒绝）
支付配置
├─ 支付通道开关（管理员授权后才能选）
├─ 商户凭证（pid + 密钥，加密存储）
├─ 异步/同步回调地址（系统自动生成）
└─ 订单流水（含退款、对账）
云端数据
├─ 云变量（key/value + 加密签名）
├─ 云函数（远程执行代码，抗破解终极方案）
└─ 远程公告（按软件/版本下发）
数据统计
├─ 验证量趋势（折线图，按小时/天/月）
├─ 设备在线热力图
├─ 收入统计（按通道/卡类/代理分维度）
└─ 防破解事件（封禁 IP/设备、签名失败次数）
安全中心
├─ IP 黑名单 / 设备黑名单
├─ 风控规则（频率阈值、地理围栏）
└─ 密钥轮换记录
```

### 6.3 代理后台（子集）
```
控制台（我的销量/分润/余额/待提现）
卡密管理（仅自己制卡、查询、封禁）
用户管理（仅自己发展的用户）
资金管理
├─ 充值（向开发者充值拿货）
├─ 提现申请（待开发者审核）
└─ 流水明细
下级代理（若开发者授权）
└─ 树形列表 + 分润明细
```

### 6.4 终端用户 H5（移动优先）
```
登录（卡密/账号密码）
个人中心
├─ 我的卡密（到期时间、剩余次数、绑定设备）
├─ 续费/购卡（H5 唤起支付，无接口选择权）
├─ 换机申请（需换机码或开发者审核）
└─ 在线设备（自助踢下线）
公告中心
工单（联系开发者）
```

## 7. 关键页面原型（资金/卡密相关，着重设计）

### 7.1 卡密生成页（开发者后台）
```
┌──────────────────────────────────────────────────────────┐
│ 卡密管理 > 生成卡密                                       │
├──────────────────────────────────────────────────────────┤
│ 所属软件   [请选择软件 ▼]                                 │
│ 卡类       [请选择卡类 ▼]  (关联: 时长/次数/功能/永久)    │
│ 数量       [100_______] 张                                │
│ 前缀       [JC-______]  (留空则无前缀)                    │
│ 字符集     ◉ 大小写字母+数字  ○ 纯数字  ○ 自定义          │
│ 长度       [24_______] 位                                 │
│ 有效期     ◉ 永久  ○ 生成后 X 天内有效 [___]              │
│ 绑定策略   ◉ 首次登录绑定  ○ 不绑定  ○ 指定 N 台 [__]     │
│ 备注       [_______________________________________]      │
│                                                          │
│ 安全提示：生成后卡密将加密存储，明文仅在本次展示一次       │
│                                                          │
│              [取消]  [预览]  [确认生成]                   │
└──────────────────────────────────────────────────────────┘
```

### 7.2 支付配置页（开发者后台，通道选择）
```
┌──────────────────────────────────────────────────────────┐
│ 支付配置                                  [保存配置]      │
├──────────────────────────────────────────────────────────┤
│ 支付网关地址   [https://pay.example.com______________]    │
│ 商户 ID        [1001_________________________________]    │
│ 商户密钥       [••••••••••••••••••••••••••••••••] [显示]  │
│                                                          │
│ 支付通道（用户不可选，由你勾选展示给用户）                 │
│   ☑ 支付宝 (alipay)                                      │
│   ☑ 微信支付 (wxpay)                                     │
│   ☐ QQ 钱包 (qqpay)                                      │
│   ☐ 银联云闪付 (unionpay)                                │
│                                                          │
│ 异步通知地址   https://api.jicek.com/pay/notify/{tenantId}│
│ 同步跳转地址   https://www.jicek.com/pay/return           │
│                                                          │
│ 加密选项                                                   │
│   卡密传输加密   ◉ RSA-2048 + AES-256-GCM                 │
│                 ○ 国密 SM2 + SM4                          │
│   回调验签强度   ◉ MD5 (V1 兼容)                          │
│                 ○ MD5 + 时间戳防重放 (推荐)               │
│                                                          │
│              [测试连接]  [保存配置]                       │
└──────────────────────────────────────────────────────────┘
```

### 7.3 资金流水（开发者后台，着重审计）
```
┌──────────────────────────────────────────────────────────┐
│ 资金流水                                                  │
├──────────────────────────────────────────────────────────┤
│ 时间范围 [今天 ▼]  通道 [全部 ▼]  状态 [全部 ▼]  [查询]   │
├──────────────────────────────────────────────────────────┤
│ 汇总：今日收入 ¥1,234.56 | 已退款 ¥0.00 | 净收入 ¥1,234.56│
├──────────────────────────────────────────────────────────┤
│ 订单号          金额    通道    状态    卡类    时间      │
│ JC20260721...   ¥9.90   支付宝  已支付   月卡    10:23    │
│ JC20260721...   ¥99.00  微信    已支付   年卡    10:21    │
│ JC20260721...   ¥1.00   支付宝  已退款   日卡    09:45    │
│ ...                                                      │
│                                                          │
│ [导出Excel]  [对账]                                       │
└──────────────────────────────────────────────────────────┘
```

### 7.4 终端用户 H5 - 我的卡密
```
┌─────────────────────────┐
│ ←  我的卡密              │ 44px 顶部导航
├─────────────────────────┤
│ ┌─────────────────────┐ │
│ │ 月卡 - 软件A         │ │ 卡片
│ │ 卡号: JC-****X8Y9    │ │
│ │ 状态: ● 有效         │ │ 绿色圆点
│ │ 到期: 2026-08-21     │ │
│ │ 剩余: 30天 12小时    │ │
│ │                     │ │
│ │ [续费] [换机] [详情] │ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ 次数卡 - 软件B       │ │
│ │ 卡号: JC-****A2B3    │ │
│ │ 状态: ● 有效         │ │
│ │ 剩余: 87 次          │ │
│ │                     │ │
│ │ [充值] [详情]        │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

## 8. 状态色映射表（资金/卡密专用）

### 8.1 订单状态
| 状态 | 标签文案 | 色彩 |
|---|---|---|
| 0 待支付 | 待支付 | 灰色 `#6B7280` |
| 1 已支付 | 已支付 | 绿色 `#2E7D5B` |
| 2 失败 | 失败 | 红色 `#B23A3A` |
| 3 已退款 | 已退款 | 暗金 `#B8860B` |
| 4 已关闭 | 已关闭 | 灰色 `#9CA3AF` |

### 8.2 卡密状态
| 状态 | 标签文案 | 色彩 |
|---|---|---|
| 0 未使用 | 未使用 | 灰色 `#6B7280` |
| 1 已使用 | 已使用 | 绿色 `#2E7D5B` |
| 2 已封禁 | 已封禁 | 红色 `#B23A3A` |
| 3 已退款 | 已退款 | 暗金 `#B8860B` |
| 4 已过期 | 已过期 | 灰色 `#9CA3AF` |

### 8.3 设备状态
| 状态 | 标签文案 | 色彩 |
|---|---|---|
| 在线 | ● 在线 | 绿色 `#2E7D5B` |
| 离线 | ● 离线 | 灰色 `#9CA3AF` |
| 封禁 | 已封禁 | 红色 `#B23A3A` |

### 8.4 H5 移动端布局（v0.13.0）
- 设计稿 375px（iPhone 标准），实际容器 max-width 480px 居中（兼容 PC 浏览）
- 顶部导航 44px（返回按钮 + 标题 + 右侧占位）
- 底部 Tab 栏 56px（4 Tab：我的卡密 / 公告 / 购卡 / 退出，当前路由高亮 `#1A4D8F`）
- 内容区内边距 16px，`overflow-y: auto`
- 字号比 PC 略大：标题 18px，正文 15px，辅助 13px
- 触控目标最小 44×44px（iOS HIG）

### 8.5 内嵌卡网管理页面（v0.13.0）
- 开发者后台：店铺表格 + 商品管理双层弹窗（店铺弹窗内嵌商品列表弹窗）
- 店铺状态切换：开启/关闭用 `el-switch` 直接切换（无需弹窗）
- 商品价格输入：AmountInput 组件（decimal.js 精度），可覆盖卡类售价
- H5 店铺页：店铺卡片 + 商品卡片纵向列表，商品卡片右侧「立即购买」按钮
- H5 订单页：数量选择器（`el-input-number` min=1 max=99）+ 支付方式 radio + 金额合计 + 确认按钮

### 8.6 H5 鉴权跳转（v0.13.0）
- H5 token 存 localStorage（key: `jicek_h5_token`），独立于开发者 JWT（key: `jicek_token`）
- 未登录访问需鉴权页面 → 跳 `/h5/login?redirect=xxx`
- `H5Layout.vue` onMounted 检查 token，无 token 跳登录
- 退出登录：调 `h5Api.logout` 后清理 localStorage + 跳 `/h5/login`

### 8.7 多语言切换组件（v0.14.0）
- 顶栏右侧下拉（el-dropdown trigger=click），与用户头像/退出按钮同区域
- 触发器：图标 + 当前语言标签（简体中文 / English），hover 态用 --jicek-primary + --jicek-bg-secondary
- 选项：简体中文 / English，当前语言 disabled
- 切换后 location.reload() 同步 Element Plus 语言包（简化方案，避免响应式 EP locale 复杂度）
- localStorage key: `jicek_locale`，持久化用户选择

### 8.8 终端用户管理页面（v0.14.0）
- 开发者后台：筛选区（软件下拉 + 用户名模糊 + 状态下拉）+ 表格 + 分页
- 表格列：用户名 / 昵称 / 软件名 / 邮箱 / 手机号 / 状态（el-tag 正常绿色/封禁红色）/ 最后登录时间 / 创建时间 / 操作
- 操作列：编辑 / 封禁-解封切换（el-button text）/ 重置密码（弹窗）/ 删除（ElMessageBox.confirm 二次确认）
- 新建/编辑弹窗：EndUserSaveDTO 表单（软件下拉 + 用户名 + 密码 + 昵称 + 邮箱 + 手机号 + 状态 + 备注），编辑时密码可空表示不改
- 重置密码弹窗：单字段（新密码）+ 确认按钮

### 8.9 管理员后台布局（v0.15.0）
- 独立于开发者后台：路由 `/admin/*`，独立 `AdminLayout.vue`（不复用 DevLayout），路由守卫校验 `jicek_admin_token`，缺失跳 `/admin/login`
- 管理员登录页（`/admin/login`）：用户名 + 密码（无租户ID，因管理员跨租户），调 `/api/auth/admin/login`，token 存 `jicek_admin_token`，用户信息存 `jicek_admin_user`
- `adminAxios` 独立 axios 实例（`src/api/admin.ts`）：请求拦截注入 `Bearer ${token}`，响应拦截 401/9001/9002/9003 清理 token + 跳 `/admin/login`
- 管理员工单管理页（`/admin/ticket`）：筛选区（租户ID + 分类 + 状态）+ 表格（工单号/标题/分类/状态/创建者/创建时间/操作）+ 详情弹窗（对话流回复 + 关闭按钮）
- 管理员开发者管理页（`/admin/dev-user`）：筛选区（租户ID + 用户名模糊 + 状态）+ 表格（用户名/昵称/租户ID/状态/最后登录/创建时间/操作）+ 封禁/解封切换 + 重置密码弹窗
- 布局沿用 5.1 后台布局规范（220px 侧栏 + 60px 顶栏），仅菜单项不同（工单管理 / 开发者管理 / 退出）

### 8.10 多语言国际化全量改造（v0.15.0）
- v0.14.0 渐进式改造 → v0.15.0 全量完成：17 个 dev 页面全部接入 vue-i18n（dashboard/software/cardType/cardKey/device/agent/withdraw/payConfig/payOrder/cloudFunc/stats/ticket/deploy/updatePackage/announcement/shop + login）
- 语言包扩展 16 个新模块（zh-CN.ts + en-US.ts 同步）：dashboard/software/cardType/cardKey/device/agent/withdraw/payConfig/payOrder/cloudFunc/stats/ticket/deploy/updatePackage/announcement/shop + admin（管理员后台专用）
- 管理员后台文案单独走 admin 模块（与开发者模块解耦，避免键名冲突）
- 所有用户可见文案（表格列名/按钮/弹窗标题/表单 label/Empty/placeholder/状态标签/确认弹窗文案）均支持 `t('module.key')` 中英文切换
- 保留 LangSwitch 组件（v0.14.0）切换机制：localStorage `jicek_locale` 持久化 + `location.reload` 同步 Element Plus 语言包

### 8.11 收入统计代理维度（v0.16.0）

- stats 页面代理维度（dimension=agent）移除「待扩展」alert 提示，正常展示柱状图+折线图+明细表格，与其他维度一致

## 9. 交互规范

### 9.1 加载状态
- 表格加载：底部进度条（极策蓝）
- 按钮加载：按钮内 spinner + 禁用
- 页面加载：骨架屏（灰块占位，禁用转圈）

### 9.2 反馈提示
- 成功：`ElMessage.success()`，2 秒自动消失
- 失败：`ElMessage.error()`，3 秒自动消失
- 警告：`ElMessage.warning()`，3 秒自动消失
- 资金操作成功：`ElNotification` 持久通知，需手动关闭

### 9.3 确认弹窗（资金/卡密必用）
```typescript
await ElMessageBox.confirm(
  '此操作将永久封禁该卡密，所有绑定设备立即下线，不可恢复。是否继续？',
  '封禁卡密确认',
  {
    confirmButtonText: '确认封禁',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }
)
```

### 9.4 表单校验
- 必填字段失焦即校验
- 金额字段强制 `BigDecimal`（前端用 decimal.js）
- 卡密/密钥字段长度校验 + 字符集校验
- 提交前全表单校验，禁用提交按钮直到通过

## 10. 响应式断点

| 设备 | 断点 | 布局调整 |
|---|---|---|
| 大屏 | ≥ 1920px | 主内容区 max-width 1600px 居中 |
| 桌面 | ≥ 1200px | 标准后台布局 |
| 平板 | 768-1199px | 左侧导航折叠为图标 |
| 移动 | < 768px | 切换为 H5 布局 |

## 11. 可访问性

- 颜色对比度 ≥ WCAG AA 标准（4.5:1）
- 所有按钮支持键盘 Tab 导航
- 表单字段必须有 `<label>` 关联
- 资金/状态信息不依赖颜色单独传达（加图标或文字）

## 12. 文件组织

```
ruoyi-ui/src/
├── views/jicek/
│   ├── admin/              # 管理员页面
│   │   ├── tenant/         # 租户管理
│   │   ├── pay-channel/    # 支付通道
│   │   ├── audit-log/      # 审计日志
│   │   └── update-panel/   # 系统更新
│   ├── dev/                # 开发者页面
│   │   ├── dashboard/      # 控制台
│   │   ├── software/       # 软件管理
│   │   ├── card-type/      # 卡类管理
│   │   ├── card-key/       # 卡密管理
│   │   ├── card-key-gen/   # 卡密生成
│   │   ├── user/           # 用户管理
│   │   ├── device/         # 设备管理
│   │   ├── agent/          # 代理管理
│   │   ├── pay-config/     # 支付配置
│   │   ├── pay-order/      # 订单流水
│   │   ├── cloud-var/      # 云变量
│   │   ├── cloud-func/     # 云函数
│   │   ├── stats/          # 数据统计
│   │   └── security/       # 安全中心
│   ├── agent/              # 代理页面
│   │   ├── dashboard/
│   │   ├── card-key/
│   │   ├── user/
│   │   └── finance/
│   └── h5/                 # 终端用户 H5
│       ├── login/
│       ├── my-cards/
│       ├── buy/
│       ├── change-device/
│       └── notice/
├── components/jicek/        # 极策k专用组件
│   ├── CardKeyTag.vue       # 卡密状态标签
│   ├── OrderStatusTag.vue   # 订单状态标签
│   ├── DeviceStatusTag.vue  # 设备状态标签
│   ├── PayChannelSelect.vue # 支付通道选择
│   ├── AmountInput.vue      # 金额输入（BigDecimal）
│   └── ConfirmDialog.vue    # 二次确认弹窗
└── styles/jicek.scss        # 极策k全局样式 + CSS 变量
```
