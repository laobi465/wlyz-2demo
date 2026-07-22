package com.jicek.license.auth.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.auth.dto.DevUserDetailDTO;
import com.jicek.license.auth.dto.DevUserResetPasswordDTO;
import com.jicek.license.auth.entity.DevUser;
import com.jicek.license.auth.mapper.DevUserMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 开发者用户（租户）管理服务（v0.15.0，管理员端）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 *  - 管理员后台查询/封禁/解封/重置密码开发者账号
 *  - 不限 tenantId（管理员可管理全部租户）
 *
 * 安全铁律（04/06/13/09）：
 *  - 密码 BCrypt 哈希（cn.hutool.crypto.digest.BCrypt.hashpw，单参 Hutool 风格）
 *  - 返回 DTO 不含 passwordHash（防泄露）
 *  - 状态码走 JicekConstants 常量（USER_STATUS_BANNED / USER_STATUS_NORMAL）
 */
@Slf4j
@Service
public class DevUserService {

    private final DevUserMapper devUserMapper;

    public DevUserService(DevUserMapper devUserMapper) {
        this.devUserMapper = devUserMapper;
    }

    /**
     * 分页查询所有开发者账号（管理员视角，不限租户）
     *
     * @param current  页码
     * @param size     每页大小
     * @param tenantId 租户ID（可选筛选）
     * @param username 用户名模糊（可选）
     * @param status   状态（可选）
     */
    public Page<DevUserDetailDTO> page(long current, long size, Long tenantId,
                                         String username, Integer status) {
        LambdaQueryWrapper<DevUser> qw = new LambdaQueryWrapper<>();
        qw.eq(tenantId != null, DevUser::getTenantId, tenantId)
                .like(username != null && !username.isBlank(), DevUser::getUsername, username)
                .eq(status != null, DevUser::getStatus, status)
                .orderByDesc(DevUser::getCreateTime);
        Page<DevUser> page = devUserMapper.selectPage(new Page<>(current, size), qw);

        Page<DevUserDetailDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDetailDTO).toList());
        return result;
    }

    /**
     * 开发者详情
     */
    public DevUserDetailDTO get(Long id) {
        return toDetailDTO(requireDevUser(id));
    }

    /**
     * 封禁开发者（status → 0）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ban(Long id) {
        changeStatus(id, JicekConstants.USER_STATUS_BANNED);
        log.info("【开发者管理】封禁成功 id={}", id);
    }

    /**
     * 解封开发者（status → 1）
     */
    @Transactional(rollbackFor = Exception.class)
    public void unban(Long id) {
        changeStatus(id, JicekConstants.USER_STATUS_NORMAL);
        log.info("【开发者管理】解封成功 id={}", id);
    }

    /**
     * 重置密码（管理员调用，无需原密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(DevUserResetPasswordDTO dto) {
        DevUser existing = requireDevUser(dto.getId());

        LambdaUpdateWrapper<DevUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(DevUser::getId, existing.getId())
                .set(DevUser::getPasswordHash, BCrypt.hashpw(dto.getNewPassword()))
                .set(DevUser::getUpdateTime, LocalDateTime.now());
        devUserMapper.update(null, uw);
        log.info("【开发者管理】重置密码成功 id={}", dto.getId());
    }

    /* ============ 内部工具 ============ */

    private DevUser requireDevUser(Long id) {
        DevUser user = devUserMapper.selectById(id);
        if (user == null) {
            throw new ServiceException(ResultCode.AUTH_USER_NOT_FOUND);
        }
        return user;
    }

    private void changeStatus(Long id, int status) {
        DevUser existing = requireDevUser(id);
        LambdaUpdateWrapper<DevUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(DevUser::getId, existing.getId())
                .set(DevUser::getStatus, status)
                .set(DevUser::getUpdateTime, LocalDateTime.now());
        devUserMapper.update(null, uw);
    }

    private DevUserDetailDTO toDetailDTO(DevUser u) {
        DevUserDetailDTO dto = new DevUserDetailDTO();
        dto.setId(u.getId());
        dto.setTenantId(u.getTenantId());
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setEmail(u.getEmail());
        dto.setStatus(u.getStatus());
        dto.setLastLoginTime(u.getLastLoginTime());
        dto.setLastLoginIp(u.getLastLoginIp());
        dto.setRemark(u.getRemark());
        dto.setCreateTime(u.getCreateTime());
        dto.setUpdateTime(u.getUpdateTime());
        return dto;
    }
}
