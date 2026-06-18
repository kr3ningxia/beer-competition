package com.beercompetition.security;

import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.RefreshTokenRequest;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class AuthServiceRefreshTokenRedisIntegrationTest {

    private static final String REFRESH_KEY_PREFIX = "beer-competition:auth:refresh:";

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long adminUserId;
    private String refreshTokenToCleanup;

    @AfterEach
    void cleanup() {
        if (refreshTokenToCleanup != null) {
            redisTemplate.delete(refreshKey(refreshTokenToCleanup));
        }
        if (adminUserId != null) {
            adminUserMapper.deleteById(adminUserId);
        }
    }

    @Test
    void refreshTokenRotatesAndDeletesOldRedisKey() {
        AdminUser adminUser = createAdminUser();
        LoginResponse loginResponse = authService.adminLogin(loginRequest(adminUser.getUsername(), "123456"));
        String oldRefreshToken = loginResponse.getRefreshToken();
        assertThat(redisTemplate.hasKey(refreshKey(oldRefreshToken))).isTrue();

        LoginResponse refreshResponse = authService.refreshToken(refreshRequest(oldRefreshToken));
        refreshTokenToCleanup = refreshResponse.getRefreshToken();

        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(oldRefreshToken);
        assertThat(redisTemplate.hasKey(refreshKey(oldRefreshToken))).isFalse();
        assertThat(redisTemplate.hasKey(refreshKey(refreshResponse.getRefreshToken()))).isTrue();
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest(oldRefreshToken)))
                .isInstanceOf(UnauthorizedException.class);
    }

    private AdminUser createAdminUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AdminUser adminUser = AdminUser.builder()
                .username("refresh_" + suffix)
                .password(Md5Util.encode("123456"))
                .name("刷新测试管理员")
                .status(1)
                .build();
        adminUserMapper.insert(adminUser);
        adminUserId = adminUser.getId();
        return adminUser;
    }

    private AdminLoginRequest loginRequest(String username, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private RefreshTokenRequest refreshRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private String refreshKey(String refreshToken) {
        return REFRESH_KEY_PREFIX + refreshToken.substring(0, refreshToken.indexOf('.'));
    }
}
