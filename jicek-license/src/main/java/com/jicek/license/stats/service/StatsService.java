package com.jicek.license.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.agent.entity.Agent;
import com.jicek.license.agent.mapper.AgentMapper;
import com.jicek.license.card.entity.CardKey;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardKeyMapper;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.device.entity.Device;
import com.jicek.license.device.mapper.DeviceMapper;
import com.jicek.license.pay.entity.PayOrder;
import com.jicek.license.pay.mapper.PayOrderMapper;
import com.jicek.license.stats.dto.AntiCrackStatsDTO;
import com.jicek.license.stats.dto.DeviceHeatmapDTO;
import com.jicek.license.stats.dto.IncomeStatsDTO;
import com.jicek.license.stats.dto.VerifyTrendDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计 Service
 * 作者: 极策k  日期: 2026-07-22
 *
 * 设计说明（铁律 04/06/13）：
 * 1. 所有上限/粒度值走 JicekConstants 常量，禁字面量
 * 2. 数据源仅基于现有业务表聚合，禁虚构表/字段
 * 3. 金额一律 BigDecimal，禁 float/double
 * 4. 时间分组使用 Java Stream 内存聚合，避免写复杂 SQL（与现有代码风格一致）
 * 5. 统计查询范围 ≤ STATS_MAX_RANGE_DAYS(90天)，防止全表扫描
 */
@Service
public class StatsService {

    private final CardKeyMapper cardKeyMapper;
    private final CardTypeMapper cardTypeMapper;
    private final DeviceMapper deviceMapper;
    private final PayOrderMapper payOrderMapper;
    private final AgentMapper agentMapper;

    public StatsService(CardKeyMapper cardKeyMapper,
                        CardTypeMapper cardTypeMapper,
                        DeviceMapper deviceMapper,
                        PayOrderMapper payOrderMapper,
                        AgentMapper agentMapper) {
        this.cardKeyMapper = cardKeyMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.deviceMapper = deviceMapper;
        this.payOrderMapper = payOrderMapper;
        this.agentMapper = agentMapper;
    }

    /* ============ 验证量趋势 ============ */

