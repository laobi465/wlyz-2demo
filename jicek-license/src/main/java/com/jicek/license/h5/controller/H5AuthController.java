package com.jicek.license.h5.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.h5.auth.H5AuthContext;
import com.jicek.license.h5.dto.H5CardDetailDTO;
import com.jicek.license.h5.dto.H5LoginRequestDTO;
import com.jicek.license.h5.dto.H5LoginResultDTO;
import com.jicek.license.h5.service.H5AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * H5 终端用户鉴权 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * /api/h5/auth/** 不走 JWT，登录接口公开，其余接口需 X-H5-Token。
 *
 * 接口：
 *  POST /api/h5/auth/login    卡密登录（返回 h5Token）
 *  GET  /api/h5/auth/my-card  我的卡密详情（需 X-H5-Token）
 *  POST /api/h5/auth/logout   退出登录（需 X-H5-Token）
 */
@RestController
@RequestMapping("/api/h5/auth")
public class H5AuthController {

    private final H5AuthService h5AuthService;

    public H5AuthController(H5AuthService h5AuthService) {
        this.h5AuthService = h5AuthService;
    }

    @PostMapping("/login")
    public R<H5LoginResultDTO> login(@Valid @RequestBody H5LoginRequestDTO dto, HttpServletRequest req) {
        String ip = getClientIp(req);
        String ua = req.getHeader("User-Agent");
        return R.ok(h5AuthService.login(dto, ip, ua));
    }

    @GetMapping("/my-card")
    public R<H5CardDetailDTO> myCard() {
        Long cardKeyId = H5AuthContext.currentCardKeyId();
        return R.ok(h5AuthService.getMyCardDetail(cardKeyId));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("X-H5-Token") String token) {
        h5AuthService.logout(token);
        return R.ok();
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
