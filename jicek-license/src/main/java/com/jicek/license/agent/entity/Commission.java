package com.jicek.license.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分润流水实体（不可变，仅审计查询）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全铁律（铁律 04/13）：
 * 1. 本表记录永不删除（资金流水审计）
 * 2. 退款时通过 status=0 撤销，并产生负数对冲记录（在 Service 中处理）
 * 3. commissionRate/amount 为快照，避免后续代理比例调整影响历史审计
 *
 * type：1 直接销售 2 下级分润 3 制卡差价
 * status：0 已撤销 1 有效
 */
@Data
@TableName("jicek_commission")
public class Commission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 受益代理 */
    private Long agentId;

    /** 关联支付订单 */
    private Long orderId;

    /** 订单号（冗余，便于查询） */
    private String outTradeNo;

    /** 来源代理（下级制卡/销售），null 表示终端用户购买 */
    private Long sourceAgentId;

    private Long cardTypeId;

    /** 订单原金额 */
    private BigDecimal orderAmount;

    /** 分润比例快照 */
    private BigDecimal commissionRate;

    /** 分润金额 */
    private BigDecimal commissionAmount;

    /** 1 直接销售 2 下级分润 3 制卡差价 */
    private Integer type;

    /** 0 已撤销 1 有效 */
    private Integer status;

    private LocalDateTime createTime;
}
