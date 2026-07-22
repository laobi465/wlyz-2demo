package com.jicek.license.auth.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jicek.license.auth.dto.ChangePasswordDTO;
import com.jicek.license.auth.dto.LoginDTO;
import com.jicek.license.auth.dto.LoginResultDTO;
import com.jicek.license.auth.dto.UserInfoDTO;
import com.jicek.license.auth.entity.AdminUser;
import com.jicek.license.auth.entity.DevUser;
import com.jicek.license.auth.interceptor.AuthContext;
import com.jicek.license.auth.mapper.AdminUserMapper;
import com.jicek.license.auth.mapper.DevUserMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 鉴权服务（登录 / 修改密码 / 获取当前用户）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全规范（铁律 04/06/13）：
 * 1. 密码 BCrypt 哈希校验（cn.hutool.crypto.digest.BCrypt，cost=10）
 * 2. 登录失败统一返回 AUTH_PASSWORD_ERROR（不区分用户名/密码错误，防枚举）
 * 3. 封禁账号禁止登录（AUTH_USER_BANNED）
 * 4. 登录成功更新 last_login_time / last_login_ip（审计）
 * 5. 修改密码需校验原密码 + 新密码长度 ≥ 8
 */
@Slf4j
@Service
public class AuthService {

    private final DevUserMapper devUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final JwtService jwtService;

    public AuthService(DevUserMapper devUserMapper, AdminUserMapper adminUserMapper, JwtService jwtService) {
        this.devUserMapper = devUserMapper;
        this.adminUserMapper = adminUserMapper;
        this.jwtService = jwtService;
    }

    /**
     * 开发者登录
     *
     * @param dto    登录参数（tenantId + username + password）
     * @param loginIp 登录 IP（审计用）
     */
    public LoginResultDTO devLogin(LoginDTO dto, String loginIp) {
        if (dto.getTenantId() == null) {
            throw new ServiceException(ResultCode.AUTH_ROLE_INVALID, "租户ID不能为空");
        }

        // 查询用户
        DevUser user = devUserMapper.selectOne(new LambdaQueryWrapper<DevUser>()
                .eq(DevUser::getTenantId, dto.getTenantId())
                .eq(DevUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new ServiceException(ResultCode.AUTH_PASSWORD_ERROR);
        }
        // 封禁校验
        if (user.getStatus() != null && user.getStatus() == JicekConstants.USER_STATUS_BANNED) {
            throw new ServiceException(ResultCode.AUTH_USER_BANNED);
        }
        // BCrypt 校验
        if (!BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.AUTH_PASSWORD_ERROR);
        }

        // 更新登录信息
        LambdaUpdateWrapper<DevUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(DevUser::getId, user.getId())
                .set(DevUser::getLastLoginTime, LocalDateTime.now())
                .set(DevUser::getLastLoginIp, loginIp);
        devUserMapper.update(null, uw);

        // 生成 token
        String token = jwtService.generateToken(user.getId(), JicekConstants.ROLE_DEV,
                user.getTenantId(), user.getUsername());

        log.info("开发者登录成功: tenantId={}, userId={}, username={}, ip={}",
                user.getTenantId(), user.getId(), user.getUsername(), loginIp);

        return buildLoginResult(token, user.getId(), JicekConstants.ROLE_DEV,
                user.getTenantId(), user.getUsername(), user.getNickname());
    }

    /**
     * 管理员登录
     *
     * @param dto    登录参数（username + password，tenantId 忽略）
     * @param loginIp 登录 IP
     */
    public LoginResultDTO adminLogin(LoginDTO dto, String loginIp) {
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new ServiceException(ResultCode.AUTH_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == JicekConstants.USER_STATUS_BANNED) {
            throw new ServiceException(ResultCode.AUTH_USER_BANNED);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.AUTH_PASSWORD_ERROR);
        }

        LambdaUpdateWrapper<AdminUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(AdminUser::getId, user.getId())
                .set(AdminUser::getLastLoginTime, LocalDateTime.now())
                .set(AdminUser::getLastLoginIp, loginIp);
        adminUserMapper.update(null, uw);

