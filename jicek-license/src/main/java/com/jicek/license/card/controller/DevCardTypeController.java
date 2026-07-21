package com.jicek.license.card.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 开发者卡类 Controller
 * 作者: 极策k  日期: 2026-07-21
 */
@RestController
@RequestMapping("/api/dev/card-type")
public class DevCardTypeController {

    private final CardTypeMapper cardTypeMapper;

    public DevCardTypeController(CardTypeMapper cardTypeMapper) {
        this.cardTypeMapper = cardTypeMapper;
    }

    @PostMapping
    public R<Void> save(@Valid @RequestBody CardType cardType) {
        if (cardType.getTenantId() == null || cardType.getSoftwareId() == null) {
            return R.fail("租户 ID 和软件 ID 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        if (cardType.getId() == null) {
            cardType.setCreateTime(now);
            cardType.setUpdateTime(now);
            if (cardType.getEnabled() == null) {
                cardType.setEnabled(1);
            }
            cardTypeMapper.insert(cardType);
        } else {
            cardType.setUpdateTime(now);
            cardTypeMapper.updateById(cardType);
        }
        return R.ok();
    }

    @GetMapping("/page")
    public R<Page<CardType>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long softwareId) {
        LambdaQueryWrapper<CardType> wrapper = new LambdaQueryWrapper<CardType>()
                .eq(CardType::getTenantId, tenantId)
                .eq(softwareId != null, CardType::getSoftwareId, softwareId)
                .orderByDesc(CardType::getCreateTime);
        return R.ok(cardTypeMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @GetMapping("/{id}")
    public R<CardType> get(@PathVariable Long id) {
        return R.ok(cardTypeMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cardTypeMapper.deleteById(id);
        return R.ok();
    }
}
