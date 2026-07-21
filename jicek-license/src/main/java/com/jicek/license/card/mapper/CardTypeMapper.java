package com.jicek.license.card.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.card.entity.CardType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卡类 Mapper
 * 作者: 极策k  日期: 2026-07-21
 *
 * 卡类 4 种类型：1时长卡 / 2次数卡 / 3功能卡 / 4永久卡
 */
@Mapper
public interface CardTypeMapper extends BaseMapper<CardType> {
}
