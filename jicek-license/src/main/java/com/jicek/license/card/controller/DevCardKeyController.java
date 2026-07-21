package com.jicek.license.card.controller;

import com.jicek.license.card.dto.CardKeyGenRequestDTO;
import com.jicek.license.card.dto.CardKeyGenResponseDTO;
import com.jicek.license.card.service.CardKeyService;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 开发者卡密 Controller
 * 作者: 极策k  日期: 2026-07-21
 */
@RestController
@RequestMapping("/api/dev/card")
public class DevCardKeyController {

    private final CardKeyService cardKeyService;

    public DevCardKeyController(CardKeyService cardKeyService) {
        this.cardKeyService = cardKeyService;
    }

    /**
     * 批量生成卡密
     * 明文卡密仅本次返回，前端必须立即保存
     */
    @PostMapping("/generate")
    public R<CardKeyGenResponseDTO> generate(@Valid @RequestBody CardKeyGenRequestDTO request) {
        return R.ok(cardKeyService.batchGenerate(request));
    }

    /**
     * 按卡号查询（脱敏，不返回明文）
     */
    @GetMapping("/query")
    public R<java.util.Map<String, Object>> query(
            @RequestParam Long tenantId,
            @RequestParam String cardNo) {
        return R.ok(java.util.Collections.singletonMap(
                "card", cardKeyService.getByCardNo(tenantId, cardNo)));
    }

    /**
     * 封禁卡密
     */
    @PostMapping("/ban")
    public R<Void> ban(@RequestParam Long tenantId,
                       @RequestParam Long cardKeyId,
                       @RequestParam(required = false) String reason) {
        cardKeyService.ban(tenantId, cardKeyId, reason);
        return R.ok();
    }

    /**
     * 退款卡密
     */
    @PostMapping("/refund")
    public R<Void> refund(@RequestParam Long tenantId,
                          @RequestParam Long cardKeyId) {
        cardKeyService.refund(tenantId, cardKeyId);
        return R.ok();
    }
}
