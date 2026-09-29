package com.beercompetition.service;

import com.beercompetition.registration.entry.PortalEntryService;
import com.beercompetition.registration.entry.AdminEntryService;
import com.beercompetition.registration.payment.EntryPaymentAdminService;
import com.beercompetition.registration.refund.EntryRefundService;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.PortalEntryBatchQuoteRequest;
import com.beercompetition.pojo.dto.PortalEntryBatchSubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.dto.PortalPaymentOrderBankTransferRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.PaymentOrderStatus;
import com.beercompetition.pojo.enums.RegistrationBatchStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationBatchIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private RegistrationBatchService registrationBatchService;

    @Autowired
    private BatchPaymentService batchPaymentService;

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;

    @Autowired
    private PortalEntryService portalEntryService;

    @Autowired
    private AdminEntryService adminEntryService;

    @Autowired
    private EntryPaymentAdminService entryPaymentAdminService;

    @Autowired
    private EntryRefundService entryRefundService;

    @Autowired
    private WechatPaymentService wechatPaymentService;

    @Test
    void batchSubmitIsIdempotentAndSupportsPaymentAndSingleEntryRefund() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());

        PortalEntryBatchQuoteRequest quoteRequest = new PortalEntryBatchQuoteRequest();
        quoteRequest.setEntryCount(2);
        var quote = registrationBatchService.quote(fixture.competition().getId(), quoteRequest);
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("200.00");

        PortalEntryBatchSubmitRequest request = batchRequest(fixture, "IDEMPOTENT-1", "酒款甲", "酒款乙");
        var batch = registrationBatchService.submit(fixture.competition().getId(), request);
        var repeated = registrationBatchService.submit(fixture.competition().getId(), request);

        assertThat(repeated.getId()).isEqualTo(batch.getId());
        assertThat(batch.getEntries()).hasSize(2);
        assertThat(batch.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(batch.getEntries()).allMatch(entry -> batch.getId().equals(entry.getRegistrationBatchId()));
        assertThat(batch.getEntries()).allMatch(entry -> batch.getPaymentOrderId().equals(entry.getPaymentOrderId()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM beer_entry WHERE registration_batch_id = ?", Integer.class, batch.getId()))
                .isEqualTo(2);

        batchPaymentService.simulatePayment(batch.getPaymentOrderId());
        var paid = registrationBatchService.getPortalBatch(batch.getId());
        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentOrderStatus.PAID.name());
        assertThat(paid.getEntries()).allMatch(entry -> EntryStatus.REGISTERED.name().equals(entry.getStatus()));

        PortalEntryRefundRequest refundRequest = new PortalEntryRefundRequest();
        refundRequest.setReason("批次单款退款测试");
        entryRefundService.requestPortalEntryRefund(paid.getEntries().get(0).getId(), refundRequest);

        var partiallyRefunded = registrationBatchService.getPortalBatch(batch.getId());
        assertThat(partiallyRefunded.getStatus()).isEqualTo(RegistrationBatchStatus.PARTIALLY_REFUNDED.name());
        assertThat(partiallyRefunded.getPaymentStatus()).isEqualTo(PaymentOrderStatus.PARTIALLY_REFUNDED.name());
        assertThat(partiallyRefunded.getRefundedAmount()).isEqualByComparingTo("100.00");
        assertThat(partiallyRefunded.getEntries().get(0).getPaymentStatus())
                .isEqualTo(EntryPaymentStatus.REFUNDED.name());
        assertThat(partiallyRefunded.getEntries().get(1).getPaymentStatus())
                .isEqualTo(EntryPaymentStatus.PAID.name());
    }

    @Test
    void batchBankTransferConfirmsAllEntriesTogether() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "BANK-1", "转账酒款甲", "转账酒款乙"));

        PortalPaymentOrderBankTransferRequest transferRequest = new PortalPaymentOrderBankTransferRequest();
        transferRequest.setPayerName("批次付款账户");
        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalOrderTransfer(
                batch.getPaymentOrderId(), transferRequest))
                .hasMessageContaining("请上传付款凭证");

        transferRequest.setVoucherAssetId(insertVoucherAsset(fixture.portalA().account().getId()));
        var transfer = bankTransferPaymentService.submitPortalOrderTransfer(batch.getPaymentOrderId(), transferRequest);
        assertThat(transfer.getEntryCount()).isEqualTo(2);
        assertThat(transfer.getEntries())
                .extracting(entry -> entry.getEntryName())
                .containsExactly(testRun + "-转账酒款甲", testRun + "-转账酒款乙");
        assertThat(transfer.getEntries())
                .extracting(entry -> entry.getAmount())
                .allMatch(amount -> amount.compareTo(new BigDecimal("100.00")) == 0);
        assertThat(batchPaymentService.getPortalPaymentStatus(batch.getPaymentOrderId()).getStatus())
                .isEqualTo(PaymentOrderStatus.PENDING_CONFIRM.name());

        asAdmin(1L);
        bankTransferPaymentService.confirmTransfer(transfer.getId(), new AdminBankTransferProcessRequest());

        asPortal(fixture.portalA().account().getId());
        var paid = registrationBatchService.getPortalBatch(batch.getId());
        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentOrderStatus.PAID.name());
        assertThat(paid.getEntries()).allMatch(entry -> EntryPaymentStatus.PAID.name().equals(entry.getPaymentStatus()));
    }

    @Test
    void batchWechatJsapiUsesAggregateOrderAndCompletesAllEntries() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "JSAPI-1", "JSAPI酒款甲", "JSAPI酒款乙"));

        var jsapi = batchPaymentService.createJsapiPayment(batch.getPaymentOrderId(), "mock-code");

        assertThat(jsapi.getMode()).isEqualTo("MOCK");
        assertThat(jsapi.getOutTradeNo()).isNotBlank();
        assertThat(jsapi.getPayParams()).isNotNull();
        batchPaymentService.simulatePayment(batch.getPaymentOrderId());
        assertThat(registrationBatchService.getPortalBatch(batch.getId()).getEntries())
                .allMatch(entry -> EntryPaymentStatus.PAID.name().equals(entry.getPaymentStatus()));
    }

    @Test
    void aggregateOrderEntriesRejectLegacySinglePaymentAndCancelActions() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "LEGACY-GUARD-1", "统一付款酒款甲", "统一付款酒款乙"));
        Long entryId = batch.getEntries().get(0).getId();

        assertThatThrownBy(() -> wechatPaymentService.createNativePayment(entryId))
                .hasMessageContaining("统一付款订单");
        assertThatThrownBy(() -> entryPaymentAdminService.simulatePayment(entryId))
                .hasMessageContaining("按整批完成付款");
        assertThatThrownBy(() -> portalEntryService.cancelPortalEntry(entryId))
                .hasMessageContaining("不能单独取消报名");

        PortalBankTransferSubmitRequest transferRequest = new PortalBankTransferSubmitRequest();
        transferRequest.setEntryId(entryId);
        transferRequest.setVoucherAssetId(insertVoucherAsset(fixture.portalA().account().getId()));
        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(transferRequest))
                .hasMessageContaining("按整批提交银行转账");

        asAdmin(1L);
        AdminEntryStatusRequest confirmation = new AdminEntryStatusRequest();
        confirmation.setReason("现场核对整批到账");
        entryPaymentAdminService.confirmPayment(entryId, confirmation);
        asPortal(fixture.portalA().account().getId());
        assertThat(registrationBatchService.getPortalBatch(batch.getId()).getPaymentStatus())
                .isEqualTo(PaymentOrderStatus.PAID.name());
        assertThat(registrationBatchService.getPortalBatch(batch.getId()).getEntries())
                .allMatch(item -> EntryPaymentStatus.PAID.name().equals(item.getPaymentStatus()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pay_method FROM payment_order WHERE id = ?", String.class, batch.getPaymentOrderId()))
                .isEqualTo("MANUAL");
    }

    @Test
    void manualConfirmationRequiresReasonAndRejectsCanceledOrderItem() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "MANUAL-GUARD-1", "现场确认酒款", "已取消酒款"));
        Long entryId = batch.getEntries().get(0).getId();
        asAdmin(1L);
        assertThatThrownBy(() -> entryPaymentAdminService.confirmPayment(entryId))
                .hasMessageContaining("到账依据");
        jdbcTemplate.update("UPDATE beer_entry SET status = 'CANCELED' WHERE id = ?", batch.getEntries().get(1).getId());
        AdminEntryStatusRequest confirmation = new AdminEntryStatusRequest();
        confirmation.setReason("现场核对到账");
        assertThatThrownBy(() -> entryPaymentAdminService.confirmPayment(entryId, confirmation))
                .hasMessageContaining("不处于待支付状态");
    }

    @Test
    void adminCancellationCancelsEntireUnpaidBatch() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "CANCEL-BATCH-1", "取消酒款甲", "取消酒款乙"));
        asAdmin(1L);
        AdminEntryStatusRequest cancellation = new AdminEntryStatusRequest();
        cancellation.setReason("厂商确认撤回整批报名");
        adminEntryService.cancelEntry(batch.getEntries().get(0).getId(), cancellation);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payment_order WHERE id = ?", String.class, batch.getPaymentOrderId()))
                .isEqualTo(PaymentOrderStatus.CANCELED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM beer_entry WHERE registration_batch_id = ? AND status = 'CANCELED'",
                Integer.class, batch.getId())).isEqualTo(2);
        asPortal(fixture.portalA().account().getId());
        assertThatThrownBy(() -> batchPaymentService.simulatePayment(batch.getPaymentOrderId()))
                .hasMessageContaining("不能模拟支付");
    }

    @Test
    void overdueAggregateOrderExpiresAndReopensInPlaceForRepayment() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "EXPIRY-1", "过期重付酒款"));
        Long orderId = batch.getPaymentOrderId();
        Long entryId = batch.getEntries().get(0).getId();

        batchPaymentService.createNativePayment(orderId);
        jdbcTemplate.update("UPDATE payment_order SET expire_time = DATE_SUB(NOW(), INTERVAL 5 MINUTE) WHERE id = ?",
                orderId);

        var expiredStatus = batchPaymentService.getPortalPaymentStatus(orderId);
        assertThat(expiredStatus.getStatus()).isEqualTo(PaymentOrderStatus.EXPIRED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM entry_payment WHERE beer_entry_id = ?", String.class, entryId))
                .isEqualTo(EntryPaymentStatus.EXPIRED.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payment_order_item WHERE payment_order_id = ?", String.class, orderId))
                .isEqualTo(EntryPaymentStatus.EXPIRED.name());
        // 过期不回退酒款状态，厂商仍可原地重付
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM beer_entry WHERE id = ?", String.class, entryId))
                .isEqualTo(EntryStatus.PENDING_PAYMENT.name());

        var reopened = batchPaymentService.createNativePayment(orderId);
        assertThat(reopened.getPaymentStatus()).isEqualTo(PaymentOrderStatus.UNPAID.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM entry_payment WHERE beer_entry_id = ?", String.class, entryId))
                .isEqualTo(EntryPaymentStatus.UNPAID.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT registration_batch_id FROM payment_order WHERE id = ?", Long.class, orderId))
                .isEqualTo(batch.getId());

        // 重新支付后仍可正常到账并完成报名
        batchPaymentService.simulatePayment(orderId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM beer_entry WHERE id = ?", String.class, entryId))
                .isEqualTo(EntryStatus.REGISTERED.name());
    }

    @Test
    void staleAggregateOrderIsReconciledFromLegacyPaidEntryData() {
        BeerCompetitionTestData.Fixture fixture = openFixture();
        asPortal(fixture.portalA().account().getId());
        var batch = registrationBatchService.submit(fixture.competition().getId(),
                batchRequest(fixture, "LEGACY-RECONCILE-1", "历史兼容酒款"));
        Long entryId = batch.getEntries().get(0).getId();

        jdbcTemplate.update("UPDATE entry_payment SET status = 'PAID', pay_method = 'MOCK', "
                        + "paid_amount = amount, paid_time = NOW() WHERE beer_entry_id = ?", entryId);
        jdbcTemplate.update("UPDATE beer_entry SET status = 'REGISTERED' WHERE id = ?", entryId);

        var reconciled = batchPaymentService.getPortalPaymentStatus(batch.getPaymentOrderId());
        assertThat(reconciled.getStatus()).isEqualTo(PaymentOrderStatus.PAID.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payment_order_item WHERE payment_order_id = ?", String.class,
                batch.getPaymentOrderId())).isEqualTo(EntryPaymentStatus.PAID.name());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM registration_batch WHERE id = ?", String.class,
                batch.getId())).isEqualTo(RegistrationBatchStatus.PAID.name());
    }

    private BeerCompetitionTestData.Fixture openFixture() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());
        return fixture;
    }

    private PortalEntryBatchSubmitRequest batchRequest(BeerCompetitionTestData.Fixture fixture,
                                                       String idempotencyKey, String... names) {
        PortalEntryBatchSubmitRequest request = new PortalEntryBatchSubmitRequest();
        request.setIdempotencyKey(testRun + "-" + idempotencyKey);
        request.setRulesAccepted(true);
        request.setEntries(List.of(names).stream()
                .map(name -> entryRequest(fixture, name))
                .toList());
        return request;
    }

    private PortalEntrySubmitRequest entryRequest(BeerCompetitionTestData.Fixture fixture, String name) {
        PortalEntrySubmitRequest request = new PortalEntrySubmitRequest();
        request.setName(testRun + "-" + name);
        request.setCategoryId(fixture.category().getId());
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.50"));
        return request;
    }

    private Long insertVoucherAsset(Long portalAccountId) {
        String filename = testRun + "-batch-voucher.pdf";
        jdbcTemplate.update("""
                INSERT INTO file_asset
                  (business_type, owner_type, owner_id, storage_provider, file_name, storage_path, public_url, create_time)
                VALUES ('BANK_TRANSFER_VOUCHER', 'PORTAL_ACCOUNT', ?, 'local', ?, ?, ?, NOW())
                """, portalAccountId, filename, "uploads/BANK_TRANSFER_VOUCHER/" + filename,
                "/uploads/BANK_TRANSFER_VOUCHER/" + filename);
        return jdbcTemplate.queryForObject("SELECT id FROM file_asset WHERE file_name = ? ORDER BY id DESC LIMIT 1",
                Long.class, filename);
    }
}
