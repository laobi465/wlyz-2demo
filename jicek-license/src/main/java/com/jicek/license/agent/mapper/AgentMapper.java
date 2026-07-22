package com.jicek.license.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代理 Mapper
 * 作者: 极策k  日期: 2026-07-22
 *
 * 注意：余额变动走 Service 事务 + commission 流水，禁止直接 updateById 改余额
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
