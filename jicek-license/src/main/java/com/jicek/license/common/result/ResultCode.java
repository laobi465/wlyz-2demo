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
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),

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
    CF_PARAM_INVALID(5012, "参数非法");

    private final Integer code;
    private final String msg;
}
