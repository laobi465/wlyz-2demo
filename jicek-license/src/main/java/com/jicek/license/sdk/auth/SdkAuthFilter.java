package com.jicek.license.sdk.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.result.R;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.crypto.AesCryptoService;
import com.jicek.license.crypto.HmacSignService;
import com.jicek.license.software.entity.Software;
import com.jicek.license.software.mapper.SoftwareMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * SDK 鉴权 Filter
 * 作者: 极策k  日期: 2026-07-22
 *
 * 拦截 /api/sdk/** 所有 SDK 接口，执行完整签名校验：
 *  1. 包装请求体为 CachedBodyHttpServletRequest（body 可重复读）
 *  2. X-App-Key → 查 software 表 → 校验启用状态
 *  3. X-Timestamp ±300s 时间戳容差
 *  4. X-Nonce Redis 原子防重放（5 分钟 TTL）
 *  5. X-Signature HMAC-SHA256 签名校验（含 body SHA-256，常量时间比较）
 *  6. 注入 SoftwareContext（ThreadLocal）
 *  7. finally 清理 SoftwareContext（防线程池串号）
 *
 * 安全铁律：
 *  - 签名比对用 MessageDigest.isEqual 常量时间比较（HmacSignService.verify 已实现）
 *  - nonce 防重放用 Redisson RBucket.trySet 原子操作，禁内存缓存（多实例场景）
 *  - SoftwareContext.clear() 必须在 finally 执行，防 Tomcat 线程池复用串号
 *  - software 查询无本地缓存（后续可用 Caffeine 5s TTL 优化，密钥轮换最多 5s 生效）
 *
 * 排除路径：无（所有 /api/sdk/** 均需鉴权，SDK 无公开接口）
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SdkAuthFilter extends jakarta.servlet.http.OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SdkAuthFilter.class);

    /** 时间戳容差（秒） */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;
    /** nonce 缓存时长 */
    private static final Duration NONCE_TTL = Duration.ofMinutes(5);

    private final SoftwareMapper softwareMapper;
    private final AesCryptoService aesCryptoService;
    private final HmacSignService hmacSignService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public SdkAuthFilter(SoftwareMapper softwareMapper,
                         AesCryptoService aesCryptoService,
                         HmacSignService hmacSignService,
                         RedissonClient redissonClient,
                         ObjectMapper objectMapper) {
        this.softwareMapper = softwareMapper;
        this.aesCryptoService = aesCryptoService;
        this.hmacSignService = hmacSignService;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 仅拦截 /api/sdk/**
        if (!path.startsWith("/api/sdk/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. 包装请求体（body 可重复读）
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

            // 2. 逐项校验，失败则直接写错误响应返回
            Software software = authenticate(cachedRequest, response);
            if (software == null) {
                // authenticate 已写入错误响应
                return;
            }

            // 3. 注入上下文 + 放行
            String signSecretPlain = aesCryptoService.decrypt(software.getSignSecret());
            SoftwareContext.set(software, signSecretPlain, getClientIp(cachedRequest));

            filterChain.doFilter(cachedRequest, response);
        } finally {
            // 铁律：无论是否异常，必须清理 ThreadLocal，防线程池串号
            SoftwareContext.clear();
        }
    }

    /**
     * 完整签名校验
     * @return 校验通过返回 Software，失败返回 null（已写入错误响应）
     */
    private Software authenticate(CachedBodyHttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        // 2.1 X-App-Key → 查 software
        String appKey = request.getHeader("X-App-Key");
        if (appKey == null || appKey.isBlank()) {
            writeError(response, ResultCode.SDK_APP_KEY_MISSING);
            return null;
        }
        Software software = softwareMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Software>()
                        .eq(Software::getAppKey, appKey));
        if (software == null) {
            writeError(response, ResultCode.SDK_APP_KEY_INVALID);
            return null;
        }
        if (software.getEnabled() == null || software.getEnabled() != JicekConstants.SOFTWARE_ENABLED) {
            writeError(response, ResultCode.SDK_SOFTWARE_DISABLED);
            return null;
        }

        // 2.2 X-Timestamp ±300s
        String timestamp = request.getHeader("X-Timestamp");
        if (timestamp == null || timestamp.isBlank()) {
            writeError(response, ResultCode.SDK_TIMESTAMP_MISSING);
            return null;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            writeError(response, ResultCode.SDK_TIMESTAMP_MISSING);
            return null;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE_SECONDS * 1000) {
            writeError(response, ResultCode.SDK_TIMESTAMP_EXPIRED);
            return null;
        }

        // 2.3 X-Nonce Redis 原子防重放
        String nonce = request.getHeader("X-Nonce");
        if (nonce == null || nonce.isBlank()) {
            writeError(response, ResultCode.SDK_NONCE_MISSING);
            return null;
        }
        String nonceKey = JicekConstants.REDIS_KEY_NONCE + nonce;
        RBucket<String> nonceBucket = redissonClient.getBucket(nonceKey);
        // trySet = setIfAbsent（原子），返回 true 表示设置成功（nonce 未重复）
        boolean isNew = nonceBucket.trySet("1", NONCE_TTL);
        if (!isNew) {
            writeError(response, ResultCode.SDK_NONCE_REPLAY);
            return null;
        }

        // 2.4 X-Signature HMAC-SHA256 签名校验
        String signature = request.getHeader("X-Signature");
        if (signature == null || signature.isBlank()) {
            writeError(response, ResultCode.SDK_SIGNATURE_MISSING);
            return null;
        }

        // 计算 body SHA-256（GET 或无 body 时为空串）
        String bodySha256 = "";
        byte[] bodyBytes = request.getCachedBody();
        if (bodyBytes != null && bodyBytes.length > 0) {
            bodySha256 = sha256Hex(bodyBytes);
        }

        // 构造签名原文：METHOD + \n + PATH(含query) + \n + TIMESTAMP + \n + NONCE + \n + BODY_SHA256
        String fullPath = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            fullPath = fullPath + "?" + queryString;
        }
        String method = request.getMethod();
        String signPayload = HmacSignService.buildSignPayload(method, fullPath, timestamp, nonce, bodySha256);

        // 用软件独立 signSecret 验签（非常量时间比较由 HmacSignService 内部处理）
        String signSecretPlain = aesCryptoService.decrypt(software.getSignSecret());
        boolean valid = hmacSignService.verify(signPayload, signature, signSecretPlain);
        if (!valid) {
            log.warn("【SDK鉴权】签名校验失败 appKey={} path={} nonce={}", appKey, fullPath, nonce);
            writeError(response, ResultCode.SDK_SIGNATURE_INVALID);
            return null;
        }

        return software;
    }

    /** 计算 SHA-256 十六进制小写 */
    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    /** 写 JSON 错误响应 */
    private void writeError(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        R<Void> body = R.fail(resultCode);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /** 获取客户端真实 IP（穿透代理） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                int comma = forwarded.indexOf(',');
                ip = comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
