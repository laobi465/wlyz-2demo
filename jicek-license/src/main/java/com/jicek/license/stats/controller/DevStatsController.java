package com.jicek.license.stats.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.stats.dto.AntiCrackStatsDTO;
import com.jicek.license.stats.dto.DeviceHeatmapDTO;
import com.jicek.license.stats.dto.IncomeStatsDTO;
import com.jicek.license.stats.dto.VerifyTrendDTO;
import com.jicek.license.stats.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发者数据统计 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应 UI-DESIGN.md 6.2 节「数据统计」菜单（4 子项）：
 *  1. 验证量趋势（折线图，按小时/天/月）
 *  2. 设备在线热力图
 *  3. 收入统计（按通道/卡类/代理分维度）
 *  4. 防破解事件（封禁 IP/设备、签名失败次数）
 *
 * 路由前缀：/api/dev/stats
 * 数据源全部基于现有业务表聚合，无独立统计表
 */
@RestController
@RequestMapping("/api/dev/stats")
public class DevStatsController {

    private final StatsService statsService;

    public DevStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 验证量趋势
     *
     * @param tenantId    租户 ID（必填）
     * @param softwareId  软件 ID（可选）
     * @param granularity 粒度：hour/day/month
     * @param days        天数（hour 粒度固定 1 天）
     */
    @GetMapping("/verify-trend")
    public R<VerifyTrendDTO> verifyTrend(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) Integer days) {
        return R.ok(statsService.verifyTrend(tenantId, softwareId, granularity, days));
    }

    /**
     * 设备在线热力图（近 7 天 × 24 小时）
     */
    @GetMapping("/device-heatmap")
    public R<DeviceHeatmapDTO> deviceHeatmap(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) Integer days) {
        return R.ok(statsService.deviceHeatmap(tenantId, softwareId, days));
    }

    /**
     * 收入统计（按维度分项）
     *
     * @param dimension 维度：channel/cardType/agent
     * @param days      天数
     */
    @GetMapping("/income")
    public R<IncomeStatsDTO> income(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(defaultValue = "channel") String dimension,
            @RequestParam(required = false) Integer days) {
        return R.ok(statsService.income(tenantId, softwareId, dimension, days));
    }

    /**
     * 防破解事件统计
     */
    @GetMapping("/anti-crack")
    public R<AntiCrackStatsDTO> antiCrack(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) Integer days) {
        return R.ok(statsService.antiCrack(tenantId, softwareId, days));
    }
}
