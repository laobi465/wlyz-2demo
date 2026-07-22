package com.jicek.license.cloudfunc.dto;

import lombok.Data;

/**
 * 云函数执行结果
 * 作者: 极策k  日期: 2026-07-22
 *
 * result 为 Lua 函数返回值序列化后的 JSON 字符串
 * 失败时 result 为 null，status/errorMessage 必有值
 */
@Data
public class CloudFunctionInvokeResult {

    /** 执行状态，见 JicekConstants.CF_STATUS_* */
    private Integer status;

    /** 实际执行耗时（毫秒） */
    private Integer durationMs;

    /** 输入字节数 */
    private Integer inputSize;

    /** 输出字节数 */
    private Integer outputSize;

    /** 成功时有值，失败时为 null */
    private String result;

    /** 失败时有值 */
    private String errorMessage;

    public static CloudFunctionInvokeResult success(String result, int durationMs, int inputSize, int outputSize) {
        CloudFunctionInvokeResult r = new CloudFunctionInvokeResult();
        r.status = 0;
        r.durationMs = durationMs;
        r.inputSize = inputSize;
        r.outputSize = outputSize;
        r.result = result;
        return r;
    }

    public static CloudFunctionInvokeResult fail(int status, String errorMessage, int durationMs, int inputSize, int outputSize) {
        CloudFunctionInvokeResult r = new CloudFunctionInvokeResult();
        r.status = status;
        r.durationMs = durationMs;
        r.inputSize = inputSize;
        r.outputSize = outputSize;
        r.errorMessage = errorMessage;
        return r;
    }
}