        String token = jwtService.generateToken(user.getId(), JicekConstants.ROLE_ADMIN,
                null, user.getUsername());

        log.info("管理员登录成功: userId={}, username={}, ip={}",
                user.getId(), user.getUsername(), loginIp);

        return buildLoginResult(token, user.getId(), JicekConstants.ROLE_ADMIN,
                null, user.getUsername(), user.getNickname());
    }

    /**
     * 获取当前登录用户信息（从 AuthContext 读取，再查库补全）
     */
    public UserInfoDTO currentUser() {
        AuthContext.AuthUser ctx = AuthContext.require();
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(ctx.getUserId());
        dto.setRole(ctx.getRole());

        if (ctx.getRole() == JicekConstants.ROLE_DEV) {
            DevUser user = devUserMapper.selectById(ctx.getUserId());
            if (user == null) {
                throw new ServiceException(ResultCode.AUTH_USER_NOT_FOUND);
            }
            dto.setTenantId(user.getTenantId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setEmail(user.getEmail());
            dto.setStatus(user.getStatus());
        } else if (ctx.getRole() == JicekConstants.ROLE_ADMIN) {
            AdminUser user = adminUserMapper.selectById(ctx.getUserId());
            if (user == null) {
                throw new ServiceException(ResultCode.AUTH_USER_NOT_FOUND);
            }
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setStatus(user.getStatus());
        } else {
            throw new ServiceException(ResultCode.AUTH_ROLE_INVALID);
        }
        return dto;
    }

    /**
     * 修改密码（需已登录，校验原密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        AuthContext.AuthUser ctx = AuthContext.require();

        if (dto.getNewPassword().length() < 8) {
            throw new ServiceException(ResultCode.AUTH_PASSWORD_TOO_SHORT);
        }

        if (ctx.getRole() == JicekConstants.ROLE_DEV) {
            DevUser user = devUserMapper.selectById(ctx.getUserId());
            if (user == null) {
                throw new ServiceException(ResultCode.AUTH_USER_NOT_FOUND);
            }
            if (!BCrypt.checkpw(dto.getOldPassword(), user.getPasswordHash())) {
                throw new ServiceException(ResultCode.AUTH_OLD_PASSWORD_ERROR);
            }
            String newHash = BCrypt.hashpw(dto.getNewPassword());
            LambdaUpdateWrapper<DevUser> uw = new LambdaUpdateWrapper<>();
            uw.eq(DevUser::getId, user.getId())
                    .set(DevUser::getPasswordHash, newHash)
                    .set(DevUser::getUpdateTime, LocalDateTime.now());
            devUserMapper.update(null, uw);
            log.info("开发者修改密码: userId={}", ctx.getUserId());
        } else if (ctx.getRole() == JicekConstants.ROLE_ADMIN) {
            AdminUser user = adminUserMapper.selectById(ctx.getUserId());
            if (user == null) {
                throw new ServiceException(ResultCode.AUTH_USER_NOT_FOUND);
            }
            if (!BCrypt.checkpw(dto.getOldPassword(), user.getPasswordHash())) {
                throw new ServiceException(ResultCode.AUTH_OLD_PASSWORD_ERROR);
            }
            String newHash = BCrypt.hashpw(dto.getNewPassword());
            LambdaUpdateWrapper<AdminUser> uw = new LambdaUpdateWrapper<>();
            uw.eq(AdminUser::getId, user.getId())
                    .set(AdminUser::getPasswordHash, newHash)
                    .set(AdminUser::getUpdateTime, LocalDateTime.now());
            adminUserMapper.update(null, uw);
            log.info("管理员修改密码: userId={}", ctx.getUserId());
        } else {
            throw new ServiceException(ResultCode.AUTH_ROLE_INVALID);
        }
    }

    private LoginResultDTO buildLoginResult(String token, Long userId, int role,
                                             Long tenantId, String username, String nickname) {
        LoginResultDTO result = new LoginResultDTO();
        result.setToken(token);
        result.setExpiresIn(jwtService.getExpiresInSeconds());
        result.setUserId(userId);
        result.setRole(role);
        result.setTenantId(tenantId);
        result.setUsername(username);
        result.setNickname(nickname);
        return result;
    }
}
