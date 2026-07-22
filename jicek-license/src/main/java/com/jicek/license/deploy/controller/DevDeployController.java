package com.jicek.license.deploy.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.deploy.dto.DeployStatusDTO;
import com.jicek.license.deploy.dto.ManualDeployDTO;
import com.jicek.license.deploy.dto.WebhookResultDTO;
import com.jicek.license.deploy.entity.DeployLog;
import com.jicek.license.deploy.service.DeployService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 开发者部署管理 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 对应 UI-DESIGN.md 6.2 节「系统设置 > 部署管理」+ PROJECT.md 第 7 节自动更新系统：
 *  1. GitHub Webhook 自动触发（POST /webhook）
 *  2. 管理员后台手动触发（POST /manual）
 *  3. 当前部署状态查询（GET /status）
 *  4. 部署审计日志分页（GET /log/page）
 *
 * 路由前缀：/api/dev/deploy
 * 安全：Webhook HMAC-SHA256 签名验证 + Redisson 分布式锁 + 审计日志不可篡改
 */
@RestController
@RequestMapping("/api/dev/deploy")
public class DevDeployController {

    private final DeployService deployService;

    public DevDeployController(DeployService deployService) {
        this.deployService = deployService;
    }

    /**
     * GitHub Webhook 接收入口
     *
     * GitHub 配置：Payload URL = https://your-domain/api/dev/deploy/webhook
     *             Content type = application/json
     *             Secret = 与 GITHUB_WEBHOOK_SECRET 环境变量一致
     *             Events = Just the push event
     */
    @PostMapping("/webhook")
    public R<WebhookResultDTO> webhook(HttpServletRequest request) {
        return R.ok(deployService.handleWebhook(request));
    }

    /**
     * 管理员手动触发部署
     */
    @PostMapping("/manual")
    public R<WebhookResultDTO> manualDeploy(
            @RequestBody @Valid ManualDeployDTO dto,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        return R.ok(deployService.manualDeploy(dto.getTenantId(), dto.getBranch(), ip));
    }

    /**
     * 查询当前部署状态
     */
    @GetMapping("/status")
    public R<DeployStatusDTO> status() {
        return R.ok(deployService.getStatus());
    }

    /**
     * 部署审计日志分页
     *
     * @param tenantId      租户 ID
     * @param status        状态（0进行中 1成功 2失败 3已回滚）
     * @param triggerSource 触发来源（webhook/manual）
     * @param current       当前页（从 1 开始）
     * @param size          每页条数
     */
    @GetMapping("/log/page")
    public R<Map<String, Object>> logPage(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String triggerSource,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        // 注：为简化实现，total 字段使用查询结果 size 估算（与现有 Controller 风格一致）
        // 实际分页可用 MyBatis-Plus Page，此处保留接口一致性
        java.util.List<DeployLog> records = deployService.logPage(
                tenantId, status, triggerSource, current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("current", current);
        data.put("size", size);
        // 简化：当返回满页时 total = current * size + 1（提示前端有下一页），否则 total = (current-1)*size + records.size()
        data.put("total", records.size() == size
                ? (long) current * size + 1
                : (long) (current - 1) * size + records.size());
        return R.ok(data);
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
