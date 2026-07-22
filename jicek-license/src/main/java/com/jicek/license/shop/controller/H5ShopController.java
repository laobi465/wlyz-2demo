package com.jicek.license.shop.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.shop.dto.H5CreateOrderDTO;
import com.jicek.license.shop.dto.H5CreateOrderResultDTO;
import com.jicek.license.shop.dto.H5ShopViewDTO;
import com.jicek.license.shop.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * H5 内嵌卡网 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 端点：
 *  - GET  /info?path=xxx   获取店铺+商品列表（公开，无需 X-H5-Token）
 *  - POST /order           创建订单（需 X-H5-Token，由 H5AuthInterceptor 拦截）
 *
 * 鉴权说明：
 *  - /api/h5/shop/info 在 WebMvcConfig 中放行（公开访问）
 *  - /api/h5/shop/order 走 H5AuthInterceptor 默认拦截（需 X-H5-Token）
 */
@RestController
@RequestMapping("/api/h5/shop")
public class H5ShopController {

    private final ShopService shopService;

    public H5ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 获取店铺信息（公开访问，无需 X-H5-Token）
     * @param path 店铺访问路径
     * @return 店铺视图（含上架商品列表）
     */
    @GetMapping("/info")
    public R<H5ShopViewDTO> getShopByPath(@RequestParam String path) {
        return R.ok(shopService.getShopByPath(path));
    }

    /**
     * 创建订单（需 X-H5-Token）
     * @param dto 下单请求
     * @return 下单结果（含订单号 + 金额 + 占位 payUrl）
     */
    @PostMapping("/order")
    public R<H5CreateOrderResultDTO> createOrder(@Valid @RequestBody H5CreateOrderDTO dto) {
        return R.ok(shopService.createOrder(dto));
    }
}
