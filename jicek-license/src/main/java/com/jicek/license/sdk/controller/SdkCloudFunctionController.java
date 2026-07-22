package com.jicek.license.sdk.controller;

import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeDTO;
import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeResult;
import com.jicek.license.cloudfunc.entity.CloudFunction;
import com.jicek.license.cloudfunc.service.CloudFunctionService;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.R;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.sdk.auth.SoftwareContext;
import com.jicek.license.sdk.dto.SdkCloudFunctionInvokeDTO;
import com.jicek.license.software.entity.Software;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * SDK 云函数调用 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户在开发者软件内调用云函数的入口。
 * 鉴权由 SdkAuthFilter 统一处理（HMAC-SHA256 签名 + nonce 防重放），此处无需 @AuthRequired
 * （SDK 走独立鉴权，非 JWT；SoftwareContext 由 Filter 注入，请求结束 finally 自动清理）。
 *
 * 与开发者后台 DevCloudFunctionController 的区别：
 *  - 后台传 functionId，SDK 端传 functionName（按 tenantId+softwareId+name 解析）
 *  - tenantId / softwareId 由 SoftwareContext 提供，禁客户端传入（防越权调用其他软件的云函数）
 *  - invokeSource 固定为 CF_SOURCE_SDK，便于审计区分 dev 测试与 sdk 真实调用
 *
 * 安全铁律（04/06/13）：
 *  - 云函数必须归属当前软件（tenantId + softwareId + name 三元查询，禁仅按 name 查询）
 *  - 云函数禁用状态（enabled=0）禁止调用，返回 CF_DISABLED(5002)
 *  - 云函数不存在返回 CF_NOT_FOUND(5001)
 */
@RestController
@RequestMapping("/api/sdk/cloud-function")
public class SdkCloudFunctionController {

    private final CloudFunctionService cfService;

    public SdkCloudFunctionController(CloudFunctionService cfService) {
        this.cfService = cfService;
    }

    /**
     * 调用云函数
     *
     * @param dto 调用请求（functionName + input）
     * @return 执行结果（成功含 output，失败含 status/errorMessage）
     */
    @PostMapping("/invoke")
    public R<CloudFunctionInvokeResult> invoke(@Valid @RequestBody SdkCloudFunctionInvokeDTO dto) {
        Software software = SoftwareContext.requireSoftware();
        Long tenantId = software.getTenantId();
        Long softwareId = software.getId();
        String callerIp = SoftwareContext.current().getClientIp();

        // 1. 按 (tenantId, softwareId, name) 解析云函数（禁仅按 name 查询，防越权）
        CloudFunction cf = cfService.findBySoftwareAndName(tenantId, softwareId, dto.getFunctionName());
        if (cf == null) {
            throw new ServiceException(ResultCode.CF_NOT_FOUND);
        }

        // 2. 启用状态校验（enabled=0 禁止调用）
        if (cf.getEnabled() == null || cf.getEnabled() != JicekConstants.SOFTWARE_ENABLED) {
            throw new ServiceException(ResultCode.CF_DISABLED);
        }

        // 3. 组装内部调用 DTO，复用 CloudFunctionService.invoke（统一沙箱执行 + 审计日志）
        CloudFunctionInvokeDTO invokeDTO = new CloudFunctionInvokeDTO();
        invokeDTO.setTenantId(tenantId);
        invokeDTO.setSoftwareId(softwareId);
        invokeDTO.setFunctionId(cf.getId());
        invokeDTO.setInput(dto.getInput());

        // 4. 调用来源标记为 sdk，便于审计区分 dev 测试与 sdk 真实调用
        return R.ok(cfService.invoke(invokeDTO, JicekConstants.CF_SOURCE_SDK, callerIp));
    }
}
