package com.jicek.license.update.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import com.jicek.license.sdk.auth.SoftwareContext;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import com.jicek.license.update.dto.SdkUpdateCheckResultDTO;
import com.jicek.license.update.dto.UpdatePackageDetailDTO;
import com.jicek.license.update.dto.UpdatePackageSaveDTO;
import com.jicek.license.update.dto.UploadResultDTO;
import com.jicek.license.update.entity.UpdatePackage;
import com.jicek.license.update.mapper.UpdatePackageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 更新包服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 文件上传（multipart → 本地存储 + SHA-256 计算）
 *  - CRUD + 发布/下线状态机
 *  - SDK 检查更新（按 softwareId + channel + 客户端版本范围匹配，返回最新已发布版本）
 *
 * 安全铁律：
 *  - file_path 存相对路径（相对 storage.root），禁存绝对路径
 *  - 文件类型白名单校验（exe/sh/win/lua/zip/7z）
 *  - 文件大小校验（≤ 500MB）
 *  - SHA-256 计算 + 入库，客户端下载后校验完整性
 *  - 文件名 UUID 化存储，禁用原始文件名（防路径穿越）
 *  - 所有操作校验 tenantId == AuthContext.currentTenantId()
 */
@Service
public class UpdatePackageService {

    private static final Logger log = LoggerFactory.getLogger(UpdatePackageService.class);

    private final UpdatePackageMapper updatePackageMapper;
    private final SoftwareMapper softwareMapper;
    private final JicekProperties properties;

    public UpdatePackageService(UpdatePackageMapper updatePackageMapper,
                                 SoftwareMapper softwareMapper,
                                 JicekProperties properties) {
        this.updatePackageMapper = updatePackageMapper;
        this.softwareMapper = softwareMapper;
        this.properties = properties;
    }

    /* ============ 文件上传 ============ */

