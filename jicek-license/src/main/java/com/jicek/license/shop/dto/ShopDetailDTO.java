package com.jicek.license.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺详情 DTO（开发者后台返回）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 含冗余字段 softwareName（避免前端二次查询软件信息）。
 */
@Data
public class ShopDetailDTO {

    private Long id;
    private Long tenantId;
    private Long softwareId;
    private String name;
    private String path;
    private String description;
    private String contact;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 冗余字段：软件名称 */
    private String softwareName;
}
