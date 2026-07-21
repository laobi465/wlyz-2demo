package com.jicek.license.pay.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.pay.dto.PayRequestDTO;
import com.jicek.license.pay.dto.PayResponseDTO;
import com.jicek.license.pay.entity.PayConfig;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.service.PayConfigService;
import com.jicek.license.pay.service.PayOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者支付 Controller
 * 作者: 极策k  日期: 2026-07-21
 *
 * 注意：tenantId 在生产环境应从 Sa-Token 登录态获取，此处简化为请求参数
 */
@RestController
@RequestMapping("/api/dev/pay")
public class DevPayController {

    private final PayConfigService payConfigService;
    private final PayOrderService payOrderService;

    public DevPayController(PayConfigService payConfigService, PayOrderService payOrderService) {
        this.payConfigService = payConfigService;
        this.payOrderService = payOrderService;
    }

    /**
     * 获取支付配置（脱敏）
     */
    @GetMapping("/config/{tenantId}")
    public R<PayConfig> getConfig(@PathVariable Long tenantId) {
        return R.ok(payConfigService.getByTenantIdMasked(tenantId));
    }

    /**
     * 保存或更新支付配置
     * merchantKey 为明文，Service 层加密入库
     */
    @PostMapping("/config")
    public R<Void> saveConfig(@Valid @RequestBody PayConfig config) {
        payConfigService.saveOrUpdate(config);
        return R.ok();
    }

    /**
     * 发起支付（终端用户 H5 调用，但通道由开发者配置决定，用户无权选）
     */
    @PostMapping("/create")
    public R<PayResponseDTO> createPay(@Valid @RequestBody PayRequestDTO request) {
        return R.ok(payOrderService.createPay(request));
    }

    /**
     * 分页查询订单
     */
    @GetMapping("/order/page")
    public R<java.util.Map<String, Object>> pageOrder(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Integer status) {
        return R.ok(java.util.Collections.singletonMap(
                "page", payOrderService.page(current, size, tenantId, status)));
    }

    /**
     * 退款（需管理员二次确认，前端应有弹窗）
     */
    @PostMapping("/refund")
    public R<Void> refund(@RequestParam String outTradeNo,
                          @RequestParam(required = false) String reason) {
        payOrderService.refund(outTradeNo, reason);
        return R.ok();
    }
}
