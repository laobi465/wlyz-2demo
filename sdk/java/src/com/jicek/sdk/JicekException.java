package com.jicek.sdk;

/**
 * 极策k SDK 异常
 * 作者: 极策k  日期: 2026-07-21
 */
public class JicekException extends RuntimeException {

    private final int code;

    public JicekException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public JicekException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
