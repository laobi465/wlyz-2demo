package com.jicek.license.card.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.card.entity.CardKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卡密 Mapper
 * 作者: 极策k  日期: 2026-07-21
 *
 * 注意：卡密明文禁止入库，查询仅返回 card_cipher（AES 加密）与 card_hash（SHA-256）
 */
@Mapper
public interface CardKeyMapper extends BaseMapper<CardKey> {
}
