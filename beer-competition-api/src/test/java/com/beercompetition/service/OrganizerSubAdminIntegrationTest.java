package com.beercompetition.service;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class OrganizerSubAdminIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private JwtProperties jwt;
    @Autowired private SqlSessionTemplate sqlSession;
    private final String prefix = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private Long organizerId;
    private Long mainId;

    @BeforeEach
    void createOrganizer() {
        jdbc.update("INSERT INTO enterprise_account (account_code, name, status) VALUES (?, ?, 'ACTIVE')", prefix, prefix);
        Long enterpriseId = jdbc.queryForObject("SELECT id FROM enterprise_account WHERE account_code = ?", Long.class, prefix);
        jdbc.update("INSERT INTO organizer (enterprise_account_id, name, organizer_type, status) VALUES (?, ?, 'TENANT', 'ACTIVE')", enterpriseId, prefix);
        organizerId = jdbc.queryForObject("SELECT id FROM organizer WHERE enterprise_account_id = ?", Long.class, enterpriseId);
        jdbc.update("INSERT INTO admin_user (username, password, name, status, admin_type) VALUES (?, MD5('abc123456'), ?, 1, 'ORGANIZER_ADMIN')", prefix, prefix);
        mainId = jdbc.queryForObject("SELECT id FROM admin_user WHERE username = ?", Long.class, prefix);
        jdbc.update("INSERT INTO organizer_member (organizer_id, admin_user_id, status) VALUES (?, ?, 1)", organizerId, mainId);
    }

    @AfterEach
    void clearContext() { BaseContext.clear(); }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"ORGANIZER_ADMIN", "ORGANIZER_SUB_ADMIN"})
    void createInitializeAndManageAccount(String type) throws Exception {
        String expectedType = type == null ? "ORGANIZER_ADMIN" : type;
        long id = createAccount(type);
        assertThat(jdbc.queryForObject("SELECT organizer_id FROM organizer_member WHERE admin_user_id = ?", Long.class, id)).isEqualTo(organizerId);
        mvc.perform(get("/api/admin/me").header(jwt.getHeaderName(), token(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.adminType").value(expectedType))
                .andExpect(jsonPath("$.data.organizerId").value(organizerId));
        mvc.perform(patch("/api/admin/me/credentials").header(jwt.getHeaderName(), token(id)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("username", prefix + "_ready", "newPassword", "new123456"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mustChangePassword").value(false));
        mvc.perform(post("/api/public/admin/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("username", prefix + "_ready", "password", "new123456"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.adminType").value(expectedType))
                .andExpect(jsonPath("$.data.organizerId").value(organizerId));
        mvc.perform(get("/api/admin/accounts").param("keyword", prefix + "_ready").header(jwt.getHeaderName(), token(mainId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].adminType").value(expectedType));
        mvc.perform(put("/api/admin/accounts/{id}", id).header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Updated"));
        mvc.perform(patch("/api/admin/accounts/{id}/password", id).header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content("{\"password\":\"reset123456\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/accounts/{id}/status", id).header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content("{\"status\":0}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/me").header(jwt.getHeaderName(), token(id))).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/accounts/{id}/status", id).header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content("{\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.adminType").value(expectedType));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLATFORM_SUPER_ADMIN", "PLATFORM_EVENT_ADMIN", "UNKNOWN"})
    void rejectInvalidType(String type) throws Exception {
        mvc.perform(post("/api/admin/accounts").header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content(payload(type)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_user WHERE username = ?", Integer.class, prefix + "_new")).isZero();
    }

    @Test
    void subCannotManageAccountsAndInactiveMembershipPreventsLogin() throws Exception {
        long id = createAccount("ORGANIZER_SUB_ADMIN");
        jdbc.update("UPDATE admin_user SET must_change_password = 0, must_change_username = 0 WHERE id = ?", id);
        sqlSession.clearCache();
        mvc.perform(get("/api/admin/accounts").header(jwt.getHeaderName(), token(id)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/accounts").header(jwt.getHeaderName(), token(id))
                        .contentType("application/json").content(payload("ORGANIZER_ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/accounts/{id}", mainId).header(jwt.getHeaderName(), token(id))
                        .contentType("application/json").content("{\"name\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE organizer_member SET status = 0 WHERE admin_user_id = ?", id);
        sqlSession.clearCache();
        mvc.perform(get("/api/admin/me").header(jwt.getHeaderName(), token(id)))
                .andExpect(status().isForbidden());
    }

    @Test
    void mainCannotManageSubInAnotherOrganization() throws Exception {
        long id = createAccount("ORGANIZER_SUB_ADMIN");
        Long otherId = jdbc.queryForObject("SELECT id FROM organizer WHERE organizer_type = 'PLATFORM' LIMIT 1", Long.class);
        jdbc.update("UPDATE organizer_member SET organizer_id = ? WHERE admin_user_id = ?", otherId, id);
        sqlSession.clearCache();
        mvc.perform(get("/api/admin/accounts").param("keyword", prefix + "_new").header(jwt.getHeaderName(), token(mainId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(put("/api/admin/accounts/{id}", id).header(jwt.getHeaderName(), token(mainId))
                        .contentType("application/json").content("{\"name\":\"Changed\"}"))
                .andExpect(status().isForbidden());
    }

    private long createAccount(String type) throws Exception {
        String result = mvc.perform(post("/api/admin/accounts").header(jwt.getHeaderName(), token(mainId)).contentType("application/json").content(payload(type)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mustChangeUsername").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(result).path("data").path("id").asLong();
    }

    private String payload(String type) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("username", prefix + "_new", "name", "Test admin", "password", "abc123456"));
        if (type != null) body.put("adminType", type);
        return json.writeValueAsString(body);
    }

    private String token(Long id) {
        return "Bearer " + JwtUtil.createToken(jwt.getSecretKey(), jwt.getAdminTtl(), new HashMap<>(Map.of("uid", id, "role", "ADMIN", "scope", "admin")));
    }
}
