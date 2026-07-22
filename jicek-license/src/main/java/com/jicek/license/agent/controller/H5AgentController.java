package com.jicek.license.agent.controller;

import com.jicek.license.agent.dto.AgentRegisterDTO;
import com.jicek.license.agent.service.AgentService;
import com.jicek.license.common.result.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * H5 代理注册 Controller（v0.13.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 公开接口，不走 JWT 鉴权。
 *
 * 接口：
 *  POST /api/h5/agent/register  代理注册（需邀请码）
 */
@RestController
@RequestMapping("/api/h5/agent")
public class H5AgentController {

    private final AgentService agentService;

    public H5AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 代理注册（邀请码注册）
     */
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody AgentRegisterDTO dto) {
        return R.ok(agentService.register(dto));
    }
}
