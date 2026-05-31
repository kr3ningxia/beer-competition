package com.beercompetition.security;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UserRole requiredRole = resolveRequiredRole(request.getRequestURI());
        if (requiredRole == null) {
            return true;
        }

        String authorization = request.getHeader(jwtProperties.getHeaderName());
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getSecretKey(), token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        String role = claims.get("role", String.class);
        if (!requiredRole.name().equals(role)) {
            throw new ForbiddenException("当前账号无权访问该资源");
        }

        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(Long.valueOf(claims.get("uid").toString()))
                .role(role)
                .displayName(claims.get("displayName", String.class))
                .competitionId(claims.get("competitionId", Long.class))
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.clear();
    }

    private UserRole resolveRequiredRole(String uri) {
        if (uri.startsWith("/api/admin/")) {
            return UserRole.ADMIN;
        }
        if (uri.startsWith("/api/portal/")) {
            return UserRole.PORTAL;
        }
        if (uri.startsWith("/api/judge/")) {
            return UserRole.JUDGE;
        }
        return null;
    }
}
