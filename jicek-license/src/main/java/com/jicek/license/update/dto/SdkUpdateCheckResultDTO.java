package com.jicek.license.update.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SDK 更新检查结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 无更新时所有字段为 null（客户端判断 hasUpdate == false）。
 * 有更新时返回最新版本信息 + 下载 URL + SHA-256（客户端校验完整性）。
 */
@Data
public class SdkUpdateCheckResultDTO {

    /** 是否有更新 */
    private Boolean hasUpdate;

    /** 是否强制更新（旧版拒绝运行） */
    private Boolean forceUpdate;

    private Long packageId;
    private String version;
    private String fileType;
    private String fileName;
    private Long fileSize;
    private String fileSha256;
    private String releaseNotes;
    private String downloadUrl;
    private LocalDateTime publishTime;
}
