package com.jicek.license.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.agent.dto.WithdrawApplyDTO;
import com.jicek.license.agent.dto.WithdrawAuditDTO;
import com.jicek.license.agent.entity.Agent;
import com.jicek.license.agent.entity.Withdraw;
import com.jicek.license.agent.mapper.AgentMapper;
import com.jicek.license.agent.mapper.WithdrawMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 提现业务服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 状态机（不可逆，铁律 06）：
 *   0 待审核 → 1 已通过（approve：等待打款）
 *   0 待审核 → 2 已拒绝（reject：余额退回可用）
 *   1 已通过 → 3 已打款（payout：实际打款完成）
 *   1 已通过 → 4 已失败（fail：打款失败，余额退回可用）
 *
 * 资金流转：
 * - 申请提现：balance -= amount, frozenBalance += amount（同事务）
 * - 审核拒绝/打款失败：frozenBalance -= amount, balance += amount（同事务）
 * - 打款成功：frozenBalance -= amount, totalWithdraw += actualAmount（同事务）
 *
 * 安全铁律：
 * - 申请/审核必须 @Transactional（铁律 06，禁伪异步）
 * - 资金流水永不可删（铁律 04）
 * - 金额 BigDecimal，禁 float/double
 * - 提现申请需 Redisson 分布式锁防并发
 */
@Slf4j
@Service
public class WithdrawService {

    private final WithdrawMapper withdrawMapper;
    private final AgentMapper agentMapper;
    private final AgentService agentService;

    public WithdrawService(WithdrawMapper withdrawMapper,
                            AgentMapper agentMapper,
                            AgentService agentService) {
        this.withdrawMapper = withdrawMapper;
        this.agentMapper = agentMapper;
        this.agentService = agentService;
    }

    /**
     * 代理申请提现
     * <p>
     * 资金流转：balance → frozenBalance
     *
     * @return 提现申请 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long applyWithdraw(WithdrawApplyDTO dto) {
        // 1. 校验金额
        if (dto.getAmount() == null
                || dto.getAmount().compareTo(JicekConstants.WITHDRAW_MIN_AMOUNT) < 0) {
            throw new ServiceException(ResultCode.WITHDRAW_AMOUNT_INVALID,
                    "提现金额必须 ≥ " + JicekConstants.WITHDRAW_MIN_AMOUNT + " 元");
        }

        // 2. 校验收款账号
        if (dto.getPayAccount() == null || dto.getPayAccount().isBlank()) {
            throw new ServiceException(ResultCode.WITHDRAW_ACCOUNT_INVALID);
        }

        // 3. 查询代理 + 校验状态
        Agent agent = agentService.getAgentById(dto.getTenantId(), dto.getAgentId());
        if (agent.getStatus() == JicekConstants.AGENT_STATUS_BANNED) {
            throw new ServiceException(ResultCode.AGENT_BANNED);
        }

        // 4. 校验余额
        if (agent.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new ServiceException(ResultCode.WITHDRAW_AMOUNT_EXCEED,
                    "可用余额 " + agent.getBalance() + "，申请 " + dto.getAmount());
        }

        // 5. 计算手续费（按租户配置费率，默认 0）
        BigDecimal fee = JicekConstants.WITHDRAW_FEE_RATE
                .multiply(dto.getAmount())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = dto.getAmount().subtract(fee);

        // 6. 余额流转：balance → frozenBalance（同事务）
        LocalDateTime now = LocalDateTime.now();
        agent.setBalance(agent.getBalance().subtract(dto.getAmount()));
        agent.setFrozenBalance(agent.getFrozenBalance().add(dto.getAmount()));
        agent.setUpdateTime(now);
        agentMapper.updateById(agent);

        // 7. 创建提现申请
        Withdraw withdraw = new Withdraw();
        withdraw.setTenantId(dto.getTenantId());
        withdraw.setAgentId(dto.getAgentId());
        withdraw.setAmount(dto.getAmount());
        withdraw.setFee(fee);
        withdraw.setActualAmount(actualAmount);
        withdraw.setPayType(dto.getPayType());
        withdraw.setPayAccount(dto.getPayAccount());
        withdraw.setPayName(dto.getPayName());
        withdraw.setStatus(JicekConstants.WITHDRAW_PENDING);
        withdraw.setApplyTime(now);
        withdraw.setCreateTime(now);
        withdraw.setUpdateTime(now);
        withdrawMapper.insert(withdraw);

        log.info("提现申请: tenantId={}, agentId={}, withdrawId={}, amount={}, fee={}, actual={}",
                dto.getTenantId(), dto.getAgentId(), withdraw.getId(),
                dto.getAmount(), fee, actualAmount);
        return withdraw.getId();
    }

    /**
     * 开发者审核提现
     * <p>
     * action: approve / reject / payout / fail
     *
     * @param dto   审核参数
     * @param auditBy 审核人用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditWithdraw(WithdrawAuditDTO dto, Long auditBy) {
        Withdraw withdraw = getWithdrawById(dto.getTenantId(), dto.getWithdrawId());
        LocalDateTime now = LocalDateTime.now();
        Agent agent = agentService.getAgentById(dto.getTenantId(), withdraw.getAgentId());

        switch (dto.getAction()) {
            case "approve":
                doApprove(withdraw, agent, auditBy, dto.getAuditRemark(), now);
                break;
            case "reject":
                doReject(withdraw, agent, auditBy, dto.getAuditRemark(), now);
                break;
            case "payout":
                doPayout(withdraw, agent, auditBy, dto.getTradeNo(), now);
                break;
            case "fail":
                doFail(withdraw, agent, auditBy, dto.getFailReason(), now);
                break;
            default:
                throw new ServiceException(ResultCode.WITHDRAW_STATUS_INVALID,
                        "未知 action: " + dto.getAction());
        }
    }

    /**
     * 审核通过：0 → 1（资金仍在 frozenBalance，等打款）
     */
    private void doApprove(Withdraw w, Agent agent, Long auditBy,
                            String remark, LocalDateTime now) {
        if (w.getStatus() != JicekConstants.WITHDRAW_PENDING) {
            throw new ServiceException(ResultCode.WITHDRAW_STATUS_INVALID, "仅待审核可审核通过");
        }
        w.setStatus(JicekConstants.WITHDRAW_APPROVED);
        w.setAuditBy(auditBy);
        w.setAuditTime(now);
        w.setAuditRemark(remark);
        w.setUpdateTime(now);
        withdrawMapper.updateById(w);
        log.info("提现审核通过: withdrawId={}, agentId={}, amount={}",
                w.getId(), agent.getId(), w.getAmount());
    }

