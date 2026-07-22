package com.jicek.license.sdk.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.sdk.dto.SdkLoginRequestDTO;
import com.jicek.license.sdk.dto.SdkLoginResultDTO;
import com.jicek.license.sdk.service.SdkAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * SDK 登录 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户在开发者软件内用卡密登录的入口。
 * 鉴权由 SdkAuthFilter 统一处理（HMAC-SHA256 签名 + nonce 防重放），此处无需手动校验。
 *
 * 请求头（由 SdkAuthFilter 校验）：
 *  - X-App-Key：软件 AppKey
 *  - X-Timestamp：13 位毫秒时间戳
 *  - X-Nonce：UUID v4
 *  - X-Signature：HMAC-SHA256 签名
 *  - X-Card-Cipher：RSA-2048-OAEP 加密的卡密密文（Base64）
 *
 * 请求体（可选）：设备指纹等辅助信息（后续设备自动绑定用）
 */
@RestController
@RequestMapping("/api/sdk/card")
public class SdkLoginController {

    private final SdkAuthService sdkAuthService;

    public SdkLoginController(SdkAuthService sdkAuthService) {
        this.sdkAuthService = sdkAuthService;
    }

    /**
     * 卡密登录
     *
     * @param cardCipher RSA 加密的卡密密文（X-Card-Cipher 头）
     * @param dto        设备辅助信息（可选，后续自动绑定时用）
     * @return 登录结果（卡类信息 + 软件配置）
     */
    @PostMapping("/login")
    public R<SdkLoginResultDTO> login(
            @RequestHeader("X-Card-Cipher") String cardCipher,
            @Valid @RequestBody(required = false) SdkLoginRequestDTO dto) {
        return R.ok(sdkAuthService.login(cardCipher));
    }
}
