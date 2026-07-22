package com.jicek.license.auth.service;

import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.config.JicekProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 服务（生成 / 解析 / 校验 token）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全规范（铁律 04/06）：
 * 1. 签名密钥从环境变量注入（JICEK_JWT_SECRET），禁硬编码，最少 32 字节
 * 2. 算法 HMAC-SHA256（JJWT 0.12.x），禁用 none / 弱算法
 * 3. token 过期时间可配置（默认 24 小时）
 * 4. claims 包含 uid / role / tenantId（开发者）/ username
 */
@Slf4j
@Service
public class JwtService {

    private final JicekProperties properties;
    private SecretKey secretKey;

    public JwtService(JicekProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String secret = properties.getAuth().getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            log.warn("【鉴权】JICEK_JWT_SECRET 未配置或长度不足 32 字节，鉴权功能不可用。"
                    + "请通过环境变量 JICEK_JWT_SECRET 注入（至少 32 字节随机字符串）。");
            // 不抛异常，允许应用启动；运行期调用鉴权接口会抛 AUTH_TOKEN_INVALID
            this.secretKey = null;
            return;
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("【鉴权】JWT 密钥已加载，有效期 {} 小时", properties.getAuth().getJwtExpireHours());
    }

    /**
     * 生成 token
     *
     * @param userId   用户ID
     * @param role     角色（1开发者 2管理员）
     * @param tenantId 租户ID（管理员传 null）
     * @param username 用户名
     * @return JWT token
     */
    public String generateToken(Long userId, int role, Long tenantId, String username) {
        if (secretKey == null) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_INVALID,
                    "JWT 密钥未配置，鉴权功能不可用");
        }
        long expireMs = Duration.ofHours(properties.getAuth().getJwtExpireHours()).toMillis();
        Date now = new Date();
        var builder = Jwts.builder()
                .issuer(properties.getAuth().getJwtIssuer())
                .subject(String.valueOf(userId))
                .claim(JicekConstants.JWT_CLAIM_USER_ID, userId)
                .claim(JicekConstants.JWT_CLAIM_ROLE, role)
                .claim(JicekConstants.JWT_CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(secretKey, Jwts.SIG.HS256);

        if (tenantId != null) {
            builder.claim(JicekConstants.JWT_CLAIM_TENANT_ID, tenantId);
        }
        return builder.compact();
    }

    /**
     * 解析并校验 token
     *
     * @return Claims（含 uid / role / tenantId / username）
     * @throws ServiceException token 无效或过期
     */
    public Claims parseToken(String token) {
        if (secretKey == null) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_INVALID,
                    "JWT 密钥未配置，鉴权功能不可用");
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getAuth().getJwtIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            throw new ServiceException(ResultCode.AUTH_TOKEN_INVALID);
        }
    }

    /**
     * 获取 token 有效期（秒）
     */
    public long getExpiresInSeconds() {
        return Duration.ofHours(properties.getAuth().getJwtExpireHours()).getSeconds();
    }
}
