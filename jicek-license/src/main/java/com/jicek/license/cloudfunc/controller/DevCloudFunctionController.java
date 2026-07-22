package com.jicek.license.cloudfunc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeDTO;
import com.jicek.license.cloudfunc.dto.CloudFunctionInvokeResult;
import com.jicek.license.cloudfunc.dto.CloudFunctionSaveDTO;
import com.jicek.license.cloudfunc.entity.CloudFunction;
import com.jicek.license.cloudfunc.entity.CloudFunctionLog;
import com.jicek.license.cloudfunc.service.CloudFunctionService;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者云函数管理 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 提供云函数 CRUD、调用测试、执行日志查询
 * 路由前缀：/api/dev/cloud-func
 *
 * 安全说明（铁律 04/06/13）：
 * - 所有接口必须传 tenantId 做租户隔离
 * - 调用来源固定为 CF_SOURCE_DEV（开发者测试）
 * - SDK 调用走 SdkCloudFunctionController（待后续版本实现，复用同一 Service）
 */
@RestController
@RequestMapping("/api/dev/cloud-func")
public class DevCloudFunctionController {

    private final CloudFunctionService cfService;

    public DevCloudFunctionController(CloudFunctionService cfService) {
        this.cfService = cfService;
    }

    /**
     * 新建/更新云函数
     */
    @PostMapping
    public R<Long> save(@Valid @RequestBody CloudFunctionSaveDTO dto) {
        return R.ok(cfService.save(dto));
    }

    /**
     * 分页查询云函数列表
     */
    @GetMapping("/page")
    public R<Page<CloudFunction>> page(@RequestParam Long tenantId,
                                        @RequestParam(required = false) Long softwareId,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) Integer enabled,
                                        @RequestParam(defaultValue = "1") int current,
                                        @RequestParam(defaultValue = "20") int size) {
        return R.ok(cfService.page(tenantId, softwareId, name, enabled, current, size));
    }

    /**
     * 云函数详情
     */
    @GetMapping("/{tenantId}/{functionId}")
    public R<CloudFunction> get(@PathVariable Long tenantId, @PathVariable Long functionId) {
        return R.ok(cfService.get(tenantId, functionId));
    }

    /**
     * 删除云函数
     */
    @DeleteMapping("/{tenantId}/{functionId}")
    public R<Void> delete(@PathVariable Long tenantId, @PathVariable Long functionId) {
        cfService.delete(tenantId, functionId);
        return R.ok();
    }

    /**
     * 启用/禁用切换
     */
    @PostMapping("/toggle-enabled")
    public R<Void> toggleEnabled(@RequestParam Long tenantId,
                                  @RequestParam Long functionId,
                                  @RequestParam int enabled) {
        cfService.toggleEnabled(tenantId, functionId, enabled);
        return R.ok();
    }

    /**
     * 测试执行云函数（开发者后台）
     */
    @PostMapping("/invoke")
    public R<CloudFunctionInvokeResult> invoke(@Valid @RequestBody CloudFunctionInvokeDTO dto,
                                                 HttpServletRequest request) {
        String ip = getClientIp(request);
        return R.ok(cfService.invoke(dto, JicekConstants.CF_SOURCE_DEV, ip));
    }

    /**
     * 执行日志分页查询
     */
    @GetMapping("/log/page")
    public R<Page<CloudFunctionLog>> logPage(@RequestParam Long tenantId,
                                              @RequestParam(required = false) Long functionId,
                                              @RequestParam(required = false) Long softwareId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String invokeSource,
                                              @RequestParam(defaultValue = "1") int current,
                                              @RequestParam(defaultValue = "20") int size) {
        return R.ok(cfService.logPage(tenantId, functionId, softwareId, status, invokeSource, current, size));
    }

    /**
     * 获取客户端真实 IP（穿透代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
