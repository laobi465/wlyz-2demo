package com.jicek.license.update.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新包实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 业务规则：
 *  - 同一软件 + 版本 + 通道唯一（uk_software_version）
 *  - 多格式：exe/sh/win/lua/zip/7z，客户端按 file_type 处理
 *  - 状态机：草稿(0) → 已发布(1) → 已下线(2)，不可逆
 *  - file_path 存相对路径（相对 storage.root），禁存绝对路径（铁律 04，环境隔离）
 *  - file_sha256 客户端下载后校验完整性
 */
@Data
@TableName("jicek_update_package")
public class UpdatePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long softwareId;

    /** 版本号（语义化 X.Y.Z） */
    private String version;

    /** 1稳定版 2内测版 */
    private Integer channel;

    /** exe/sh/win/lua/zip/7z */
    private String fileType;

    private String fileName;

    /** 存储相对路径（相对 storage.root） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 SHA-256（小写 64 位十六进制） */
    private String fileSha256;

    /** 可选 RSA 签名 Base64 */
    private String signature;

    /** 更新说明（Markdown） */
    private String releaseNotes;

    /** 最低适用客户端版本（含），null=不限 */
    private String minClientVersion;

    /** 最高适用客户端版本（含），null=不限 */
    private String maxClientVersion;

    /** 0草稿 1已发布 2已下线 */
    private Integer status;

    /** 0普通 1强制更新 */
    private Integer forceUpdate;

    private Long downloadCount;

    private LocalDateTime publishTime;

    private LocalDateTime offlineTime;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
