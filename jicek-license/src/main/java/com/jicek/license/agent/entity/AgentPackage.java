package com.jicek.license.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理套餐实体（代理可售的卡类 + 制卡价）
 * 作者: 极策k  日期: 2026-07-22
 *
 * agentId=0 表示该租户下所有代理的默认套餐
 * agentPrice 必须 ≤ 卡类零售价（校验在 Service 层）
 */
@Data
@TableName("jicek_agent_package")
public class AgentPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 0 表示所有代理默认套餐 */
    private Long agentId;

    private Long softwareId;

    private Long cardTypeId;

    /** 代理制卡价 */
    private BigDecimal agentPrice;

    /** 0 禁用 1 启用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
