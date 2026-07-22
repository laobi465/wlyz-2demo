package com.jicek.license.stats.dto;

import lombok.Data;

import java.util.List;

/**
 * 设备在线热力图 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 数据源：jicek_device.last_heartbeat（按天 × 小时聚合在线设备数）
 * 维度：近 N 天 × 24 小时（默认 7 天）
 *
 * 用于 ECharts heatmap 系列：x=小时(0-23)、y=日期、value=在线设备数
 */
@Data
public class DeviceHeatmapDTO {

    /** 日期标签（YYYY-MM-DD，从早到晚） */
    private List<String> days;

    /** 小时标签（0-23） */
    private List<Integer> hours;

    /**
     * 热力点列表 [day, hour, value]
     * value 为该天该小时内有心跳的设备数（基于 last_heartbeat 落在该小时窗口）
     */
    private List<List<Long>> points;

    /** 汇总：当前在线设备数（online_status=1） */
    private Long currentOnline;

    /** 汇总：总设备数 */
    private Long totalDevice;
}
