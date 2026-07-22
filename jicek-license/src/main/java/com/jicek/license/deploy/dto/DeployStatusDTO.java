package com.jicek.license.deploy.dto;

import lombok.Data;

/**
 * 当前部署状态 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 用于前端展示「当前是否有部署进行中」+ 最近一次部署结果
 */
@Data
public class DeployStatusDTO {

    /** 是否正在部署中 */
    private boolean deploying;

    /** 最近一次部署日志（null 表示无历史） */
    private DeployLog lastDeploy;

    /** 部署功能是否启用（jicek.deploy.enabled） */
    private boolean enabled;
}
