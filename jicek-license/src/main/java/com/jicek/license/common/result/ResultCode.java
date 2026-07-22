package com.jicek.license.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 * 作者: 极策k  日期: 2026-07-21
 *
 * 范围：
 *  200        成功
 *  401/403/500 沿用 HTTP 标准
 *  1001-1999  卡密模块
 *  2001-2999  支付模块
 *  3001-3999  设备/心跳模块
 *  4001-4999  代理/分润/提现模块
 *  5001-5999  云函数模块
 *  6001-6999  数据统计模块
 *  7001-7999  部署模块
 *  8001-8999  工单模块
 *  9001-9999  鉴权模块
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    PARAM_ERROR(400, "参数非法"),

    /* ============ 卡密模块 1001-1999 ============ */
    CARD_NOT_FOUND(1001, "卡密不存在"),
    CARD_ALREADY_USED(1002, "卡密已使用"),
    CARD_BANNED(1003, "卡密已封禁"),
    CARD_EXPIRED(1004, "卡密已过期"),
    CARD_REFUNDED(1005, "卡密已退款"),
    CARD_DEVICE_LIMIT(1006, "设备数超限"),
    CARD_SIGN_ERROR(1007, "卡密签名错误"),
    CARD_DECRYPT_FAIL(1008, "卡密解密失败"),
    CARD_TYPE_NOT_FOUND(1009, "卡类不存在"),
    CARD_GEN_FAIL(1010, "卡密生成失败"),
    CARD_QUERY_RATE_LIMIT(1011, "卡密查询频率超限"),

    /* ============ 软件模块 1012-1020 ============ */
    SOFTWARE_NOT_FOUND(1012, "软件不存在"),
    SOFTWARE_NAME_EXISTS(1013, "同租户下软件名称已存在"),
    SOFTWARE_HAS_CARD_TYPE(1014, "软件下存在卡类，无法删除"),
    SOFTWARE_HAS_DEVICE(1015, "软件下存在设备，无法删除"),
    SOFTWARE_HAS_CLOUD_FUNC(1016, "软件下存在云函数，无法删除"),
    SOFTWARE_DISABLED(1017, "软件已禁用"),
    SOFTWARE_PARAM_INVALID(1018, "软件参数非法"),
    SOFTWARE_PERMISSION_DENIED(1019, "无权操作该软件"),

    /* ============ 公告模块 1021-1030 ============ */
    ANNOUNCEMENT_NOT_FOUND(1021, "公告不存在"),
    ANNOUNCEMENT_TITLE_REQUIRED(1022, "公告标题不能为空"),
    ANNOUNCEMENT_CONTENT_REQUIRED(1023, "公告内容不能为空"),
    ANNOUNCEMENT_SOFTWARE_INVALID(1024, "公告所属软件无效或无权操作"),
    ANNOUNCEMENT_STATUS_INVALID(1025, "公告状态非法"),
    ANNOUNCEMENT_TYPE_INVALID(1026, "公告类型非法"),
    ANNOUNCEMENT_ALREADY_PUBLISHED(1027, "公告已发布，不能重复发布"),
    ANNOUNCEMENT_ALREADY_OFFLINE(1028, "公告已下线"),
    ANNOUNCEMENT_NOT_PUBLISHED(1029, "公告未发布，不能下线"),

    /* ============ 更新包模块 1031-1040 ============ */
    UPDATE_PACKAGE_NOT_FOUND(1031, "更新包不存在"),
    UPDATE_PACKAGE_VERSION_REQUIRED(1032, "版本号不能为空"),
    UPDATE_PACKAGE_FILE_TYPE_INVALID(1033, "文件类型不支持（仅支持 exe/sh/win/lua/zip/7z）"),
    UPDATE_PACKAGE_FILE_TOO_LARGE(1034, "文件大小超过限制（500MB）"),
    UPDATE_PACKAGE_FILE_EMPTY(1035, "上传文件为空"),
    UPDATE_PACKAGE_SOFTWARE_INVALID(1036, "更新包所属软件无效或无权操作"),
    UPDATE_PACKAGE_VERSION_DUPLICATE(1037, "同软件同通道版本号已存在"),
    UPDATE_PACKAGE_STATUS_INVALID(1038, "更新包状态非法"),
    UPDATE_PACKAGE_ALREADY_PUBLISHED(1039, "更新包已发布，不能重复发布"),
    UPDATE_PACKAGE_ALREADY_OFFLINE(1040, "更新包已下线"),
    UPDATE_PACKAGE_NOT_PUBLISHED(1041, "更新包未发布，不能下线"),
    UPDATE_PACKAGE_HASH_MISMATCH(1042, "文件 Sha-256 校验失败（文件可能损坏）"),

    // H5 终端用户（v0.13.0）
    H5_TOKEN_MISSING(1043, "X-H5-Token 头缺失"),
    H5_TOKEN_INVALID(1044, "H5 令牌无效或已过期"),
    H5_CARD_NOT_FOUND(1045, "卡密不存在"),
    H5_CARD_EXPIRED(1046, "卡密已过期"),
    H5_CARD_BANNED(1047, "卡密已封禁"),
    H5_SOFTWARE_DISABLED(1048, "软件已禁用"),
    H5_LOGIN_FAIL(1049, "卡密登录失败"),
    H5_SHOP_NOT_FOUND(1050, "店铺不存在"),
    H5_SHOP_DISABLED(1051, "店铺已关闭"),
    H5_PRODUCT_NOT_FOUND(1052, "商品不存在"),

    /* ============ 终端用户账号（v0.14.0） ============ */
    END_USER_NOT_FOUND(1053, "终端用户不存在"),
    END_USER_USERNAME_EXISTS(1054, "用户名已存在"),
    END_USER_BANNED(1055, "终端用户已封禁"),
    END_USER_PASSWORD_ERROR(1056, "用户名或密码错误"),
    END_USER_SOFTWARE_INVALID(1057, "软件无效或无权操作"),

    /* ============ 支付模块 2001-2999 ============ */
    PAY_CONFIG_NOT_FOUND(2001, "支付配置不存在"),
    PAY_MERCHANT_KEY_ERROR(2002, "商户密钥错误"),
    PAY_ORDER_NOT_FOUND(2003, "订单不存在"),
    PAY_ORDER_STATUS_INVALID(2004, "订单状态非法"),
    PAY_SIGN_VERIFY_FAIL(2005, "签名验证失败"),
    PAY_NOTIFY_DUPLICATE(2006, "重复回调"),
    PAY_REFUND_FAIL(2007, "退款失败"),
    PAY_CHANNEL_DISABLED(2008, "支付通道未启用"),
    PAY_AMOUNT_MISMATCH(2009, "金额不匹配"),
    PAY_ORDER_TIMEOUT(2010, "订单超时"),
    PAY_LOCK_FAIL(2011, "订单处理中，请稍后"),

    /* ============ 设备/心跳模块 3001-3999 ============ */
    DEVICE_FP_COLLECT_FAIL(3001, "设备指纹采集失败"),
    DEVICE_BANNED(3002, "设备已封禁"),
    IP_BANNED(3003, "IP 已被封禁"),
    CONCURRENT_LIMIT(3004, "并发会话超限"),
    HEARTBEAT_TIMEOUT(3005, "心跳超时"),
    DEVICE_LIMIT(3006, "设备数超限"),
    RATE_LIMIT(3007, "频率超限"),
    ANTI_BRUTE_FORCE(3008, "防爆破触发"),
    DEVICE_FINGERPRINT_INVALID(3009, "设备指纹校验失败"),
    DEVICE_NOT_FOUND(3010, "设备不存在"),
    DEVICE_NOT_BOUND(3011, "设备未绑定"),
    DEVICE_ALREADY_BOUND(3012, "设备已被其他用户绑定"),
    BIND_CODE_INVALID(3013, "换机码无效或已过期"),
    BIND_CODE_USED(3014, "换机码已使用"),
    HEARTBEAT_SIGN_FAIL(3015, "心跳签名校验失败"),
    HEARTBEAT_NONCE_REPLAY(3016, "心跳 nonce 重复（疑似重放）"),

    /* ============ SDK 鉴权子段 3100-3199 ============ */
    SDK_APP_KEY_MISSING(3100, "X-App-Key 头缺失"),
    SDK_APP_KEY_INVALID(3101, "AppKey 无效或软件不存在"),
    SDK_SOFTWARE_DISABLED(3102, "软件已禁用"),
    SDK_TIMESTAMP_MISSING(3103, "X-Timestamp 头缺失"),
    SDK_TIMESTAMP_EXPIRED(3104, "时间戳超出 ±300s 容差"),
    SDK_NONCE_MISSING(3105, "X-Nonce 头缺失"),
    SDK_NONCE_REPLAY(3106, "Nonce 重复（疑似重放攻击）"),
    SDK_SIGNATURE_MISSING(3107, "X-Signature 头缺失"),
    SDK_SIGNATURE_INVALID(3108, "签名校验失败"),
    SDK_CARD_CIPHER_MISSING(3109, "X-Card-Cipher 头缺失（卡密密文）"),
    SDK_CARD_NOT_BELONG_TO_SOFTWARE(3110, "卡密不属于当前软件"),

    /* ============ 代理/分润/提现模块 4001-4999 ============ */
    AGENT_NOT_FOUND(4001, "代理不存在"),
    AGENT_BANNED(4002, "代理已封禁"),
    AGENT_USERNAME_EXISTS(4003, "代理用户名已存在"),
    AGENT_BALANCE_INSUFFICIENT(4004, "代理余额不足"),
    AGENT_PARENT_INVALID(4005, "上级代理无效或层级超限"),
    AGENT_PACKAGE_NOT_FOUND(4006, "代理套餐不存在"),
    AGENT_PRICE_INVALID(4007, "代理制卡价必须 ≤ 卡类零售价"),
    AGENT_NOT_AUTHORIZED(4008, "代理无权操作该资源"),
    COMMISSION_CALC_FAIL(4009, "分润计算失败"),
    COMMISSION_REVOKE_FAIL(4010, "分润撤销失败（已撤销或订单状态非法）"),
    WITHDRAW_NOT_FOUND(4011, "提现申请不存在"),
    WITHDRAW_STATUS_INVALID(4012, "提现状态非法，无法操作"),
    WITHDRAW_AMOUNT_INVALID(4013, "提现金额必须 ≥ 最低提现金额"),
    WITHDRAW_AMOUNT_EXCEED(4014, "提现金额超出可用余额"),
    WITHDRAW_LOCK_FAIL(4015, "提现申请处理中，请稍后"),
    WITHDRAW_ACCOUNT_INVALID(4016, "收款账号无效"),
    AGENT_BALANCE_LOCK_FAIL(4017, "代理余额变动处理中，请稍后"),

    // 代理邀请码（v0.13.0）
    AGENT_INVITE_CODE_INVALID(4018, "邀请码无效"),
    AGENT_INVITE_CODE_USED(4019, "邀请码已被使用"),
    AGENT_INVITE_CODE_DISABLED(4020, "邀请码已禁用"),
    AGENT_INVITE_CODE_SELF(4021, "不能使用自己的邀请码"),

    /* ============ 云函数模块 5001-5999 ============ */
    CF_NOT_FOUND(5001, "云函数不存在"),
    CF_DISABLED(5002, "云函数已禁用"),
    CF_CODE_TOO_LARGE(5003, "代码超过长度限制（64KB）"),
    CF_TIMEOUT(5004, "云函数执行超时"),
    CF_RUNTIME_ERROR(5005, "云函数运行时错误"),
    CF_INPUT_TOO_LARGE(5006, "输入超过大小限制"),
    CF_OUTPUT_TOO_LARGE(5007, "输出超过大小限制"),
    CF_COMPILE_FAIL(5008, "代码编译失败（语法错误）"),
    CF_NAME_EXISTS(5009, "同一软件下函数名已存在"),
    CF_MEMORY_LIMIT(5010, "云函数内存超限"),
    CF_LOCK_FAIL(5011, "云函数执行处理中，请稍后"),
    CF_PARAM_INVALID(5012, "参数非法"),

    /* ============ 数据统计模块 6001-6999 ============ */
    STATS_GRANULARITY_INVALID(6001, "统计粒度非法（仅支持 hour/day/month）"),
    STATS_DIMENSION_INVALID(6002, "统计维度非法（仅支持 channel/cardType/agent）"),
    STATS_RANGE_EXCEED(6003, "统计时间范围超过最大限制（90 天）"),
    STATS_PARAM_INVALID(6004, "统计参数非法"),

    /* ============ 部署模块 7001-7999 ============ */
    DEPLOY_LOCK_FAIL(7001, "已有部署任务进行中，请稍后"),
    DEPLOY_WEBHOOK_SIGN_FAIL(7002, "Webhook 签名验证失败"),
    DEPLOY_WEBHOOK_EVENT_INVALID(7003, "非 push 事件，忽略"),
    DEPLOY_SECRET_NOT_CONFIGURED(7004, "Webhook Secret 未配置"),
    DEPLOY_GIT_PULL_FAIL(7005, "git pull 失败"),
    DEPLOY_BUILD_FAIL(7006, "构建失败"),
    DEPLOY_RESTART_FAIL(7007, "重启失败"),
    DEPLOY_HEALTH_CHECK_FAIL(7008, "健康检查失败"),
    DEPLOY_ROLLBACK_FAIL(7009, "回滚失败"),
    DEPLOY_PARAM_INVALID(7010, "部署参数非法"),

    /* ============ 工单模块 8001-8999 ============ */
    TICKET_NOT_FOUND(8001, "工单不存在"),
    TICKET_STATUS_INVALID(8002, "工单状态非法，无法操作"),
    TICKET_ALREADY_CLOSED(8003, "工单已关闭，无法回复"),
    TICKET_PERMISSION_DENIED(8004, "无权操作该工单"),
    TICKET_TARGET_INVALID(8005, "工单目标非法（仅支持 1开发者 2管理员）"),
    TICKET_CATEGORY_INVALID(8006, "工单分类非法"),
    TICKET_CONTENT_TOO_LONG(8007, "工单内容超过长度限制"),
    TICKET_REPLY_EMPTY(8008, "回复内容不能为空"),
    TICKET_CREATOR_TYPE_INVALID(8009, "创建者类型非法"),
    TICKET_PARAM_INVALID(8010, "工单参数非法"),

    /* ============ 鉴权模块 9001-9999 ============ */
    AUTH_TOKEN_MISSING(9001, "未登录或令牌缺失"),
    AUTH_TOKEN_INVALID(9002, "令牌无效或已过期"),
    AUTH_TOKEN_WRONG_ROLE(9003, "令牌角色不匹配"),
    AUTH_USER_NOT_FOUND(9004, "用户不存在"),
    AUTH_PASSWORD_ERROR(9005, "用户名或密码错误"),
    AUTH_USER_BANNED(9006, "账号已被封禁"),
    AUTH_USER_ALREADY_EXISTS(9007, "用户名已存在"),
    AUTH_OLD_PASSWORD_ERROR(9008, "原密码错误"),
    AUTH_PASSWORD_TOO_SHORT(9009, "密码长度不足（至少 8 位）"),
    AUTH_ROLE_INVALID(9010, "角色非法"),
    AUTH_NO_PERMISSION(9011, "无权限访问该资源");

    private final Integer code;
    private final String msg;
}
