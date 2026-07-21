package com.jicek.license.common.constant;

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
}
