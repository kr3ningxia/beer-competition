package com.beercompetition.security;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    private static final String SECRET_KEY = "auth-interceptor-test-secret-123456";

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private AdminIdentityService adminIdentityService;

    private JwtProperties jwtProperties;
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(SECRET_KEY);
        jwtProperties.setHeaderName("Authorization");
        authInterceptor = new AuthInterceptor(jwtProperties, adminUserMapper, adminIdentityService);
    }

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @Test
    void portalTokenWithoutPasswordClaimDefaultsToFalse() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", 1461L);
        claims.put("role", "PORTAL");
        claims.put("scope", "portal");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/portal/me");
        request.addHeader("Authorization", "Bearer " + JwtUtil.createToken(SECRET_KEY, 60_000, claims));

        boolean handled = authInterceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(handled).isTrue();
        assertThat(BaseContext.getCurrentUser().getMustChangePassword()).isFalse();
    }
}
