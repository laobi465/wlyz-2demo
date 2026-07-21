package com.jicek.license.common.exception;

import com.jicek.license.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * 作者: 极策k  日期: 2026-07-21
 */
@Getter
public class ServiceException extends RuntimeException {

    private final Integer code;

    public ServiceException(String msg) {
        super(msg);
        this.code = ResultCode.FAIL.getCode();
    }

    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public ServiceException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public ServiceException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
    }
}
