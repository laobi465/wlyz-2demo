package com.jicek.license.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明（铁律 04/06/13）：
 * 1. passwordHash 使用 BCrypt 加密，禁明文
 * 2. balance/frozenBalance/totalEarnings/totalWithdraw 均为 BigDecimal，禁 float/double
 * 3. 余额变动必须在事务内（铁律 06），且产生 commission 流水
 * 4. commissionRate 为分润比例（0-100），如 10 表示 10%
 *
 * status：0 封禁 1 正常
 */
@Data
@TableName("jicek_agent")
public class Agent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 上级代理 ID，0 为顶级 */
    private Long parentId;

    private String username;

    /** BCrypt 加密 */
    private String passwordHash;

    private String realName;

    private String contact;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结余额（提现中） */
    private BigDecimal frozenBalance;

    /** 累计收益 */
    private BigDecimal totalEarnings;

    /** 累计已提现 */
    private BigDecimal totalWithdraw;

    /** 分润比例 0-100 */
    private BigDecimal commissionRate;

    /** 允许发展的下级层级数，0 不可发展 */
    private Integer maxSubLevel;

    /** 0 封禁 1 正常 */
    private Integer status;

    /** 代理级别 1=顶级 */
    private Integer level;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
