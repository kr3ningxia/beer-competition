package com.beercompetition.service;

import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
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
    private EntryService entryService;

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
        entryService.requestPortalEntryRefund(paid.getEntries().get(0).getId(), refundRequest);

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
        var transfer = bankTransferPaymentService.submitPortalOrderTransfer(batch.getPaymentOrderId(), transferRequest);
        assertThat(transfer.getEntryCount()).isEqualTo(2);
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
        assertThatThrownBy(() -> entryService.simulatePayment(entryId))
                .hasMessageContaining("按整批完成付款");
        assertThatThrownBy(() -> entryService.cancelPortalEntry(entryId))
                .hasMessageContaining("不能单独取消报名");

        PortalBankTransferSubmitRequest transferRequest = new PortalBankTransferSubmitRequest();
        transferRequest.setEntryId(entryId);
        assertThatThrownBy(() -> bankTransferPaymentService.submitPortalTransfer(transferRequest))
                .hasMessageContaining("按整批提交银行转账");

        asAdmin(1L);
        assertThatThrownBy(() -> entryService.confirmPayment(entryId))
                .hasMessageContaining("不能单独确认付款");
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
}
