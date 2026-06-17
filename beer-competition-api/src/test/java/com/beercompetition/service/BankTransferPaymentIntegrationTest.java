package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.enums.BankTransferPaymentStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankTransferPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;

    @Autowired
    private EntryService entryService;

    @Test
    void submitTransferLocksEntryAndAdminConfirmRegistersIt() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "银行转账");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null));

        assertThat(submitted.getStatus()).isEqualTo(BankTransferPaymentStatus.SUBMITTED.name());
        assertThat(submitted.getEntryCount()).isEqualTo(1);
        assertThat(submitted.getEntryId()).isEqualTo(entry.getId());
        assertThat(submitted.getAmount()).isEqualByComparingTo("100.00");
        assertPayment(entry.getId(), EntryPaymentStatus.PENDING_CONFIRM, EntryPayMethod.BANK_TRANSFER, submitted.getId());

        asAdmin(1L);
        AdminBankTransferProcessRequest processRequest = new AdminBankTransferProcessRequest();
        processRequest.setAdminNote("财务已核对到账");
        var confirmed = bankTransferPaymentService.confirmTransfer(submitted.getId(), processRequest);

        assertThat(confirmed.getStatus()).isEqualTo(BankTransferPaymentStatus.CONFIRMED.name());
        assertThatEntryStatus(entry.getId(), EntryStatus.REGISTERED);
        assertPayment(entry.getId(), EntryPaymentStatus.PAID, EntryPayMethod.BANK_TRANSFER, submitted.getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paid_amount FROM entry_payment WHERE beer_entry_id = ?",
                BigDecimal.class,
                entry.getId())).isEqualByComparingTo("100.00");
    }

    @Test
    void duplicateSubmittedTransferIsBlockedUntilTheFirstOneIsResolved() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "重复提交");

        asPortal(fixture.portalA().account().getId());
        bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null));

        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("等待转账确认");
    }

    @Test
    void updateSubmittedTransferKeepsTheSameTransferRecord() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "修改转账");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "原付款户名", null));
        PortalBankTransferSubmitRequest updateRequest = submitRequest(entry.getId(), "新付款户名", null);
        updateRequest.setRemark(testRun + "-更新后的转账备注");
        updateRequest.setTransferTime(LocalDateTime.now().minusMinutes(1));

        var updated = bankTransferPaymentService.updatePortalTransfer(submitted.getId(), updateRequest);

        assertThat(updated.getId()).isEqualTo(submitted.getId());
        assertThat(updated.getPayerName()).isEqualTo("新付款户名");
        assertThat(updated.getRemark()).isEqualTo(testRun + "-更新后的转账备注");
        assertPayment(entry.getId(), EntryPaymentStatus.PENDING_CONFIRM, EntryPayMethod.BANK_TRANSFER, submitted.getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bank_transfer_payment WHERE entry_payment_id = ?",
                Long.class,
                updated.getPaymentId())).isEqualTo(1L);
    }

    @Test
    void rejectTransferReleasesEntryPaymentForRetry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "驳回后重提");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null));

        asAdmin(1L);
        AdminBankTransferProcessRequest rejectRequest = new AdminBankTransferProcessRequest();
        rejectRequest.setAdminNote("未查到到账记录");
        var rejected = bankTransferPaymentService.rejectTransfer(submitted.getId(), rejectRequest);

        assertThat(rejected.getStatus()).isEqualTo(BankTransferPaymentStatus.REJECTED.name());
        assertThatEntryStatus(entry.getId(), EntryStatus.PENDING_PAYMENT);
        assertPayment(entry.getId(), EntryPaymentStatus.UNPAID, EntryPayMethod.MANUAL, null);

        asPortal(fixture.portalA().account().getId());
        var retried = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null));
        assertThat(retried.getStatus()).isEqualTo(BankTransferPaymentStatus.SUBMITTED.name());
    }

    @Test
    void portalCanCancelOwnSubmittedTransferOnly() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "取消提交");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", null));

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
    void portalCanCancelUnpaidEntryButNotPendingBankTransferEntry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var unpaidEntry = createPendingEntry(fixture, "待支付取消报名");
        var pendingTransferEntry = createPendingEntry(fixture, "转账确认中取消报名");

        asPortal(fixture.portalA().account().getId());
        var canceled = entryService.cancelPortalEntry(unpaidEntry.getId());

        assertThat(canceled.getStatus()).isEqualTo(EntryStatus.CANCELED.name());
        assertPayment(unpaidEntry.getId(), EntryPaymentStatus.CANCELED, EntryPayMethod.MOCK, null);

        bankTransferPaymentService.submitPortalTransfer(submitRequest(
                pendingTransferEntry.getId(), "测试付款户名", null));
        assertThatThrownBy(() -> entryService.cancelPortalEntry(pendingTransferEntry.getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("银行转账确认中");
    }

    @Test
    void voucherAssetMustBelongToCurrentPortalAccount() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "凭证归属");
        Long otherPortalVoucherId = insertVoucherAsset(fixture.portalB().account().getId());

        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(submitRequest(
                entry.getId(), "测试付款户名", otherPortalVoucherId)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("转账凭证不存在");
    }

    @Test
    void bankTransferRefundIsCompletedAsManualRefundWithoutWechatRetry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "线下退款");

        asPortal(fixture.portalA().account().getId());
        var submitted = bankTransferPaymentService.submitPortalTransfer(submitRequest(entry.getId(), "测试付款户名", null));

        asAdmin(1L);
        bankTransferPaymentService.confirmTransfer(submitted.getId(), new AdminBankTransferProcessRequest());

        Long paymentId = jdbcTemplate.queryForObject("SELECT id FROM entry_payment WHERE beer_entry_id = ?",
                Long.class, entry.getId());
        jdbcTemplate.update("""
                INSERT INTO entry_refund
                  (beer_entry_id, entry_payment_id, refund_no, amount, status, reason,
                   requested_by_portal_id, requested_time)
                VALUES (?, ?, ?, 100.00, 'REQUESTED', '测试退款', ?, NOW())
                """, entry.getId(), paymentId, testRun + "-RF-BANK", fixture.portalA().account().getId());
        Long refundId = jdbcTemplate.queryForObject("SELECT id FROM entry_refund WHERE refund_no = ?",
                Long.class, testRun + "-RF-BANK");

        entryService.approveRefund(refundId, null);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_refund WHERE id = ?",
                String.class, refundId)).isEqualTo(EntryRefundStatus.SUCCESS.name());
        assertThat(jdbcTemplate.queryForObject("SELECT wechat_refund_status FROM entry_refund WHERE id = ?",
                String.class, refundId)).isEqualTo("MANUAL_SUCCESS");
        assertPayment(entry.getId(), EntryPaymentStatus.REFUNDED, EntryPayMethod.BANK_TRANSFER, submitted.getId());
        assertThatEntryStatus(entry.getId(), EntryStatus.CANCELED);
    }

    private com.beercompetition.pojo.po.BeerEntry createPendingEntry(BeerCompetitionTestData.Fixture fixture,
                                                                     String name) {
        return testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-" + name, EntryStatus.PENDING_PAYMENT, false);
    }

    private PortalBankTransferSubmitRequest submitRequest(Long entryId, String payerName, Long voucherAssetId) {
        PortalBankTransferSubmitRequest request = new PortalBankTransferSubmitRequest();
        request.setEntryId(entryId);
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
