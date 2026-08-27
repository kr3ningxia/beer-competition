package com.beercompetition.security;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.pojo.po.AdminUser;
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
    private static final int ADMIN_STATUS_ACTIVE = 1;

    private final JwtProperties jwtProperties;
    private final AdminUserMapper adminUserMapper;
    private final AdminIdentityService adminIdentityService;

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

        Long userId = parseUserId(claims);
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
        AdminSessionIdentity adminIdentity = null;
        if (requiredRole == UserRole.ADMIN) {
            Long adminId = userId;
            AdminUser adminUser = adminUserMapper.selectById(adminId);
            if (adminUser == null || adminUser.getStatus() == null || adminUser.getStatus() != ADMIN_STATUS_ACTIVE) {
                throw new UnauthorizedException("管理员账号已停用，请重新登录");
            }
            adminIdentity = adminIdentityService.resolve(adminUser);
            if (adminIdentity.mustChangePassword() && !isPasswordSetupEndpoint(request.getRequestURI())) {
                throw new ForbiddenException("首次登录请先修改密码");
            }
        }

        // 将用户信息写入当前线程上下文，后续 Controller/Service 通过 BaseContext 即可获取
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(userId)
                .role(role)
                .displayName(claims.get("displayName", String.class))
                .competitionId(claimLong(claims, "competitionId"))
                .adminType(adminIdentity == null ? claims.get("adminType", String.class) : adminIdentity.adminType().name())
                .organizerId(adminIdentity == null ? claimLong(claims, "organizerId") : adminIdentity.organizerId())
                .mustChangePassword(adminIdentity == null ? claimBoolean(claims, "mustChangePassword") : adminIdentity.mustChangePassword())
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

    private Long parseUserId(Claims claims) {
        return claimLong(claims, "uid");
    }

    private Long claimLong(Claims claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }
    }

    private Boolean claimBoolean(Claims claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            // 该声明仅用于管理员强制改密；普通用户令牌缺少此字段时按 false 处理
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        throw new UnauthorizedException("未登录或登录状态已失效");
    }

    private boolean isPasswordSetupEndpoint(String uri) {
        return "/api/admin/me".equals(uri)
                || "/api/admin/me/password".equals(uri);
    }
}
