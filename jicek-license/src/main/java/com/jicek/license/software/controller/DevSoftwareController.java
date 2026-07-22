package com.jicek.license.software.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.software.dto.SoftwareCreateResultDTO;
import com.jicek.license.software.dto.SoftwareDetailDTO;
import com.jicek.license.software.dto.SoftwareSaveDTO;
import com.jicek.license.software.service.SoftwareService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者软件 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 全部接口 @AuthRequired(role=ROLE_DEV)，tenantId 从 AuthContext 获取，前端禁传。
 *
 * 安全说明：
 *  - POST / 返回含 signSecret + rsaPrivateKey 明文，仅此一次，前端必须提示用户保存
 *  - POST /{id}/regenerate-sign-secret 与 /{id}/regenerate-rsa-key 同样返回明文（轮换场景）
 *  - GET 接口返回脱敏 signSecret，永不返回 rsaPrivateKey
 */
@RestController
@RequestMapping("/api/dev/software")
@AuthRequired(role = JicekConstants.ROLE_DEV)
public class DevSoftwareController {

    private final SoftwareService softwareService;

    public DevSoftwareController(SoftwareService softwareService) {
        this.softwareService = softwareService;
    }

    /** 创建软件（返回含明文密钥，仅此一次） */
    @PostMapping
    public R<SoftwareCreateResultDTO> create(@Valid @RequestBody SoftwareSaveDTO dto) {
        return R.ok(softwareService.create(dto));
    }

    /** 更新软件（仅非敏感字段） */
    @PutMapping
    public R<Void> update(@Valid @RequestBody SoftwareSaveDTO dto) {
        softwareService.update(dto);
        return R.ok();
    }

    /** 分页查询当前租户的软件列表 */
    @GetMapping("/page")
    public R<Page<SoftwareDetailDTO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer enabled) {
        return R.ok(softwareService.page(current, size, name, enabled));
    }

    /** 软件详情（signSecret 脱敏，无 rsaPrivateKey） */
    @GetMapping("/{id}")
    public R<SoftwareDetailDTO> get(@PathVariable Long id) {
        return R.ok(softwareService.get(id));
    }

    /** 删除软件（关联卡类/设备/云函数时拒绝） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        softwareService.delete(id);
        return R.ok();
    }

    /** 轮换签名密钥（返回新明文，仅此一次） */
    @PostMapping("/{id}/regenerate-sign-secret")
    public R<SoftwareCreateResultDTO> regenerateSignSecret(@PathVariable Long id) {
        return R.ok(softwareService.regenerateSignSecret(id));
    }

    /** 轮换 RSA 密钥对（返回新公钥 + 私钥明文，仅此一次） */
    @PostMapping("/{id}/regenerate-rsa-key")
    public R<SoftwareCreateResultDTO> regenerateRsaKey(@PathVariable Long id) {
        return R.ok(softwareService.regenerateRsaKey(id));
    }
}
