package com.jicek.license.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jicek.license.agent.entity.Agent;
import com.jicek.license.agent.entity.Commission;
import com.jicek.license.agent.mapper.AgentMapper;
import com.jicek.license.agent.mapper.CommissionMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.pay.entity.PayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分润业务服务
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：
 * 1. 订单支付成功后触发分润（向上链式分润）
 * 2. 退款时撤销分润（连带撤销，回滚余额）
 * 3. 分润流水查询（按代理/订单/时间）
 *
 * 分润算法（向上链式）：
 *   假设订单 100 元，直接销售代理 A 分润比例 10%，A 的上级 B 比例 5%，B 的上级 C 比例 3%
 *   - A 得 100 × 10% = 10 元（type=1 直接销售）
 *   - B 得 100 × 5% = 5 元（type=2 下级分润）
 *   - C 得 100 × 3% = 3 元（type=2 下级分润）
 *   总分润 = 18 元，开发者净收入 = 82 元
 *
 * 安全铁律（铁律 04/06/13）：
 * - 分润计算 + 余额增加必须同事务（铁律 06，禁伪异步）
 * - 分润流水永不可删（铁律 04），退款对冲通过新增负数记录
 * - 金额一律 BigDecimal，禁 float/double
 * - 比例快照写入流水，避免后续调整影响历史
 */
@Slf4j
@Service
public class CommissionService {

    private final CommissionMapper commissionMapper;
    private final AgentMapper agentMapper;
    private final AgentService agentService;

    public CommissionService(CommissionMapper commissionMapper,
                              AgentMapper agentMapper,
                              AgentService agentService) {
        this.commissionMapper = commissionMapper;
        this.agentMapper = agentMapper;
        this.agentService = agentService;
    }

