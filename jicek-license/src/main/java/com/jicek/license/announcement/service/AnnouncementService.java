package com.jicek.license.announcement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.announcement.dto.AnnouncementDetailDTO;
import com.jicek.license.announcement.dto.AnnouncementSaveDTO;
import com.jicek.license.announcement.dto.SdkAnnouncementDTO;
import com.jicek.license.announcement.entity.Announcement;
import com.jicek.license.announcement.mapper.AnnouncementMapper;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.sdk.auth.SoftwareContext;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 开发者后台 CRUD（tenantId + creatorId 从 AuthContext 获取）
 *  - 状态机：草稿 → 已发布 → 已下线（不可逆）
 *  - SDK 拉取已发布公告（按 softwareId + 客户端版本范围匹配）
 *
 * 安全铁律：
 *  - 所有操作校验公告 tenantId == AuthContext.currentTenantId()
 *  - SDK 拉取只返回 status=1 的公告，且通过 SoftwareContext 校验软件归属
 *  - 状态机不可逆：已发布不能再改回草稿，已下线不能再发布
 */
@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementMapper announcementMapper;
    private final SoftwareMapper softwareMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper, SoftwareMapper softwareMapper) {
        this.announcementMapper = announcementMapper;
        this.softwareMapper = softwareMapper;
    }

    /* ============ 开发者后台 CRUD ============ */

    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDetailDTO create(AnnouncementSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();
        Long creatorId = AuthContext.currentUserId();
        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validateType(dto.getType());

        LocalDateTime now = LocalDateTime.now();
        Announcement announcement = new Announcement();
        announcement.setTenantId(tenantId);
        announcement.setSoftwareId(dto.getSoftwareId());
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType());
        announcement.setStatus(JicekConstants.ANNOUNCEMENT_STATUS_DRAFT);
        announcement.setMinVersion(dto.getMinVersion());
        announcement.setMaxVersion(dto.getMaxVersion());
        announcement.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        announcement.setPinned(dto.getPinned() != null ? dto.getPinned() : 0);
        announcement.setViewCount(0L);
        announcement.setCreatorId(creatorId);
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        announcementMapper.insert(announcement);

        log.info("【公告】创建成功 id={} tenantId={} softwareId={} title={}",
                announcement.getId(), tenantId, dto.getSoftwareId(), dto.getTitle());
        return toDetailDTO(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(AnnouncementSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_NOT_FOUND, "更新时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        Announcement existing = requireOwnedAnnouncement(dto.getId(), tenantId);

        // 已发布公告不允许修改核心字段（避免影响线上展示），仅允许改草稿
        if (existing.getStatus() != JicekConstants.ANNOUNCEMENT_STATUS_DRAFT) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_STATUS_INVALID,
                    "已发布或已下线的公告不可编辑，请新建公告替换");
        }

        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validateType(dto.getType());

        existing.setSoftwareId(dto.getSoftwareId());
        existing.setTitle(dto.getTitle());
        existing.setContent(dto.getContent());
        existing.setType(dto.getType());
        existing.setMinVersion(dto.getMinVersion());
        existing.setMaxVersion(dto.getMaxVersion());
        existing.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : existing.getSortOrder());
        existing.setPinned(dto.getPinned() != null ? dto.getPinned() : existing.getPinned());
        existing.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(existing);

        log.info("【公告】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    public Page<AnnouncementDetailDTO> page(long current, long size, Long softwareId,
                                             Integer status, Integer type, String title) {
        Long tenantId = requireCurrentTenantId();
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getTenantId, tenantId)
                .eq(softwareId != null, Announcement::getSoftwareId, softwareId)
                .eq(status != null, Announcement::getStatus, status)
                .eq(type != null, Announcement::getType, type)
                .like(title != null && !title.isBlank(), Announcement::getTitle, title)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> page = announcementMapper.selectPage(new Page<>(current, size), wrapper);

        Page<AnnouncementDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDetailDTO).toList());
        return result;
    }

    public AnnouncementDetailDTO get(Long id) {
        Long tenantId = requireCurrentTenantId();
        Announcement announcement = requireOwnedAnnouncement(id, tenantId);
        return toDetailDTO(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedAnnouncement(id, tenantId);
        announcementMapper.deleteById(id);
        log.info("【公告】删除成功 id={} tenantId={}", id, tenantId);
    }

    /* ============ 状态机：发布 / 下线 ============ */

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Long tenantId = requireCurrentTenantId();
        Announcement announcement = requireOwnedAnnouncement(id, tenantId);

        if (announcement.getStatus() == JicekConstants.ANNOUNCEMENT_STATUS_PUBLISHED) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_ALREADY_PUBLISHED);
        }
        if (announcement.getStatus() == JicekConstants.ANNOUNCEMENT_STATUS_OFFLINE) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_ALREADY_OFFLINE);
        }

        announcement.setStatus(JicekConstants.ANNOUNCEMENT_STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        log.info("【公告】发布成功 id={} tenantId={}", id, tenantId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        Long tenantId = requireCurrentTenantId();
        Announcement announcement = requireOwnedAnnouncement(id, tenantId);

        if (announcement.getStatus() != JicekConstants.ANNOUNCEMENT_STATUS_PUBLISHED) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_NOT_PUBLISHED);
        }

        announcement.setStatus(JicekConstants.ANNOUNCEMENT_STATUS_OFFLINE);
        announcement.setOfflineTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        log.info("【公告】下线成功 id={} tenantId={}", id, tenantId);
    }

    /* ============ SDK 拉取（终端用户客户端调用） ============ */

    /**
     * SDK 拉取已发布公告
     *
     * @param clientVersion 客户端版本（用于版本范围匹配，null 表示不限）
     * @return 按 pinned DESC + sort_order DESC + publish_time DESC 排序的公告列表
     */
    public List<SdkAnnouncementDTO> fetchPublished(String clientVersion) {
        Software software = SoftwareContext.requireSoftware();

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getTenantId, software.getTenantId())
                .eq(Announcement::getSoftwareId, software.getId())
                .eq(Announcement::getStatus, JicekConstants.ANNOUNCEMENT_STATUS_PUBLISHED)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getPublishTime)
                .last("LIMIT " + JicekConstants.ANNOUNCEMENT_SDK_FETCH_LIMIT);

        List<Announcement> announcements = announcementMapper.selectList(wrapper);

        // 客户端版本范围过滤（内存过滤，避免 SQL 复杂度）
        if (clientVersion != null && !clientVersion.isBlank()) {
            announcements = announcements.stream()
                    .filter(a -> versionMatch(clientVersion, a.getMinVersion(), a.getMaxVersion()))
                    .toList();
        }

        // 异步累加 viewCount（此处简单同步累加，后续可改 Redis 计数 + 定时刷库）
        if (!announcements.isEmpty()) {
            try {
                for (Announcement a : announcements) {
                    Announcement update = new Announcement();
                    update.setId(a.getId());
                    update.setViewCount((a.getViewCount() == null ? 0 : a.getViewCount()) + 1);
                    announcementMapper.updateById(update);
                }
            } catch (Exception e) {
                log.warn("【公告】viewCount 累加失败，忽略不影响主流程", e);
            }
        }

        return announcements.stream().map(this::toSdkDTO).toList();
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
            throw new ServiceException(ResultCode.ANNOUNCEMENT_SOFTWARE_INVALID);
        }
    }

    private Announcement requireOwnedAnnouncement(Long id, Long tenantId) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_NOT_FOUND);
        }
        if (!announcement.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_NOT_FOUND);
        }
        return announcement;
    }

    private void validateType(Integer type) {
        if (type == null || type < JicekConstants.ANNOUNCEMENT_TYPE_NOTICE
                || type > JicekConstants.ANNOUNCEMENT_TYPE_BANNER) {
            throw new ServiceException(ResultCode.ANNOUNCEMENT_TYPE_INVALID);
        }
    }

    /**
     * 语义化版本范围匹配（简化实现：字符串比较，适用于 X.Y.Z 格式）
     * - minVersion 为 null 表示无下限
     * - maxVersion 为 null 表示无上限
     */
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
            // 版本号格式异常时，保守返回 true（不阻断展示）
            return true;
        }
    }

    /** 语义化版本比较：1.2.3 vs 1.10.0 → 按数字段比较而非字符串 */
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

    private AnnouncementDetailDTO toDetailDTO(Announcement a) {
        AnnouncementDetailDTO dto = new AnnouncementDetailDTO();
        dto.setId(a.getId());
        dto.setTenantId(a.getTenantId());
        dto.setSoftwareId(a.getSoftwareId());
        dto.setTitle(a.getTitle());
        dto.setContent(a.getContent());
        dto.setType(a.getType());
        dto.setStatus(a.getStatus());
        dto.setMinVersion(a.getMinVersion());
        dto.setMaxVersion(a.getMaxVersion());
        dto.setSortOrder(a.getSortOrder());
        dto.setPinned(a.getPinned());
        dto.setViewCount(a.getViewCount());
        dto.setPublishTime(a.getPublishTime());
        dto.setOfflineTime(a.getOfflineTime());
        dto.setCreatorId(a.getCreatorId());
        dto.setCreateTime(a.getCreateTime());
        dto.setUpdateTime(a.getUpdateTime());
        return dto;
    }

    private SdkAnnouncementDTO toSdkDTO(Announcement a) {
        SdkAnnouncementDTO dto = new SdkAnnouncementDTO();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setContent(a.getContent());
        dto.setType(a.getType());
        dto.setPinned(a.getPinned());
        dto.setSortOrder(a.getSortOrder());
        dto.setPublishTime(a.getPublishTime());
        return dto;
    }
}
