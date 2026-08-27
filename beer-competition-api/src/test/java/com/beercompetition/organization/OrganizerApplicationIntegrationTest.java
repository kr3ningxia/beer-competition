package com.beercompetition.organization;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.organization.application.OrganizerApplicationService;
import com.beercompetition.organization.application.OrganizerProvisioningService;
import com.beercompetition.pojo.dto.OrganizerApplicationReviewRequest;
import com.beercompetition.pojo.dto.OrganizerApplicationSubmitRequest;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.OrganizerApplicationStatus;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.OrganizerApplicationSubmitVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class OrganizerApplicationIntegrationTest {

    @Autowired
    private OrganizerApplicationService applicationService;

    @Autowired
    private OrganizerProvisioningService provisioningService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long applicationId;
    private Long platformAdminId;
    private Long issuedOrganizerId;
    private Long issuedAdminId;
    private Long foreignApplicationId;
    private Long legacyApplicationId;

    @AfterEach
    void cleanData() {
        BaseContext.clear();
        if (applicationId != null) {
            List<String> paths = jdbcTemplate.query("SELECT storage_path FROM file_asset WHERE owner_type = 'ORGANIZER_APPLICATION' AND owner_id = ?",
                    (rs, rowNum) -> rs.getString(1), applicationId);
            paths.forEach(this::deleteStoredFile);
            jdbcTemplate.update("DELETE FROM file_asset WHERE owner_type = 'ORGANIZER_APPLICATION' AND owner_id = ?", applicationId);
            jdbcTemplate.update("DELETE FROM admin_operation_log WHERE target_type = 'ORGANIZER_APPLICATION' AND target_public_id = ?",
                    String.valueOf(applicationId));
            jdbcTemplate.update("DELETE FROM organizer_application WHERE id = ?", applicationId);
        }
        if (foreignApplicationId != null) {
            jdbcTemplate.update("DELETE FROM organizer_application WHERE id = ?", foreignApplicationId);
        }
        if (legacyApplicationId != null) {
            jdbcTemplate.update("DELETE FROM organizer_application WHERE id = ?", legacyApplicationId);
        }
        if (issuedAdminId != null) {
            jdbcTemplate.update("DELETE FROM organizer_member WHERE admin_user_id = ?", issuedAdminId);
            jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", issuedAdminId);
        }
        if (issuedOrganizerId != null) {
            Long enterpriseId = jdbcTemplate.queryForObject("SELECT enterprise_account_id FROM organizer WHERE id = ?",
                    Long.class, issuedOrganizerId);
            jdbcTemplate.update("DELETE FROM organizer WHERE id = ?", issuedOrganizerId);
            if (enterpriseId != null) {
                jdbcTemplate.update("DELETE FROM enterprise_account WHERE id = ?", enterpriseId);
            }
        }
        if (platformAdminId != null) {
            jdbcTemplate.update("DELETE FROM admin_operation_log WHERE admin_user_id = ?", platformAdminId);
            jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", platformAdminId);
        }
    }

    @Test
    void signedInPortalApplicationScopeUsesAccountOwnership() {
        var account = jdbcTemplate.queryForMap(
                "SELECT id, phone FROM portal_account WHERE status = 1 ORDER BY id LIMIT 1");
        Long portalAccountId = ((Number) account.get("id")).longValue();
        String portalPhone = (String) account.get("phone");
        Long foreignPortalAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM portal_account WHERE status = 1 AND id <> ? ORDER BY id LIMIT 1",
                Long.class, portalAccountId);

        OrganizerApplicationSubmitRequest ownRequest = request("登录账号归属测试");
        ownRequest.setContactPhone(portalPhone);
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(portalAccountId)
                .role(UserRole.PORTAL.name())
                .build());
        OrganizerApplicationSubmitVO ownResult = applicationService.submitForCurrentPortal(ownRequest);
        applicationId = findApplicationId(ownResult.getApplicationNo());

        OrganizerApplicationSubmitRequest foreignRequest = request("其他账号申请测试");
        OrganizerApplicationSubmitVO foreignResult = applicationService.submit(foreignRequest);
        foreignApplicationId = findApplicationId(foreignResult.getApplicationNo());
        jdbcTemplate.update("UPDATE organizer_application SET portal_account_id = ? WHERE id = ?",
                foreignPortalAccountId, foreignApplicationId);

        OrganizerApplicationSubmitRequest legacyRequest = request("历史公开申请兼容测试");
        legacyRequest.setContactPhone(portalPhone);
        OrganizerApplicationSubmitVO legacyResult = applicationService.submit(legacyRequest);
        legacyApplicationId = findApplicationId(legacyResult.getApplicationNo());

        var visible = applicationService.listForCurrentPortal();
        assertThat(visible).extracting(item -> item.getApplicationNo())
                .contains(ownResult.getApplicationNo())
                .contains(legacyResult.getApplicationNo())
                .doesNotContain(foreignResult.getApplicationNo());
        assertThat(visible).filteredOn(item -> ownResult.getApplicationNo().equals(item.getApplicationNo()))
                .singleElement()
                .satisfies(item -> assertThat(item.getAdminUsername()).isNull());
    }

    @Test
    void multipartMaterialIsBoundToCreatedApplicationAndHasNoPublicUrl() {
        OrganizerApplicationSubmitRequest request = request("材料绑定测试");
        MockMultipartFile material = new MockMultipartFile(
                "material", "../business-license.pdf", "application/pdf", "pdf-content".getBytes(StandardCharsets.UTF_8));

        OrganizerApplicationSubmitVO result = applicationService.submit(request, material);
        applicationId = findApplicationId(result.getApplicationNo());

        Long assetId = jdbcTemplate.queryForObject("SELECT material_asset_id FROM organizer_application WHERE id = ?",
                Long.class, applicationId);
        assertThat(assetId).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT business_type FROM file_asset WHERE id = ?", String.class, assetId))
                .isEqualTo("ORGANIZER_APPLICATION_MATERIAL");
        assertThat(jdbcTemplate.queryForObject("SELECT owner_type FROM file_asset WHERE id = ?", String.class, assetId))
                .isEqualTo("ORGANIZER_APPLICATION");
        assertThat(jdbcTemplate.queryForObject("SELECT owner_id FROM file_asset WHERE id = ?", Long.class, assetId))
                .isEqualTo(applicationId);
        assertThat(jdbcTemplate.queryForObject("SELECT public_url FROM file_asset WHERE id = ?", String.class, assetId))
                .isNull();
    }

    @Test
    void clientCannotBindArbitraryMaterialAssetId() {
        OrganizerApplicationSubmitRequest request = request("非法文件绑定测试");
        request.setMaterialAssetId(1L);

        assertThatThrownBy(() -> applicationService.submit(request))
                .isInstanceOf(BaseException.class)
                .hasMessage("材料文件必须通过上传接口提交");
    }

    @Test
    void reviewAndProvisionAreIdempotentAndInitialPasswordIsOneTimeOnly() {
        applicationId = findApplicationId(applicationService.submit(request("账号发放测试")).getApplicationNo());
        platformAdminId = insertPlatformAdmin();
        asPlatformAdmin();

        review(OrganizerApplicationStatus.UNDER_REVIEW);
        review(OrganizerApplicationStatus.APPROVED);

        OrganizerProvisioningService.ProvisioningResult first = provisioningService.provisionApprovedApplication(applicationId);
        issuedOrganizerId = first.organizerId();
        issuedAdminId = first.adminUserId();
        assertThat(first.username()).isEqualTo("org_admin_" + applicationId);
        assertThat(first.initialPassword()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject("SELECT must_change_password FROM admin_user WHERE id = ?", Integer.class, issuedAdminId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT password FROM admin_user WHERE id = ?", String.class, issuedAdminId))
                .isNotEqualTo(first.initialPassword());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_operation_log WHERE target_type = 'ORGANIZER_APPLICATION' AND target_public_id = ? AND summary LIKE ?",
                Integer.class, String.valueOf(applicationId), "%" + first.initialPassword() + "%"))
                .isZero();

        OrganizerProvisioningService.ProvisioningResult second = provisioningService.provisionApprovedApplication(applicationId);
        assertThat(second.organizerId()).isEqualTo(first.organizerId());
        assertThat(second.adminUserId()).isEqualTo(first.adminUserId());
        assertThat(second.initialPassword()).isNull();
    }

    private OrganizerApplicationSubmitRequest request(String organizationName) {
        OrganizerApplicationSubmitRequest request = new OrganizerApplicationSubmitRequest();
        request.setOrganizationName(organizationName);
        request.setContactName("申请联系人");
        request.setContactPhone("138" + String.format("%08d", Math.abs(System.nanoTime()) % 100000000L));
        return request;
    }

    private Long findApplicationId(String applicationNo) {
        return jdbcTemplate.queryForObject("SELECT id FROM organizer_application WHERE application_no = ?", Long.class, applicationNo);
    }

    private Long insertPlatformAdmin() {
        String username = "it-platform-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password)
                VALUES (?, ?, '申请审核测试管理员', 1, 'PLATFORM_SUPER_ADMIN', 0)
                """, username, Md5Util.encode("123456"));
        return jdbcTemplate.queryForObject("SELECT id FROM admin_user WHERE username = ?", Long.class, username);
    }

    private void asPlatformAdmin() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(platformAdminId)
                .role(UserRole.ADMIN.name())
                .adminType(AdminType.PLATFORM_SUPER_ADMIN.name())
                .displayName("申请审核测试管理员")
                .build());
    }

    private void review(OrganizerApplicationStatus status) {
        OrganizerApplicationReviewRequest request = new OrganizerApplicationReviewRequest();
        request.setStatus(status);
        request.setRemark("测试审核");
        applicationService.review(applicationId, request);
    }

    private void deleteStoredFile(String storagePath) {
        if (storagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (Exception ignored) {
            // 测试清理不影响业务断言。
        }
    }
}
