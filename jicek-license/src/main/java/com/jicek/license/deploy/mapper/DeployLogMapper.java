package com.jicek.license.deploy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.deploy.entity.DeployLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部署审计日志 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 审计表，禁 UPDATE/DELETE，仅 INSERT + SELECT
 */
@Mapper
public interface DeployLogMapper extends BaseMapper<DeployLog> {
}
