package com.jicek.license.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.HmacSignService;
import com.jicek.license.device.dto.DeviceHeartbeatDTO;
import com.jicek.license.device.entity.Device;
import com.jicek.license.device.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备心跳服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 1. 校验心跳签名（HMAC-SHA256，防伪造）
 * 2. nonce 防重放（Redis 缓存 5 分钟）
 * 3. 时间戳容差校验（±300s）
 * 4. 更新设备在线状态 + 最后心跳时间
 * 5. 下发心跳间隔（由软件配置控制，5-300s）
 * 6. 超时设备置为离线（定时任务扫描）
 *
 * 安全铁律：
 * - 心跳间隔由服务端控制，禁客户端固定值（铁律 06）
 * - 签名比对使用 MessageDigest.isEqual 常量时间比较
 * - nonce 必须存 Redis，禁内存缓存（多实例场景）
 */
@Slf4j
@Service
public class DeviceHeartbeatService {

    /** 时间戳容差（秒） */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    /** nonce 缓存时长（分钟） */
    private static final long NONCE_CACHE_MINUTES = 5;

    private final DeviceMapper deviceMapper;
    private final HmacSignService hmacSignService;
    private final RedissonClient redissonClient;

    public DeviceHeartbeatService(DeviceMapper deviceMapper,
                                  HmacSignService hmacSignService,
                                  RedissonClient redissonClient) {
        this.deviceMapper = deviceMapper;
        this.hmacSignService = hmacSignService;
        this.redissonClient = redissonClient;
    }

    /**
     * 处理心跳
     *
     * @param dto          心跳数据
     * @param signSecret   软件签名密钥（从 software.sign_secret 解密后传入）
     * @param heartbeatInterval 软件配置的心跳间隔（秒）
     * @return 下一次心跳间隔（秒） + 服务器时间戳
     */
    public Map<String, Object> heartbeat(DeviceHeartbeatDTO dto,
                                          String signSecret,
                                          int heartbeatInterval) {
        // 1. 基础校验
        if (dto.getFingerprint() == null || dto.getTimestamp() == null
                || dto.getSign() == null || dto.getNonce() == null) {
            throw new ServiceException(ResultCode.PARAM_ERROR, "心跳数据不完整");
        }

        // 2. 时间戳容差校验（±300s，防重放）
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - dto.getTimestamp());
        if (diff > TIMESTAMP_TOLERANCE_SECONDS * 1000) {
            throw new ServiceException(ResultCode.HEARTBEAT_TIMEOUT,
                    "时间戳超出容差范围（±300s）");
        }

        // 3. nonce 防重放（Redis 检查 + 写入）
        String nonceKey = JicekConstants.REDIS_KEY_NONCE + dto.getNonce();
        RBucket<String> nonceBucket = redissonClient.getBucket(nonceKey);
        if (nonceBucket.isExists()) {
            throw new ServiceException(ResultCode.HEARTBEAT_NONCE_REPLAY,
                    "nonce 已存在，疑似重放攻击");
        }
        nonceBucket.set("1", Duration.ofMinutes(NONCE_CACHE_MINUTES));

        // 4. 签名校验（HMAC-SHA256）
        // 待签名串：fingerprint + timestamp + nonce
        String signPayload = dto.getFingerprint() + dto.getTimestamp() + dto.getNonce();
        if (!hmacSignService.verify(signPayload, dto.getSign(), signSecret)) {
            throw new ServiceException(ResultCode.HEARTBEAT_SIGN_FAIL, "心跳签名校验失败");
        }

        // 5. 查询设备
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, dto.getTenantId())
                        .eq(Device::getSoftwareId, dto.getSoftwareId())
                        .eq(Device::getDeviceFingerprint, dto.getFingerprint()));
        if (device == null) {
            throw new ServiceException(ResultCode.DEVICE_NOT_FOUND, "设备未注册");
        }
        if (device.getStatus() != null
                && device.getStatus() == JicekConstants.DEVICE_STATUS_BANNED) {
            throw new ServiceException(ResultCode.DEVICE_BANNED, "设备已封禁");
        }

        // 6. 更新心跳
        LocalDateTime nowTime = LocalDateTime.now();
        device.setLastHeartbeat(nowTime);
        device.setOnlineStatus(JicekConstants.DEVICE_ONLINE);
        device.setUpdateTime(nowTime);
        deviceMapper.updateById(device);

        // 7. 下发下次心跳间隔（服务端控制，5-300s）
        int nextInterval = clampInterval(heartbeatInterval);

        Map<String, Object> result = new HashMap<>();
        result.put("nextInterval", nextInterval);
        result.put("serverTime", now);
        return result;
    }

    /**
     * 扫描超时设备，置为离线
     *
     * @param tenantId       租户 ID
     * @param softwareId     软件 ID
     * @param heartbeatInterval 心跳间隔（秒）
     * @return 置离线设备数
     */
    public int markTimeoutDevicesOffline(Long tenantId, Long softwareId, int heartbeatInterval) {
        int interval = clampInterval(heartbeatInterval);
        // 超时阈值 = 3 * interval
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3L * interval);

        // 查询在线但心跳超时的设备
        var timeoutDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(Device::getSoftwareId, softwareId)
                        .eq(Device::getOnlineStatus, JicekConstants.DEVICE_ONLINE)
                        .lt(Device::getLastHeartbeat, threshold));

        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Device device : timeoutDevices) {
            device.setOnlineStatus(JicekConstants.DEVICE_OFFLINE);
            device.setUpdateTime(now);
            deviceMapper.updateById(device);
            count++;
        }
        if (count > 0) {
            log.info("超时设备置离线: tenantId={}, softwareId={}, count={}",
                    tenantId, softwareId, count);
        }
        return count;
    }

    /**
     * 心跳间隔范围限制（5-300s）
     */
    private int clampInterval(int interval) {
        if (interval < JicekConstants.HEARTBEAT_MIN_INTERVAL) {
            return JicekConstants.HEARTBEAT_MIN_INTERVAL;
        }
        if (interval > JicekConstants.HEARTBEAT_MAX_INTERVAL) {
            return JicekConstants.HEARTBEAT_MAX_INTERVAL;
        }
        return interval;
    }
}
