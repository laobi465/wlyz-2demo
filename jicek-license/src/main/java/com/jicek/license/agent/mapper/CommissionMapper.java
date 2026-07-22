package com.jicek.license.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.agent.entity.Commission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分润流水 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全铁律：本表记录禁删除（资金审计），退款对冲通过新增负数记录实现
 */
@Mapper
public interface CommissionMapper extends BaseMapper<Commission> {
}
