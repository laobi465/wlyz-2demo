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
    HEARTBEAT_NONCE_REPLAY(3016, "心跳 nonce 重复（疑似重放）");

    private final Integer code;
    private final String msg;
}
