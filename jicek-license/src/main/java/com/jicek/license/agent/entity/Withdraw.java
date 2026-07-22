package com.jicek.license.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 状态机（不可逆，铁律 06）：
 *   0 待审核 → 1 已通过（开发者审核通过，等待打款）
 *   0 待审核 → 2 已拒绝（余额退回可用）
 *   1 已通过 → 3 已打款（实际打款完成）
 *   1 已通过 → 4 已失败（打款失败，余额退回可用）
 *
 * 安全铁律：
 * 1. 申请提现：可用余额 → 冻结余额（同事务）
 * 2. 审核拒绝/打款失败：冻结余额 → 可用余额（同事务）
 * 3. 审核通过并打款：冻结余额 → 累计已提现（同事务）
 * 4. 资金流水永不可删（铁律 04）
 */
@Data
@TableName("jicek_withdraw")
public class Withdraw {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long agentId;

    /** 申请提现金额 */
    private BigDecimal amount;

    /** 手续费 */
    private BigDecimal fee;

    /** 实际到账金额 = amount - fee */
    private BigDecimal actualAmount;

    /** alipay/wxpay/bank */
    private String payType;

    private String payAccount;

    private String payName;

    /** 0 待审核 1 已通过 2 已拒绝 3 已打款 4 已失败 */
    private Integer status;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String auditRemark;

    /** 打款流水号 */
    private String tradeNo;

    private String failReason;

    private LocalDateTime applyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
