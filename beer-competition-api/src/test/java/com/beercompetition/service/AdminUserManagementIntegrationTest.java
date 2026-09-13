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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
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
    void adminListCanFilterByAdminType() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_typefilter", "类型筛选管理员", 1);
        String eventAdminUsername = usernamePrefix + "_eventadmin";
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type)
                VALUES (?, MD5('abc123456'), ?, 1, 'PLATFORM_EVENT_ADMIN')
                """, eventAdminUsername, "赛事管理员");

        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .param("keyword", usernamePrefix)
                        .param("adminType", "PLATFORM_EVENT_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].username", hasItem(eventAdminUsername)))
                .andExpect(jsonPath("$.data[*].adminType", everyItem(equalTo("PLATFORM_EVENT_ADMIN"))));

        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .param("keyword", usernamePrefix)
                        .param("adminType", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest());
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

    @Test
    void createdAdminOnlyNeedsToChangePasswordOnFirstLogin() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_creator", "创建者", 1);
        String managedUsername = usernamePrefix + "_newbie";

        mockMvc.perform(post("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "name": "新建管理员",
                                  "password": "abc123456"
                                }
                                """.formatted(managedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangeUsername").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
        Long newAdminId = findAdminId(managedUsername);

        mockMvc.perform(patch("/api/admin/me/credentials")
                        .header(jwtProperties.getHeaderName(), bearerToken(newAdminId))
                        .contentType("application/json")
                        .content("{\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(managedUsername))
                .andExpect(jsonPath("$.data.mustChangeUsername").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));

        mockMvc.perform(post("/api/public/admin/login")
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"654321\"}".formatted(managedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    void adminCannotResetOwnPasswordFromAccountList() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_selfreset", "当前管理员", 1);
        String token = bearerToken(operatorId);

        mockMvc.perform(patch("/api/admin/accounts/{id}/password", operatorId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"password\":\"new123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("请到账号设置修改自己的密码"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT must_change_password FROM admin_user WHERE id = ?", Integer.class, operatorId)).isZero();
        mockMvc.perform(get("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token))
                .andExpect(status().isOk());
    }

    @Test
    void adminPasswordResetDoesNotForceTargetToChangeCredentials() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_resetop", "重置操作人", 1);
        String managedUsername = usernamePrefix + "_resettarget";

        mockMvc.perform(post("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "name": "待重置管理员",
                                  "password": "abc123456"
                                }
                                """.formatted(managedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
        Long managedId = findAdminId(managedUsername);

        mockMvc.perform(patch("/api/admin/accounts/{id}/password", managedId)
                        .header(jwtProperties.getHeaderName(), bearerToken(operatorId))
                        .contentType("application/json")
                        .content("{\"password\":\"reset123456\"}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT must_change_password FROM admin_user WHERE id = ?", Integer.class, managedId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT must_change_username FROM admin_user WHERE id = ?", Integer.class, managedId)).isZero();

        mockMvc.perform(post("/api/public/admin/login")
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"reset123456\"}".formatted(managedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false))
                .andExpect(jsonPath("$.data.mustChangeUsername").value(false));
    }

    @Test
    void platformSuperAdminCanCreateEachPlatformTypeAndCannotCreateOrganizerType() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_platform", "平台管理员", 1);
        String token = bearerToken(operatorId);

        for (String adminType : new String[]{"PLATFORM_SUPER_ADMIN", "PLATFORM_EVENT_ADMIN"}) {
            String username = usernamePrefix + "_" + adminType.toLowerCase();
            mockMvc.perform(post("/api/admin/accounts")
                            .header(jwtProperties.getHeaderName(), token)
                            .contentType("application/json")
                            .content("""
                                    {"username": "%s", "name": "平台账号", "password": "abc123456", "adminType": "%s"}
                                    """.formatted(username, adminType)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.adminType").value(adminType));
        }

        mockMvc.perform(post("/api/admin/accounts")
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("""
                                {"username": "%s", "name": "越权账号", "password": "abc123456", "adminType": "ORGANIZER_ADMIN"}
                                """.formatted(usernamePrefix + "_escalate")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminTypeCanBeChangedWithinSameSideOnlyAndNeverOnSelf() throws Exception {
        Long operatorId = insertAdmin(usernamePrefix + "_typer", "类型管理员", 1);
        String token = bearerToken(operatorId);
        Long targetId = insertAdmin(usernamePrefix + "_target", "待调整管理员", 1);

        mockMvc.perform(put("/api/admin/accounts/{id}", targetId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"name\":\"待调整管理员\",\"adminType\":\"PLATFORM_EVENT_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminType").value("PLATFORM_EVENT_ADMIN"));

        mockMvc.perform(put("/api/admin/accounts/{id}", targetId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"name\":\"待调整管理员\",\"adminType\":\"ORGANIZER_ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("只能在同一侧内调整管理员类型"));

        mockMvc.perform(put("/api/admin/accounts/{id}", operatorId)
                        .header(jwtProperties.getHeaderName(), token)
                        .contentType("application/json")
                        .content("{\"name\":\"类型管理员\",\"adminType\":\"PLATFORM_EVENT_ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("不能修改自己的管理员类型"));
    }

    @Test
    void distributedAdminCanSetNewUsernameAndPasswordThenLoginAgain() throws Exception {
        String initialUsername = usernamePrefix + "_distributed";
        String finalUsername = usernamePrefix + "_official";
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password, must_change_username)
                VALUES (?, MD5(?), ?, 1, 'PLATFORM_SUPER_ADMIN', 1, 1)
                """, initialUsername, "123456", "入驻主办方");
        Long adminId = findAdminId(initialUsername);

        mockMvc.perform(patch("/api/admin/me/credentials")
                        .header(jwtProperties.getHeaderName(), bearerToken(adminId))
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"newPassword\":\"654321\"}".formatted(finalUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(finalUsername))
                .andExpect(jsonPath("$.data.mustChangeUsername").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));

        mockMvc.perform(post("/api/public/admin/login")
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"654321\"}".formatted(finalUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(finalUsername))
                .andExpect(jsonPath("$.data.mustChangeUsername").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    void distributedAdminCannotSkipUsernameSetupWithLegacyPasswordEndpoint() throws Exception {
        String initialUsername = usernamePrefix + "_blocked";
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password, must_change_username)
                VALUES (?, MD5(?), ?, 1, 'PLATFORM_SUPER_ADMIN', 1, 1)
                """, initialUsername, "123456", "待设置主办方");
        Long adminId = findAdminId(initialUsername);

        mockMvc.perform(patch("/api/admin/me/password")
                        .header(jwtProperties.getHeaderName(), bearerToken(adminId))
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("请先完成账号设置"));
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
