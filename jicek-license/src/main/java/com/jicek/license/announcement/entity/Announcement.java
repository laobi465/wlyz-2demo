package com.jicek.license.announcement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 远程公告实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 业务规则：
 *  - 同一软件下可有多条已发布公告，SDK 拉取时按 pinned DESC + sort_order DESC + publish_time DESC 排序
 *  - min_version / max_version 可选，SDK 拉取时传入客户端版本做范围匹配
 *  - 草稿状态对 SDK 不可见
 *  - 状态机：草稿(0) → 已发布(1) → 已下线(2)，不可逆
 */
@Data
@TableName("jicek_announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 所属软件 */
    private Long softwareId;

    /** 公告标题 */
    private String title;

    /** 公告内容（Markdown） */
    private String content;

    /** 1通知条 2弹窗 3置顶横幅 */
    private Integer type;

    /** 0草稿 1已发布 2已下线 */
    private Integer status;

    /** 最低适用客户端版本（含），null 表示不限 */
    private String minVersion;

    /** 最高适用客户端版本（含），null 表示不限 */
    private String maxVersion;

    /** 排序值，越大越靠前 */
    private Integer sortOrder;

    /** 0普通 1置顶 */
    private Integer pinned;

    /** SDK 拉取次数 */
    private Long viewCount;

    private LocalDateTime publishTime;

    private LocalDateTime offlineTime;

    /** 创建者（开发者用户ID） */
    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
