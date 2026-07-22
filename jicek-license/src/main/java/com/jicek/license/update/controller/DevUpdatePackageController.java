package com.jicek.license.update.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.update.dto.UpdatePackageDetailDTO;
import com.jicek.license.update.dto.UpdatePackageSaveDTO;
import com.jicek.license.update.dto.UploadResultDTO;
import com.jicek.license.update.service.UpdatePackageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 开发者更新包 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 全部 @AuthRequired(role=ROLE_DEV)，tenantId/creatorId 从 AuthContext 获取。
 *
 * 文件上传流程：
 *  1. POST /upload              上传文件 → 返回 filePath/fileSize/fileSha256/fileType
 *  2. POST /                    创建记录（草稿），携带上一步返回的字段
 *  3. POST /{id}/publish        发布（草稿 → 已发布）
 */
@RestController
@RequestMapping("/api/dev/update-package")
@AuthRequired(role = JicekConstants.ROLE_DEV)
public class DevUpdatePackageController {

    private final UpdatePackageService updatePackageService;

    public DevUpdatePackageController(UpdatePackageService updatePackageService) {
        this.updatePackageService = updatePackageService;
    }

    /** 上传更新包文件（multipart） */
    @PostMapping("/upload")
    public R<UploadResultDTO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(updatePackageService.upload(file));
    }

    @PostMapping
    public R<UpdatePackageDetailDTO> create(@Valid @RequestBody UpdatePackageSaveDTO dto) {
        return R.ok(updatePackageService.create(dto));
    }

    @PutMapping
    public R<Void> update(@Valid @RequestBody UpdatePackageSaveDTO dto) {
        updatePackageService.update(dto);
        return R.ok();
    }

    @GetMapping("/page")
    public R<Page<UpdatePackageDetailDTO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer channel,
            @RequestParam(required = false) String version) {
        return R.ok(updatePackageService.page(current, size, softwareId, status, channel, version));
    }

    @GetMapping("/{id}")
    public R<UpdatePackageDetailDTO> get(@PathVariable Long id) {
        return R.ok(updatePackageService.get(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        updatePackageService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        updatePackageService.publish(id);
        return R.ok();
    }

    @PostMapping("/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        updatePackageService.offline(id);
        return R.ok();
    }
}
