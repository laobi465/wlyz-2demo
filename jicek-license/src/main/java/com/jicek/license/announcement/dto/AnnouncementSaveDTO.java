package com.jicek.license.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新建/编辑 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * tenantId 从 AuthContext 获取，creatorId 从 AuthContext.currentUserId() 获取，前端禁传。
 */
@Data
public class AnnouncementSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    /** 所属软件 ID */
    @NotNull(message = "软件ID不能为空")
    private Long softwareId;

    /** 公告标题 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 128, message = "标题最长 128 字符")
    private String title;

    /** 公告内容（Markdown） */
    @NotBlank(message = "公告内容不能为空")
    private String content;

    /** 1通知条 2弹窗 3置顶横幅 */
    @NotNull(message = "公告类型不能为空")
    private Integer type;

    /** 最低适用客户端版本（含），null 表示不限 */
    @Size(max = 20, message = "版本号最长 20 字符")
    private String minVersion;

    /** 最高适用客户端版本（含），null 表示不限 */
    @Size(max = 20, message = "版本号最长 20 字符")
    private String maxVersion;

    /** 排序值，越大越靠前 */
    private Integer sortOrder = 0;

    /** 0普通 1置顶 */
    private Integer pinned = 0;
}
