package com.beercompetition.security;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.competition.query.CompetitionFeedbackQueryService;
import com.beercompetition.competition.query.CompetitionScoringExportService;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.service.JudgeService;
import com.beercompetition.service.JudgePerformanceService;
import com.beercompetition.service.JudgeRecruitmentService;
import com.beercompetition.service.AdminExportService;
import com.beercompetition.service.AdminUserService;
import com.beercompetition.registration.entry.AdminEntryService;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.service.impl.AdminOperationLogServiceImpl;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.vo.AdminOperationLogVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import com.beercompetition.common.util.Md5Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("local")
class OrganizerScopeIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private JudgeService judgeService;

    @Autowired
    private JudgePerformanceService judgePerformanceService;

    @Autowired
    private JudgeRecruitmentService judgeRecruitmentService;

    @Autowired
    private CompetitionFeedbackQueryService feedbackQueryService;

    @Autowired
    private CompetitionScoringExportService scoringExportService;

    @Autowired
    private AdminExportService adminExportService;

    @Autowired
    private AdminEntryService adminEntryService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AdminOperationLogServiceImpl operationLogService;

    @Autowired
    private JdbcTemplate localJdbcTemplate;

    private Long tenantEnterpriseId;
    private Long tenantOrganizerId;
    private Long tenantAdminId;
    private String tenantAdminUsername;

    @AfterEach
    void cleanTenantIdentity() {
        BaseContext.clear();
        if (tenantAdminId != null) {
            localJdbcTemplate.update("DELETE FROM organizer_member WHERE admin_user_id = ?", tenantAdminId);
            localJdbcTemplate.update("DELETE FROM admin_operation_log WHERE admin_user_id = ?", tenantAdminId);
            localJdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", tenantAdminId);
        }
        if (tenantOrganizerId != null) {
            localJdbcTemplate.update("DELETE FROM organizer WHERE id = ?", tenantOrganizerId);
        }
        if (tenantEnterpriseId != null) {
            localJdbcTemplate.update("DELETE FROM enterprise_account WHERE id = ?", tenantEnterpriseId);
        }
    }

    @Test
    void organizerCannotListOrOpenJudgeFromAnotherOrganizer() {
        createTenantIdentity();
        Competition tenantCompetition = tenantCompetition();
        Competition platformCompetition = testData.createCompetition(testRun + "-PLATFORM", CompetitionStatus.DRAFT);

        String basePhone = "139" + String.format("%08d", Math.floorMod(testRun.hashCode(), 100_000_000));
        var ownJudge = testData.createJudge(testRun, "OWN", basePhone, JudgeAccountStatus.ACTIVE);
        var foreignJudge = testData.createJudge(testRun, "FOREIGN", incrementPhone(basePhone), JudgeAccountStatus.ACTIVE);
        var tenantTable = testData.createBaseJudgeTable(tenantCompetition.getId(), testRun + "-tenant-table");
        testData.createAssignment(tenantCompetition.getId(), tenantTable.getId(), ownJudge.getId(), JudgeRoleType.PROFESSIONAL);
        var platformTable = testData.createBaseJudgeTable(platformCompetition.getId(), testRun + "-platform-table");
        testData.createAssignment(platformCompetition.getId(), platformTable.getId(), foreignJudge.getId(), JudgeRoleType.PROFESSIONAL);

        asTenantAdmin();

        List<JudgeAccountVO> visibleJudges = judgeService.listJudges(null, null);
        assertThat(visibleJudges).extracting(JudgeAccountVO::getPublicId)
                .containsExactly(ownJudge.getPublicId());
        assertThatThrownBy(() -> judgeService.getJudgeDetail(foreignJudge.getPublicId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> feedbackQueryService.getFeedbackReviewEntries(platformCompetition.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> scoringExportService.exportScoringData(platformCompetition.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> adminExportService.exportEntries(
                platformCompetition.getId(), null, null, null, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void organizerJudgePerformanceOnlyIncludesItsOwnCompetitions() {
        createTenantIdentity();
        Competition tenantCompetition = tenantCompetition();
        Competition platformCompetition = testData.createCompetition(testRun + "-PERF-PLATFORM", CompetitionStatus.ARCHIVED);
        String phone = "138" + String.format("%08d", Math.floorMod(testRun.hashCode(), 100_000_000));
        var sharedJudge = testData.createJudge(testRun, "PERF-SHARED", phone, JudgeAccountStatus.ACTIVE);
        var tenantTable = testData.createBaseJudgeTable(tenantCompetition.getId(), testRun + "-performance-tenant-table");
        var platformTable = testData.createBaseJudgeTable(platformCompetition.getId(), testRun + "-performance-platform-table");
        testData.createAssignment(tenantCompetition.getId(), tenantTable.getId(), sharedJudge.getId(), JudgeRoleType.PROFESSIONAL);
        testData.createAssignment(platformCompetition.getId(), platformTable.getId(), sharedJudge.getId(), JudgeRoleType.PROFESSIONAL);
        insertConfirmedPerformance(tenantCompetition.getId(), sharedJudge.getId(), "88.0");
        insertConfirmedPerformance(platformCompetition.getId(), sharedJudge.getId(), "96.0");

        asTenantAdmin();

        var history = judgePerformanceService.getAccountHistory(sharedJudge.getPublicId());
        assertThat(history.getEvaluatedCompetitionCount()).isEqualTo(1);
        assertThat(history.getAverageScore()).isEqualByComparingTo("88.0");
        assertThat(history.getHistory()).extracting(item -> item.getCompetitionId())
                .containsExactly(tenantCompetition.getId());
        var overview = judgePerformanceService.listAccountOverviews(List.of(sharedJudge.getPublicId()));
        assertThat(overview).singleElement().satisfies(item -> {
            assertThat(item.getEvaluatedCompetitionCount()).isEqualTo(1);
            assertThat(item.getAverageScore()).isEqualByComparingTo("88.0");
        });
        assertThatThrownBy(() -> judgePerformanceService.listCompetitionPerformances(platformCompetition.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void recruitmentApplicantScoreOnlyCountsOwnOrganizerCompetitions() {
        createTenantIdentity();
        Competition tenantCompetition = tenantCompetition();
        Competition platformCompetition = testData.createCompetition(testRun + "-RECRUIT-PLATFORM", CompetitionStatus.DRAFT);
        String phone = "137" + String.format("%08d", Math.floorMod(testRun.hashCode(), 100_000_000));
        var applicant = testData.createJudge(testRun, "RECRUIT", phone, JudgeAccountStatus.ACTIVE);
        insertConfirmedPerformance(tenantCompetition.getId(), applicant.getId(), "88.0");
        insertConfirmedPerformance(platformCompetition.getId(), applicant.getId(), "96.0");
        Long recruitmentId = insertRecruitment(tenantCompetition.getId());
        insertApplication(recruitmentId, applicant.getId());

        asTenantAdmin();

        var applications = judgeRecruitmentService.applications(recruitmentId, null, null);
        assertThat(applications).singleElement().satisfies(item -> {
            assertThat(item.getJudgePublicId()).isEqualTo(applicant.getPublicId());
            assertThat(item.getJudgeScoreCompetitionCount()).isEqualTo(1);
            assertThat(item.getJudgeScoreAverage()).isEqualByComparingTo("88.0");
            assertThat(item.getJudgeScoreLatest()).isEqualByComparingTo("88.0");
            assertThat(item.getJudgeScoreLatestCompetition()).isEqualTo(tenantCompetition.getName());
        });
    }

    @Test
    void organizerOperationLogsResolveScopeFromCompetitionTarget() {
        createTenantIdentity();
        Competition tenantCompetition = tenantCompetition();
        Competition platformCompetition = testData.createCompetition(testRun + "-LOG-PLATFORM", CompetitionStatus.DRAFT);
        localJdbcTemplate.update("""
                INSERT INTO admin_operation_log (admin_user_id, action, target_type, target_public_id, summary)
                VALUES (?, 'TENANT_COMPETITION_ACTION', 'COMPETITION', ?, 'tenant')
                """, tenantAdminId, String.valueOf(tenantCompetition.getId()));
        localJdbcTemplate.update("""
                INSERT INTO admin_operation_log (admin_user_id, action, target_type, target_public_id, summary)
                VALUES (?, 'PLATFORM_COMPETITION_ACTION', 'COMPETITION', ?, 'platform')
                """, tenantAdminId, String.valueOf(platformCompetition.getId()));

        asTenantAdmin();

        PageResult<AdminOperationLogVO> result = operationLogService.listAdminOperationLogs(
                null, null, null, null, null, null, 1, 100);

        assertThat(result.getRecords()).extracting(AdminOperationLogVO::getTargetPublicId)
                .contains(String.valueOf(tenantCompetition.getId()))
                .doesNotContain(String.valueOf(platformCompetition.getId()));
    }

    @Test
    void organizerEntryListWithoutCompetitionFilterStaysWithinOrganization() {
        createTenantIdentity();
        BeerCompetitionTestData.Fixture tenantFixture = testData.createFixture(testRun + "-TEN");
        localJdbcTemplate.update("UPDATE competition SET organizer_id = ? WHERE id = ?",
                tenantOrganizerId, tenantFixture.competition().getId());
        BeerCompetitionTestData.Fixture platformFixture = testData.createFixture(testRun + "-PLT");

        asTenantAdmin();

        var result = adminEntryService.listAdminEntries(null, null, null, null,
                null, null, null, null, 1, 100);

        assertThat(result.getRecords()).extracting(AdminEntryVO::getId)
                .contains(tenantFixture.entryA1().getId(), tenantFixture.entryA2().getId(), tenantFixture.entryB1().getId())
                .doesNotContain(platformFixture.entryA1().getId(), platformFixture.entryA2().getId(), platformFixture.entryB1().getId());
    }

    @Test
    void platformCanDisplayAccountWithStoppedOrganizerMembership() {
        createTenantIdentity();
        localJdbcTemplate.update("UPDATE organizer_member SET status = 0 WHERE admin_user_id = ?", tenantAdminId);

        asAdmin(1L);

        var users = adminUserService.listAdminUsers(null, null, tenantAdminUsername);

        assertThat(users).anySatisfy(user -> {
            assertThat(user.getUsername()).isEqualTo(tenantAdminUsername);
            assertThat(user.getOrganizerId()).isEqualTo(tenantOrganizerId);
        });
    }

    private Competition tenantCompetition() {
        Competition competition = testData.createCompetition(testRun + "-TENANT", CompetitionStatus.DRAFT);
        localJdbcTemplate.update("UPDATE competition SET organizer_id = ? WHERE id = ?", tenantOrganizerId, competition.getId());
        return competition;
    }

    private void createTenantIdentity() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String accountCode = testRun + "-EA-" + suffix;
        localJdbcTemplate.update("INSERT INTO enterprise_account (account_code, name, status) VALUES (?, ?, 'ACTIVE')",
                accountCode, testRun + " tenant");
        tenantEnterpriseId = localJdbcTemplate.queryForObject(
                "SELECT id FROM enterprise_account WHERE account_code = ?", Long.class, accountCode);
        localJdbcTemplate.update("INSERT INTO organizer (enterprise_account_id, name, organizer_type, status) VALUES (?, ?, 'TENANT', 'ACTIVE')",
                tenantEnterpriseId, testRun + " tenant");
        tenantOrganizerId = localJdbcTemplate.queryForObject(
                "SELECT id FROM organizer WHERE enterprise_account_id = ?", Long.class, tenantEnterpriseId);
        tenantAdminUsername = testRun + "_admin_" + suffix;
        localJdbcTemplate.update("""
                INSERT INTO admin_user (username, password, name, status, admin_type, must_change_password)
                VALUES (?, ?, ?, 1, 'ORGANIZER_ADMIN', 0)
                """, tenantAdminUsername, Md5Util.encode("123456"), testRun + " admin");
        tenantAdminId = localJdbcTemplate.queryForObject(
                "SELECT id FROM admin_user WHERE username = ?", Long.class, tenantAdminUsername);
        localJdbcTemplate.update("INSERT INTO organizer_member (organizer_id, admin_user_id, status) VALUES (?, ?, 1)",
                tenantOrganizerId, tenantAdminId);
    }

    private void asTenantAdmin() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(tenantAdminId)
                .role(UserRole.ADMIN.name())
                .adminType(AdminType.ORGANIZER_ADMIN.name())
                .organizerId(tenantOrganizerId)
                .build());
    }

    private void insertConfirmedPerformance(Long competitionId, Long judgeId, String totalScore) {
        localJdbcTemplate.update("""
                INSERT INTO competition_judge_evaluation
                    (competition_id, judge_account_id, manual_score, comment_score, total_score,
                     status, excellent_candidate, task_completed_count, task_total_count, completion_rate)
                VALUES (?, ?, 60.0, 28.0, ?, 'CONFIRMED', 1, 1, 1, 100.00)
                """, competitionId, judgeId, totalScore);
    }

    private Long insertRecruitment(Long competitionId) {
        String publicId = testRun + "-JR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        localJdbcTemplate.update("""
                INSERT INTO judge_recruitment
                    (public_id, competition_id, status, recruitment_start, recruitment_deadline, venue)
                VALUES (?, ?, 'OPEN', ?, ?, '测试场地')
                """, publicId, competitionId,
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                Timestamp.valueOf(LocalDateTime.now().plusDays(10)));
        return localJdbcTemplate.queryForObject("SELECT id FROM judge_recruitment WHERE public_id = ?", Long.class, publicId);
    }

    private void insertApplication(Long recruitmentId, Long judgeId) {
        localJdbcTemplate.update("""
                INSERT INTO judge_recruitment_application (public_id, recruitment_id, judge_account_id, status)
                VALUES (?, ?, ?, 'APPLIED')
                """, testRun + "-JRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                recruitmentId, judgeId);
    }

    private String incrementPhone(String phone) {
        long value = Long.parseLong(phone) + 1;
        return String.format("%011d", value);
    }
}
