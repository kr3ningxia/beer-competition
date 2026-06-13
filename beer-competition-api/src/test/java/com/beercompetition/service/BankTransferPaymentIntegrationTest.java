package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.enums.BankTransferPaymentStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankTransferPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;

    @Test
    void submitTransferLocksEntriesAndAdminConfirmRegistersThem() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entryA = createPendingEntry(fixture, "银行转账A");
        var entryB = createPendingEntry(fixture, "银行转账B");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entryA.getId(), entryB.getId()), "200.00", "测试付款户名", null));

        assertThat(submitted.getStatus()).isEqualTo(BankTransferPaymentStatus.SUBMITTED.name());
        assertThat(submitted.getEntryCount()).isEqualTo(2);
        assertPayment(entryA.getId(), EntryPaymentStatus.PENDING_CONFIRM, EntryPayMethod.BANK_TRANSFER,
                submitted.getId());
        assertPayment(entryB.getId(), EntryPaymentStatus.PENDING_CONFIRM, EntryPayMethod.BANK_TRANSFER,
                submitted.getId());

        asAdmin(1L);
        AdminBankTransferProcessRequest processRequest = new AdminBankTransferProcessRequest();
        processRequest.setAdminNote("财务已核对到账");
        var confirmed = bankTransferPaymentService.confirmTransfer(submitted.getId(), processRequest);

        assertThat(confirmed.getStatus()).isEqualTo(BankTransferPaymentStatus.CONFIRMED.name());
        assertThatEntryStatus(entryA.getId(), EntryStatus.REGISTERED);
        assertThatEntryStatus(entryB.getId(), EntryStatus.REGISTERED);
        assertPayment(entryA.getId(), EntryPaymentStatus.PAID, EntryPayMethod.BANK_TRANSFER, submitted.getId());
        assertPayment(entryB.getId(), EntryPaymentStatus.PAID, EntryPayMethod.BANK_TRANSFER, submitted.getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paid_amount FROM entry_payment WHERE beer_entry_id = ?",
                BigDecimal.class,
                entryA.getId())).isEqualByComparingTo("100.00");
    }

    @Test
    void duplicateSubmittedTransferIsBlockedUntilTheFirstOneIsResolved() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "重复提交");

        asPortal(fixture.portalA().account().getId());
        bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", null));

        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", null)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("等待确认");
    }

    @Test
    void rejectTransferReleasesEntryPaymentForRetry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "驳回后重提");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", null));

        asAdmin(1L);
        AdminBankTransferProcessRequest rejectRequest = new AdminBankTransferProcessRequest();
        rejectRequest.setAdminNote("未查到到账记录");
        var rejected = bankTransferPaymentService.rejectTransfer(submitted.getId(), rejectRequest);

        assertThat(rejected.getStatus()).isEqualTo(BankTransferPaymentStatus.REJECTED.name());
        assertThatEntryStatus(entry.getId(), EntryStatus.PENDING_PAYMENT);
        assertPayment(entry.getId(), EntryPaymentStatus.UNPAID, EntryPayMethod.MANUAL, null);

        asPortal(fixture.portalA().account().getId());
        var retried = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", null));
        assertThat(retried.getStatus()).isEqualTo(BankTransferPaymentStatus.SUBMITTED.name());
    }

    @Test
    void portalCanCancelOwnSubmittedTransferOnly() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "取消提交");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", null));

        asPortal(fixture.portalB().account().getId());
        assertThatThrownBy(() -> bankTransferPaymentService.cancelPortalTransfer(submitted.getId()))
                .isInstanceOf(ForbiddenException.class);

        asPortal(fixture.portalA().account().getId());
        var canceled = bankTransferPaymentService.cancelPortalTransfer(submitted.getId());
        assertThat(canceled.getStatus()).isEqualTo(BankTransferPaymentStatus.CANCELED.name());
        assertThatEntryStatus(entry.getId(), EntryStatus.PENDING_PAYMENT);
        assertPayment(entry.getId(), EntryPaymentStatus.UNPAID, EntryPayMethod.MANUAL, null);
    }

    @Test
    void voucherAssetMustBelongToCurrentPortalAccount() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "凭证归属");
        Long otherPortalVoucherId = insertVoucherAsset(fixture.portalB().account().getId());

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(submitRequest(
                List.of(entry.getId()), "100.00", "测试付款户名", otherPortalVoucherId)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("转账凭证不存在");
    }

    private com.beercompetition.pojo.po.BeerEntry createPendingEntry(BeerCompetitionTestData.Fixture fixture,
                                                                     String name) {
        return testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-" + name, EntryStatus.PENDING_PAYMENT, false);
    }

    private PortalBankTransferSubmitRequest submitRequest(List<Long> entryIds, String amount, String payerName,
                                                          Long voucherAssetId) {
        PortalBankTransferSubmitRequest request = new PortalBankTransferSubmitRequest();
        request.setEntryIds(entryIds);
        request.setAmount(new BigDecimal(amount));
        request.setPayerName(payerName);
        request.setTransferTime(LocalDateTime.now().minusMinutes(5));
        request.setRemark(testRun + "-厂牌转账备注");
        request.setVoucherAssetId(voucherAssetId);
        return request;
    }

    private Long insertVoucherAsset(Long portalAccountId) {
        String filename = testRun + "-voucher.pdf";
        jdbcTemplate.update("""
                INSERT INTO file_asset
                  (business_type, owner_type, owner_id, storage_provider, file_name, storage_path, public_url, create_time)
                VALUES ('BANK_TRANSFER_VOUCHER', 'PORTAL_ACCOUNT', ?, 'local', ?, ?, ?, NOW())
                """, portalAccountId, filename, "uploads/BANK_TRANSFER_VOUCHER/" + filename,
                "/uploads/BANK_TRANSFER_VOUCHER/" + filename);
        return jdbcTemplate.queryForObject("SELECT id FROM file_asset WHERE file_name = ?", Long.class, filename);
    }

    private void assertEntryStatus(Long entryId, EntryStatus expected) {
        assertThatEntryStatus(entryId, expected);
    }

    private void assertThatEntryStatus(Long entryId, EntryStatus expected) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class,
                entryId)).isEqualTo(expected.name());
    }

    private void assertPayment(Long entryId, EntryPaymentStatus status, EntryPayMethod payMethod,
                               Long bankTransferId) {
        var payment = jdbcTemplate.queryForMap("""
                SELECT status, pay_method, bank_transfer_id
                FROM entry_payment
                WHERE beer_entry_id = ?
                """, entryId);
        assertThat(payment.get("status")).isEqualTo(status.name());
        assertThat(payment.get("pay_method")).isEqualTo(payMethod.name());
        assertThat(payment.get("bank_transfer_id")).isEqualTo(bankTransferId);
    }
}
