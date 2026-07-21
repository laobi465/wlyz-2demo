package com.jicek.license.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jicek.license.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备 Mapper
 * 作者: 极策k  日期: 2026-07-21
 *
 * 注意：deviceInfo 字段为 AES 加密 JSON，查询后需手动解密展示
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
