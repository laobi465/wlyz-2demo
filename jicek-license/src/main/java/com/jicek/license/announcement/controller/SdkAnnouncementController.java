package com.jicek.license.announcement.controller;

import com.jicek.license.announcement.dto.SdkAnnouncementDTO;
import com.jicek.license.announcement.service.AnnouncementService;
import com.jicek.license.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDK 公告 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户客户端通过 SDK 拉取已发布公告。
 * 鉴权由 SdkAuthFilter 统一处理（X-App-Key + HMAC 签名），softwareId 从 SoftwareContext 获取。
 *
 * 返回仅含展示字段（title/content/type/pinned/sortOrder/publishTime），
 * 不含 status/creatorId/viewCount 等管理字段。
 */
@RestController
@RequestMapping("/api/sdk/announcement")
public class SdkAnnouncementController {

    private final AnnouncementService announcementService;

    public SdkAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * 拉取已发布公告
     *
     * @param clientVersion 客户端版本（可选，用于版本范围匹配，格式 X.Y.Z）
     * @return 按 pinned DESC + sort_order DESC + publish_time DESC 排序的公告列表（最多 20 条）
     */
    @GetMapping
    public R<List<SdkAnnouncementDTO>> fetch(@RequestParam(required = false) String clientVersion) {
        return R.ok(announcementService.fetchPublished(clientVersion));
    }
}