    /**
     * 验证量趋势统计
     *
     * @param tenantId   租户 ID（必填）
     * @param softwareId 软件 ID（可选，null 表示全部）
     * @param granularity 粒度：hour/day/month
     * @param days       天数范围（仅 day/month 粒度有效，hour 固定查近 1 天）
     */
    public VerifyTrendDTO verifyTrend(Long tenantId, Long softwareId, String granularity, Integer days) {
        validateGranularity(granularity);
        int actualDays = sanitizeDays(days);

        // hour 粒度强制查近 1 天（24 小时）
        if (JicekConstants.STATS_GRANULARITY_HOUR.equals(granularity)) {
            actualDays = 1;
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(actualDays).truncatedTo(ChronoUnit.DAYS);

        // 1. 卡密激活数据（first_use_time 落在窗口内）
        List<CardKey> cards = cardKeyMapper.selectList(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(softwareId != null, CardKey::getSoftwareId, softwareId)
                        .isNotNull(CardKey::getFirstUseTime)
                        .ge(CardKey::getFirstUseTime, start)
                        .lt(CardKey::getFirstUseTime, end));
        Map<String, Long> cardBucket = bucketByTime(cards.stream()
                .map(CardKey::getFirstUseTime)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()), granularity);

        // 2. 新增设备数据（bind_time 落在窗口内）
        List<Device> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId)
                        .isNotNull(Device::getBindTime)
                        .ge(Device::getBindTime, start)
                        .lt(Device::getBindTime, end));
        Map<String, Long> deviceBucket = bucketByTime(devices.stream()
                .map(Device::getBindTime)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()), granularity);

        // 3. 生成时间标签序列（保证连续，无数据补 0）
        List<String> labels = buildTimeLabels(start, end, granularity);
        List<Long> activateCounts = labels.stream()
                .map(l -> cardBucket.getOrDefault(l, 0L))
                .collect(Collectors.toList());
        List<Long> newDeviceCounts = labels.stream()
                .map(l -> deviceBucket.getOrDefault(l, 0L))
                .collect(Collectors.toList());

        VerifyTrendDTO dto = new VerifyTrendDTO();
        dto.setLabels(labels);
        dto.setActivateCounts(activateCounts);
        dto.setNewDeviceCounts(newDeviceCounts);
        dto.setTotalActivate(activateCounts.stream().mapToLong(Long::longValue).sum());
        dto.setTotalNewDevice(newDeviceCounts.stream().mapToLong(Long::longValue).sum());
        return dto;
    }

    /* ============ 设备在线热力图 ============ */

    /**
     * 设备在线热力图（近 N 天 × 24 小时）
     *
     * 数据源：jicek_device.last_heartbeat（落在某天某小时窗口内视为该时段在线）
     */
    public DeviceHeatmapDTO deviceHeatmap(Long tenantId, Long softwareId, Integer days) {
        int actualDays = Math.min(
                Math.max(days == null || days <= 0 ? JicekConstants.STATS_HEATMAP_DAYS : days, 1),
                JicekConstants.STATS_HEATMAP_DAYS);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(actualDays - 1L);

        // 查询窗口内有心跳的设备
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Device> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId)
                        .isNotNull(Device::getLastHeartbeat)
                        .ge(Device::getLastHeartbeat, start)
                        .lt(Device::getLastHeartbeat, end));

        // 按 [day, hour] 聚合
        Map<String, Map<Integer, Long>> bucket = new HashMap<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Device d : devices) {
            LocalDateTime t = d.getLastHeartbeat();
            if (t == null) continue;
            String day = t.toLocalDate().format(dayFmt);
            int hour = t.getHour();
            bucket.computeIfAbsent(day, k -> new HashMap<>())
                    .merge(hour, 1L, Long::sum);
        }

        // 生成日期标签 + 热力点
        List<String> dayLabels = new ArrayList<>();
        List<List<Long>> points = new ArrayList<>();
        for (int i = 0; i < actualDays; i++) {
            LocalDate d = startDate.plusDays(i);
            String dayStr = d.format(dayFmt);
            dayLabels.add(dayStr);
            Map<Integer, Long> hourMap = bucket.getOrDefault(dayStr, Collections.emptyMap());
            for (int h = 0; h < JicekConstants.STATS_HOURS_PER_DAY; h++) {
                points.add(Arrays.asList((long) i, (long) h, hourMap.getOrDefault(h, 0L)));
            }
        }

        // 汇总
        Long currentOnline = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId)
                        .eq(Device::getOnlineStatus, JicekConstants.DEVICE_ONLINE));
        Long totalDevice = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId));

        DeviceHeatmapDTO dto = new DeviceHeatmapDTO();
        dto.setDays(dayLabels);
        List<Integer> hourLabels = new ArrayList<>();
        for (int h = 0; h < JicekConstants.STATS_HOURS_PER_DAY; h++) {
            hourLabels.add(h);
        }
        dto.setHours(hourLabels);
        dto.setPoints(points);
        dto.setCurrentOnline(currentOnline);
        dto.setTotalDevice(totalDevice);
        return dto;
    }

    /* ============ 收入统计 ============ */

    /**
     * 收入统计（按维度分项）
     *
     * @param dimension 维度：channel(支付通道) / cardType(卡类) / agent(代理)
     * @param days      天数范围
     */
    public IncomeStatsDTO income(Long tenantId, Long softwareId, String dimension, Integer days) {
        validateDimension(dimension);
        int actualDays = sanitizeDays(days);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(actualDays).truncatedTo(ChronoUnit.DAYS);

        // 查询已支付订单
        List<PayOrder> orders = payOrderMapper.selectList(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getTenantId, tenantId)
                        .eq(PayOrder::getStatus, JicekConstants.ORDER_STATUS_PAID)
                        .ge(PayOrder::getPayTime, start)
                        .lt(PayOrder::getPayTime, end));

        IncomeStatsDTO dto = new IncomeStatsDTO();
        dto.setDimension(dimension);

        BigDecimal totalAmount = orders.stream()
                .map(PayOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalAmount(totalAmount);
        dto.setTotalCount((long) orders.size());

        switch (dimension) {
            case JicekConstants.STATS_DIMENSION_CHANNEL:
                dto.setItems(groupByChannel(orders));
                break;
            case JicekConstants.STATS_DIMENSION_CARD_TYPE:
                dto.setItems(groupByCardType(orders, tenantId));
                break;
            case JicekConstants.STATS_DIMENSION_AGENT:
                dto.setItems(groupByAgent(orders, tenantId));
                break;
            default:
                break;
        }
        return dto;
    }

    /**
     * 按支付通道分组（alipay/wxpay/qqpay/unionpay）
     */
    private List<IncomeStatsDTO.IncomeItem> groupByChannel(List<PayOrder> orders) {
        Map<String, List<PayOrder>> grouped = orders.stream()
                .filter(o -> o.getPayType() != null)
                .collect(Collectors.groupingBy(PayOrder::getPayType));

        List<IncomeStatsDTO.IncomeItem> items = new ArrayList<>();
        for (Map.Entry<String, List<PayOrder>> e : grouped.entrySet()) {
            IncomeStatsDTO.IncomeItem item = new IncomeStatsDTO.IncomeItem();
            item.setKey(e.getKey());
            item.setName(channelName(e.getKey()));
            item.setAmount(e.getValue().stream()
                    .map(PayOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setCount((long) e.getValue().size());
            items.add(item);
        }
        // 按金额降序
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    /**
     * 按卡类分组
     */
    private List<IncomeStatsDTO.IncomeItem> groupByCardType(List<PayOrder> orders, Long tenantId) {
        // 预加载卡类名（避免 N+1）
        Map<Long, String> cardTypeNameMap = cardTypeMapper.selectList(
                new LambdaQueryWrapper<CardType>().eq(CardType::getTenantId, tenantId))
                .stream()
                .collect(Collectors.toMap(CardType::getId, CardType::getName, (a, b) -> a));

        Map<Long, List<PayOrder>> grouped = orders.stream()
                .filter(o -> o.getCardTypeId() != null)
                .collect(Collectors.groupingBy(PayOrder::getCardTypeId));

        List<IncomeStatsDTO.IncomeItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<PayOrder>> e : grouped.entrySet()) {
            IncomeStatsDTO.IncomeItem item = new IncomeStatsDTO.IncomeItem();
            item.setKey(String.valueOf(e.getKey()));
            item.setName(cardTypeNameMap.getOrDefault(e.getKey(), "卡类#" + e.getKey()));
            item.setAmount(e.getValue().stream()
                    .map(PayOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setCount((long) e.getValue().size());
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    /**
     * 按代理分组
     *
     * 数据源：PayOrder.agentId（v0.15.0 起已扩展，null 表示终端用户购买，过滤掉）
     * 代理名预加载避免 N+1（与 groupByCardType 风格一致）
     */
    private List<IncomeStatsDTO.IncomeItem> groupByAgent(List<PayOrder> orders, Long tenantId) {
        // 预加载代理名（避免 N+1）
        Map<Long, String> agentNameMap = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>().eq(Agent::getTenantId, tenantId))
                .stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getUsername, (a, b) -> a));

        Map<Long, List<PayOrder>> grouped = orders.stream()
                .filter(o -> o.getAgentId() != null)
                .collect(Collectors.groupingBy(PayOrder::getAgentId));

        List<IncomeStatsDTO.IncomeItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<PayOrder>> e : grouped.entrySet()) {
            IncomeStatsDTO.IncomeItem item = new IncomeStatsDTO.IncomeItem();
            item.setKey(String.valueOf(e.getKey()));
            item.setName(agentNameMap.getOrDefault(e.getKey(), "代理#" + e.getKey()));
            item.setAmount(e.getValue().stream()
                    .map(PayOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setCount((long) e.getValue().size());
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    /**
     * 支付通道 code → 中文名（铁律 04：禁字面量硬编码，走常量）
     */
    private String channelName(String code) {
        if (JicekConstants.CHANNEL_ALIPAY.equals(code)) return "支付宝";
        if (JicekConstants.CHANNEL_WXPAY.equals(code)) return "微信支付";
        if (JicekConstants.CHANNEL_QQPAY.equals(code)) return "QQ 钱包";
        if (JicekConstants.CHANNEL_UNIONPAY.equals(code)) return "银联";
        return code;
    }

    /* ============ 防破解事件统计 ============ */

    /**
     * 防破解事件统计（近 N 天）
     */
    public AntiCrackStatsDTO antiCrack(Long tenantId, Long softwareId, Integer days) {
        int actualDays = sanitizeDays(days);

        // 当前封禁设备数
        Long bannedDevice = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId)
                        .eq(Device::getStatus, JicekConstants.DEVICE_STATUS_BANNED));

        // 当前封禁卡密数
        Long bannedCard = cardKeyMapper.selectCount(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(softwareId != null, CardKey::getSoftwareId, softwareId)
                        .eq(CardKey::getStatus, JicekConstants.CARD_STATUS_BANNED));

        // 封禁设备去重 IP 数（近似封禁 IP 数）
        List<Device> bannedDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(softwareId != null, Device::getSoftwareId, softwareId)
                        .eq(Device::getStatus, JicekConstants.DEVICE_STATUS_BANNED));
        long bannedIp = bannedDevices.stream()
                .map(Device::getBindIp)
                .filter(Objects::nonNull)
                .filter(ip -> !ip.isEmpty())
                .distinct()
                .count();

        // 时间趋势（按天）
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(actualDays).truncatedTo(ChronoUnit.DAYS);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 封禁设备趋势：update_time 落在窗口内（封禁时 update）
        Map<String, Long> deviceTrend = bannedDevices.stream()
                .map(Device::getUpdateTime)
                .filter(t -> t != null && !t.isBefore(start) && t.isBefore(end))
                .collect(Collectors.groupingBy(
                        t -> t.format(dayFmt),
                        Collectors.counting()));

        // 封禁卡密趋势：update_time 落在窗口内
        List<CardKey> bannedCards = cardKeyMapper.selectList(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getTenantId, tenantId)
                        .eq(softwareId != null, CardKey::getSoftwareId, softwareId)
                        .eq(CardKey::getStatus, JicekConstants.CARD_STATUS_BANNED));
        Map<String, Long> cardTrend = bannedCards.stream()
                .map(CardKey::getUpdateTime)
                .filter(t -> t != null && !t.isBefore(start) && t.isBefore(end))
                .collect(Collectors.groupingBy(
                        t -> t.format(dayFmt),
                        Collectors.counting()));

        // 生成连续日期标签
        List<String> labels = new ArrayList<>();
        List<Long> deviceSeries = new ArrayList<>();
        List<Long> cardSeries = new ArrayList<>();
        for (int i = 0; i < actualDays; i++) {
            LocalDate d = start.toLocalDate().plusDays(i);
            String dayStr = d.format(dayFmt);
            labels.add(dayStr);
            deviceSeries.add(deviceTrend.getOrDefault(dayStr, 0L));
            cardSeries.add(cardTrend.getOrDefault(dayStr, 0L));
        }

        AntiCrackStatsDTO dto = new AntiCrackStatsDTO();
        dto.setBannedDeviceCount(bannedDevice);
        dto.setBannedCardCount(bannedCard);
        dto.setBannedIpCount(bannedIp);
        dto.setLabels(labels);
        dto.setBannedDeviceTrend(deviceSeries);
        dto.setBannedCardTrend(cardSeries);
        return dto;
    }

    /* ============ 工具方法 ============ */

    /**
     * 校验统计粒度
     */
    private void validateGranularity(String granularity) {
        if (granularity == null
                || (!JicekConstants.STATS_GRANULARITY_HOUR.equals(granularity)
                && !JicekConstants.STATS_GRANULARITY_DAY.equals(granularity)
                && !JicekConstants.STATS_GRANULARITY_MONTH.equals(granularity))) {
            throw new ServiceException(ResultCode.STATS_GRANULARITY_INVALID);
        }
    }

    /**
     * 校验收入统计维度
     */
    private void validateDimension(String dimension) {
        if (dimension == null
                || (!JicekConstants.STATS_DIMENSION_CHANNEL.equals(dimension)
                && !JicekConstants.STATS_DIMENSION_CARD_TYPE.equals(dimension)
                && !JicekConstants.STATS_DIMENSION_AGENT.equals(dimension))) {
            throw new ServiceException(ResultCode.STATS_DIMENSION_INVALID);
        }
    }

    /**
     * 规范化天数参数（null/<=0 用默认值，超过最大值用最大值）
     */
    private int sanitizeDays(Integer days) {
        if (days == null || days <= 0) {
            return JicekConstants.STATS_DEFAULT_RANGE_DAYS;
        }
        if (days > JicekConstants.STATS_MAX_RANGE_DAYS) {
            throw new ServiceException(ResultCode.STATS_RANGE_EXCEED);
        }
        return days;
    }

    /**
     * 按粒度分桶
     */
    private Map<String, Long> bucketByTime(List<LocalDateTime> times, String granularity) {
        DateTimeFormatter fmt;
        switch (granularity) {
            case JicekConstants.STATS_GRANULARITY_HOUR:
                fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
                break;
            case JicekConstants.STATS_GRANULARITY_DAY:
                fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                break;
            case JicekConstants.STATS_GRANULARITY_MONTH:
                fmt = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            default:
                fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
        return times.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t.format(fmt), Collectors.counting()));
    }

    /**
     * 生成连续时间标签序列
     */
    private List<String> buildTimeLabels(LocalDateTime start, LocalDateTime end, String granularity) {
        List<String> labels = new ArrayList<>();
        switch (granularity) {
            case JicekConstants.STATS_GRANULARITY_HOUR:
                // 按小时遍历（包含 start 所在小时，到 end 所在小时）
                DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
                LocalDateTime cursor = start.truncatedTo(ChronoUnit.HOURS);
                while (cursor.isBefore(end)) {
                    labels.add(cursor.format(hourFmt));
                    cursor = cursor.plusHours(1);
                }
                break;
            case JicekConstants.STATS_GRANULARITY_DAY:
                DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate dayCursor = start.toLocalDate();
                LocalDate dayEnd = end.toLocalDate();
                while (!dayCursor.isAfter(dayEnd)) {
                    labels.add(dayCursor.format(dayFmt));
                    dayCursor = dayCursor.plusDays(1);
                }
                break;
            case JicekConstants.STATS_GRANULARITY_MONTH:
                DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth monthCursor = YearMonth.from(start);
                YearMonth monthEnd = YearMonth.from(end);
                while (!monthCursor.isAfter(monthEnd)) {
                    labels.add(monthCursor.format(monthFmt));
                    monthCursor = monthCursor.plusMonths(1);
                }
                break;
            default:
                break;
        }
        return labels;
    }
}
