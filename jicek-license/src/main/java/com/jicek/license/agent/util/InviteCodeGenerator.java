package com.jicek.license.agent.util;

import com.jicek.license.common.constant.JicekConstants;
import java.security.SecureRandom;

/**
 * 邀请码生成器
 * 作者: 极策k  日期: 2026-07-22
 *
 * 使用 SecureRandom 生成 8 位邀请码（禁用 Math.random，铁律）。
 * 字符集：去易混淆字符（无 I/O/0/1）。
 */
public class InviteCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        String charset = JicekConstants.INVITE_CODE_CHARSET;
        StringBuilder sb = new StringBuilder(JicekConstants.INVITE_CODE_LENGTH);
        for (int i = 0; i < JicekConstants.INVITE_CODE_LENGTH; i++) {
            sb.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
