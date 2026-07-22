package com.jicek.license.update.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新包详情 DTO（开发者后台返回）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 不含 signature（内部字段）。downloadUrl 由下载基础 URL + filePath 拼接，
 * 仅在状态为已发布时返回（草稿/已下线不返回下载地址）。
 */
@Data
public class UpdatePackageDetailDTO {

    private Long id;
    private Long tenantId;
    private Long softwareId;
    private String version;
    private Integer channel;
    private String fileType;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileSha256;
    private String releaseNotes;
    private String minClientVersion;
    private String maxClientVersion;
    private Integer status;
    private Integer forceUpdate;
    private Long downloadCount;
    private LocalDateTime publishTime;
    private LocalDateTime offlineTime;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 下载 URL（仅已发布时返回） */
    private String downloadUrl;
}
