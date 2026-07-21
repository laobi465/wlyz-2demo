package com.jicek.license.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.common.result.R;
import com.jicek.license.device.entity.Device;
import com.jicek.license.device.service.DeviceService;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者设备管理 Controller
 * 作者: 极策k  日期: 2026-07-21
 *
 * 提供设备分页查询、按指纹查询、封禁/解封
 * 注意：设备绑定/解绑走 SDK 接口（SdkDeviceController），开发者不能直接绑定
 */
@RestController
@RequestMapping("/api/dev/device")
public class DevDeviceController {

    private final DeviceService deviceService;

    public DevDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 设备分页查询
     */
    @GetMapping("/page")
    public R<Page<Device>> page(@RequestParam Long tenantId,
                                @RequestParam(required = false) Long softwareId,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) Integer onlineStatus,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return R.ok(deviceService.pageDevices(tenantId, softwareId, status, onlineStatus, page, size));
    }

    /**
     * 按指纹精确查询
     */
    @GetMapping("/by-fingerprint")
    public R<Device> getByFingerprint(@RequestParam Long tenantId,
                                       @RequestParam Long softwareId,
                                       @RequestParam String fingerprint) {
        return R.ok(deviceService.getByFingerprint(tenantId, softwareId, fingerprint));
    }

    /**
     * 按 ID 查询详情
     */
    @GetMapping("/{tenantId}/{deviceId}")
    public R<Device> getById(@PathVariable Long tenantId, @PathVariable Long deviceId) {
        return R.ok(deviceService.getDeviceById(tenantId, deviceId));
    }

    /**
     * 封禁设备（所有 session 立即失效）
     */
    @PostMapping("/ban")
    public R<Void> ban(@RequestParam Long tenantId, @RequestParam Long deviceId) {
        deviceService.banDevice(tenantId, deviceId);
        return R.ok();
    }

    /**
     * 解封设备
     */
    @PostMapping("/unban")
    public R<Void> unban(@RequestParam Long tenantId, @RequestParam Long deviceId) {
        deviceService.unbanDevice(tenantId, deviceId);
        return R.ok();
    }
}
