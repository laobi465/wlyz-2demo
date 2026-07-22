package com.jicek.license.sdk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SDK 云函数调用请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户在开发者软件内调用云函数的请求体。
 * 与开发者后台 CloudFunctionInvokeDTO 的区别：
 *  - SDK 端只需 functionName（开发者后台传 functionId），由服务端按 (tenantId, softwareId, name) 解析
 *  - tenantId / softwareId 由 SdkAuthFilter 注入的 SoftwareContext 提供，禁客户端传入（防越权）
 *
 * input 为任意 JSON 字符串，沙箱内通过 jicek.input 全局变量访问；
 * 字节大小二次校验在 CloudFunctionService 层完成（@Size 仅做粗校验）。
 */
@Data
public class SdkCloudFunctionInvokeDTO {

    /** 云函数名（同一软件下唯一） */
    @NotBlank(message = "云函数名不能为空")
    private String functionName;

    /** JSON 字符串输入（最长 256KB，Service 层按云函数 maxInputKb 二次校验） */
    @Size(max = 262144, message = "输入最长 256KB")
    private String input;
}
