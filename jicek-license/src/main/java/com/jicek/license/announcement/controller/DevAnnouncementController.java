package com.jicek.license.announcement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.announcement.dto.AnnouncementDetailDTO;
import com.jicek.license.announcement.dto.AnnouncementSaveDTO;
import com.jicek.license.announcement.service.AnnouncementService;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者公告 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 全部 @AuthRequired(role=ROLE_DEV)，tenantId/creatorId 从 AuthContext 获取。
 *
 * 状态机：
 *  - POST   /          创建（初始为草稿）
 *  - PUT    /          编辑（仅草稿可编辑）
 *  - POST   /{id}/publish   草稿 → 已发布
 *  - POST   /{id}/offline   已发布 → 已下线
 */
@RestController
@RequestMapping("/api/dev/announcement")
@AuthRequired(role = JicekConstants.ROLE_DEV)
public class DevAnnouncementController {

    private final AnnouncementService announcementService;

    public DevAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    public R<AnnouncementDetailDTO> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        return R.ok(announcementService.create(dto));
    }

    @PutMapping
    public R<Void> update(@Valid @RequestBody AnnouncementSaveDTO dto) {
        announcementService.update(dto);
        return R.ok();
    }

    @GetMapping("/page")
    public R<Page<AnnouncementDetailDTO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String title) {
        return R.ok(announcementService.page(current, size, softwareId, status, type, title));
    }

    @GetMapping("/{id}")
    public R<AnnouncementDetailDTO> get(@PathVariable Long id) {
        return R.ok(announcementService.get(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        announcementService.publish(id);
        return R.ok();
    }

    @PostMapping("/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        announcementService.offline(id);
        return R.ok();
    }
}
