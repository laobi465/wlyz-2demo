package com.jicek.license.update.controller;

import com.jicek.license.common.result.R;
import com.jicek.license.update.dto.SdkUpdateCheckResultDTO;
import com.jicek.license.update.service.UpdatePackageService;
import org.springframework.web.bind.annotation.*;

/**
 * SDK 更新检查 Controller
 * 作者: 极策k  日期: 2026-07-22
 *
 * 终端用户客户端通过 SDK 检查软件更新。
 * 鉴权由 SdkAuthFilter 统一处理（X-App-Key + HMAC 签名），softwareId 从 SoftwareContext 获取。
 *
 * 返回：
 *  - hasUpdate=true：有更新，客户端展示更新提示 + 下载 URL + SHA-256
 *  - hasUpdate=false：无更新，客户端继续运行
 *  - forceUpdate=true：强制更新，客户端旧版拒绝运行
 */
@RestController
@RequestMapping("/api/sdk/update")
public class SdkUpdateController {

    private final UpdatePackageService updatePackageService;

    public SdkUpdateController(UpdatePackageService updatePackageService) {
        this.updatePackageService = updatePackageService;
    }

    /**
     * 检查更新
     *
     * @param clientVersion 客户端当前版本（必填，格式 X.Y.Z）
     * @param channel       通道（1稳定版 2内测版，默认 1）
     * @return 更新检查结果
     */
    @GetMapping("/check")
    public R<SdkUpdateCheckResultDTO> check(
            @RequestParam String clientVersion,
            @RequestParam(required = false, defaultValue = "1") Integer channel) {
        return R.ok(updatePackageService.checkUpdate(clientVersion, channel));
    }
}
