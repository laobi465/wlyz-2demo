package com.jicek.license.update.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新包新建/编辑 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 文件通过 multipart 上传到 POST /api/dev/update-package/upload，
 * 上传成功返回 filePath/fileSize/fileSha256，前端再调用本 DTO 创建记录。
 * 编辑时仅允许修改 releaseNotes/minClientVersion/maxClientVersion/forceUpdate（文件不可改）。
 */
@Data
public class UpdatePackageSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    @NotNull(message = "软件ID不能为空")
    private Long softwareId;

    @NotBlank(message = "版本号不能为空")
    @Size(max = 20, message = "版本号最长 20 字符")
    private String version;

    /** 1稳定版 2内测版 */
    @NotNull(message = "通道不能为空")
    private Integer channel;

    /** 上传接口返回的相对路径 */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /** 上传接口返回的原始文件名 */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 上传接口返回的文件大小 */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /** 上传接口返回的 SHA-256 */
    @NotBlank(message = "文件 SHA-256 不能为空")
    private String fileSha256;

    /** 文件类型（由 fileName 后缀推导） */
    @NotBlank(message = "文件类型不能为空")
    private String fileType;

    /** 更新说明（Markdown） */
    private String releaseNotes;

    /** 最低适用客户端版本（含），null=不限 */
    @Size(max = 20)
    private String minClientVersion;

    /** 最高适用客户端版本（含），null=不限 */
    @Size(max = 20)
    private String maxClientVersion;

    /** 0普通 1强制更新 */
    private Integer forceUpdate = 0;
}