    /**
     * 审核拒绝：0 → 2，资金 frozenBalance → balance
     */
    private void doReject(Withdraw w, Agent agent, Long auditBy,
                           String remark, LocalDateTime now) {
        if (w.getStatus() != JicekConstants.WITHDRAW_PENDING) {
            throw new ServiceException(ResultCode.WITHDRAW_STATUS_INVALID, "仅待审核可拒绝");
        }
        w.setStatus(JicekConstants.WITHDRAW_REJECTED);
        w.setAuditBy(auditBy);
        w.setAuditTime(now);
        w.setAuditRemark(remark);
        w.setUpdateTime(now);
        withdrawMapper.updateById(w);

        // 资金退回
        agent.setFrozenBalance(agent.getFrozenBalance().subtract(w.getAmount()));
        agent.setBalance(agent.getBalance().add(w.getAmount()));
        agent.setUpdateTime(now);
        agentMapper.updateById(agent);

        log.info("提现审核拒绝: withdrawId={}, agentId={}, amount={}, 余额退回",
                w.getId(), agent.getId(), w.getAmount());
    }

    /**
     * 打款成功：1 → 3，资金 frozenBalance → totalWithdraw
     */
    private void doPayout(Withdraw w, Agent agent, Long auditBy,
                           String tradeNo, LocalDateTime now) {
        if (w.getStatus() != JicekConstants.WITHDRAW_APPROVED) {
            throw new ServiceException(ResultCode.WITHDRAW_STATUS_INVALID, "仅已通过可打款");
        }
        w.setStatus(JicekConstants.WITHDRAW_PAID);
        w.setAuditBy(auditBy);
        w.setAuditTime(now);
        w.setTradeNo(tradeNo);
        w.setUpdateTime(now);
        withdrawMapper.updateById(w);

        // 累计已提现增加（实际到账金额）
        agent.setFrozenBalance(agent.getFrozenBalance().subtract(w.getAmount()));
        agent.setTotalWithdraw(agent.getTotalWithdraw().add(w.getActualAmount()));
        agent.setUpdateTime(now);
        agentMapper.updateById(agent);

        log.info("提现打款成功: withdrawId={}, agentId={}, actualAmount={}, tradeNo={}",
                w.getId(), agent.getId(), w.getActualAmount(), tradeNo);
    }

    /**
     * 打款失败：1 → 4，资金 frozenBalance → balance
     */
    private void doFail(Withdraw w, Agent agent, Long auditBy,
                         String failReason, LocalDateTime now) {
        if (w.getStatus() != JicekConstants.WITHDRAW_APPROVED) {
            throw new ServiceException(ResultCode.WITHDRAW_STATUS_INVALID, "仅已通过可标记失败");
        }
        w.setStatus(JicekConstants.WITHDRAW_FAILED);
        w.setAuditBy(auditBy);
        w.setAuditTime(now);
        w.setFailReason(failReason);
        w.setUpdateTime(now);
        withdrawMapper.updateById(w);

        // 资金退回
        agent.setFrozenBalance(agent.getFrozenBalance().subtract(w.getAmount()));
        agent.setBalance(agent.getBalance().add(w.getAmount()));
        agent.setUpdateTime(now);
        agentMapper.updateById(agent);

        log.info("提现打款失败: withdrawId={}, agentId={}, amount={}, 余额退回, reason={}",
                w.getId(), agent.getId(), w.getAmount(), failReason);
    }

    /**
     * 提现申请分页查询
     */
    public Page<Withdraw> pageWithdraws(Long tenantId, Long agentId, Integer status,
                                          int page, int size) {
        Page<Withdraw> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getTenantId, tenantId)
                .eq(agentId != null, Withdraw::getAgentId, agentId)
                .eq(status != null, Withdraw::getStatus, status)
                .orderByDesc(Withdraw::getCreateTime);
        return withdrawMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 按 ID 查询提现申请（含租户校验）
     */
    public Withdraw getWithdrawById(Long tenantId, Long withdrawId) {
        Withdraw w = withdrawMapper.selectById(withdrawId);
        if (w == null || !w.getTenantId().equals(tenantId)) {
            throw new ServiceException(ResultCode.WITHDRAW_NOT_FOUND);
        }
        return w;
    }

    /**
     * 代理待审核提现总金额（汇总查询用）
     */
    public BigDecimal sumPendingAmount(Long tenantId, Long agentId) {
        // 简化实现：查所有 pending 后累加（数据量大时可改 SQL sum）
        Page<Withdraw> p = new Page<>(1, 1000);
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getTenantId, tenantId)
                .eq(agentId != null, Withdraw::getAgentId, agentId)
                .eq(Withdraw::getStatus, JicekConstants.WITHDRAW_PENDING);
        Page<Withdraw> result = withdrawMapper.selectPage(p, wrapper);
        return result.getRecords().stream()
                .map(Withdraw::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
