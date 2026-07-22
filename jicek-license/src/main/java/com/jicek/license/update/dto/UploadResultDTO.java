package com.jicek.license.update.dto;

import lombok.Data;

/**
 * 更新包文件上传结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 上传成功后返回，前端持有这些字段再调用 create 接口。
 */
@Data
public class UploadResultDTO {
    /** 存储相对路径（相对 storage.root） */
    private String filePath;
    /** 原始文件名 */
    private String fileName;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 文件 SHA-256（小写 64 位十六进制） */
    private String fileSha256;
    /** 文件类型（后缀小写） */
    private String fileType;
}
