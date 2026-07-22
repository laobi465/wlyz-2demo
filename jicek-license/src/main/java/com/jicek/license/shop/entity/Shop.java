package com.jicek.license.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内嵌卡网 - 店铺实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 业务规则：
 *  - 同租户下 path 唯一（uk_tenant_path），用于 H5 公开访问 URL
 *  - software_id 必须归属于当前租户
 *  - status：0关闭 1开启（关闭后 H5 无法访问店铺首页）
 *  - 删除店铺时级联删除 jicek_shop_product
 */
@Data
@TableName("jicek_shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 关联软件 */
    private Long softwareId;

    /** 店铺名称 */
    private String name;

    /** 店铺访问路径（同租户唯一，例如 myshop） */
    private String path;

    /** 店铺描述 */
    private String description;

    /** 联系方式 */
    private String contact;

    /** 0关闭 1开启 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
