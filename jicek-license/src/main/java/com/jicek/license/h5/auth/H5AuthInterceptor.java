package com.jicek.license.h5.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import com.jicek.license.h5.entity.H5Session;
import com.jicek.license.h5.mapper.H5SessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * H5 鉴权拦截器
 * 作者: 极策k  日期: 2026-07-22
 *
 * 校验 X-H5-Token 头，从 Redis 缓存读取会话（回源 DB），
 * 校验通过后装入 H5AuthContext，afterCompletion 清理。
 *
 * 说明：项目统一使用 Redisson（无 spring-boot-starter-data-redis），
 * 因此这里用 RBucket 而非 StringRedisTemplate。
 */
@Component
public class H5AuthInterceptor implements HandlerInterceptor {

    private final H5SessionMapper h5SessionMapper;
    private final RedissonClient redissonClient;

    public H5AuthInterceptor(H5SessionMapper h5SessionMapper, RedissonClient redissonClient) {
        this.h5SessionMapper = h5SessionMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String token = req.getHeader(JicekConstants.H5_AUTH_HEADER);
        if (token == null || token.isBlank()) {
            throw new ServiceException(ResultCode.H5_TOKEN_MISSING);
        }

        // 先查 Redis 缓存（仅做存在性标记，命中后仍回源 DB 取完整 session）
        String redisKey = JicekConstants.REDIS_KEY_H5_SESSION + token;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        boolean cached = bucket.isExists();

        H5Session session = h5SessionMapper.selectOne(
            new LambdaQueryWrapper<H5Session>().eq(H5Session::getH5Token, token));

        if (session == null) {
            // 缓存命中但 DB 无记录：可能 session 已被 logout 删除，清理脏缓存
            if (cached) {
                bucket.delete();
            }
            throw new ServiceException(ResultCode.H5_TOKEN_INVALID);
        }

        // 缓存未命中且会话有效：写入缓存，TTL 与会话剩余有效期一致
        if (!cached) {
            long remainSeconds = Duration.between(LocalDateTime.now(), session.getExpireTime()).getSeconds();
            if (remainSeconds > 0) {
                bucket.set("1", Duration.ofSeconds(remainSeconds));
            }
        }

        if (session.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.H5_TOKEN_INVALID);
        }

        H5AuthContext.set(new H5AuthContext.H5SessionInfo(
            session.getTenantId(), session.getSoftwareId(),
            session.getCardKeyId(), session.getH5Token()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        H5AuthContext.clear();
    }
}
