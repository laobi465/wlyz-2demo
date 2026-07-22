package com.jicek.license.shop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.shop.dto.ShopDetailDTO;
import com.jicek.license.shop.dto.ShopProductDTO;
import com.jicek.license.shop.dto.ShopProductSaveDTO;
import com.jicek.license.shop.dto.ShopSaveDTO;
import com.jicek.license.shop.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开发者内嵌卡网 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 全部需 JWT 鉴权（@AuthRequired(role=ROLE_DEV) 标在类上），tenantId 从 AuthContext 获取。
 *
 * 端点：
 *  - POST   /                       创建店铺
 *  - PUT    /                       更新店铺
 *  - DELETE /{id}                   删除店铺（级联商品）
 *  - GET    /page                   分页查询
 *  - GET    /{id}                   店铺详情
 *  - POST   /{id}/open              开启店铺
 *  - POST   /{id}/close             关闭店铺
 *  - POST   /product                添加商品
 *  - PUT    /product                更新商品
 *  - DELETE /product/{shopId}/{productId}  删除商品
 *  - GET    /{shopId}/products      店铺商品列表
 */
@RestController
@RequestMapping("/api/dev/shop")
@AuthRequired(role = JicekConstants.ROLE_DEV)
public class DevShopController {

    private final ShopService shopService;

    public DevShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    public R<ShopDetailDTO> create(@Valid @RequestBody ShopSaveDTO dto) {
        return R.ok(shopService.create(dto));
    }

    @PutMapping
    public R<Void> update(@Valid @RequestBody ShopSaveDTO dto) {
        shopService.update(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        shopService.delete(id);
        return R.ok();
    }

    @GetMapping("/page")
    public R<Page<ShopDetailDTO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return R.ok(shopService.page(current, size, softwareId, name, status));
    }

    @GetMapping("/{id}")
    public R<ShopDetailDTO> get(@PathVariable Long id) {
        return R.ok(shopService.get(id));
    }

    @PostMapping("/{id}/open")
    public R<Void> open(@PathVariable Long id) {
        shopService.openShop(id);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id) {
        shopService.closeShop(id);
        return R.ok();
    }

    @PostMapping("/product")
    public R<ShopProductDTO> addProduct(@Valid @RequestBody ShopProductSaveDTO dto) {
        return R.ok(shopService.addProduct(dto));
    }

    @PutMapping("/product")
    public R<Void> updateProduct(@Valid @RequestBody ShopProductSaveDTO dto) {
        shopService.updateProduct(dto);
        return R.ok();
    }

    @DeleteMapping("/product/{shopId}/{productId}")
    public R<Void> removeProduct(@PathVariable Long shopId, @PathVariable Long productId) {
        shopService.removeProduct(shopId, productId);
        return R.ok();
    }

    @GetMapping("/{shopId}/products")
    public R<List<ShopProductDTO>> listProducts(@PathVariable Long shopId) {
        return R.ok(shopService.listProducts(shopId));
    }
}
