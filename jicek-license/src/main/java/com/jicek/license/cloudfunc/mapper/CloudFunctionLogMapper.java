package com.jicek.license.cloudfunc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.cloudfunc.entity.CloudFunctionLog;

/**
 * 云函数执行日志 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 审计表，禁 UPDATE/DELETE，仅 INSERT + SELECT
 */
public interface CloudFunctionLogMapper extends BaseMapper<CloudFunctionLog> {
}
