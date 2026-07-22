package com.jicek.license.enduser.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.enduser.dto.EndUserDetailDTO;
import com.jicek.license.enduser.dto.EndUserResetPasswordDTO;
import com.jicek.license.enduser.dto.EndUserSaveDTO;
import com.jicek.license.enduser.dto.H5EndUserLoginDTO;
import com.jicek.license.enduser.dto.H5EndUserLoginResultDTO;
import com.jicek.license.enduser.entity.EndUser;
import com.jicek.license.enduser.mapper.EndUserMapper;
import com.jicek.license.h5.entity.H5Session;
import com.jicek.license.h5.mapper.H5SessionMapper;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 终端用户服务（v0.14.0，独立账号体系）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 开发者后台 CRUD（tenantId 从 AuthContext 获取，前端禁传，防越权）
 *  - 封禁/解封状态切换
 *  - 重置密码（开发者后台调用，无需原密码）
 *  - H5 账号密码登录（与卡密登录并存，复用 H5Session 机制）
 *
 * 安全铁律（04/06/13/09）：
 *  - 密码 BCrypt 哈希存储（cn.hutool.crypto.digest.BCrypt.hashpw，单参 Hutool 风格）
 *  - 所有后台操作校验 EndUser.tenantId == AuthContext.currentTenantId()
 *  - 所有后台操作校验 softwareId 归属当前租户
 *  - tenantId + softwareId + username 三元唯一
 *  - 登录失败统一返回 END_USER_PASSWORD_ERROR（不区分用户名/密码错误，防枚举）
 *  - 不硬编码，长度/状态全部走 JicekConstants 常量
 *
 * 实现说明：
 *  - H5 登录复用 H5Session 表（cardKeyId=null，userId=终端用户ID），不改动 H5AuthService
 *  - H5 token 为 UUID（去 -），有效期走 JicekConstants.H5_TOKEN_EXPIRE_HOURS
 *  - DB + Redis 双写（与 H5AuthInterceptor 缓存协议一致）
 */
@Service
public class EndUserService {

    private static final Logger log = LoggerFactory.getLogger(EndUserService.class);

    private final EndUserMapper endUserMapper;
    private final SoftwareMapper softwareMapper;
    private final H5SessionMapper h5SessionMapper;
    private final RedissonClient redissonClient;

    public EndUserService(EndUserMapper endUserMapper, SoftwareMapper softwareMapper,
                          H5SessionMapper h5SessionMapper, RedissonClient redissonClient) {
        this.endUserMapper = endUserMapper;
        this.softwareMapper = softwareMapper;
        this.h5SessionMapper = h5SessionMapper;
        this.redissonClient = redissonClient;
    }

    /* ============ 开发者后台 CRUD ============ */

    /**
     * 创建终端用户
     *
     * @param dto 创建参数（softwareId/username/password 必填）
     * @return 新建用户详情（含 softwareName 冗余）
     * @throws ServiceException 软件无权操作 / 用户名已存在 / 密码为空
     */
    @Transactional(rollbackFor = Exception.class)
    public EndUserDetailDTO create(EndUserSaveDTO dto) {
        Long tenantId = requireCurrentTenantId();
        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validateUsernameUnique(tenantId, dto.getSoftwareId(), dto.getUsername(), null);

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ServiceException(ResultCode.PARAM_ERROR, "创建终端用户时密码不能为空");
        }
        validatePasswordLength(dto.getPassword());

        LocalDateTime now = LocalDateTime.now();
        EndUser user = new EndUser();
        user.setTenantId(tenantId);
        user.setSoftwareId(dto.getSoftwareId());
        user.setUsername(dto.getUsername());
        user.setPasswordHash(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : JicekConstants.END_USER_STATUS_NORMAL);
        user.setRemark(dto.getRemark());
        user.setCreateTime(now);
        user.setUpdateTime(now);
        endUserMapper.insert(user);

