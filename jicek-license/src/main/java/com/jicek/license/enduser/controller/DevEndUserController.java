package com.jicek.license.enduser.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.enduser.dto.EndUserDetailDTO;
import com.jicek.license.enduser.dto.EndUserResetPasswordDTO;
import com.jicek.license.enduser.dto.EndUserSaveDTO;
import com.jicek.license.enduser.service.EndUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者后台 - 终端用户 Controller（v0.14.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 全部 @AuthRequired(role=ROLE_DEV)，tenantId 从 AuthContext 获取，前端禁传（防越权）。
 *
 * 接口：
 *  POST   /                  创建终端用户
 *  PUT    /                  更新终端用户（password 可空表示不改）
 *  DELETE /{id}              删除终端用户
 *  GET    /page              分页查询（softwareId/username/status 可选过滤）
 *  GET    /{id}              终端用户详情
 *  POST   /{id}/ban          封禁
 *  POST   /{id}/unban        解封
 *  POST   /reset-password    重置密码
 */
@RestController
@RequestMapping("/api/dev/end-user")
@AuthRequired(role = JicekConstants.ROLE_DEV)
public class DevEndUserController {

    private final EndUserService endUserService;

    public DevEndUserController(EndUserService endUserService) {
        this.endUserService = endUserService;
    }

    /** 创建终端用户 */
    @PostMapping
    public R<EndUserDetailDTO> create(@Valid @RequestBody EndUserSaveDTO dto) {
        return R.ok(endUserService.create(dto));
    }

    /** 更新终端用户（password 为空表示不修改） */
    @PutMapping
    public R<Void> update(@Valid @RequestBody EndUserSaveDTO dto) {
        endUserService.update(dto);
        return R.ok();
    }

    /** 删除终端用户 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        endUserService.delete(id);
        return R.ok();
    }

    /** 分页查询 */
    @GetMapping("/page")
    public R<Page<EndUserDetailDTO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return R.ok(endUserService.page(current, size, softwareId, username, status));
    }

    /** 终端用户详情 */
    @GetMapping("/{id}")
    public R<EndUserDetailDTO> get(@PathVariable Long id) {
        return R.ok(endUserService.get(id));
    }

    /** 封禁 */
    @PostMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id) {
        endUserService.ban(id);
        return R.ok();
    }

    /** 解封 */
    @PostMapping("/{id}/unban")
    public R<Void> unban(@PathVariable Long id) {
        endUserService.unban(id);
        return R.ok();
    }

    /** 重置密码 */
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody EndUserResetPasswordDTO dto) {
        endUserService.resetPassword(dto);
        return R.ok();
    }
}
