package com.jicek.license.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.device.dto.DeviceBindRequestDTO;
import com.jicek.license.device.dto.DeviceFingerprintDTO;
import com.jicek.license.device.dto.DeviceUnbindRequestDTO;
import com.jicek.license.device.entity.Device;
import com.jicek.license.device.fingerprint.DeviceFingerprintService;
import com.jicek.license.device.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备业务服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 1. 设备绑定（卡密 + 设备指纹 + 用户）
 * 2. 设备解绑（换机码校验 + 新设备绑定，同事务）
 * 3. 设备查询（开发者分页 / 按指纹精确查）
 * 4. 设备封禁/解封
 * 5. 换机码生成（SecureRandom，16 位）
 *
 * 安全铁律：
 * - 绑定/解绑必须在同一事务内（铁律 06，禁伪异步）
 * - 换机码使用 SecureRandom 生成，禁 Math.random
 * - 设备封禁后所有相关 session 立即失效（由心跳服务负责）
 */
@Slf4j
@Service
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceFingerprintService fingerprintService;
    private final AesCryptoService aesCryptoService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceService(DeviceMapper deviceMapper,
                         DeviceFingerprintService fingerprintService,
                         AesCryptoService aesCryptoService) {
        this.deviceMapper = deviceMapper;
        this.fingerprintService = fingerprintService;
        this.aesCryptoService = aesCryptoService;
    }

    /**
     * 设备绑定
     *
     * 流程：
     * 1. 校验卡密有效性（解密 RSA 卡密 → 哈希查询 → 状态校验）
     * 2. 校验设备指纹（RSA 解密 → 5 维融合 → 常量时间比对）
     * 3. 校验设备是否被封禁
     * 4. 校验设备是否已被其他用户绑定
     * 5. 校验该用户已绑定设备数是否超限（cardType.maxDevices）
     * 6. 写入设备记录 + 生成换机码
     * 7. 卡密标记为已使用 + 绑定用户
     *
     * @return 换机码（16 位）
     */
    @Transactional(rollbackFor = Exception.class)
    public String bindDevice(DeviceBindRequestDTO req, Long userId) {
        // 1. 校验设备指纹（独立计算，防篡改）
        Map<String, String> dimensions = fingerprintService.verifyAndParse(req.getFingerprint());
        String fingerprint = fingerprintService.computeFingerprint(
                dimensions, req.getFingerprint().getIsVm(), req.getFingerprint().getVmExtra());

        // 2. 查询设备是否已存在
        Device existing = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, req.getTenantId())
                        .eq(Device::getSoftwareId, req.getSoftwareId())
                        .eq(Device::getDeviceFingerprint, fingerprint));

        if (existing != null) {
            // 已存在设备
            if (existing.getStatus() != null && existing.getStatus() == JicekConstants.DEVICE_STATUS_BANNED) {
                throw new ServiceException(ResultCode.DEVICE_BANNED, "设备已封禁，请联系管理员");
            }
            if (existing.getUserId() != null && !existing.getUserId().equals(userId)) {
                throw new ServiceException(ResultCode.DEVICE_ALREADY_BOUND,
                        "设备已被其他用户绑定，请使用换机码");
            }
            // 同一用户重复绑定，返回原换机码
            return existing.getBindCode();
        }

        // 3. 校验卡密（此处简化，实际应调用 CardKeyService.useCard）
        // 卡密校验通过后，查询卡类获取 maxDevices
        // TODO: 接入 CardKeyService.useCard 完整流程（v0.3.1），由 CardKeyService 返回 CardType
        int maxDevices = 1;

        // 4. 校验该用户已绑定设备数
        Long boundCount = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, req.getTenantId())
                        .eq(Device::getSoftwareId, req.getSoftwareId())
                        .eq(Device::getUserId, userId)
                        .eq(Device::getStatus, JicekConstants.DEVICE_STATUS_NORMAL));
        if (boundCount >= maxDevices) {
            throw new ServiceException(ResultCode.DEVICE_LIMIT,
                    "已绑定设备数超限（上限 " + maxDevices + " 台），请先解绑");
        }

        // 5. 写入设备记录
        LocalDateTime now = LocalDateTime.now();
        String bindCode = generateBindCode();
        String detailJson = fingerprintService.buildDetailJson(dimensions, req.getFingerprint());
        String encryptedDetail = aesCryptoService.encrypt(detailJson);

        Device device = new Device();
        device.setTenantId(req.getTenantId());
        device.setSoftwareId(req.getSoftwareId());
        device.setUserId(userId);
        device.setDeviceFingerprint(fingerprint);
        device.setDeviceInfo(encryptedDetail);
        device.setDeviceName(req.getFingerprint().getDeviceName());
        device.setOsType(req.getFingerprint().getOsType());
        device.setOsVersion(req.getFingerprint().getOsVersion());
        device.setClientVersion(req.getFingerprint().getClientVersion());
        device.setIsVm(req.getFingerprint().getIsVm() == null ? 0 : req.getFingerprint().getIsVm());
        device.setVmExtra(req.getFingerprint().getVmExtra());
        device.setBindIp(req.getClientIp());
        device.setBindCode(bindCode);
        device.setOnlineStatus(JicekConstants.DEVICE_OFFLINE);
        device.setStatus(JicekConstants.DEVICE_STATUS_NORMAL);
        device.setBindTime(now);
        device.setCreateTime(now);
        device.setUpdateTime(now);
        deviceMapper.insert(device);

        log.info("设备绑定成功: tenantId={}, softwareId={}, userId={}, fingerprint={}, bindCode={}",
                req.getTenantId(), req.getSoftwareId(), userId, fingerprint, bindCode);

        return bindCode;
    }

    /**
     * 设备解绑（换机场景）
     *
     * 流程：
     * 1. 根据换机码查询旧设备
     * 2. 校验换机码有效性 + 是否过期（24h）
     * 3. 校验新设备指纹
     * 4. 旧设备 userId 置空 + bindCode 失效
     * 5. 新设备绑定到原用户（同事务）
     *
     * @return 新换机码
     */
    @Transactional(rollbackFor = Exception.class)
    public String unbindAndRebind(DeviceUnbindRequestDTO req) {
        // 1. 查询旧设备
        Device oldDevice = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, req.getTenantId())
                        .eq(Device::getSoftwareId, req.getSoftwareId())
                        .eq(Device::getBindCode, req.getBindCode()));
        if (oldDevice == null) {
            throw new ServiceException(ResultCode.BIND_CODE_INVALID, "换机码无效");
        }

        // 2. 校验换机码时效（24h）
        if (oldDevice.getBindTime() != null
                && oldDevice.getBindTime().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.BIND_CODE_INVALID, "换机码已过期（24h）");
        }

        // 3. 校验新设备指纹
        Map<String, String> dimensions = fingerprintService.verifyAndParse(req.getNewFingerprint());
        String newFingerprint = fingerprintService.computeFingerprint(
                dimensions, req.getNewFingerprint().getIsVm(), req.getNewFingerprint().getVmExtra());

        // 4. 校验新设备未被其他用户绑定
        Device newDevice = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, req.getTenantId())
                        .eq(Device::getSoftwareId, req.getSoftwareId())
                        .eq(Device::getDeviceFingerprint, newFingerprint));
        if (newDevice != null && newDevice.getUserId() != null) {
            throw new ServiceException(ResultCode.DEVICE_ALREADY_BOUND, "新设备已被其他用户绑定");
        }
        if (newDevice != null && newDevice.getStatus() != null
                && newDevice.getStatus() == JicekConstants.DEVICE_STATUS_BANNED) {
            throw new ServiceException(ResultCode.DEVICE_BANNED, "新设备已封禁");
        }

        Long userId = oldDevice.getUserId();
        LocalDateTime now = LocalDateTime.now();

        // 5. 旧设备解绑（userId 置空 + bindCode 失效 + 离线）
        oldDevice.setUserId(null);
        oldDevice.setBindCode(oldDevice.getBindCode() + "_USED_" + now);
        oldDevice.setOnlineStatus(JicekConstants.DEVICE_OFFLINE);
        oldDevice.setUpdateTime(now);
        deviceMapper.updateById(oldDevice);

        // 6. 新设备绑定
        String newBindCode = generateBindCode();
        String detailJson = fingerprintService.buildDetailJson(dimensions, req.getNewFingerprint());
        String encryptedDetail = aesCryptoService.encrypt(detailJson);

        if (newDevice == null) {
            newDevice = new Device();
            newDevice.setTenantId(req.getTenantId());
            newDevice.setSoftwareId(req.getSoftwareId());
            newDevice.setDeviceFingerprint(newFingerprint);
            newDevice.setOnlineStatus(JicekConstants.DEVICE_OFFLINE);
            newDevice.setStatus(JicekConstants.DEVICE_STATUS_NORMAL);
            newDevice.setCreateTime(now);
        }
        newDevice.setUserId(userId);
        newDevice.setDeviceInfo(encryptedDetail);
        newDevice.setDeviceName(req.getNewFingerprint().getDeviceName());
        newDevice.setOsType(req.getNewFingerprint().getOsType());
        newDevice.setOsVersion(req.getNewFingerprint().getOsVersion());
        newDevice.setClientVersion(req.getNewFingerprint().getClientVersion());
        newDevice.setIsVm(req.getNewFingerprint().getIsVm() == null ? 0 : req.getNewFingerprint().getIsVm());
        newDevice.setVmExtra(req.getNewFingerprint().getVmExtra());
        newDevice.setBindIp(req.getClientIp());
        newDevice.setBindCode(newBindCode);
        newDevice.setBindTime(now);
        newDevice.setUpdateTime(now);

        if (newDevice.getId() == null) {
            deviceMapper.insert(newDevice);
        } else {
            deviceMapper.updateById(newDevice);
        }

        log.info("设备换机成功: tenantId={}, softwareId={}, userId={}, oldFp={}, newFp={}, newBindCode={}",
                req.getTenantId(), req.getSoftwareId(), userId,
                oldDevice.getDeviceFingerprint(), newFingerprint, newBindCode);

        return newBindCode;
    }

    /**
     * 设备封禁
     */
    @Transactional(rollbackFor = Exception.class)
    public void banDevice(Long tenantId, Long deviceId) {
        Device device = getDeviceById(tenantId, deviceId);
        device.setStatus(JicekConstants.DEVICE_STATUS_BANNED);
        device.setOnlineStatus(JicekConstants.DEVICE_OFFLINE);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("设备封禁: tenantId={}, deviceId={}, fingerprint={}",
                tenantId, deviceId, device.getDeviceFingerprint());
    }

    /**
     * 设备解封
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbanDevice(Long tenantId, Long deviceId) {
        Device device = getDeviceById(tenantId, deviceId);
        device.setStatus(JicekConstants.DEVICE_STATUS_NORMAL);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("设备解封: tenantId={}, deviceId={}", tenantId, deviceId);
    }

    /**
     * 分页查询设备
     */
    public Page<Device> pageDevices(Long tenantId, Long softwareId, Integer status,
                                    Integer onlineStatus, int page, int size) {
        Page<Device> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getTenantId, tenantId)
                .eq(softwareId != null, Device::getSoftwareId, softwareId)
                .eq(status != null, Device::getStatus, status)
                .eq(onlineStatus != null, Device::getOnlineStatus, onlineStatus)
                .orderByDesc(Device::getCreateTime);
        return deviceMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 按指纹精确查询
     */
    public Device getByFingerprint(Long tenantId, Long softwareId, String fingerprint) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getTenantId, tenantId)
                        .eq(Device::getSoftwareId, softwareId)
                        .eq(Device::getDeviceFingerprint, fingerprint));
    }

    /**
     * 按 ID 查询（含租户校验）
     */
    public Device getDeviceById(Long tenantId, Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null || !device.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        return device;
    }

    /**
     * 生成换机码（16 位大写字母+数字）
     * 使用 SecureRandom，禁用 Math.random（铁律 04）
     */
    private String generateBindCode() {
        String charset = JicekConstants.CHARSET_UPPER_ALNUM;
        StringBuilder sb = new StringBuilder(JicekConstants.BIND_CODE_LENGTH);
        for (int i = 0; i < JicekConstants.BIND_CODE_LENGTH; i++) {
            sb.append(charset.charAt(secureRandom.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
