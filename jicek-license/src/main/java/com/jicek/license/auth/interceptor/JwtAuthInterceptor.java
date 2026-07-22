package com.jicek.license.auth.interceptor;

import com.jicek.license.auth.service.JwtService;
import com.jicek.license.common.constant.JicekConstants;
import com.jicek.license.common.exception.ServiceException;
import com.jicek.license.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 * 作者: 极策k  日期: 2026-07-22
 *
 * 工作流程：
 * 1. 非 HandlerMethod（静态资源等）直接放行
 * 2. 扫描 HandlerMethod 上的 @AuthRequired 注解（方法级优先于类级）
 * 3. 未标注 → 放行（兼容现有裸传参数接口）
 * 4. 标注 → 解析 Authorization 头 → JWT 校验 → 角色匹配 → 填充 AuthContext
 * 5. finally 清理 AuthContext（铁律：防 ThreadLocal 串号）
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 查找 @AuthRequired 注解（方法级优先，类级兜底）
        AuthRequired authRequired = handlerMethod.getMethodAnnotation(AuthRequired.class);
        if (authRequired == null) {
            authRequired = handlerMethod.getBeanType().getAnnotation(AuthRequired.class);
        }

        // 未标注 → 放行（兼容现有接口，过渡期）
        if (authRequired == null) {
            return true;
        }

        // 提取 Authorization 头
        String header = request.getHeader(JicekConstants.AUTH_HEADER);
        if (header == null || header.isEmpty()) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_MISSING);
        }
        if (!header.startsWith(JicekConstants.JWT_HEADER_PREFIX)) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_MISSING);
        }
        String token = header.substring(JicekConstants.JWT_HEADER_PREFIX.length());

        // 解析并校验 JWT
        Claims claims = jwtService.parseToken(token);
        Long userId = claims.get(JicekConstants.JWT_CLAIM_USER_ID, Long.class);
        Integer role = claims.get(JicekConstants.JWT_CLAIM_ROLE, Integer.class);
        Long tenantId = claims.get(JicekConstants.JWT_CLAIM_TENANT_ID, Long.class);
        String username = claims.get(JicekConstants.JWT_CLAIM_USERNAME, String.class);

        if (userId == null || role == null) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_INVALID);
        }

        // 角色匹配
        int requiredRole = authRequired.role();
        if (requiredRole != 0 && requiredRole != role) {
            throw new ServiceException(ResultCode.AUTH_TOKEN_WRONG_ROLE);
        }

        // 填充 ThreadLocal 上下文
        AuthContext.set(new AuthContext.AuthUser(userId, role, tenantId, username));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 铁律：必须清理，防止线程池复用导致身份串号
        AuthContext.clear();
    }
}
