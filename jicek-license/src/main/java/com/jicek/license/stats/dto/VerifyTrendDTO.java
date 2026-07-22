package com.jicek.license.stats.dto;

import lombok.Data;

import java.util.List;

/**
 * 验证量趋势统计 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 数据源：jicek_card_key.first_use_time（卡密首次激活视为一次验证）+ jicek_device.bind_time（新增设备绑定）
 * 粒度：hour/day/month
 */
@Data
public class VerifyTrendDTO {

    /** 时间标签（按粒度：YYYY-MM-DD HH:00 / YYYY-MM-DD / YYYY-MM） */
    private List<String> labels;

    /** 卡密激活次数序列 */
    private List<Long> activateCounts;

    /** 新增设备数序列 */
    private List<Long> newDeviceCounts;

    /** 汇总：总激活次数 */
    private Long totalActivate;

    /** 汇总：总新增设备 */
    private Long totalNewDevice;
}
