package com.jicek.license.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.pay.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付订单 Mapper
 * 作者: 极策k  日期: 2026-07-21
 */
@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {
}
