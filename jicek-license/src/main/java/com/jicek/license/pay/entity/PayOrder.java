package com.jicek.license.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 * 作者: 极策k  日期: 2026-07-21
 *
 * 状态机（不可逆）：
 *   0 待支付 → 1 已支付（仅异步回调验签通过时）
 *   0 待支付 → 2 失败（异步回调返回失败）
 *   0 待支付 → 4 已关闭（定时任务超时扫描）
 *   1 已支付 → 3 已退款（管理员手动发起 + V1 api.php?act=refund 成功）
 */
@Data
@TableName("jicek_pay_order")
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 商户订单号 */
    private String outTradeNo;

    /** 易支付流水号 */
    private String tradeNo;

    /** 购买的卡类ID */
    private Long cardTypeId;

    /** 关联代理ID（null 表示终端用户购买，非空时支付成功后触发分润） */
    private Long agentId;

    /** 购买数量 */
    private Integer quantity;

    /** 金额（禁 float，强制 BigDecimal） */
    private BigDecimal amount;

    /** 支付通道：alipay/wxpay/qqpay/unionpay */
    private String payType;

    /** 状态：0待支付 1已支付 2失败 3已退款 4已关闭 */
    private Integer status;

    /** 用户支付 IP */
    private String userIp;

    /** 设备类型：pc/mobile */
    private String device;

    /** 业务扩展参数（原样回传） */
    private String param;

    /** 支付完成时间 */
    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
