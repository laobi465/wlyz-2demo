package com.jicek.license.cloudfunc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 云函数调用请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * input 为任意 JSON 字符串，由调用方组装，沙箱内通过 jicek.input 全局变量访问
 * input 字节大小校验在 Service 层完成（@Size 注解仅做粗校验）
 */
@Data
public class CloudFunctionInvokeDTO {

    @NotNull
    private Long tenantId;

    @NotNull
    private Long softwareId;

    @NotNull
    private Long functionId;

    /** JSON 字符串输入（最长 256KB，Service 层按 maxInputKb 二次校验） */
    @Size(max = 262144, message = "输入最长 256KB")
    private String input;
}
