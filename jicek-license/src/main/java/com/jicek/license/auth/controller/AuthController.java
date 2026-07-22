package com.jicek.license.auth.controller;

import com.jicek.license.auth.dto.ChangePasswordDTO;
import com.jicek.license.auth.dto.LoginDTO;
import com.jicek.license.auth.dto.LoginResultDTO;
import com.jicek.license.auth.dto.UserInfoDTO;
import com.jicek.license.auth.interceptor.AuthRequired;
import com.jicek.license.auth.service.AuthService;
import com.jicek.license.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 鉴权 Controller（登录 / 当前用户 / 修改密码）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 路由前缀：/api/auth
 *
 * 接口：
 *  POST /dev/login       开发者登录（无需 token）
 *  POST /admin/login     管理员登录（无需 token）
 *  GET  /me              获取当前登录用户（需 token，任意角色）
 *  POST /change-password 修改密码（需 token，任意角色）
 *
 * 安全说明（铁律 04/06/13）：
 *  - 登录接口不鉴权（@AuthRequired 未标注 → 拦截器放行）
 *  - /me 和 /change-password 标注 @AuthRequired，从 AuthContext 读取身份
 *  - 登录 IP 从请求头穿透代理获取（X-Forwarded-For / X-Real-IP）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 开发者登录
     */
    @PostMapping("/dev/login")
    public R<LoginResultDTO> devLogin(@RequestBody @Valid LoginDTO dto,
                                       HttpServletRequest request) {
        return R.ok(authService.devLogin(dto, getClientIp(request)));
    }

    /**
     * 管理员登录
     */
    @PostMapping("/admin/login")
    public R<LoginResultDTO> adminLogin(@RequestBody @Valid LoginDTO dto,
                                         HttpServletRequest request) {
        return R.ok(authService.adminLogin(dto, getClientIp(request)));
    }

    /**
     * 获取当前登录用户信息
     */
    @AuthRequired
    @GetMapping("/me")
    public R<UserInfoDTO> me() {
        return R.ok(authService.currentUser());
    }

    /**
     * 修改密码
     */
    @AuthRequired
    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return R.ok();
    }

    /**
     * 获取客户端真实 IP（穿透代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
