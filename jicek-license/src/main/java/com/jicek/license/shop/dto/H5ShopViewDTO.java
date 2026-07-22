package com.jicek.license.shop.dto;

import lombok.Data;

import java.util.List;

/**
 * H5 端店铺视图 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * H5 公开访问店铺首页时返回，仅含客户端展示所需字段（不含 tenantId 等内部字段）。
 * products 仅含上架商品（status=1）。
 */
@Data
public class H5ShopViewDTO {

    private Long id;
    private String name;
    private String description;
    private String contact;

    /** 冗余字段：软件名称 */
    private String softwareName;

    /** 上架商品列表（按 sort_order DESC 排序） */
    private List<ShopProductDTO> products;
}