    public UploadResultDTO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_EMPTY);
        }
        if (file.getSize() > JicekConstants.UPDATE_MAX_FILE_SIZE) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TYPE_INVALID);
        }
        String fileType = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!isValidFileType(fileType)) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TYPE_INVALID);
        }

        // 存储目录：{root}/{updateSubDir}/{yyyy/MM/dd}/
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
        String relativePath = properties.getStorage().getUpdateSubDir() + "/" + dateDir + "/" + storedFileName;

        Path rootPath = Paths.get(properties.getStorage().getRoot());
        Path fullPath = rootPath.resolve(relativePath).toAbsolutePath().normalize();

        // 防路径穿越：确保最终路径在 root 内
        if (!fullPath.startsWith(rootPath.toAbsolutePath().normalize())) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TYPE_INVALID, "非法文件路径");
        }

        try {
            Files.createDirectories(fullPath.getParent());
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("【更新包】文件存储失败 fileName={}", originalName, e);
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TYPE_INVALID, "文件存储失败");
        }

        // 计算 SHA-256
        String sha256;
        try {
            sha256 = sha256Hex(Files.readAllBytes(fullPath));
        } catch (IOException e) {
            log.error("【更新包】SHA-256 计算失败 path={}", fullPath, e);
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_HASH_MISMATCH);
        }

        UploadResultDTO result = new UploadResultDTO();
        result.setFilePath(relativePath);
        result.setFileName(originalName);
        result.setFileSize(file.getSize());
        result.setFileSha256(sha256);
        result.setFileType(fileType);
        log.info("【更新包】文件上传成功 fileName={} size={} sha256={} path={}",
                originalName, file.getSize(), sha256, relativePath);
        return result;
    }

    /* ============ CRUD ============ */

    @Transactional(rollbackFor = Exception.class)
    public UpdatePackageDetailDTO create(UpdatePackageSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();
        Long creatorId = AuthContext.currentUserId();
        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validateChannel(dto.getChannel());
        validateFileType(dto.getFileType());

        // 版本唯一性校验（同软件 + 同通道 + 同版本）
        Long existsCount = updatePackageMapper.selectCount(new LambdaQueryWrapper<UpdatePackage>()
                .eq(UpdatePackage::getTenantId, tenantId)
                .eq(UpdatePackage::getSoftwareId, dto.getSoftwareId())
                .eq(UpdatePackage::getVersion, dto.getVersion())
                .eq(UpdatePackage::getChannel, dto.getChannel()));
        if (existsCount > 0) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_VERSION_DUPLICATE);
        }

        LocalDateTime now = LocalDateTime.now();
        UpdatePackage pkg = new UpdatePackage();
        pkg.setTenantId(tenantId);
        pkg.setSoftwareId(dto.getSoftwareId());
        pkg.setVersion(dto.getVersion());
        pkg.setChannel(dto.getChannel());
        pkg.setFileType(dto.getFileType());
        pkg.setFileName(dto.getFileName());
        pkg.setFilePath(dto.getFilePath());
        pkg.setFileSize(dto.getFileSize());
        pkg.setFileSha256(dto.getFileSha256());
        pkg.setReleaseNotes(dto.getReleaseNotes());
        pkg.setMinClientVersion(dto.getMinClientVersion());
        pkg.setMaxClientVersion(dto.getMaxClientVersion());
        pkg.setStatus(JicekConstants.UPDATE_STATUS_DRAFT);
        pkg.setForceUpdate(dto.getForceUpdate() != null ? dto.getForceUpdate() : 0);
        pkg.setDownloadCount(0L);
        pkg.setCreatorId(creatorId);
        pkg.setCreateTime(now);
        pkg.setUpdateTime(now);
        updatePackageMapper.insert(pkg);

        log.info("【更新包】创建成功 id={} tenantId={} softwareId={} version={} channel={}",
                pkg.getId(), tenantId, dto.getSoftwareId(), dto.getVersion(), dto.getChannel());
        return toDetailDTO(pkg, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(UpdatePackageSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_NOT_FOUND, "更新时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        UpdatePackage existing = requireOwnedPackage(dto.getId(), tenantId);

        // 仅草稿可编辑，且仅允许改 releaseNotes/版本范围/强制更新（文件不可改）
        if (existing.getStatus() != JicekConstants.UPDATE_STATUS_DRAFT) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_STATUS_INVALID,
                    "已发布或已下线的更新包不可编辑，请新建版本替换");
        }

        existing.setReleaseNotes(dto.getReleaseNotes());
        existing.setMinClientVersion(dto.getMinClientVersion());
        existing.setMaxClientVersion(dto.getMaxClientVersion());
        existing.setForceUpdate(dto.getForceUpdate() != null ? dto.getForceUpdate() : existing.getForceUpdate());
        existing.setUpdateTime(LocalDateTime.now());
        updatePackageMapper.updateById(existing);

        log.info("【更新包】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    public Page<UpdatePackageDetailDTO> page(long current, long size, Long softwareId,
                                              Integer status, Integer channel, String version) {
        Long tenantId = requireCurrentTenantId();
        LambdaQueryWrapper<UpdatePackage> wrapper = new LambdaQueryWrapper<UpdatePackage>()
                .eq(UpdatePackage::getTenantId, tenantId)
                .eq(softwareId != null, UpdatePackage::getSoftwareId, softwareId)
                .eq(status != null, UpdatePackage::getStatus, status)
                .eq(channel != null, UpdatePackage::getChannel, channel)
                .like(version != null && !version.isBlank(), UpdatePackage::getVersion, version)
                .orderByDesc(UpdatePackage::getCreateTime);
        Page<UpdatePackage> page = updatePackageMapper.selectPage(new Page<>(current, size), wrapper);

        Page<UpdatePackageDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(p -> toDetailDTO(p, false)).toList());
        return result;
    }

    public UpdatePackageDetailDTO get(Long id) {
        Long tenantId = requireCurrentTenantId();
        UpdatePackage pkg = requireOwnedPackage(id, tenantId);
        return toDetailDTO(pkg, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireCurrentTenantId();
        UpdatePackage pkg = requireOwnedPackage(id, tenantId);

        // 删除数据库记录 + 物理文件
        updatePackageMapper.deleteById(id);
        deletePhysicalFile(pkg.getFilePath());
        log.info("【更新包】删除成功 id={} tenantId={}", id, tenantId);
    }

    /* ============ 状态机：发布 / 下线 ============ */

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Long tenantId = requireCurrentTenantId();
        UpdatePackage pkg = requireOwnedPackage(id, tenantId);

        if (pkg.getStatus() == JicekConstants.UPDATE_STATUS_PUBLISHED) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_ALREADY_PUBLISHED);
        }
        if (pkg.getStatus() == JicekConstants.UPDATE_STATUS_OFFLINE) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_ALREADY_OFFLINE);
        }

        pkg.setStatus(JicekConstants.UPDATE_STATUS_PUBLISHED);
        pkg.setPublishTime(LocalDateTime.now());
        pkg.setUpdateTime(LocalDateTime.now());
        updatePackageMapper.updateById(pkg);

        log.info("【更新包】发布成功 id={} tenantId={} version={}", id, tenantId, pkg.getVersion());
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        Long tenantId = requireCurrentTenantId();
        UpdatePackage pkg = requireOwnedPackage(id, tenantId);

        if (pkg.getStatus() != JicekConstants.UPDATE_STATUS_PUBLISHED) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_NOT_PUBLISHED);
        }

        pkg.setStatus(JicekConstants.UPDATE_STATUS_OFFLINE);
        pkg.setOfflineTime(LocalDateTime.now());
        pkg.setUpdateTime(LocalDateTime.now());
        updatePackageMapper.updateById(pkg);

        log.info("【更新包】下线成功 id={} tenantId={}", id, tenantId);
    }

    /* ============ SDK 检查更新 ============ */

    /**
     * SDK 检查更新
     *
     * @param clientVersion 客户端当前版本
     * @param channel       通道（1稳定版 2内测版），默认 1
     * @return 有更新返回最新版本信息，无更新返回 hasUpdate=false
     */
    public SdkUpdateCheckResultDTO checkUpdate(String clientVersion, Integer channel) {
        Software software = SoftwareContext.requireSoftware();
        int targetChannel = (channel != null) ? channel : JicekConstants.UPDATE_CHANNEL_STABLE;

        // 查询该软件该通道所有已发布的更新包，按 publish_time DESC
        List<UpdatePackage> packages = updatePackageMapper.selectList(new LambdaQueryWrapper<UpdatePackage>()
                .eq(UpdatePackage::getTenantId, software.getTenantId())
                .eq(UpdatePackage::getSoftwareId, software.getId())
                .eq(UpdatePackage::getStatus, JicekConstants.UPDATE_STATUS_PUBLISHED)
                .eq(UpdatePackage::getChannel, targetChannel)
                .orderByDesc(UpdatePackage::getPublishTime));

        if (packages.isEmpty()) {
            return noUpdateResult();
        }

        // 客户端版本范围过滤
        List<UpdatePackage> matched = packages.stream()
                .filter(p -> versionMatch(clientVersion, p.getMinClientVersion(), p.getMaxClientVersion()))
                .toList();

        if (matched.isEmpty()) {
            return noUpdateResult();
        }

        UpdatePackage latest = matched.get(0);

        // 比较版本号：若客户端版本 >= 最新版本，则无更新
        if (clientVersion != null && !clientVersion.isBlank()
                && compareVersion(clientVersion, latest.getVersion()) >= 0) {
            return noUpdateResult();
        }

        // 有更新
        SdkUpdateCheckResultDTO result = new SdkUpdateCheckResultDTO();
        result.setHasUpdate(true);
        result.setForceUpdate(latest.getForceUpdate() != null && latest.getForceUpdate() == 1);
        result.setPackageId(latest.getId());
        result.setVersion(latest.getVersion());
        result.setFileType(latest.getFileType());
        result.setFileName(latest.getFileName());
        result.setFileSize(latest.getFileSize());
        result.setFileSha256(latest.getFileSha256());
        result.setReleaseNotes(latest.getReleaseNotes());
        result.setDownloadUrl(buildDownloadUrl(latest.getFilePath()));
        result.setPublishTime(latest.getPublishTime());

        // 累加 download_count（简化：检查即累加，后续可改 SDK 下载接口单独累加）
        try {
            UpdatePackage update = new UpdatePackage();
            update.setId(latest.getId());
            update.setDownloadCount((latest.getDownloadCount() == null ? 0 : latest.getDownloadCount()) + 1);
            updatePackageMapper.updateById(update);
        } catch (Exception e) {
            log.warn("【更新包】download_count 累加失败，忽略", e);
        }

        return result;
    }

    /* ============ 内部工具 ============ */

    private Long requireCurrentTenantId() {
        Long tenantId = AuthContext.currentTenantId();
        if (tenantId == null) {
            throw new ServiceException(ResultCode.AUTH_NO_PERMISSION, "当前用户无租户身份");
        }
        return tenantId;
    }

    private void validateSoftwareOwnership(Long softwareId, Long tenantId) {
        Software software = softwareMapper.selectById(softwareId);
        if (software == null || !software.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_SOFTWARE_INVALID);
        }
    }

    private UpdatePackage requireOwnedPackage(Long id, Long tenantId) {
        UpdatePackage pkg = updatePackageMapper.selectById(id);
        if (pkg == null || !pkg.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_NOT_FOUND);
        }
        return pkg;
    }

    private void validateChannel(Integer channel) {
        if (channel == null || channel < JicekConstants.UPDATE_CHANNEL_STABLE
                || channel > JicekConstants.UPDATE_CHANNEL_BETA) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_STATUS_INVALID, "通道非法");
        }
    }

    private void validateFileType(String fileType) {
        if (fileType == null || !isValidFileType(fileType)) {
            throw new ServiceException(ResultCode.UPDATE_PACKAGE_FILE_TYPE_INVALID);
        }
    }

    private boolean isValidFileType(String fileType) {
        return Arrays.asList(JicekConstants.UPDATE_FILE_TYPES).contains(fileType);
    }

    private void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path rootPath = Paths.get(properties.getStorage().getRoot());
            Path fullPath = rootPath.resolve(relativePath).toAbsolutePath().normalize();
            if (fullPath.startsWith(rootPath.toAbsolutePath().normalize())) {
                Files.deleteIfExists(fullPath);
            }
        } catch (IOException e) {
            log.warn("【更新包】物理文件删除失败 path={}", relativePath, e);
        }
    }

    private String buildDownloadUrl(String filePath) {
        String baseUrl = properties.getStorage().getDownloadBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            // 未配置下载基础 URL，返回相对路径（开发环境用，由 Nginx 反代）
            return "/files/" + filePath;
        }
        return baseUrl + "/" + filePath;
    }

    private boolean versionMatch(String clientVersion, String minVersion, String maxVersion) {
        try {
            if (minVersion != null && !minVersion.isBlank()) {
                if (compareVersion(clientVersion, minVersion) < 0) {
                    return false;
                }
            }
            if (maxVersion != null && !maxVersion.isBlank()) {
                if (compareVersion(clientVersion, maxVersion) > 0) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    private SdkUpdateCheckResultDTO noUpdateResult() {
        SdkUpdateCheckResultDTO result = new SdkUpdateCheckResultDTO();
        result.setHasUpdate(false);
        result.setForceUpdate(false);
        return result;
    }

    private UpdatePackageDetailDTO toDetailDTO(UpdatePackage p, boolean includeDownloadUrl) {
        UpdatePackageDetailDTO dto = new UpdatePackageDetailDTO();
        dto.setId(p.getId());
        dto.setTenantId(p.getTenantId());
        dto.setSoftwareId(p.getSoftwareId());
        dto.setVersion(p.getVersion());
        dto.setChannel(p.getChannel());
        dto.setFileType(p.getFileType());
        dto.setFileName(p.getFileName());
        dto.setFilePath(p.getFilePath());
        dto.setFileSize(p.getFileSize());
        dto.setFileSha256(p.getFileSha256());
        dto.setReleaseNotes(p.getReleaseNotes());
        dto.setMinClientVersion(p.getMinClientVersion());
        dto.setMaxClientVersion(p.getMaxClientVersion());
        dto.setStatus(p.getStatus());
        dto.setForceUpdate(p.getForceUpdate());
        dto.setDownloadCount(p.getDownloadCount());
        dto.setPublishTime(p.getPublishTime());
        dto.setOfflineTime(p.getOfflineTime());
        dto.setCreatorId(p.getCreatorId());
        dto.setCreateTime(p.getCreateTime());
        dto.setUpdateTime(p.getUpdateTime());
        if (includeDownloadUrl && p.getStatus() == JicekConstants.UPDATE_STATUS_PUBLISHED) {
            dto.setDownloadUrl(buildDownloadUrl(p.getFilePath()));
        }
        return dto;
    }
}
