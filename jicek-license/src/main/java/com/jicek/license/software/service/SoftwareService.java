package com.jicek.license.software.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.card.entity.CardType;
import com.jicek.license.card.mapper.CardTypeMapper;
import com.jicek.license.cloudfunc.entity.CloudFunction;
import com.jicek.license.cloudfunc.mapper.CloudFunctionMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.device.entity.Device;
import com.jicek.license.device.mapper.DeviceMapper;
import com.jicek.license.software.dto.SoftwareCreateResultDTO;
import com.jicek.license.software.dto.SoftwareDetailDTO;
import com.jicek.license.software.dto.SoftwareSaveDTO;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 软件服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 软件 CRUD（tenantId 从 AuthContext 获取，禁前端裸传，防越权）
 *  - appKey / signSecret / RSA 密钥对自动生成
 *  - signSecret / RSA 密钥轮换（明文仅返回一次）
 *  - 删除前关联校验（卡类 / 设备 / 云函数）
 *
 * 安全铁律：
 *  - signSecret / rsaPrivateKey 入库前必须 AES-256-GCM 加密
 *  - 查询接口返回脱敏 signSecret，永不返回 rsaPrivateKey
 *  - 所有操作必须校验 software.tenantId == AuthContext.currentTenantId()
 *  - appKey 全局唯一，生成后查重防冲突（虽概率极低）
 */
@Service
public class SoftwareService {

    private static final Logger log = LoggerFactory.getLogger(SoftwareService.class);

    private final SoftwareMapper softwareMapper;
    private final CardTypeMapper cardTypeMapper;
    private final DeviceMapper deviceMapper;
    private final CloudFunctionMapper cloudFunctionMapper;
    private final AesCryptoService aesCryptoService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SoftwareService(SoftwareMapper softwareMapper,
                           CardTypeMapper cardTypeMapper,
                           DeviceMapper deviceMapper,
                           CloudFunctionMapper cloudFunctionMapper,
                           AesCryptoService aesCryptoService) {
        this.softwareMapper = softwareMapper;
        this.cardTypeMapper = cardTypeMapper;
        this.deviceMapper = deviceMapper;
        this.cloudFunctionMapper = cloudFunctionMapper;
        this.aesCryptoService = aesCryptoService;
    }

    /* ============ 创建 ============ */

    /**
     * 创建软件：自动生成 appKey + signSecret + RSA 密钥对
     * @return 含明文密钥的创建结果（仅此一次返回）
     */
    public SoftwareCreateResultDTO create(SoftwareSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();

        // 校验同租户下名称唯一
        Long nameConflict = softwareMapper.selectCount(new LambdaQueryWrapper<Software>()
                .eq(Software::getTenantId, tenantId)
                .eq(Software::getName, dto.getName()));
        if (nameConflict != null && nameConflict > 0) {
            throw new ServiceException(ResultCode.SOFTWARE_NAME_EXISTS);
        }

        // 生成密钥材料
        String appKey = generateUniqueAppKey();
        String signSecretPlain = generateSignSecret();
        RsaKeyPair rsaPair = generateRsaKeyPair();

        LocalDateTime now = LocalDateTime.now();
        Software software = new Software();
        software.setTenantId(tenantId);
        software.setName(dto.getName());
        software.setAppKey(appKey);
        software.setSignSecret(aesCryptoService.encrypt(signSecretPlain));
        software.setRsaPublicKey(rsaPair.publicKeyBase64);
        software.setRsaPrivateKey(aesCryptoService.encrypt(rsaPair.privateKeyBase64));
        software.setVersion(dto.getVersion());
        software.setMinVersion(dto.getMinVersion());
        software.setHeartbeatInterval(dto.getHeartbeatInterval() != null ? dto.getHeartbeatInterval() : JicekConstants.HEARTBEAT_DEFAULT_INTERVAL);
        software.setMaxConcurrent(dto.getMaxConcurrent() != null ? dto.getMaxConcurrent() : 1);
        software.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : JicekConstants.SOFTWARE_ENABLED);
        software.setCreateTime(now);
        software.setUpdateTime(now);
        softwareMapper.insert(software);

        log.info("【软件】创建成功 id={} tenantId={} name={} appKey={}",
                software.getId(), tenantId, software.getName(), appKey);