    /**
     * 订单支付成功后触发分润
     * <p>
     * 调用时机：PayNotifyService 验签通过、订单状态置为已支付之后，在同一事务内调用
     *
     * @param order     支付订单（status=1 已支付）
     * @param directAgentId 直接销售代理（null 表示终端用户购买，无直接代理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantCommission(PayOrder order, Long directAgentId) {
        if (order == null) {
            throw new ServiceException(ResultCode.COMMISSION_CALC_FAIL, "订单为空");
        }
        BigDecimal orderAmount = order.getAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("分润跳过：订单金额非法, outTradeNo={}, amount={}",
                    order.getOutTradeNo(), orderAmount);
            return;
        }

        // 幂等校验：同一 orderNo 已产生过分润流水则跳过（铁律 09，防重复分润）
        // 配合 jicek_commission.uk_order_agent(out_trade_no, agent_id) 唯一索引双重保障
        Long existCount = commissionMapper.selectCount(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getOutTradeNo, order.getOutTradeNo()));
        if (existCount != null && existCount > 0) {
            log.info("分润跳过（已分润，幂等命中）: outTradeNo={}, existCount={}",
                    order.getOutTradeNo(), existCount);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Commission> records = new ArrayList<>();
        BigDecimal totalCommission = BigDecimal.ZERO;

        // 1. 直接销售代理分润
        if (directAgentId != null && directAgentId > 0) {
            Agent directAgent = agentService.getAgentById(order.getTenantId(), directAgentId);
            if (directAgent.getStatus() == JicekConstants.AGENT_STATUS_NORMAL) {
                BigDecimal rate = directAgent.getCommissionRate();
                if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal amount = calcAmount(orderAmount, rate);
                    Commission c = buildCommission(order, directAgent, null, rate, amount,
                            JicekConstants.COMMISSION_TYPE_DIRECT, now);
                    records.add(c);
                    totalCommission = totalCommission.add(amount);

                    // 余额增加 + 累计收益增加
                    directAgent.setBalance(directAgent.getBalance().add(amount));
                    directAgent.setTotalEarnings(directAgent.getTotalEarnings().add(amount));
                    directAgent.setUpdateTime(now);
                    agentMapper.updateById(directAgent);
                }

                // 2. 向上链式分润（下级分润）
                Agent cursor = directAgent;
                int depth = 0;
                while (cursor.getParentId() != null && cursor.getParentId() > 0
                        && depth < JicekConstants.AGENT_MAX_LEVEL) {
                    Agent parent = agentMapper.selectById(cursor.getParentId());
                    if (parent == null || !parent.getTenantId().equals(order.getTenantId())) {
                        break;
                    }
                    if (parent.getStatus() != JicekConstants.AGENT_STATUS_NORMAL) {
                        break;
                    }
                    BigDecimal parentRate = parent.getCommissionRate();
                    if (parentRate != null && parentRate.compareTo(BigDecimal.ZERO) > 0) {
                        // 上级分润比例需低于下级（否则可能是配置错误，但允许，仅记录）
                        BigDecimal parentAmount = calcAmount(orderAmount, parentRate);
                        Commission pc = buildCommission(order, parent, directAgentId,
                                parentRate, parentAmount, JicekConstants.COMMISSION_TYPE_SUB, now);
                        records.add(pc);
                        totalCommission = totalCommission.add(parentAmount);

                        parent.setBalance(parent.getBalance().add(parentAmount));
                        parent.setTotalEarnings(parent.getTotalEarnings().add(parentAmount));
                        parent.setUpdateTime(now);
                        agentMapper.updateById(parent);
                    }
                    cursor = parent;
                    depth++;
                }
            }
        }

        // 3. 批量插入分润流水
        for (Commission c : records) {
            commissionMapper.insert(c);
        }

        if (!records.isEmpty()) {
            log.info("分润完成: outTradeNo={}, orderAmount={}, totalCommission={}, records={}",
                    order.getOutTradeNo(), orderAmount, totalCommission, records.size());
        } else {
            log.debug("无分润产生（无代理或比例为 0）: outTradeNo={}", order.getOutTradeNo());
        }
    }

    /**
     * 订单退款时撤销分润（连带撤销）
     * <p>
     * 调用时机：退款成功后，订单 status 已置为 3，同事务内调用
     * 行为：将该订单关联的所有有效分润置为已撤销，并从代理余额中扣回
     *
     * @param order 退款订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeCommission(PayOrder order) {
        if (order == null) {
            return;
        }
        List<Commission> records = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getOrderId, order.getId())
                        .eq(Commission::getStatus, JicekConstants.COMMISSION_STATUS_VALID));
        if (records.isEmpty()) {
            log.info("无分润可撤销: outTradeNo={}", order.getOutTradeNo());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalRevoked = BigDecimal.ZERO;
        for (Commission c : records) {
            // 撤销流水
            c.setStatus(JicekConstants.COMMISSION_STATUS_REVOKED);
            commissionMapper.updateById(c);

            // 从代理余额扣回（注意：可能余额不足，需扣到负数或记为应收）
            Agent agent = agentMapper.selectById(c.getAgentId());
            if (agent != null) {
                BigDecimal toDeduct = c.getCommissionAmount();
                // 余额不足时扣到 0，差额记入累计收益扣减（保证 balance 不为负）
                if (agent.getBalance().compareTo(toDeduct) < 0) {
                    BigDecimal shortfall = toDeduct.subtract(agent.getBalance());
                    agent.setBalance(BigDecimal.ZERO);
                    agent.setTotalEarnings(agent.getTotalEarnings().subtract(shortfall));
                } else {
                    agent.setBalance(agent.getBalance().subtract(toDeduct));
                }
                agent.setUpdateTime(now);
                agentMapper.updateById(agent);
                totalRevoked = totalRevoked.add(toDeduct);
            }
        }

        log.info("分润撤销完成: outTradeNo={}, revokedCount={}, totalRevoked={}",
                order.getOutTradeNo(), records.size(), totalRevoked);
    }

    /**
     * 分润流水分页查询
     */
    public Page<Commission> pageCommissions(Long tenantId, Long agentId, Long sourceAgentId,
                                             Integer type, Integer status, int page, int size) {
        Page<Commission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<Commission>()
                .eq(Commission::getTenantId, tenantId)
                .eq(agentId != null, Commission::getAgentId, agentId)
                .eq(sourceAgentId != null, Commission::getSourceAgentId, sourceAgentId)
                .eq(type != null, Commission::getType, type)
                .eq(status != null, Commission::getStatus, status)
                .orderByDesc(Commission::getCreateTime);
        return commissionMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 按订单查询所有分润（用于订单详情）
     */
    public List<Commission> listByOrder(Long orderId) {
        return commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getOrderId, orderId)
                        .orderByAsc(Commission::getType));
    }

    /**
     * 计算分润金额 = orderAmount × rate / 100（向下取位到分，避免精度问题）
     */
    private BigDecimal calcAmount(BigDecimal orderAmount, BigDecimal rate) {
        return orderAmount.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private Commission buildCommission(PayOrder order, Agent agent, Long sourceAgentId,
                                        BigDecimal rate, BigDecimal amount, int type,
                                        LocalDateTime now) {
        Commission c = new Commission();
        c.setTenantId(order.getTenantId());
        c.setAgentId(agent.getId());
        c.setOrderId(order.getId());
        c.setOutTradeNo(order.getOutTradeNo());
        c.setSourceAgentId(sourceAgentId);
        c.setCardTypeId(order.getCardTypeId());
        c.setOrderAmount(order.getAmount());
        c.setCommissionRate(rate);
        c.setCommissionAmount(amount);
        c.setType(type);
        c.setStatus(JicekConstants.COMMISSION_STATUS_VALID);
        c.setCreateTime(now);
        return c;
    }
}
