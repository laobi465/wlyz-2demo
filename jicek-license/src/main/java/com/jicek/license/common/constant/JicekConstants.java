package com.jicek.license.common.constant;

import java.math.BigDecimal;

/**
 * 极策k 常量定义
 * 作者: 极策k  日期: 2026-07-21
 */
public final class JicekConstants {

    private JicekConstants() {}

    /* ============ 订单状态 ============ */
    public static final int ORDER_STATUS_PENDING = 0;     // 待支付
    public static final int ORDER_STATUS_PAID = 1;        // 已支付
    public static final int ORDER_STATUS_FAILED = 2;      // 失败
    public static final int ORDER_STATUS_REFUNDED = 3;    // 已退款
    public static final int ORDER_STATUS_CLOSED = 4;      // 已关闭

    /* ============ 卡密状态 ============ */
    public static final int CARD_STATUS_UNUSED = 0;       // 未使用
    public static final int CARD_STATUS_USED = 1;         // 已使用
    public static final int CARD_STATUS_BANNED = 2;       // 已封禁
    public static final int CARD_STATUS_REFUNDED = 3;     // 已退款
    public static final int CARD_STATUS_EXPIRED = 4;      // 已过期

    /* ============ 卡类类型 ============ */
    public static final int CARD_TYPE_DURATION = 1;       // 时长卡
    public static final int CARD_TYPE_COUNT = 2;          // 次数卡
    public static final int CARD_TYPE_FEATURE = 3;        // 功能卡
    public static final int CARD_TYPE_PERMANENT = 4;      // 永久卡

    /* ============ 绑定策略 ============ */
    public static final int BIND_STRATEGY_NONE = 0;       // 不绑定
    public static final int BIND_STRATEGY_FIRST_LOGIN = 1;// 首次登录绑定
    public static final int BIND_STRATEGY_FIXED_N = 2;    // 指定 N 台

    /* ============ 支付通道 ============ */
    public static final String CHANNEL_ALIPAY = "alipay";
    public static final String CHANNEL_WXPAY = "wxpay";
    public static final String CHANNEL_QQPAY = "qqpay";
    public static final String CHANNEL_UNIONPAY = "unionpay";

    /* ============ 设备类型 ============ */
    public static final String DEVICE_PC = "pc";
    public static final String DEVICE_MOBILE = "mobile";

    /* ============ 彩虹易支付 V1 接口路径 ============ */
    public static final String EPAY_V1_SUBMIT_PATH = "/submit.php";    // 页面跳转
    public static final String EPAY_V1_MAPI_PATH = "/mapi.php";        // API 接口
    public static final String EPAY_V1_API_PATH = "/api.php";          // 查询/退款
    public static final String EPAY_V1_ACT_QUERY = "query";
    public static final String EPAY_V1_ACT_REFUND = "refund";

    /* ============ Redis Key 前缀 ============ */
    public static final String REDIS_KEY_NOTIFY_LOCK = "jicek:pay:notify:lock:";
    public static final String REDIS_KEY_NONCE = "jicek:sdk:nonce:";
    public static final String REDIS_KEY_QUERY_LIMIT = "jicek:card:query:limit:";
    public static final String REDIS_KEY_HEARTBEAT = "jicek:device:heartbeat:";

    /* ============ 易支付异步回调返回值 ============ */
    public static final String EPAY_NOTIFY_RETURN_SUCCESS = "success";

    /* ============ 字符集 ============ */
    public static final String CHARSET_ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final String CHARSET_NUM = "0123456789";
    public static final String CHARSET_UPPER_ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /* ============ 设备状态 ============ */
    public static final int DEVICE_STATUS_NORMAL = 0;     // 正常
    public static final int DEVICE_STATUS_BANNED = 1;     // 封禁

    public static final int DEVICE_OFFLINE = 0;           // 离线
    public static final int DEVICE_ONLINE = 1;            // 在线

    /* ============ 设备指纹维度 ============ */
    public static final int FP_DIMENSION_COUNT = 5;       // 5 维：CPU/主板/硬盘/网卡/BIOS

    /* ============ 心跳默认间隔（秒） ============ */
    public static final int HEARTBEAT_MIN_INTERVAL = 5;
    public static final int HEARTBEAT_MAX_INTERVAL = 300;
    public static final int HEARTBEAT_DEFAULT_INTERVAL = 60;

    /* ============ 换机码长度 ============ */
    public static final int BIND_CODE_LENGTH = 16;

    /* ============ 代理状态 ============ */
    public static final int AGENT_STATUS_BANNED = 0;   // 封禁
    public static final int AGENT_STATUS_NORMAL = 1;   // 正常

    /* ============ 分润类型 ============ */
    public static final int COMMISSION_TYPE_DIRECT = 1;     // 直接销售
    public static final int COMMISSION_TYPE_SUB = 2;        // 下级分润
    public static final int COMMISSION_TYPE_DIFF = 3;       // 制卡差价

