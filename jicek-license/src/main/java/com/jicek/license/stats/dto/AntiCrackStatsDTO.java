package com.jicek.license.stats.dto;

import lombok.Data;

import java.util.List;

/**
 * 防破解事件统计 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 数据源：
 *  - jicek_device.status=1（封禁设备）
 *  - jicek_card_key.status=2（封禁卡密，含盗刷/共享等）
 *  - 设备绑定 IP 维度（取 bind_ip，封禁 IP 数为封禁设备的去重 IP）
 *
 * 趋势按天聚合（封禁设备 + 封禁卡密之和）
 */
@Data
public class AntiCrackStatsDTO {

    /** 当前封禁设备数 */
    private Long bannedDeviceCount;

    /** 当前封禁卡密数 */
    private Long bannedCardCount;

    /** 封禁设备去重 IP 数（近似封禁 IP 数） */
    private Long bannedIpCount;

    /** 时间标签（YYYY-MM-DD） */
    private List<String> labels;

    /** 每日封禁设备数序列 */
    private List<Long> bannedDeviceTrend;

    /** 每日封禁卡密数序列 */
    private List<Long> bannedCardTrend;
}
