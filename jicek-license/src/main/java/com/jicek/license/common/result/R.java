package com.jicek.license.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体（沿用 RuoYi-Vue-Plus 风格）
 * 作者: 极策k  日期: 2026-07-21
 */
@Data
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.FAIL);
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(ResultCode.FAIL.getCode());
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.setCode(resultCode.getCode());
        r.setMsg(resultCode.getMsg());
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public boolean isOk() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
