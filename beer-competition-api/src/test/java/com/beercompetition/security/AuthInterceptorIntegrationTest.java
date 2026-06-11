package com.beercompetition.security;

import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void privateAdminApiRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateAdminApiRejectsWrongRoleToken() throws Exception {
        mockMvc.perform(get("/api/admin/me")
                        .header(jwtProperties.getHeaderName(), bearerToken(1001L, UserRole.PORTAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void privatePortalApiRejectsAdminToken() throws Exception {
        mockMvc.perform(get("/api/portal/me")
                        .header(jwtProperties.getHeaderName(), bearerToken(1L, UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void privateJudgeApiRejectsPortalToken() throws Exception {
        mockMvc.perform(get("/api/judge/me")
                        .header(jwtProperties.getHeaderName(), bearerToken(1001L, UserRole.PORTAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicPortalApiDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/api/portal/public/home"))
                .andExpect(status().isOk());
    }

    private String bearerToken(Long userId, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role.name());
        claims.put("scope", role.name().toLowerCase());
        claims.put("displayName", "测试用户");
        String token = JwtUtil.createToken(jwtProperties.getSecretKey(), jwtProperties.getAdminTtl(), claims);
        return "Bearer " + token;
    }
}
