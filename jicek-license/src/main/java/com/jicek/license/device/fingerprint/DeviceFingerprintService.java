package com.jicek.license.device.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.Md5SignService;
import com.jicek.license.crypto.RsaCryptoService;
import com.jicek.license.device.dto.DeviceFingerprintDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备指纹服务（设备安全核心）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：
 * 1. 解析客户端 RSA 加密的 5 维哈希 JSON
 * 2. 独立计算最终指纹（防客户端篡改）
 * 3. 常量时间比对客户端上报指纹与本地计算结果（防时序攻击）
 * 4. VM/容器场景的补充维度融合
 * 5. 生成 AES 加密的设备详情 JSON（用于审计）
 *
 * 5 维融合算法（防伪造）：
 *   客户端采集：CPU 序列号 / 主板序列号 / 硬盘序列号 / 网卡 MAC / BIOS UUID
 *   客户端对每维 SHA-256 哈希（保护隐私，服务端拿不到原始序列号）
 *   最终指纹 = SHA-256(cpuHash + mainboardHash + diskHash + macHash + biosHash)
 *   VM 场景：最终指纹 = SHA-256(5维哈希 + vmExtra)
 *
 * 安全铁律：
 * - 服务端必须独立计算指纹，禁信任客户端上报值（铁律 06）
 * - 比对使用 MessageDigest.isEqual 常量时间比较
 * - 客户端原始数据 RSA 加密传输，防止中间人篡改
 */
@Slf4j
@Service
public class DeviceFingerprintService {

    private final RsaCryptoService rsaCryptoService;
    private final ObjectMapper objectMapper;

    public DeviceFingerprintService(RsaCryptoService rsaCryptoService,
                                    ObjectMapper objectMapper) {
        this.rsaCryptoService = rsaCryptoService;
        this.objectMapper = objectMapper;
    }

    /**
     * 验证并解析客户端上报的指纹
     *
     * @param dto 客户端上报数据
     * @return 解析后的 5 维哈希 Map（key: cpu/mainboard/disk/mac/bios）
     * @throws ServiceException 验签失败 / RSA 解密失败 / JSON 解析失败
     */
    public Map<String, String> verifyAndParse(DeviceFingerprintDTO dto) {
        if (dto == null || dto.getFingerprint() == null || dto.getEncryptedDetail() == null) {
            throw new ServiceException(ResultCode.PARAM_ERROR, "设备指纹数据不完整");
        }

        // 1. RSA 解密 5 维哈希 JSON
        String detailJson;
        try {
            detailJson = rsaCryptoService.decryptByPrivateKey(dto.getEncryptedDetail());
        } catch (Exception e) {
            log.warn("设备指纹 RSA 解密失败: {}", e.getMessage());
            throw new ServiceException(ResultCode.DEVICE_FINGERPRINT_INVALID, "设备指纹解密失败");
        }

        // 2. 解析 JSON
        Map<String, String> dimensions;
        try {
            dimensions = objectMapper.readValue(detailJson, Map.class);
        } catch (Exception e) {
            log.warn("设备指纹 JSON 解析失败: {}", e.getMessage());
            throw new ServiceException(ResultCode.DEVICE_FINGERPRINT_INVALID, "设备指纹格式错误");
        }

        // 3. 校验维度完整性
        validateDimensions(dimensions);

        // 4. 独立计算最终指纹
        String computedFingerprint = computeFingerprint(dimensions, dto.getIsVm(), dto.getVmExtra());

        // 5. 常量时间比对（防时序攻击）
        if (!MessageDigest.isEqual(
                dto.getFingerprint().getBytes(StandardCharsets.UTF_8),
                computedFingerprint.getBytes(StandardCharsets.UTF_8))) {
            log.warn("设备指纹比对失败: client={}, computed={}",
                    dto.getFingerprint(), computedFingerprint);
            throw new ServiceException(ResultCode.DEVICE_FINGERPRINT_INVALID, "设备指纹校验失败");
        }

        return dimensions;
    }

    /**
     * 计算最终指纹（5 维 SHA-256 融合）
     *
     * @param dimensions 5 维哈希 Map
     * @param isVm       是否 VM
     * @param vmExtra    VM 补充维度
     * @return 64 字符 SHA-256
     */
    public String computeFingerprint(Map<String, String> dimensions, Integer isVm, String vmExtra) {
        StringBuilder sb = new StringBuilder(320);
        sb.append(dimensions.getOrDefault("cpu", ""));
        sb.append(dimensions.getOrDefault("mainboard", ""));
        sb.append(dimensions.getOrDefault("disk", ""));
        sb.append(dimensions.getOrDefault("mac", ""));
        sb.append(dimensions.getOrDefault("bios", ""));

        // VM/容器场景：追加补充维度，防止 VM 克隆导致指纹重复
        if (isVm != null && isVm == 1 && vmExtra != null && !vmExtra.isBlank()) {
            sb.append(vmExtra);
        }

        return Md5SignService.sha256Hex(sb.toString());
    }

    /**
     * 校验 5 维哈希完整性
     */
    private void validateDimensions(Map<String, String> dimensions) {
        if (dimensions == null || dimensions.size() < JicekConstants.FP_DIMENSION_COUNT) {
            throw new ServiceException(ResultCode.DEVICE_FINGERPRINT_INVALID,
                    "设备指纹维度不完整，需 5 维");
        }
        String[] required = {"cpu", "mainboard", "disk", "mac", "bios"};
        for (String key : required) {
            String val = dimensions.get(key);
            if (val == null || val.length() != 64) {
                throw new ServiceException(ResultCode.DEVICE_FINGERPRINT_INVALID,
                        "设备指纹维度 " + key + " 不合法（需 64 字符 SHA-256）");
            }
        }
    }

    /**
     * 生成设备详情 JSON（用于审计，AES 加密后入库）
     * 明文字段：5 维哈希 + 设备元信息
     *
     * @param dimensions 5 维哈希
     * @param dto        客户端元信息
     * @return JSON 字符串
     */
    public String buildDetailJson(Map<String, String> dimensions, DeviceFingerprintDTO dto) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("cpu", dimensions.get("cpu"));
        detail.put("mainboard", dimensions.get("mainboard"));
        detail.put("disk", dimensions.get("disk"));
        detail.put("mac", dimensions.get("mac"));
        detail.put("bios", dimensions.get("bios"));
        detail.put("deviceName", dto.getDeviceName());
        detail.put("osType", dto.getOsType());
        detail.put("osVersion", dto.getOsVersion());
        detail.put("clientVersion", dto.getClientVersion());
        detail.put("isVm", dto.getIsVm());
        detail.put("vmExtra", dto.getVmExtra());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            throw new RuntimeException("设备详情 JSON 序列化失败", e);
        }
    }
}
