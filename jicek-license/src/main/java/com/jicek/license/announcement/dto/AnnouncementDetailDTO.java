package com.jicek.license.announcement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告详情 DTO（开发者后台返回）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 与 SDK 返回的 SdkAnnouncementDTO 区别：本 DTO 含 status/creatorId/viewCount 等管理字段，
 * SDK 返回的 DTO 仅含客户端展示所需字段。
 */
@Data
public class AnnouncementDetailDTO {

    private Long id;
    private Long tenantId;
    private Long softwareId;
    private String title;
    private String content;
    private Integer type;
    private Integer status;
    private String minVersion;
    private String maxVersion;
    private Integer sortOrder;
    private Integer pinned;
    private Long viewCount;
    private LocalDateTime publishTime;
    private LocalDateTime offlineTime;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
