package com.jicek.license.common.exception;

import com.jicek.license.common.result.R;
import com.jicek.license.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理
 * 作者: 极策k  日期: 2026-07-21
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e) {
        // 鉴权类异常降级日志级别为 debug（避免日志被刷屏），其他业务异常 warn
        if (e.getCode() != null && e.getCode() >= 9001 && e.getCode() <= 9999) {
            log.debug("鉴权异常: code={}, msg={}", e.getCode(), e.getMessage());
        } else {
            log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        }
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.debug("接口不存在: {}", e.getRequestURL());
        return R.fail(ResultCode.FAIL.getCode(), "接口不存在");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return R.fail(ResultCode.FAIL.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数绑定失败");
        log.warn("参数绑定失败: {}", msg);
        return R.fail(ResultCode.FAIL.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .orElse("约束校验失败");
        log.warn("约束校验失败: {}", msg);
        return R.fail(ResultCode.FAIL.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.FAIL.getCode(), "系统异常: " + e.getMessage());
    }
}
