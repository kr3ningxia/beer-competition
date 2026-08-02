package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.dto.AdminEntryDeleteRequest;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.CompetitionRefundPolicyUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntryLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private EntryService entryService;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private RoundService roundService;

    @Test
    void simulatePaymentIsOwnedAndIdempotentForPendingEntry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var pending = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-待支付", EntryStatus.PENDING_PAYMENT, false);

        asPortal(fixture.portalA().account().getId());
        var paid = entryService.simulatePayment(pending.getId());
        assertThat(paid.getStatus()).isEqualTo(EntryStatus.REGISTERED.name());
        assertThat(paid.getPayment().getStatus()).isEqualTo(EntryPaymentStatus.PAID.name());

        var repeated = entryService.simulatePayment(pending.getId());
        assertThat(repeated.getStatus()).isEqualTo(EntryStatus.REGISTERED.name());
        assertThat(repeated.getPayment().getStatus()).isEqualTo(EntryPaymentStatus.PAID.name());
    }

    @Test
    void canceledEntryCannotBePaid() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var canceled = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-已取消", EntryStatus.CANCELED, false);

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> entryService.simulatePayment(canceled.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("不能支付");
    }

    @Test
    void submitEntryRejectsExtraFieldValueLongerThanStorageLimit() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("""
                INSERT INTO entry_field_config
                    (competition_id, field_key, field_label, field_type, required_flag, visible_to_judges, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, fixture.competition().getId(), "longNote", "补充说明", "textarea", 0, 1, 1);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());

        PortalEntrySubmitRequest request = new PortalEntrySubmitRequest();
        request.setName(testRun + "-超长补充字段");
        request.setCategoryId(fixture.category().getId());
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.0"));
        request.setExtraFields(Map.of("longNote", "a".repeat(256)));

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> entryService.submitPortalEntry(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("补充说明不能超过255个字符");
    }

    @Test
    void submitEntrySupportsSingleSelectAndRejectsUnknownOption() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("""
                INSERT INTO entry_field_config
                    (competition_id, field_key, field_label, field_type, options_json, required_flag, visible_to_judges, sort_order)
                VALUES (?, 'color', '颜色', 'select', '[\"金色\",\"琥珀\"]', 1, 0, 1)
                """, fixture.competition().getId());
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());
        PortalEntrySubmitRequest request = new PortalEntrySubmitRequest();
        request.setName(testRun + "-单选");
        request.setCategoryId(fixture.category().getId());
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.0"));
        request.setExtraFields(Map.of("color", "金色"));
        asPortal(fixture.portalA().account().getId());
        var submitted = entryService.submitPortalEntry(fixture.competition().getId(), request);
        assertThat(submitted.getExtraFields()).anySatisfy(field -> assertThat(field.getValue()).isEqualTo("金色"));

        request.setName(testRun + "-单选非法");
        request.setExtraFields(Map.of("color", "黑色"));
        assertThatThrownBy(() -> entryService.submitPortalEntry(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("选项不合法");
    }

    @Test
    void judgeViewsExposeOnlyJudgeVisibleExtraFieldsAndKeepBoundStyleSnapshot() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        Long boundStyleId = jdbcTemplate.queryForObject("""
                SELECT id FROM competition_style_config
                WHERE competition_id = ? AND name = ?
                ORDER BY id ASC
                LIMIT 1
                """, Long.class, fixture.competition().getId(), testRun + "-风格");
        jdbcTemplate.update("""
                UPDATE competition_style_config
                SET description = '历史风格说明', active_flag = 0
                WHERE id = ?
                """, boundStyleId);
        var replacementStyle = testData.createStyle(fixture.competition().getId(), testRun + "-风格");
        jdbcTemplate.update("""
                UPDATE competition_style_config
                SET description = '当前风格说明', active_flag = 1
                WHERE id = ?
                """, replacementStyle.getId());
        jdbcTemplate.update("UPDATE beer_entry SET style_config_id = ? WHERE id = ?",
                boundStyleId, fixture.entryA1().getId());
        jdbcTemplate.update("""
                INSERT INTO entry_field_config
                    (competition_id, field_key, field_label, field_type, required_flag,
                     visible_to_judges, sort_order, active_flag)
                VALUES
                    (?, 'visibleNote', '评审备注', 'text', 0, 1, 1, 1),
                    (?, 'hiddenNote', '厂商内部信息', 'text', 0, 0, 2, 1),
                    (?, 'retiredNote', '历史评审备注', 'text', 0, 1, 3, 0)
                """, fixture.competition().getId(), fixture.competition().getId(), fixture.competition().getId());
        jdbcTemplate.update("""
                INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
                VALUES
                    (?, 'visibleNote', '评审备注', '酒花香明显'),
                    (?, 'hiddenNote', '厂商内部信息', '不要展示'),
                    (?, 'retiredNote', '历史评审备注', '保留历史值')
                """, fixture.entryA1().getId(), fixture.entryA1().getId(), fixture.entryA1().getId());
        var rankingRound = testData.createRankingRound(fixture, List.of(fixture.entryA1()),
                RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.professional().getId());
        var judgeEntry = entryService.getJudgeEntry(fixture.entryA1().getUuid());
        assertThat(judgeEntry.getStyleDescription()).isEqualTo("历史风格说明");
        assertThat(judgeEntry.getExtraFields())
                .extracting(field -> field.getKey())
                .containsExactlyInAnyOrder("visibleNote", "retiredNote");

        var roundTable = roundService.getMyRoundTable(rankingRound.table().getId());
        var roundEntry = roundTable.getEntries().get(0);
        assertThat(roundEntry.getStyleDescription()).isEqualTo("历史风格说明");
        assertThat(roundEntry.getExtraFields())
                .extracting(field -> field.getKey())
                .containsExactlyInAnyOrder("visibleNote", "retiredNote");
    }

    @Test
    void adminCanDeletePaidEntryAfterExplicitRefundConfirmation() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-待删除", EntryStatus.REGISTERED, true);
        String shortCode = jdbcTemplate.queryForObject("SELECT short_code FROM entry_scan_label WHERE beer_entry_id = ?",
                String.class, entry.getId());
        asAdmin(1L);
        AdminEntryDeleteRequest request = new AdminEntryDeleteRequest();
        request.setReason("测试清理重复报名");
        request.setConfirmationCode(shortCode);
        request.setPaymentDisposition("MANUAL_REFUNDED");
        request.setHighRiskConfirmed(false);
        entryService.administrativelyDeleteEntry(entry.getId(), request);

        assertThat(jdbcTemplate.queryForObject("SELECT deleted_flag FROM beer_entry WHERE id = ?", Integer.class, entry.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_scan_label WHERE beer_entry_id = ?", String.class, entry.getId())).isEqualTo("DISABLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?", String.class, entry.getId())).isEqualTo("REFUNDED");
    }

    @Test
    void portalUpdateEntryKeepsCategoryAndLabelIdentity() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-可修改", EntryStatus.REGISTERED, true);
        testData.createStyle(fixture.competition().getId(), testRun + "-新风格");
        jdbcTemplate.update("""
                INSERT INTO entry_field_config
                    (competition_id, field_key, field_label, field_type, required_flag, visible_to_judges, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, fixture.competition().getId(), "note", "补充说明", "text", 0, 1, 1);

        String oldLabelCode = jdbcTemplate.queryForObject(
                "SELECT label_code FROM entry_scan_label WHERE beer_entry_id = ?",
                String.class,
                entry.getId());
        String oldShortCode = jdbcTemplate.queryForObject(
                "SELECT short_code FROM entry_scan_label WHERE beer_entry_id = ?",
                String.class,
                entry.getId());
        String oldScanToken = jdbcTemplate.queryForObject(
                "SELECT scan_token FROM entry_scan_label WHERE beer_entry_id = ?",
                String.class,
                entry.getId());

        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-修改后酒名");
        request.setStyle(testRun + "-新风格");
        request.setAbv(new BigDecimal("6.2"));
        request.setExtraFields(Map.of("note", "已修正"));

        asPortal(fixture.portalA().account().getId());
        var updated = entryService.updatePortalEntry(entry.getId(), request);

        assertThat(updated.getName()).isEqualTo(testRun + "-修改后酒名");
        assertThat(updated.getStyle()).isEqualTo(testRun + "-新风格");
        assertThat(updated.getCategoryId()).isEqualTo(fixture.category().getId());
        assertThat(updated.getLabelCode()).isEqualTo(oldLabelCode);
        assertThat(updated.getShortCode()).isEqualTo(oldShortCode);
        assertThat(updated.getScanToken()).isEqualTo(oldScanToken);
        assertThat(updated.getExtraFields()).anySatisfy(field -> {
            assertThat(field.getKey()).isEqualTo("note");
            assertThat(field.getValue()).isEqualTo("已修正");
        });
    }

    @Test
    void portalUpdateEntryIsAllowedDuringSampleCheckAfterRegistrationDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-截止后修改", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("UPDATE competition SET status = ?, registration_deadline = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), LocalDateTime.now().minusDays(1),
                fixture.competition().getId());

        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-修改后");
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.5"));

        asPortal(fixture.portalA().account().getId());
        var detail = entryService.getPortalEntry(entry.getId());
        var updated = entryService.updatePortalEntry(entry.getId(), request);

        assertThat(detail.getCanUpdateInfo()).isTrue();
        assertThat(detail.getUpdateInfoDisabledReason()).isNull();
        assertThat(updated.getName()).isEqualTo(testRun + "-修改后");
    }

    @Test
    void portalUpdateEntryIsAllowedAfterRegistrationIsClosedEarly() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-提前截止后修改", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("UPDATE competition SET status = ?, registration_deadline = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), LocalDateTime.now().plusDays(1),
                fixture.competition().getId());

        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-修改后");
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.5"));

        asPortal(fixture.portalA().account().getId());
        var detail = entryService.getPortalEntry(entry.getId());
        var updated = entryService.updatePortalEntry(entry.getId(), request);

        assertThat(detail.getCanUpdateInfo()).isTrue();
        assertThat(detail.getUpdateInfoDisabledReason()).isNull();
        assertThat(updated.getName()).isEqualTo(testRun + "-修改后");
    }

    @Test
    void portalUpdateEntryIsRejectedAfterSampleStored() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), fixture.competition().getId());
        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-入库后修改");
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.5"));

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> entryService.updatePortalEntry(fixture.entryA1().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("样品已入库");
    }

    @Test
    void portalUpdateEntryIsRejectedAfterJudgingPreparationStarts() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-评审准备后修改", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.JUDGING_PREP.name(), fixture.competition().getId());

        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-修改后");
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.5"));

        asPortal(fixture.portalA().account().getId());
        var detail = entryService.getPortalEntry(entry.getId());

        assertThat(detail.getCanUpdateInfo()).isFalse();
        assertThat(detail.getUpdateInfoDisabledReason()).contains("赛事已进入评审准备");
        assertThatThrownBy(() -> entryService.updatePortalEntry(entry.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("赛事已进入评审准备");
    }

    @Test
    void autoApprovedManualPaymentRefundWaitsForOfflineCompletion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var refundable = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-可退款", EntryStatus.REGISTERED, true);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("测试退款");
        entryService.requestPortalEntryRefund(refundable.getId(), request);

        Long refundId = jdbcTemplate.queryForObject(
                "SELECT id FROM entry_refund WHERE beer_entry_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                refundable.getId());

        var entryStatus = jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, refundable.getId());
        var paymentStatus = jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, refundable.getId());
        var refundStatus = jdbcTemplate.queryForObject("SELECT status FROM entry_refund WHERE id = ?",
                String.class, refundId);

        assertThat(entryStatus).isEqualTo(EntryStatus.REGISTERED.name());
        assertThat(paymentStatus).isEqualTo(EntryPaymentStatus.PAID.name());
        assertThat(refundStatus).isEqualTo(EntryRefundStatus.APPROVED.name());
    }

    @Test
    void manualReviewModeKeepsRequestPendingUntilAdminDecision() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET refund_approval_mode = ? WHERE id = ?",
                RefundApprovalMode.MANUAL_REVIEW.name(), fixture.competition().getId());
        var refundable = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-人工审批退款", EntryStatus.REGISTERED, true);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("等待管理员审批");
        entryService.requestPortalEntryRefund(refundable.getId(), request);

        Map<String, Object> refund = jdbcTemplate.queryForMap("""
                SELECT status, approval_mode_snapshot
                FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, refundable.getId());
        assertThat(refund.get("status")).isEqualTo(EntryRefundStatus.REQUESTED.name());
        assertThat(refund.get("approval_mode_snapshot")).isEqualTo(RefundApprovalMode.MANUAL_REVIEW.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, refundable.getId())).isEqualTo(EntryStatus.REGISTERED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, refundable.getId())).isEqualTo(EntryPaymentStatus.PAID.name());

        Long refundId = jdbcTemplate.queryForObject("""
                SELECT id FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
        """, Long.class, refundable.getId());
        asAdmin(1L);
        AdminEntryStatusRequest approveRequest = new AdminEntryStatusRequest();
        approveRequest.setReason("同意退款");
        entryService.approveRefund(refundId, approveRequest);

        assertThat(latestRefundStatus(refundable.getId())).isEqualTo(EntryRefundStatus.APPROVED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, refundable.getId())).isEqualTo(EntryStatus.REGISTERED.name());
    }

    @Test
    void manualReviewRefundCanBeRejectedWithoutInvalidatingPayment() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET refund_approval_mode = ? WHERE id = ?",
                RefundApprovalMode.MANUAL_REVIEW.name(), fixture.competition().getId());
        var refundable = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-驳回退款", EntryStatus.REGISTERED, true);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest refundRequest = new PortalEntryRefundRequest();
        refundRequest.setReason("申请退款");
        entryService.requestPortalEntryRefund(refundable.getId(), refundRequest);
        Long refundId = jdbcTemplate.queryForObject("""
                SELECT id FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, refundable.getId());

        asAdmin(1L);
        AdminEntryStatusRequest rejectRequest = new AdminEntryStatusRequest();
        rejectRequest.setReason("不符合退款条件");
        entryService.rejectRefund(refundId, rejectRequest);

        assertThat(latestRefundStatus(refundable.getId())).isEqualTo(EntryRefundStatus.REJECTED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, refundable.getId())).isEqualTo(EntryPaymentStatus.PAID.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, refundable.getId())).isEqualTo(EntryStatus.REGISTERED.name());
    }

    @Test
    void changingRefundPolicyOnlyAffectsLaterRequests() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET refund_approval_mode = ? WHERE id = ?",
                RefundApprovalMode.MANUAL_REVIEW.name(), fixture.competition().getId());
        var pendingReview = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-切换前", EntryStatus.REGISTERED, true);
        var autoApproved = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-切换后", EntryStatus.REGISTERED, true);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("切换审批方式");
        entryService.requestPortalEntryRefund(pendingReview.getId(), request);

        asAdmin(1L);
        CompetitionRefundPolicyUpdateRequest policyRequest = new CompetitionRefundPolicyUpdateRequest();
        policyRequest.setRefundApprovalMode(RefundApprovalMode.AUTO_APPROVE.name());
        competitionService.updateRefundPolicy(fixture.competition().getId(), policyRequest);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM admin_operation_log
                WHERE action = 'COMPETITION_REFUND_POLICY_UPDATE'
                  AND target_public_id = ?
                """, Integer.class, String.valueOf(fixture.competition().getId()))).isEqualTo(1);

        asPortal(fixture.portalA().account().getId());
        entryService.requestPortalEntryRefund(autoApproved.getId(), request);

        assertThat(latestRefundStatus(pendingReview.getId())).isEqualTo(EntryRefundStatus.REQUESTED.name());
        assertThat(latestRefundStatus(autoApproved.getId())).isEqualTo(EntryRefundStatus.APPROVED.name());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT approval_mode_snapshot FROM entry_refund
                WHERE beer_entry_id = ? ORDER BY id DESC LIMIT 1
                """, String.class, pendingReview.getId())).isEqualTo(RefundApprovalMode.MANUAL_REVIEW.name());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT approval_mode_snapshot FROM entry_refund
                WHERE beer_entry_id = ? ORDER BY id DESC LIMIT 1
                """, String.class, autoApproved.getId())).isEqualTo(RefundApprovalMode.AUTO_APPROVE.name());
    }

    @Test
    void refundPolicyCannotChangeAfterRefundDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), fixture.competition().getId());
        CompetitionRefundPolicyUpdateRequest request = new CompetitionRefundPolicyUpdateRequest();
        request.setRefundApprovalMode(RefundApprovalMode.MANUAL_REVIEW.name());

        asAdmin(1L);

        assertThatThrownBy(() -> competitionService.updateRefundPolicy(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("退款申请时间已截止");
    }

    @Test
    void storedEntryManualPaymentRefundWaitsForOfflineCompletion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("入库后退款");
        entryService.requestPortalEntryRefund(fixture.entryA1().getId(), request);

        var entryStatus = jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, fixture.entryA1().getId());
        var paymentStatus = jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, fixture.entryA1().getId());
        var refundStatus = jdbcTemplate.queryForObject("""
                SELECT status FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, String.class, fixture.entryA1().getId());

        assertThat(entryStatus).isEqualTo(EntryStatus.STORED.name());
        assertThat(paymentStatus).isEqualTo(EntryPaymentStatus.PAID.name());
        assertThat(refundStatus).isEqualTo(EntryRefundStatus.APPROVED.name());
    }

    @Test
    void refundRequestIsRejectedAfterRegistrationDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var refundable = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-截止后退款", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), fixture.competition().getId());

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("测试退款");

        assertThatThrownBy(() -> entryService.requestPortalEntryRefund(refundable.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("报名截止后不能申请退款");
    }

    @Test
    void failedRefundCannotCreateDuplicatePortalRefundRequest() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var refundable = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-失败退款", EntryStatus.REGISTERED, true);
        Long paymentId = jdbcTemplate.queryForObject("SELECT id FROM entry_payment WHERE beer_entry_id = ?",
                Long.class, refundable.getId());
        jdbcTemplate.update("""
                INSERT INTO entry_refund
                  (beer_entry_id, entry_payment_id, refund_no, amount, status, reason,
                   requested_by_portal_id, requested_time)
                VALUES (?, ?, ?, 100.00, 'FAILED', '测试退款失败', ?, NOW())
                """, refundable.getId(), paymentId, testRun + "-RF-FAILED", fixture.portalA().account().getId());

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("再次退款");

        assertThatThrownBy(() -> entryService.requestPortalEntryRefund(refundable.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("退款暂未成功");
    }

    private String latestRefundStatus(Long entryId) {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, String.class, entryId);
    }
}
