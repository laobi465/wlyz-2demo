package com.jicek.license.agent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理树形节点 DTO（用于多级代理树展示）
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class AgentTreeNode {

    private Long id;

    private Long tenantId;

    private Long parentId;

    private String username;

    private String realName;

    private String contact;

    private BigDecimal balance;

    private BigDecimal frozenBalance;

    private BigDecimal totalEarnings;

    private BigDecimal commissionRate;

    private Integer maxSubLevel;

    private Integer status;

    private Integer level;

    /** 下级代理数量（直接） */
    private Integer subCount;

    /** 子节点 */
    private List<AgentTreeNode> children = new ArrayList<>();

    private String createTime;
}
