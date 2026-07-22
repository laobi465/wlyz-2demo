package com.jicek.license.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.agent.dto.AgentSaveDTO;
import com.jicek.license.agent.dto.AgentTreeNode;
import com.jicek.license.agent.entity.Agent;
import com.jicek.license.agent.mapper.AgentMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代理业务服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 * 1. 代理 CRUD（创建/查询/更新/封禁/解封）
 * 2. 多级代理树形查询（递归构建，限制最大深度 AGENT_MAX_LEVEL）
 * 3. 余额调整（充值/扣款，必须事务 + 流水）
 * 4. 上级代理校验 + 层级计算
 *
 * 安全铁律：
 * - 密码使用 BCrypt 加密（铁律 04），禁明文
 * - 余额变动必须 @Transactional（铁律 06）
 * - 代理树深度 ≤ AGENT_MAX_LEVEL（防深度爆炸）
 */
@Slf4j
@Service
public class AgentService {

    private final AgentMapper agentMapper;

    public AgentService(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    /**
     * 创建代理
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentSaveDTO dto) {
        // 1. 校验用户名唯一
        Long exists = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getTenantId, dto.getTenantId())
                        .eq(Agent::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new ServiceException(ResultCode.AGENT_USERNAME_EXISTS);
        }

        // 2. 校验上级代理 + 计算层级
        int level = 1;
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Agent parent = getAgentById(dto.getTenantId(), dto.getParentId());
            if (parent.getStatus() == JicekConstants.AGENT_STATUS_BANNED) {
                throw new ServiceException(ResultCode.AGENT_BANNED, "上级代理已封禁，无法挂靠");
            }
            if (parent.getMaxSubLevel() == null || parent.getMaxSubLevel() <= 0) {
                throw new ServiceException(ResultCode.AGENT_PARENT_INVALID,
                        "上级代理无权发展下级");
            }
            level = parent.getLevel() + 1;
            if (level > JicekConstants.AGENT_MAX_LEVEL) {
                throw new ServiceException(ResultCode.AGENT_PARENT_INVALID,
                        "代理层级超过最大深度 " + JicekConstants.AGENT_MAX_LEVEL);
            }
        }

        // 3. 密码 BCrypt 加密（Hutool 实现）
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new ServiceException(ResultCode.FAIL, "密码至少 6 位");
        }
        String passwordHash = BCrypt.hashpw(dto.getPassword());

        // 4. 写入
        LocalDateTime now = LocalDateTime.now();
        Agent agent = new Agent();
        agent.setTenantId(dto.getTenantId());
        agent.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        agent.setUsername(dto.getUsername());
        agent.setPasswordHash(passwordHash);
        agent.setRealName(dto.getRealName());
        agent.setContact(dto.getContact());
        agent.setBalance(BigDecimal.ZERO);
        agent.setFrozenBalance(BigDecimal.ZERO);
        agent.setTotalEarnings(BigDecimal.ZERO);
        agent.setTotalWithdraw(BigDecimal.ZERO);
        agent.setCommissionRate(dto.getCommissionRate());
        agent.setMaxSubLevel(dto.getMaxSubLevel() == null ? 0 : dto.getMaxSubLevel());
        agent.setStatus(JicekConstants.AGENT_STATUS_NORMAL);
        agent.setLevel(level);
        agent.setRemark(dto.getRemark());
        agent.setCreateTime(now);
        agent.setUpdateTime(now);
        agentMapper.insert(agent);

        log.info("代理创建成功: tenantId={}, agentId={}, username={}, parentId={}, level={}",
                dto.getTenantId(), agent.getId(), agent.getUsername(), agent.getParentId(), level);
        return agent.getId();
    }

    /**
     * 更新代理（不含密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(AgentSaveDTO dto) {
        Agent agent = getAgentById(dto.getTenantId(), dto.getId());

        // 用户名变更需校验唯一
        if (!agent.getUsername().equals(dto.getUsername())) {
            Long exists = agentMapper.selectCount(
                    new LambdaQueryWrapper<Agent>()
                            .eq(Agent::getTenantId, dto.getTenantId())
                            .eq(Agent::getUsername, dto.getUsername())
                            .ne(Agent::getId, dto.getId()));
            if (exists != null && exists > 0) {
                throw new ServiceException(ResultCode.AGENT_USERNAME_EXISTS);
            }
            agent.setUsername(dto.getUsername());
        }

        // 上级变更需重新校验层级
        if (dto.getParentId() != null && !dto.getParentId().equals(agent.getParentId())) {
            if (dto.getParentId() > 0) {
                if (dto.getParentId().equals(agent.getId())) {
                    throw new ServiceException(ResultCode.AGENT_PARENT_INVALID, "不能将自己设为上级");
                }
                Agent newParent = getAgentById(dto.getTenantId(), dto.getParentId());
                if (newParent.getStatus() == JicekConstants.AGENT_STATUS_BANNED) {
                    throw new ServiceException(ResultCode.AGENT_BANNED, "上级代理已封禁");
                }
                // 防环：检查新上级是否在当前代理的子树中
                if (isDescendant(dto.getTenantId(), agent.getId(), dto.getParentId())) {
                    throw new ServiceException(ResultCode.AGENT_PARENT_INVALID,
                            "不能将下级设为上级（防环）");
                }
                agent.setLevel(newParent.getLevel() + 1);
            } else {
                agent.setLevel(1);
            }
            agent.setParentId(dto.getParentId());
        }

        // 密码（可选更新）
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 6) {
                throw new ServiceException(ResultCode.FAIL, "密码至少 6 位");
            }
            agent.setPasswordHash(BCrypt.hashpw(dto.getPassword()));
        }

        agent.setRealName(dto.getRealName());
        agent.setContact(dto.getContact());
        agent.setCommissionRate(dto.getCommissionRate());
        agent.setMaxSubLevel(dto.getMaxSubLevel() == null ? 0 : dto.getMaxSubLevel());
        agent.setRemark(dto.getRemark());
        agent.setUpdateTime(LocalDateTime.now());
        agentMapper.updateById(agent);

        log.info("代理更新成功: tenantId={}, agentId={}", dto.getTenantId(), agent.getId());
    }

    /**
     * 封禁代理（不删除，仅状态置 0）
     */
    @Transactional(rollbackFor = Exception.class)
    public void banAgent(Long tenantId, Long agentId) {
        Agent agent = getAgentById(tenantId, agentId);
        agent.setStatus(JicekConstants.AGENT_STATUS_BANNED);
        agent.setUpdateTime(LocalDateTime.now());
        agentMapper.updateById(agent);
        log.info("代理封禁: tenantId={}, agentId={}", tenantId, agentId);
    }

    /**
     * 解封代理
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbanAgent(Long tenantId, Long agentId) {
        Agent agent = getAgentById(tenantId, agentId);
        agent.setStatus(JicekConstants.AGENT_STATUS_NORMAL);
        agent.setUpdateTime(LocalDateTime.now());
        agentMapper.updateById(agent);
        log.info("代理解封: tenantId={}, agentId={}", tenantId, agentId);
    }

    /**
     * 开发者给代理充值（余额 += amount）
     * 注意：充值仅增加 balance，不产生分润流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long tenantId, Long agentId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResultCode.FAIL, "充值金额必须 > 0");
        }
        Agent agent = getAgentById(tenantId, agentId);
        agent.setBalance(agent.getBalance().add(amount));
        agent.setUpdateTime(LocalDateTime.now());
        agentMapper.updateById(agent);
        log.info("代理充值: tenantId={}, agentId={}, amount={}, remark={}",
                tenantId, agentId, amount, remark);
    }

    /**
     * 代理余额扣减（制卡场景）
     * @return 扣减后的余额
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal deductBalance(Long tenantId, Long agentId, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResultCode.FAIL, "扣款金额必须 > 0");
        }
        Agent agent = getAgentById(tenantId, agentId);
        if (agent.getStatus() == JicekConstants.AGENT_STATUS_BANNED) {
            throw new ServiceException(ResultCode.AGENT_BANNED);
        }
        if (agent.getBalance().compareTo(amount) < 0) {
            throw new ServiceException(ResultCode.AGENT_BALANCE_INSUFFICIENT,
                    "余额不足：当前 " + agent.getBalance() + "，需扣 " + amount);
        }
        agent.setBalance(agent.getBalance().subtract(amount));
        agent.setUpdateTime(LocalDateTime.now());
        agentMapper.updateById(agent);
        log.info("代理余额扣减: tenantId={}, agentId={}, amount={}, reason={}, newBalance={}",
                tenantId, agentId, amount, reason, agent.getBalance());
        return agent.getBalance();
    }

    /**
     * 分页查询代理（扁平）
     */
    public Page<Agent> pageAgents(Long tenantId, Long parentId, Integer status,
                                   int page, int size) {
        Page<Agent> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<Agent>()
                .eq(Agent::getTenantId, tenantId)
                .eq(parentId != null, Agent::getParentId, parentId)
                .eq(status != null, Agent::getStatus, status)
                .orderByDesc(Agent::getCreateTime);
        return agentMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 构建代理树（从指定 parentId 开始，递归到最大深度）
     */
    public List<AgentTreeNode> buildAgentTree(Long tenantId, Long rootParentId) {
        // 一次查询租户下所有代理（性能考虑：单租户代理数有限）
        List<Agent> all = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getTenantId, tenantId)
                        .orderByAsc(Agent::getLevel, Agent::getCreateTime));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 parentId 分组
        Map<Long, List<Agent>> byParent = all.stream()
                .collect(Collectors.groupingBy(a -> a.getParentId() == null ? 0L : a.getParentId()));

        // 递归构建
        Long root = rootParentId == null ? 0L : rootParentId;
        return buildChildren(root, byParent);
    }

