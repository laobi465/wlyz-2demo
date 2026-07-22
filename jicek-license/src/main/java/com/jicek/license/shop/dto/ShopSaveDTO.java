package com.jicek.license.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 店铺新建/编辑 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * tenantId 从 AuthContext 获取，前端禁传。
 */
@Data
public class ShopSaveDTO {

    /** 主键（更新时必填，新建时为空） */
    private Long id;

    /** 店铺名称 */
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称最长 64 字符")
    private String name;

    /** 关联软件 ID */
    @NotNull(message = "软件ID不能为空")
    private Long softwareId;

    /** 店铺访问路径（同租户唯一，仅允许字母数字下划线连字符） */
    @NotBlank(message = "店铺访问路径不能为空")
    @Size(max = 64, message = "店铺访问路径最长 64 字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "店铺路径仅允许字母、数字、下划线、连字符")
    private String path;

    /** 店铺描述 */
    @Size(max = 255, message = "店铺描述最长 255 字符")
    private String description;

    /** 联系方式 */
    @Size(max = 128, message = "联系方式最长 128 字符")
    private String contact;

    /** 状态：0关闭 1开启（可选，新建时默认 1） */
    private Integer status;
}
