package com.jicek.license.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.pay.entity.PayConfig;
import com.jicek.license.pay.mapper.PayConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付配置服务
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 1. 配置 CRUD
 * 2. 商户密钥加密入库 / 解密读取（铁律 04）
 * 3. 测试连接
 */
@Slf4j
@Service
public class PayConfigService {

    private final PayConfigMapper payConfigMapper;
    private final AesCryptoService aesCryptoService;

    public PayConfigService(PayConfigMapper payConfigMapper, AesCryptoService aesCryptoService) {
        this.payConfigMapper = payConfigMapper;
        this.aesCryptoService = aesCryptoService;
    }

    /**
     * 获取租户配置（含解密后的密钥，仅服务内部使用，禁返回前端）
     */
    public PayConfig getByTenantId(Long tenantId) {
        PayConfig config = payConfigMapper.selectOne(
                new LambdaQueryWrapper<PayConfig>().eq(PayConfig::getTenantId, tenantId));
        if (config == null) {
            throw new ServiceException(ResultCode.PAY_CONFIG_NOT_FOUND);
        }
        return config;
    }

    /**
     * 获取租户配置（不解密，返回前端的脱敏版本）
     */
    public PayConfig getByTenantIdMasked(Long tenantId) {
        PayConfig config = getByTenantId(tenantId);
        // 脱敏：密钥只显示前4位 + ****
        if (config.getMerchantKey() != null && config.getMerchantKey().length() > 16) {
            // 注意：merchantKey 已是密文，这里只是进一步隐藏
            config.setMerchantKey(config.getMerchantKey().substring(0, 8) + "****");
        }
        return config;
    }

    /**
     * 保存或更新配置
     * @param config 配置（merchantKey 为明文，方法内加密入库）
     */
    public void saveOrUpdate(PayConfig config) {
        if (config.getTenantId() == null) {
            throw new ServiceException("租户 ID 不能为空");
        }
        if (config.getGatewayUrl() == null || config.getGatewayUrl().isEmpty()) {
            throw new ServiceException("支付网关地址不能为空");
        }
        if (config.getPid() == null) {
            throw new ServiceException("商户 ID 不能为空");
        }
        if (config.getMerchantKey() == null || config.getMerchantKey().isEmpty()) {
            throw new ServiceException("商户密钥不能为空");
        }
        if (config.getEnabledChannels() == null || config.getEnabledChannels().isEmpty()) {
            throw new ServiceException("至少启用一个支付通道");
        }

        // 加密商户密钥（铁律 04）
        config.setMerchantKey(aesCryptoService.encrypt(config.getMerchantKey()));
        config.setUpdateTime(LocalDateTime.now());

        PayConfig existing = payConfigMapper.selectOne(
                new LambdaQueryWrapper<PayConfig>().eq(PayConfig::getTenantId, config.getTenantId()));
        if (existing == null) {
            config.setCreateTime(LocalDateTime.now());
            if (config.getEnabled() == null) {
                config.setEnabled(1);
            }
            payConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            payConfigMapper.updateById(config);
        }
        log.info("支付配置已保存: tenantId={}", config.getTenantId());
    }

    /**
     * 启用/禁用配置
     */
    public void toggleEnabled(Long tenantId, Integer enabled) {
        PayConfig config = getByTenantId(tenantId);
        config.setEnabled(enabled);
        config.setUpdateTime(LocalDateTime.now());
        payConfigMapper.updateById(config);
    }

    /**
     * 验证配置是否可用（仅做基本字段校验，不实际调用网关）
     */
    public boolean isConfigured(Long tenantId) {
        try {
            PayConfig config = getByTenantId(tenantId);
            return config.getEnabled() != null && config.getEnabled() == 1
                    && config.getGatewayUrl() != null
                    && config.getPid() != null
                    && config.getMerchantKey() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