    private List<AgentTreeNode> buildChildren(Long parentId, Map<Long, List<Agent>> byParent) {
        List<Agent> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        List<AgentTreeNode> result = new ArrayList<>(children.size());
        for (Agent a : children) {
            AgentTreeNode node = toTreeNode(a);
            List<AgentTreeNode> subChildren = buildChildren(a.getId(), byParent);
            node.setChildren(subChildren);
            node.setSubCount(subChildren.size());
            result.add(node);
        }
        return result;
    }

    private AgentTreeNode toTreeNode(Agent a) {
        AgentTreeNode node = new AgentTreeNode();
        node.setId(a.getId());
        node.setTenantId(a.getTenantId());
        node.setParentId(a.getParentId());
        node.setUsername(a.getUsername());
        node.setRealName(a.getRealName());
        node.setContact(a.getContact());
        node.setBalance(a.getBalance());
        node.setFrozenBalance(a.getFrozenBalance());
        node.setTotalEarnings(a.getTotalEarnings());
        node.setCommissionRate(a.getCommissionRate());
        node.setMaxSubLevel(a.getMaxSubLevel());
        node.setStatus(a.getStatus());
        node.setLevel(a.getLevel());
        node.setCreateTime(a.getCreateTime() == null ? null : a.getCreateTime().toString());
        return node;
    }

