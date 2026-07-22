package com.jicek.license.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.dto.DevUserDetailDTO;
import com.jicek.license.auth.dto.DevUserResetPasswordDTO;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.auth.service.DevUserService;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 - 开发者（租户）管理 Controller（v0.15.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 路由前缀：/api/admin/dev-user
 * 全部 @AuthRequired(role=ROLE_ADMIN)，仅管理员可访问。
 *
 * 接口：
 *  GET  /page              分页查询所有开发者账号（tenantId/username/status 筛选）
 *  GET  /{id}              开发者详情
 *  POST /{id}/ban          封禁（status=0）
 *  POST /{id}/unban        解封（status=1）
 *  POST /reset-password    重置密码
 *
 * 安全说明（铁律 04/06/13/09）：
 *  - 管理员可管理全部租户开发者账号，不限 tenantId
 *  - 返回 DTO 不含 passwordHash
 *  - 重置密码 BCrypt 哈希存储
 */
@RestController
@RequestMapping("/api/admin/dev-user")
@AuthRequired(role = JicekConstants.ROLE_ADMIN)
public class AdminDevUserController {

    private final DevUserService devUserService;

    public AdminDevUserController(DevUserService devUserService) {
        this.devUserService = devUserService;
    }

    /**
     * 分页查询所有开发者账号
     */
    @GetMapping("/page")
    public R<Page<DevUserDetailDTO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) Long tenantId,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) Integer status) {
        return R.ok(devUserService.page(current, size, tenantId, username, status));
    }

    /**
     * 开发者详情
     */
    @GetMapping("/{id}")
    public R<DevUserDetailDTO> get(@PathVariable Long id) {
        return R.ok(devUserService.get(id));
    }

    /**
     * 封禁开发者
     */
    @PostMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id) {
        devUserService.ban(id);
        return R.ok();
    }

    /**
     * 解封开发者
     */
    @PostMapping("/{id}/unban")
    public R<Void> unban(@PathVariable Long id) {
        devUserService.unban(id);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody DevUserResetPasswordDTO dto) {
        devUserService.resetPassword(dto);
        return R.ok();
    }
}
