package com.jicek.license.h5.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * H5 登录结果 DTO
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class H5LoginResultDTO {
    /** H5 会话令牌（后续请求放 X-H5-Token 头） */
    private String h5Token;
    /** 卡号脱敏 */
    private String cardNoMasked;
    /** 卡类名称 */
    private String cardTypeName;
    /** 卡类类型：1时长卡 2次数卡 3功能卡 4永久卡 */
    private Integer cardType;
    /** 到期时间 */
    private LocalDateTime expireTime;
    /** 剩余次数（次数卡） */
    private Integer remainingCount;
    /** 功能列表（功能卡） */
    private List<String> features;
    /** 软件名称 */
    private String softwareName;
    /** 令牌过期时间 */
    private LocalDateTime tokenExpireTime;
}
