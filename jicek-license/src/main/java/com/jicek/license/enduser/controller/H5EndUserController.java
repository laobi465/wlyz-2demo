package com.jicek.license.enduser.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.enduser.dto.H5EndUserLoginDTO;
import com.jicek.license.enduser.dto.H5EndUserLoginResultDTO;
import com.jicek.license.enduser.service.EndUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * H5 终端用户账号 Controller（v0.14.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 与卡密登录并存：终端用户通过 appKey + username + password 登录，
 * 登录后获得 H5 token（X-H5-Token），后续访问其它 H5 接口与卡密登录用户共享鉴权。
 *
 * 接口：
 *  POST /api/h5/end-user/login  账号密码登录（公开，无需 X-H5-Token）
 *
 * 路径放行：WebMvcConfig 中 H5AuthInterceptor.excludePathPatterns 已加入本路径。
 */
@RestController
@RequestMapping("/api/h5/end-user")
public class H5EndUserController {

    private final EndUserService endUserService;

    public H5EndUserController(EndUserService endUserService) {
        this.endUserService = endUserService;
    }

    /**
     * 终端用户账号密码登录
     *
     * @param dto 登录参数（appKey + username + password）
     * @param req HTTP 请求（取 IP / User-Agent）
     * @return 登录结果（h5Token + 用户信息 + 过期时间）
     */
    @PostMapping("/login")
    public R<H5EndUserLoginResultDTO> login(@Valid @RequestBody H5EndUserLoginDTO dto,
                                            HttpServletRequest req) {
        String ip = getClientIp(req);
        String ua = req.getHeader("User-Agent");
        return R.ok(endUserService.login(dto, ip, ua));
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
