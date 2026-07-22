package com.jicek.license.enduser.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * H5 终端用户账号登录结果 DTO（v0.14.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 复用 H5 会话机制（X-H5-Token + 24h 有效期），
 * 与卡密登录返回的 H5LoginResultDTO 字段不同，此处只返回账号相关信息。
 */
@Data
public class H5EndUserLoginResultDTO {

    /** H5 会话令牌（UUID，后续请求放在 X-H5-Token 头） */
    private String h5Token;

    /** 终端用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 软件名称（冗余，展示用） */
    private String softwareName;

    /** token 过期时间 */
    private LocalDateTime tokenExpireTime;
}
