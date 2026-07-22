package com.jicek.license.h5.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * H5 卡密登录请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户在 H5 页面输入卡密明文 + appKey 登录。
 * 卡密明文传输（H5 走 HTTPS，不做 RSA 加密，与 SDK 不同）。
 */
@Data
public class H5LoginRequestDTO {
    /** 软件 AppKey */
    @NotBlank(message = "AppKey 不能为空")
    private String appKey;
    /** 卡密明文 */
    @NotBlank(message = "卡密不能为空")
    private String cardKey;
}