    /* ============ 分润流水状态 ============ */
    public static final int COMMISSION_STATUS_REVOKED = 0;  // 已撤销（退款连带）
    public static final int COMMISSION_STATUS_VALID = 1;    // 有效

    /* ============ 提现状态（不可逆状态机） ============ */
    public static final int WITHDRAW_PENDING = 0;   // 待审核
    public static final int WITHDRAW_APPROVED = 1;  // 已通过（等待打款）
    public static final int WITHDRAW_REJECTED = 2;  // 已拒绝（余额退回）
    public static final int WITHDRAW_PAID = 3;      // 已打款
    public static final int WITHDRAW_FAILED = 4;    // 打款失败（余额退回）

    /* ============ 提现配置 ============ */
    public static final BigDecimal WITHDRAW_MIN_AMOUNT = new BigDecimal("10.00");
    public static final BigDecimal WITHDRAW_FEE_RATE = new BigDecimal("0.00"); // 默认 0%，可由租户配置
    public static final int AGENT_MAX_LEVEL = 10;   // 代理最大层级数（防深度爆炸）

    /* ============ Redis Key 前缀（代理模块） ============ */
    public static final String REDIS_KEY_WITHDRAW_LOCK = "jicek:withdraw:lock:";
    public static final String REDIS_KEY_AGENT_BALANCE_LOCK = "jicek:agent:balance:lock:";

    /* ============ 云函数配置 ============ */
    /** 代码长度上限（64KB） */
    public static final int CF_CODE_MAX_BYTES = 64 * 1024;
    /** 默认执行超时（3s） */
    public static final int CF_DEFAULT_TIMEOUT_MS = 3000;
    /** 最大执行超时（30s） */
    public static final int CF_MAX_TIMEOUT_MS = 30_000;
    /** 默认内存上限提示（8MB，实际靠 JVM -Xmx 限制） */
    public static final int CF_DEFAULT_MEMORY_KB = 8192;
    /** 默认输入大小上限（32KB） */
    public static final int CF_DEFAULT_INPUT_KB = 32;
    /** 默认输出大小上限（32KB） */
    public static final int CF_DEFAULT_OUTPUT_KB = 32;
    /** 输入/输出大小绝对上限（256KB，硬截断） */
    public static final int CF_ABSOLUTE_IO_KB = 256;
    /** 错误信息截断长度（4KB） */
    public static final int CF_ERROR_MSG_MAX_BYTES = 4 * 1024;

    /* ============ 云函数执行状态 ============ */
    public static final int CF_STATUS_SUCCESS = 0;          // 成功
    public static final int CF_STATUS_COMPILE_FAIL = 1;     // 编译失败（语法错误）
    public static final int CF_STATUS_RUNTIME_ERROR = 2;    // 运行时错误
    public static final int CF_STATUS_TIMEOUT = 3;          // 超时
    public static final int CF_STATUS_MEMORY_LIMIT = 4;     // 内存超限
    public static final int CF_STATUS_INPUT_LIMIT = 5;      // 输入超限
    public static final int CF_STATUS_OUTPUT_LIMIT = 6;     // 输出超限

    /* ============ 云函数调用来源 ============ */
    public static final String CF_SOURCE_DEV = "dev";       // 开发者测试
    public static final String CF_SOURCE_SDK = "sdk";       // 客户端调用

    /* ============ 云函数运行时 ============ */
    public static final String CF_RUNTIME_LUA = "lua";

    /* ============ Redis Key 前缀（云函数模块，执行并发限流用） ============ */
    public static final String REDIS_KEY_CF_INVOKE_LOCK = "jicek:cloud-func:invoke:lock:";
    public static final String REDIS_KEY_CF_RATE_LIMIT = "jicek:cloud-func:rate:";

    /* ============ 数据统计模块 ============ */
    /** 统计粒度：按小时 */
    public static final String STATS_GRANULARITY_HOUR = "hour";
    /** 统计粒度：按天 */
    public static final String STATS_GRANULARITY_DAY = "day";
    /** 统计粒度：按月 */
    public static final String STATS_GRANULARITY_MONTH = "month";

    /** 收入统计维度：按支付通道 */
    public static final String STATS_DIMENSION_CHANNEL = "channel";
    /** 收入统计维度：按卡类 */
    public static final String STATS_DIMENSION_CARD_TYPE = "cardType";
    /** 收入统计维度：按代理 */
    public static final String STATS_DIMENSION_AGENT = "agent";

    /** 统计查询最大天数范围（防止全表扫描，铁律 04 禁硬编码） */
    public static final int STATS_MAX_RANGE_DAYS = 90;
    /** 统计查询默认天数 */
    public static final int STATS_DEFAULT_RANGE_DAYS = 7;
    /** 热力图固定天数（近 7 天 × 24 小时） */
    public static final int STATS_HEATMAP_DAYS = 7;
    /** 热力图小时维度（24 小时） */
    public static final int STATS_HOURS_PER_DAY = 24;
}
