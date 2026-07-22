package com.jicek.license.h5.controller;

import com.jicek.license.announcement.dto.SdkAnnouncementDTO;
import com.jicek.license.announcement.service.AnnouncementService;
import com.jicek.license.common.result.R;
import com.jicek.license.h5.auth.H5AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * H5 公告 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 复用 AnnouncementService 的拉取逻辑（H5 重载版本，传入 softwareId）。
 * 需 X-H5-Token 鉴权。
 *
 * 实现说明：原设计稿调用 fetchPublishedForSdk，实际 AnnouncementService
 * 提供 fetchPublished(Long softwareId, String clientVersion) 重载方法（v0.13.0 新增），
 * 此处调用该重载，由 H5AuthContext 提供 softwareId。
 */
@RestController
@RequestMapping("/api/h5/announcement")
public class H5AnnouncementController {

    private final AnnouncementService announcementService;

    public H5AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public R<List<SdkAnnouncementDTO>> list(@RequestParam(required = false) String clientVersion) {
        Long softwareId = H5AuthContext.currentSoftwareId();
        return R.ok(announcementService.fetchPublished(softwareId, clientVersion));
    }
}
