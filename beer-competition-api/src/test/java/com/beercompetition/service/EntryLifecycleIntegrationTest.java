package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    void refundApprovalCancelsEntryAndMarksPaymentRefunded() {
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

        asAdmin(1L);
        entryService.approveRefund(refundId, null);

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
}
