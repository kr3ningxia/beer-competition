package com.beercompetition.security;

import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;

import java.util.Map;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long forcedPasswordAdminId;
    private Long membershipAdminId;
    private Long membershipId;
    private Long membershipOrganizerId;
    private Long membershipEnterpriseId;

    @AfterEach
    void cleanData() {
        if (forcedPasswordAdminId != null) {
            jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", forcedPasswordAdminId);
        }
        if (membershipId != null) {
            jdbcTemplate.update("DELETE FROM organizer_member WHERE id = ?", membershipId);
        }
        if (membershipAdminId != null) {
            jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", membershipAdminId);
        }
        if (membershipOrganizerId != null) {
            jdbcTemplate.update("DELETE FROM organizer WHERE id = ?", membershipOrganizerId);
        }
        if (membershipEnterpriseId != null) {
            jdbcTemplate.update("DELETE FROM enterprise_account WHERE id = ?", membershipEnterpriseId);
        }
    }

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

    @Test
    void adminWithForcedPasswordCanOnlyReadMeAndChangePassword() throws Exception {
        forcedPasswordAdminId = insertForcedPasswordAdmin();
        String token = bearerToken(forcedPasswordAdminId, UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/me")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));

        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/me/password")
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isOk());
    }

    @Test
    void disablingOrganizerMembershipInvalidatesExistingAccessToken() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        jdbcTemplate.update("""
                INSERT INTO enterprise_account (account_code, name, status)
                VALUES (?, ?, 'ACTIVE')
                """, "it-member-ea-" + suffix, "成员关系测试企业");
        membershipEnterpriseId = jdbcTemplate.queryForObject(
                "SELECT id FROM enterprise_account WHERE account_code = ?", Long.class, "it-member-ea-" + suffix);
        jdbcTemplate.update("""
                INSERT INTO organizer (enterprise_account_id, name, organizer_type, status)
                VALUES (?, ?, 'TENANT', 'ACTIVE')
                """, membershipEnterpriseId, "成员关系测试组织");
        membershipOrganizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM organizer WHERE enterprise_account_id = ?", Long.class, membershipEnterpriseId);
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password)
                VALUES (?, MD5(?), ?, 1, 'ORGANIZER_ADMIN', 0)
                """, "it-member-admin-" + suffix, "123456", "成员关系测试管理员");
        membershipAdminId = jdbcTemplate.queryForObject(
                "SELECT id FROM admin_user WHERE username = ?", Long.class, "it-member-admin-" + suffix);
        jdbcTemplate.update("""
                INSERT INTO organizer_member (organizer_id, admin_user_id, status)
                VALUES (?, ?, 1)
                """, membershipOrganizerId, membershipAdminId);
        membershipId = jdbcTemplate.queryForObject("""
                SELECT id FROM organizer_member WHERE organizer_id = ? AND admin_user_id = ?
                """, Long.class, membershipOrganizerId, membershipAdminId);

        String token = bearerToken(membershipAdminId, UserRole.ADMIN);
        mockMvc.perform(get("/api/admin/me")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizerId").value(membershipOrganizerId));

        jdbcTemplate.update("UPDATE organizer_member SET status = 0 WHERE id = ?", membershipId);

        mockMvc.perform(get("/api/admin/me")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isForbidden());
    }

    private Long insertForcedPasswordAdmin() {
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password)
                VALUES (?, MD5(?), ?, 1, 'PLATFORM_SUPER_ADMIN', 1)
                """, "forced_password_" + System.nanoTime(), "123456", "强制改密测试账号");
        return jdbcTemplate.queryForObject("SELECT id FROM admin_user ORDER BY id DESC LIMIT 1", Long.class);
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
