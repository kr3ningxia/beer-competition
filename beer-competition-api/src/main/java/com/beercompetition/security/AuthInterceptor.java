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

/**
 * 统一鉴权拦截器，基于 JWT + URL 路径前缀进行身份和角色校验
 * 角色体系：管理员(ADMIN)、参赛者(PORTAL)、裁判(JUDGE)
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行，浏览器 CORS 会先发 OPTIONS 探测
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 根据 URL 前缀判断当前接口要求什么角色，返回 null 表示无需登录
        UserRole requiredRole = resolveRequiredRole(request.getRequestURI());
        if (requiredRole == null) {
            return true;
        }

        // 从请求头取出 Authorization，格式为 "Bearer xxx"
        String authorization = request.getHeader(jwtProperties.getHeaderName());
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }

        // 解析 JWT，验证签名和有效期
        String token = authorization.substring(BEARER_PREFIX.length());
        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getSecretKey(), token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }

        // JWT 中的角色与 URL 要求的角色必须一致，防止跨角色越权
        String role = claims.get("role", String.class);
        if (!requiredRole.name().equals(role)) {
            throw new ForbiddenException("当前账号无权访问该资源");
        }

        // 将用户信息写入当前线程上下文，后续 Controller/Service 通过 BaseContext 即可获取
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
        // 请求结束后清理线程变量，避免线程池复用时串数据
        BaseContext.clear();
    }

    /**
     * 按 URL 路径前缀解析当前接口要求的角色。
     * /api/portal/public/ 是公开接口（如登录、支付回调），返回 null 表示不拦截。
     */
    private UserRole resolveRequiredRole(String uri) {
        if (uri.startsWith("/api/portal/public/")) {
            return null;
        }
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
