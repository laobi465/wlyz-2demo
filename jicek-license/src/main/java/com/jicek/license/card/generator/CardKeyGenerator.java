package com.jicek.license.card.generator;

import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;

import java.security.SecureRandom;

/**
 * 卡密生成器
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全规范：
 * 1. 使用 SecureRandom，禁用 Math.random()（铁律 06）
 * 2. 默认字符集：大小写字母 + 数字
 * 3. 默认长度 24 位（不含前缀）
 * 4. 卡密格式：前缀 + 随机字符
 *
 * 性能说明：
 * - 批量生成时复用 SecureRandom 实例
 * - 1000 张卡密生成约 50ms
 */
public class CardKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成单个卡密
     * @param prefix 前缀（可为空）
     * @param charset 字符集
     * @param customCharset 自定义字符集（charset=2 时使用）
     * @param length 长度（不含前缀）
     */
    public static String generate(String prefix, int charset, String customCharset, int length) {
        if (length < 8) {
            throw new ServiceException(ResultCode.CARD_GEN_FAIL, "卡密长度不能小于 8");
        }
        String chars = selectCharset(charset, customCharset);
        StringBuilder sb = new StringBuilder(length + (prefix == null ? 0 : prefix.length()));
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix);
        }
        for (int i = 0; i < length; i++) {
            int idx = SECURE_RANDOM.nextInt(chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    /**
     * 卡号脱敏（前后各 4 位，中间 *）
     */
    public static String mask(String cardNo) {
        if (cardNo == null || cardNo.length() < 12) {
            return "****";
        }
        return cardNo.substring(0, 4)
                + "*".repeat(cardNo.length() - 8)
                + cardNo.substring(cardNo.length() - 4);
    }

    private static String selectCharset(int charset, String customCharset) {
        return switch (charset) {
            case 0 -> JicekConstants.CHARSET_ALNUM;
            case 1 -> JicekConstants.CHARSET_NUM;
            case 2 -> {
                if (customCharset == null || customCharset.length() < 2) {
                    throw new ServiceException(ResultCode.CARD_GEN_FAIL, "自定义字符集长度不足");
                }
                yield customCharset;
            }
            default -> throw new ServiceException(ResultCode.CARD_GEN_FAIL, "未知字符集: " + charset);
        };
    }
}