        log.info("【终端用户】创建成功 id={} tenantId={} softwareId={} username={}",
                user.getId(), tenantId, dto.getSoftwareId(), dto.getUsername());
        return toDetailDTO(user, lookupSoftwareName(dto.getSoftwareId()));
    }

    /**
     * 更新终端用户（password 为空表示不修改密码）
     *
     * @param dto 更新参数（id 必填）
     * @throws ServiceException 用户不存在 / 跨租户 / 用户名冲突
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(EndUserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException(ResultCode.END_USER_NOT_FOUND, "更新时 id 不能为空");
        }
        Long tenantId = requireCurrentTenantId();
        EndUser existing = requireOwnedEndUser(dto.getId(), tenantId);

        // 软件归属校验（允许迁移到同租户其它软件）
        validateSoftwareOwnership(dto.getSoftwareId(), tenantId);
        validateUsernameUnique(tenantId, dto.getSoftwareId(), dto.getUsername(), dto.getId());

        existing.setSoftwareId(dto.getSoftwareId());
        existing.setUsername(dto.getUsername());
        existing.setNickname(dto.getNickname());
        existing.setEmail(dto.getEmail());
        existing.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        existing.setRemark(dto.getRemark());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            validatePasswordLength(dto.getPassword());
            existing.setPasswordHash(BCrypt.hashpw(dto.getPassword()));
        }
        existing.setUpdateTime(LocalDateTime.now());
        endUserMapper.updateById(existing);

        log.info("【终端用户】更新成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    /**
     * 删除终端用户
     *
     * @param id 用户ID
     * @throws ServiceException 用户不存在 / 跨租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireCurrentTenantId();
        requireOwnedEndUser(id, tenantId);
        endUserMapper.deleteById(id);
        log.info("【终端用户】删除成功 id={} tenantId={}", id, tenantId);
    }

    /**
     * 分页查询当前租户的终端用户
     *
     * @param current    页码
     * @param size       每页大小
     * @param softwareId 软件ID（可选过滤）
     * @param username   用户名模糊（可选）
     * @param status     状态（可选）
     * @return 分页结果（含 softwareName 冗余）
     */
    public Page<EndUserDetailDTO> page(long current, long size, Long softwareId,
                                        String username, Integer status) {
        Long tenantId = requireCurrentTenantId();
        LambdaQueryWrapper<EndUser> wrapper = new LambdaQueryWrapper<EndUser>()
                .eq(EndUser::getTenantId, tenantId)
                .eq(softwareId != null, EndUser::getSoftwareId, softwareId)
                .eq(status != null, EndUser::getStatus, status)
                .like(username != null && !username.isBlank(), EndUser::getUsername, username)
                .orderByDesc(EndUser::getCreateTime);
        Page<EndUser> page = endUserMapper.selectPage(new Page<>(current, size), wrapper);

        // 批量查询 softwareName，避免 N+1
        Map<Long, String> softwareNameMap = lookupSoftwareNames(page.getRecords());

        Page<EndUserDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(u -> toDetailDTO(u, softwareNameMap.get(u.getSoftwareId())))
                .toList());
        return result;
    }

    /**
     * 获取终端用户详情
     *
     * @param id 用户ID
     * @return 用户详情（含 softwareName 冗余）
     * @throws ServiceException 用户不存在 / 跨租户
     */
    public EndUserDetailDTO get(Long id) {
        Long tenantId = requireCurrentTenantId();
        EndUser user = requireOwnedEndUser(id, tenantId);
        return toDetailDTO(user, lookupSoftwareName(user.getSoftwareId()));
    }

    /**
     * 封禁终端用户（status → 0）
     *
     * @param id 用户ID
     * @throws ServiceException 用户不存在 / 跨租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void ban(Long id) {
        changeStatus(id, JicekConstants.END_USER_STATUS_BANNED);
        log.info("【终端用户】封禁成功 id={}", id);
    }

    /**
     * 解封终端用户（status → 1）
     *
     * @param id 用户ID
     * @throws ServiceException 用户不存在 / 跨租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void unban(Long id) {
        changeStatus(id, JicekConstants.END_USER_STATUS_NORMAL);
        log.info("【终端用户】解封成功 id={}", id);
    }

    /**
     * 重置密码（开发者后台调用，无需原密码）
     *
     * @param dto 重置参数（id + newPassword）
     * @throws ServiceException 用户不存在 / 跨租户
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(EndUserResetPasswordDTO dto) {
        Long tenantId = requireCurrentTenantId();
        EndUser existing = requireOwnedEndUser(dto.getId(), tenantId);
        validatePasswordLength(dto.getNewPassword());

        LambdaUpdateWrapper<EndUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(EndUser::getId, existing.getId())
                .set(EndUser::getPasswordHash, BCrypt.hashpw(dto.getNewPassword()))
                .set(EndUser::getUpdateTime, LocalDateTime.now());
        endUserMapper.update(null, uw);
        log.info("【终端用户】重置密码成功 id={} tenantId={}", dto.getId(), tenantId);
    }

    /* ============ H5 账号密码登录 ============ */

    /**
     * H5 终端用户账号登录
     *
     * 流程：appKey → 解析 Software → 按 (tenantId, softwareId, username) 查 EndUser
     *      → BCrypt.checkpw 校验密码 → 校验 status → 生成 H5 token → 写 H5Session + Redis
     *
     * @param dto       登录参数（appKey + username + password）
     * @param clientIp  客户端 IP（审计）
     * @param userAgent User-Agent（设备信息，截断 255）
     * @return 登录结果（h5Token + 用户信息 + 过期时间）
     * @throws ServiceException 软件不存在/已禁用 / 用户名或密码错误 / 已封禁
     */
    @Transactional(rollbackFor = Exception.class)
    public H5EndUserLoginResultDTO login(H5EndUserLoginDTO dto, String clientIp, String userAgent) {
        // 1. 解析软件
        Software software = softwareMapper.selectOne(
                new LambdaQueryWrapper<Software>().eq(Software::getAppKey, dto.getAppKey()));
        if (software == null) {
            throw new ServiceException(ResultCode.END_USER_SOFTWARE_INVALID);
        }
        if (software.getEnabled() == null
                || software.getEnabled() != JicekConstants.SOFTWARE_ENABLED) {
            throw new ServiceException(ResultCode.H5_SOFTWARE_DISABLED);
        }

        // 2. 按 (tenantId, softwareId, username) 查终端用户
        EndUser user = endUserMapper.selectOne(
                new LambdaQueryWrapper<EndUser>()
                        .eq(EndUser::getTenantId, software.getTenantId())
                        .eq(EndUser::getSoftwareId, software.getId())
                        .eq(EndUser::getUsername, dto.getUsername()));

        // 3. 用户不存在 / 密码错误 统一返回 END_USER_PASSWORD_ERROR（防枚举）
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.END_USER_PASSWORD_ERROR);
        }

        // 4. 封禁校验
        if (user.getStatus() != null
                && user.getStatus() == JicekConstants.END_USER_STATUS_BANNED) {
            throw new ServiceException(ResultCode.END_USER_BANNED);
        }

        // 5. 生成 H5 token（UUID 去 -）
        String h5Token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusHours(JicekConstants.H5_TOKEN_EXPIRE_HOURS);

        // 6. 写 H5Session（cardKeyId=null，userId=终端用户ID）
        H5Session session = new H5Session();
        session.setTenantId(software.getTenantId());
        session.setSoftwareId(software.getId());
        session.setCardKeyId(null);
        session.setUserId(user.getId());
        session.setH5Token(h5Token);
        session.setDeviceInfo(userAgent != null
                ? userAgent.substring(0, Math.min(userAgent.length(), 255)) : null);
        session.setClientIp(clientIp);
        session.setExpireTime(expireTime);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        h5SessionMapper.insert(session);

        // 7. 写 Redis 缓存标记（与 H5AuthInterceptor 协议一致：key=前缀+token，value="1"，TTL=剩余有效期）
        try {
            String redisKey = JicekConstants.REDIS_KEY_H5_SESSION + h5Token;
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            long remainSeconds = Duration.between(now, expireTime).getSeconds();
            if (remainSeconds > 0) {
                bucket.set("1", Duration.ofSeconds(remainSeconds));
            }
        } catch (Exception e) {
            // Redis 异常不阻断登录（DB 已落库，H5AuthInterceptor 会回源 DB）
            log.warn("【终端用户】登录写 Redis 缓存失败，不影响登录 h5Token={}", h5Token, e);
        }

        // 8. 更新最后登录时间/IP（审计）
        LambdaUpdateWrapper<EndUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(EndUser::getId, user.getId())
                .set(EndUser::getLastLoginTime, now)
                .set(EndUser::getLastLoginIp, clientIp);
        endUserMapper.update(null, uw);

        log.info("【终端用户】H5 账号登录成功 userId={} tenantId={} softwareId={} username={} ip={}",
                user.getId(), software.getTenantId(), software.getId(), user.getUsername(), clientIp);

        // 9. 组装返回
        H5EndUserLoginResultDTO result = new H5EndUserLoginResultDTO();
        result.setH5Token(h5Token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setSoftwareName(software.getName());
        result.setTokenExpireTime(expireTime);
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
            throw new ServiceException(ResultCode.END_USER_SOFTWARE_INVALID);
        }
    }

    private void validateUsernameUnique(Long tenantId, Long softwareId, String username, Long excludeId) {
        EndUser exists = endUserMapper.selectOne(
                new LambdaQueryWrapper<EndUser>()
                        .eq(EndUser::getTenantId, tenantId)
                        .eq(EndUser::getSoftwareId, softwareId)
                        .eq(EndUser::getUsername, username)
                        .ne(excludeId != null, EndUser::getId, excludeId));
        if (exists != null) {
            throw new ServiceException(ResultCode.END_USER_USERNAME_EXISTS);
        }
    }

    private void validatePasswordLength(String password) {
        if (password.length() < JicekConstants.END_USER_PASSWORD_MIN_LEN
                || password.length() > JicekConstants.END_USER_PASSWORD_MAX_LEN) {
            throw new ServiceException(ResultCode.PARAM_ERROR,
                    "密码长度 " + JicekConstants.END_USER_PASSWORD_MIN_LEN
                            + "-" + JicekConstants.END_USER_PASSWORD_MAX_LEN + " 字符");
        }
    }

    private EndUser requireOwnedEndUser(Long id, Long tenantId) {
        EndUser user = endUserMapper.selectById(id);
        if (user == null || !user.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.END_USER_NOT_FOUND);
        }
        return user;
    }

    private void changeStatus(Long id, int status) {
        Long tenantId = requireCurrentTenantId();
        EndUser existing = requireOwnedEndUser(id, tenantId);
        LambdaUpdateWrapper<EndUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(EndUser::getId, existing.getId())
                .set(EndUser::getStatus, status)
                .set(EndUser::getUpdateTime, LocalDateTime.now());
        endUserMapper.update(null, uw);
    }

    private String lookupSoftwareName(Long softwareId) {
        if (softwareId == null) return null;
        Software software = softwareMapper.selectById(softwareId);
        return software != null ? software.getName() : null;
    }

    private Map<Long, String> lookupSoftwareNames(List<EndUser> users) {
        Map<Long, String> result = new HashMap<>();
        if (users == null || users.isEmpty()) return result;
        List<Long> softwareIds = users.stream()
                .map(EndUser::getSoftwareId)
                .distinct()
                .toList();
        if (softwareIds.isEmpty()) return result;
        List<Software> softwares = softwareMapper.selectBatchIds(softwareIds);
        for (Software s : softwares) {
            result.put(s.getId(), s.getName());
        }
        return result;
    }

    private EndUserDetailDTO toDetailDTO(EndUser u, String softwareName) {
        EndUserDetailDTO dto = new EndUserDetailDTO();
        dto.setId(u.getId());
        dto.setTenantId(u.getTenantId());
        dto.setSoftwareId(u.getSoftwareId());
        dto.setSoftwareName(softwareName);
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setStatus(u.getStatus());
        dto.setLastLoginTime(u.getLastLoginTime());
        dto.setLastLoginIp(u.getLastLoginIp());
        dto.setRemark(u.getRemark());
        dto.setCreateTime(u.getCreateTime());
        dto.setUpdateTime(u.getUpdateTime());
        return dto;
    }
}