        return buildCreateResult(software, signSecretPlain, rsaPair.privateKeyBase64);
    }

    /* ============ 更新（仅非敏感字段） ============ */

    public void update(SoftwareSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.SOFTWARE_PARAM_INVALID, "更新时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        Software existing = requireOwnedSoftware(dto.getId(), tenantId);

        // 名称若变更，校验同租户下唯一
        if (!existing.getName().equals(dto.getName())) {
            Long nameConflict = softwareMapper.selectCount(new LambdaQueryWrapper<Software>()
                    .eq(Software::getTenantId, tenantId)
                    .eq(Software::getName, dto.getName())
                    .ne(Software::getId, dto.getId()));
            if (nameConflict != null && nameConflict > 0) {
                throw new ServiceException(ResultCode.SOFTWARE_NAME_EXISTS);
            }
        }

        existing.setName(dto.getName());
        existing.setVersion(dto.getVersion());
        existing.setMinVersion(dto.getMinVersion());
        existing.setHeartbeatInterval(dto.getHeartbeatInterval() != null ? dto.getHeartbeatInterval() : existing.getHeartbeatInterval());
        existing.setMaxConcurrent(dto.getMaxConcurrent() != null ? dto.getMaxConcurrent() : existing.getMaxConcurrent());
        existing.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : existing.getEnabled());
        existing.setUpdateTime(LocalDateTime.now());
        // appKey / signSecret / RSA 不可通过此接口修改
        softwareMapper.updateById(existing);

        log.info("【软件】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    /* ============ 分页查询 ============ */

    public Page<SoftwareDetailDTO> page(long current, long size, String name, Integer enabled) {
        Long tenantId = requireCurrentTenantId();
        LambdaQueryWrapper<Software> wrapper = new LambdaQueryWrapper<Software>()
                .eq(Software::getTenantId, tenantId)
                .like(name != null && !name.isBlank(), Software::getName, name)
                .eq(enabled != null, Software::getEnabled, enabled)
                .orderByDesc(Software::getCreateTime);
        Page<Software> page = softwareMapper.selectPage(new Page<>(current, size), wrapper);

        Page<SoftwareDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDetailDTO).toList());
        return result;
    }

    /* ============ 详情（脱敏） ============ */

    public SoftwareDetailDTO get(Long id) {
        Long tenantId = requireCurrentTenantId();
        Software software = requireOwnedSoftware(id, tenantId);
        return toDetailDTO(software);
    }

    /* ============ 删除（关联校验） ============ */

    public void delete(Long id) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedSoftware(id, tenantId);

        // 关联卡类校验
        Long cardTypeCount = cardTypeMapper.selectCount(new LambdaQueryWrapper<CardType>()
                .eq(CardType::getSoftwareId, id));
        if (cardTypeCount != null && cardTypeCount > 0) {
            throw new ServiceException(ResultCode.SOFTWARE_HAS_CARD_TYPE);
        }

        // 关联设备校验
        Long deviceCount = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getSoftwareId, id));
        if (deviceCount != null && deviceCount > 0) {
            throw new ServiceException(ResultCode.SOFTWARE_HAS_DEVICE);
        }

        // 关联云函数校验
        Long cfCount = cloudFunctionMapper.selectCount(new LambdaQueryWrapper<CloudFunction>()
                .eq(CloudFunction::getSoftwareId, id));
        if (cfCount != null && cfCount > 0) {
            throw new ServiceException(ResultCode.SOFTWARE_HAS_CLOUD_FUNC);
        }

        softwareMapper.deleteById(id);
        log.info("【软件】删除成功 id={} tenantId={}", id, tenantId);
    }

    /* ============ 轮换签名密钥 ============ */

    /**
     * 轮换 signSecret，返回新明文（仅此一次）
     */
    public SoftwareCreateResultDTO regenerateSignSecret(Long id) {
        Long tenantId = requireCurrentTenantId();
        Software software = requireOwnedSoftware(id, tenantId);

        String newSecret = generateSignSecret();
        software.setSignSecret(aesCryptoService.encrypt(newSecret));
        software.setUpdateTime(LocalDateTime.now());
        softwareMapper.updateById(software);

        log.info("【软件】轮换签名密钥 id={} tenantId={}", id, tenantId);
        // 返回时需解密 RSA 私钥明文（便于客户端一并备份）
        return buildCreateResult(software, newSecret, aesCryptoService.decrypt(software.getRsaPrivateKey()));
    }

    /* ============ 轮换 RSA 密钥对 ============ */

    /**
     * 轮换 RSA 密钥对，返回新公钥 + 私钥明文（仅此一次）
     */
    public SoftwareCreateResultDTO regenerateRsaKey(Long id) {
        Long tenantId = requireCurrentTenantId();
        Software software = requireOwnedSoftware(id, tenantId);

        RsaKeyPair newPair = generateRsaKeyPair();
        software.setRsaPublicKey(newPair.publicKeyBase64);
        software.setRsaPrivateKey(aesCryptoService.encrypt(newPair.privateKeyBase64));
        software.setUpdateTime(LocalDateTime.now());
        softwareMapper.updateById(software);

        log.info("【软件】轮换 RSA 密钥对 id={} tenantId={}", id, tenantId);
        // 返回时需解密 signSecret 明文（便于客户端一并备份）
        return buildCreateResult(software, aesCryptoService.decrypt(software.getSignSecret()), newPair.privateKeyBase64);
    }

    /* ============ 内部工具 ============ */

    private Long requireCurrentTenantId() {
        Long tenantId = AuthContext.currentTenantId();
        if (tenantId == null) {
            throw new ServiceException(ResultCode.AUTH_NO_PERMISSION, "当前用户无租户身份");
        }
        return tenantId;
    }

    /**
     * 校验软件存在且属于当前租户
     */
    private Software requireOwnedSoftware(Long id, Long tenantId) {
        Software software = softwareMapper.selectById(id);
        if (software == null) {
            throw new ServiceException(ResultCode.SOFTWARE_NOT_FOUND);
        }
        if (!software.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.SOFTWARE_PERMISSION_DENIED);
        }
        return software;
    }

    private SoftwareDetailDTO toDetailDTO(Software software) {
        SoftwareDetailDTO dto = new SoftwareDetailDTO();
        dto.setId(software.getId());
        dto.setTenantId(software.getTenantId());
        dto.setName(software.getName());
        dto.setAppKey(software.getAppKey());
        dto.setSignSecretMasked(maskSecret(aesCryptoService.decrypt(software.getSignSecret())));
        dto.setRsaPublicKey(software.getRsaPublicKey());
        dto.setVersion(software.getVersion());
        dto.setMinVersion(software.getMinVersion());
        dto.setHeartbeatInterval(software.getHeartbeatInterval());
        dto.setMaxConcurrent(software.getMaxConcurrent());
        dto.setEnabled(software.getEnabled());
        dto.setCreateTime(software.getCreateTime());
        dto.setUpdateTime(software.getUpdateTime());
        return dto;
    }

    private SoftwareCreateResultDTO buildCreateResult(Software software, String signSecretPlain, String rsaPrivateKeyPlain) {
        SoftwareCreateResultDTO result = new SoftwareCreateResultDTO();
        result.setId(software.getId());
        result.setTenantId(software.getTenantId());
        result.setName(software.getName());
        result.setAppKey(software.getAppKey());
        result.setSignSecret(signSecretPlain);
        result.setRsaPublicKey(software.getRsaPublicKey());
        result.setRsaPrivateKey(rsaPrivateKeyPlain);
        result.setVersion(software.getVersion());
        result.setMinVersion(software.getMinVersion());
        result.setHeartbeatInterval(software.getHeartbeatInterval());
        result.setMaxConcurrent(software.getMaxConcurrent());
        result.setEnabled(software.getEnabled());
        return result;
    }

    /**
     * 生成全局唯一的 appKey（32 字符大写字母+数字）
     * 冲突时重试最多 5 次（概率极低，但铁律要求健壮）
     */
    private String generateUniqueAppKey() {
        for (int i = 0; i < 5; i++) {
            String appKey = randomString(JicekConstants.CHARSET_UPPER_ALNUM, JicekConstants.SOFTWARE_APP_KEY_LENGTH);
            Long count = softwareMapper.selectCount(new LambdaQueryWrapper<Software>()
                    .eq(Software::getAppKey, appKey));
            if (count == null || count == 0) {
                return appKey;
            }
            log.warn("【软件】appKey 冲突，重试第 {} 次", i + 1);
        }
        throw new ServiceException(ResultCode.FAIL, "appKey 生成冲突，请重试");
    }

    /**
     * 生成签名密钥（32 字节随机 → Base64 编码，44 字符）
     */
    private String generateSignSecret() {
        byte[] bytes = new byte[JicekConstants.SOFTWARE_SIGN_SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 生成 RSA-2048 密钥对
     */
    private RsaKeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(JicekConstants.SOFTWARE_RSA_KEY_SIZE, secureRandom);
            KeyPair keyPair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return new RsaKeyPair(publicKey, privateKey);
        } catch (Exception e) {
            log.error("【软件】RSA 密钥对生成失败", e);
            throw new ServiceException(ResultCode.FAIL, "RSA 密钥对生成失败");
        }
    }

    private String randomString(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(secureRandom.nextInt(charset.length())));
        }
        return sb.toString();
    }

    /**
     * 密钥脱敏：前 4 字符 + ****
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= JicekConstants.SOFTWARE_SECRET_MASK_PREFIX) {
            return "****";
        }
        return secret.substring(0, JicekConstants.SOFTWARE_SECRET_MASK_PREFIX) + "****";
    }

    /**
     * RSA 密钥对内部载体
     */
    private record RsaKeyPair(String publicKeyBase64, String privateKeyBase64) {}
}
