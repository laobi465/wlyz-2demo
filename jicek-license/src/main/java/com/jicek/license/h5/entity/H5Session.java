package com.jicek.license.h5.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * H5 终端用户会话实体
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
@TableName("jicek_h5_session")
public class H5Session {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long softwareId;
    private Long cardKeyId;
    private String cardNoMasked;
    private String h5Token;
    private String deviceInfo;
    private String clientIp;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