    /**
     * 按 ID 查询代理（含租户校验）
     */
    public Agent getAgentById(Long tenantId, Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.AGENT_NOT_FOUND);
        }
        return agent;
    }

    /**
     * 检查 candidateId 是否是 ancestorId 的后代（防环）
     */
    private boolean isDescendant(Long tenantId, Long ancestorId, Long candidateId) {
        // 从 candidateId 向上找，若遇到 ancestorId 则为后代
        Long cursor = candidateId;
        int safety = 0;
        while (cursor != null && cursor > 0 && safety < JicekConstants.AGENT_MAX_LEVEL + 5) {
            Agent cur = agentMapper.selectById(cursor);
            if (cur == null || !cur.getTenantId().equals(tenantId)) {
                return false;
            }
            if (ancestorId.equals(cur.getParentId())) {
                return true;
            }
            cursor = cur.getParentId();
            safety++;
        }
        return false;
    }

    /**
     * 收集某代理的所有后代 ID（用于封禁连带等场景）
     */
    public List<Long> collectDescendantIds(Long tenantId, Long agentId) {
        List<Agent> all = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getTenantId, tenantId)
                        .select(Agent::getId, Agent::getParentId));
        Map<Long, List<Long>> byParent = new HashMap<>();
        for (Agent a : all) {
            Long pid = a.getParentId() == null ? 0L : a.getParentId();
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(a.getId());
        }
        List<Long> result = new ArrayList<>();
        collectChildren(agentId, byParent, result, 0);
        return result;
    }

    private void collectChildren(Long parentId, Map<Long, List<Long>> byParent,
                                  List<Long> result, int depth) {
        if (depth > JicekConstants.AGENT_MAX_LEVEL) {
            return;
        }
        List<Long> children = byParent.get(parentId);
        if (children == null) return;
        for (Long cid : children) {
            result.add(cid);
            collectChildren(cid, byParent, result, depth + 1);
        }
    }
}
