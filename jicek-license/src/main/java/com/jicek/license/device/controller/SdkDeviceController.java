package com.jicek.license.device.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.device.dto.DeviceBindRequestDTO;
import com.jicek.license.device.dto.DeviceHeartbeatDTO;
import com.jicek.license.device.dto.DeviceUnbindRequestDTO;
import com.jicek.license.device.service.DeviceHeartbeatService;
import com.jicek.license.device.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SDK 设备接口 Controller（终端用户/客户端调用）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 提供设备绑定、换机、心跳接口
 * 所有接口需后续接入 Sa-Token 鉴权（v0.3.1）
 *
 * 安全：
 * - bind: 需卡密 + 设备指纹（RSA 加密传输）
 * - unbind: 需换机码 + 新设备指纹
 * - heartbeat: 需 HMAC-SHA256 签名 + nonce 防重放
 *
 * 注意：signSecret 与 heartbeatInterval 当前从请求参数传入，
 * 后续接入 software 表后从数据库读取（v0.3.1）
 */
@RestController
@RequestMapping("/api/sdk/device")
public class SdkDeviceController {

    private final DeviceService deviceService;
    private final DeviceHeartbeatService heartbeatService;

    public SdkDeviceController(DeviceService deviceService,
                                DeviceHeartbeatService heartbeatService) {
        this.deviceService = deviceService;
        this.heartbeatService = heartbeatService;
    }

    /**
     * 设备绑定
     *
     * @param userId   用户 ID（后续从 Sa-Token 提取）
     * @param req      绑定请求
     * @param request  HttpServletRequest（用于获取真实 IP）
     * @return 换机码（16 位）
     */
    @PostMapping("/bind")
    public R<String> bind(@RequestParam Long userId,
                          @RequestBody DeviceBindRequestDTO req,
                          HttpServletRequest request) {
        if (req.getClientIp() == null || req.getClientIp().isBlank()) {
            req.setClientIp(getClientIp(request));
        }
        String bindCode = deviceService.bindDevice(req, userId);
        return R.ok("设备绑定成功", bindCode);
    }

    /**
     * 设备换机（解绑旧 + 绑定新，同事务）
     *
     * @param req     换机请求
     * @param request HttpServletRequest
     * @return 新换机码
     */
    @PostMapping("/unbind")
    public R<String> unbind(@RequestBody DeviceUnbindRequestDTO req,
                             HttpServletRequest request) {
        if (req.getClientIp() == null || req.getClientIp().isBlank()) {
            req.setClientIp(getClientIp(request));
        }
        String newBindCode = deviceService.unbindAndRebind(req);
        return R.ok("换机成功", newBindCode);
    }

    /**
     * 设备心跳
     *
     * @param dto               心跳数据
     * @param signSecret        软件签名密钥（明文，v0.3.1 改为服务端从 software 表查询）
     * @param heartbeatInterval 心跳间隔（秒，v0.3.1 改为服务端从 software 表查询）
     * @return 下一次心跳间隔 + 服务器时间戳
     */
    @PostMapping("/heartbeat")
    public R<Map<String, Object>> heartbeat(@RequestBody DeviceHeartbeatDTO dto,
                                             @RequestHeader("X-Sign-Secret") String signSecret,
                                             @RequestHeader("X-Heartbeat-Interval") int heartbeatInterval) {
        Map<String, Object> result = heartbeatService.heartbeat(dto, signSecret, heartbeatInterval);
        return R.ok(result);
    }

    /**
     * 获取客户端真实 IP（禁信任客户端 X-Forwarded-For，需配合反向代理白名单）
     * 优先级：X-Real-IP > X-Forwarded-For 第一段 > remoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                int comma = forwarded.indexOf(',');
                ip = comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
