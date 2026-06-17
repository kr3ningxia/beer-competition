package com.beercompetition.service;

import com.beercompetition.common.util.Md5Util;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminUserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String usernamePrefix = "it_admin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @AfterEach
    void cleanData() {
        jdbcTemplate.update("DELETE FROM admin_operation_log WHERE summary LIKE ?", "%" + usernamePrefix + "%");
        jdbcTemplate.update("DELETE FROM admin_user WHERE username LIKE ?", usernamePrefix + "%");
    }

    @Test
    void adminCanCreateUpdateDisableAndListAccounts() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_operator", "测试管理员", 1);
        String token = bearerToken(operatorId);
        String managedUsername = usernamePrefix + "_managed";

        mockMvc.perform(post("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "name": "现场执行",
                                  "password": "abc123456"
                                }
                                """.formatted(managedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(managedUsername))
                .andExpect(jsonPath("$.data.status").value(1));
        Long managedId = findAdminId(managedUsername);

        mockMvc.perform(put("/api/admin/accounts/{id}", managedId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"name\":\"现场执行负责人\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("现场执行负责人"));

        mockMvc.perform(patch("/api/admin/accounts/{id}/status", managedId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));

        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token)
                        .param("keyword", usernamePrefix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].username", hasItem(managedUsername)))
                .andExpect(jsonPath("$.data[*].username", hasItem(usernamePrefix + "_operator")));
    }

    @Test
    void disabledAdminTokenCannotAccessPrivateAdminApi() throws Exception {
        Long disabledId = insertAdmin(usernamePrefix + "_disabled", "停用管理员", 0);

        mockMvc.perform(get("/api/admin/me")
                        .header(jwtProperties.getHeaderName(), bearerToken(disabledId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("管理员账号已停用，请重新登录"));
    }

    @Test
    void adminCannotDisableSelf() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_self", "当前管理员", 1);

        mockMvc.perform(patch("/api/admin/accounts/{id}/status", operatorId)
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .contentType("application/json")
                        .content("{\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("不能停用当前登录账号"));
    }

    private Long insertAdmin(String username, String name, Integer status) {
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status)
                VALUES (?, ?, ?, ?)
                """, username, Md5Util.encode("123456"), name, status);
        return findAdminId(username);
    }

    private Long findAdminId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM admin_user WHERE username = ?", Long.class, username);
    }

    private String bearerToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", UserRole.ADMIN.name());
        claims.put("scope", UserRole.ADMIN.name().toLowerCase());
        claims.put("displayName", "测试管理员");
        String token = JwtUtil.createToken(jwtProperties.getSecretKey(), jwtProperties.getAdminTtl(), claims);
        return "Bearer " + token;
    }
}
