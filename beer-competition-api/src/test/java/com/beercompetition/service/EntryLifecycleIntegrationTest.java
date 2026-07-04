package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntryLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private EntryService entryService;

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
    void portalUpdateEntryKeepsCategoryAndLabelIdentity() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
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
    void portalUpdateEntryIsRejectedAfterRegistrationDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-截止后修改", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), fixture.competition().getId());

        PortalEntryUpdateRequest request = new PortalEntryUpdateRequest();
        request.setName(testRun + "-修改后");
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.5"));

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> entryService.updatePortalEntry(entry.getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("报名截止后不能修改报名资料");
    }

    @Test
    void portalUpdateEntryIsRejectedAfterSampleStored() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
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
    void refundRequestBeforeRegistrationDeadlineAutoApprovesAndCancelsEntry() {
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

        assertThat(entryStatus).isEqualTo(EntryStatus.CANCELED.name());
        assertThat(paymentStatus).isEqualTo(EntryPaymentStatus.REFUNDED.name());
        assertThat(refundStatus).isEqualTo(EntryRefundStatus.SUCCESS.name());
    }

    @Test
    void storedEntryCanRequestRefundBeforeRegistrationDeadline() {
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

        assertThat(entryStatus).isEqualTo(EntryStatus.CANCELED.name());
        assertThat(paymentStatus).isEqualTo(EntryPaymentStatus.REFUNDED.name());
        assertThat(refundStatus).isEqualTo(EntryRefundStatus.SUCCESS.name());
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
}
