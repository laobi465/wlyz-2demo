package com.jicek.license.announcement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SDK 公告返回 DTO（终端用户客户端展示用）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 仅含客户端展示所需字段，不含 status/creatorId/viewCount 等管理字段。
 * 草稿/已下线的公告不会出现在 SDK 返回中。
 */
@Data
public class SdkAnnouncementDTO {

    private Long id;
    private String title;
    private String content;
    /** 1通知条 2弹窗 3置顶横幅 */
    private Integer type;
    /** 0普通 1置顶 */
    private Integer pinned;
    private Integer sortOrder;
    private LocalDateTime publishTime;
}
