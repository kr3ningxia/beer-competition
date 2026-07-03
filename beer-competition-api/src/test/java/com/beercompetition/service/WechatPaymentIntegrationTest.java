package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private WechatPaymentService wechatPaymentService;

    @Autowired
    private EntryService entryService;

    @Test
    void mockNativePaymentSuccessNotifyRegistersEntryAndIsIdempotent() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "微信支付成功");

        asPortal(fixture.portalA().account().getId());
        var order = wechatPaymentService.createNativePayment(entry.getId());

        assertThat(order.getMode()).isEqualTo("MOCK");
        assertThat(order.getCodeUrl()).startsWith("mock://wechat-pay/native/");
        assertPayment(entry.getId(), EntryPaymentStatus.UNPAID, EntryPayMethod.WECHAT);

        String body = """
                {"id":"%s-WX-NOTIFY","event_type":"TRANSACTION.SUCCESS","out_trade_no":"%s","transaction_id":"MOCK-TX-%s","trade_state":"SUCCESS","amount":{"total":10000}}
                """.formatted(testRun, order.getOutTradeNo(), order.getOutTradeNo());
        var notify = new WechatPayClient.WechatNotifyRequest(null, null, null, null, body);

        wechatPaymentService.handlePaymentNotify(notify);
        wechatPaymentService.handlePaymentNotify(notify);

        assertEntryStatus(entry.getId(), EntryStatus.REGISTERED);
        assertPayment(entry.getId(), EntryPaymentStatus.PAID, EntryPayMethod.WECHAT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wechat_pay_notify WHERE notify_id = ?",
                Integer.class,
                testRun + "-WX-NOTIFY")).isEqualTo(1);
    }

    @Test
    void wechatPaidEntryRefundsAutomaticallyBeforeRegistrationDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "微信自动退款");

        asPortal(fixture.portalA().account().getId());
        var order = wechatPaymentService.createNativePayment(entry.getId());
        String body = """
                {"id":"%s-WX-AUTO-REFUND-PAY","event_type":"TRANSACTION.SUCCESS","out_trade_no":"%s","transaction_id":"MOCK-TX-%s","trade_state":"SUCCESS","amount":{"total":10000}}
                """.formatted(testRun, order.getOutTradeNo(), order.getOutTradeNo());
        wechatPaymentService.handlePaymentNotify(new WechatPayClient.WechatNotifyRequest(null, null, null, null, body));

        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("报名调整");
        entryService.requestPortalEntryRefund(entry.getId(), request);

        assertEntryStatus(entry.getId(), EntryStatus.CANCELED);
        assertPayment(entry.getId(), EntryPaymentStatus.REFUNDED, EntryPayMethod.WECHAT);
        var refund = jdbcTemplate.queryForMap("""
                SELECT status, amount, out_refund_no, wechat_refund_status
                FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, entry.getId());
        assertThat(refund.get("status")).isEqualTo(EntryRefundStatus.SUCCESS.name());
        assertThat(refund.get("amount").toString()).isEqualTo("100.00");
        assertThat((String) refund.get("out_refund_no")).startsWith("WRF");
        assertThat(refund.get("wechat_refund_status")).isEqualTo("SUCCESS");
    }

    @Test
    void amountMismatchNotifyDoesNotAdvancePayment() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var entry = createPendingEntry(fixture, "微信金额异常");

        asPortal(fixture.portalA().account().getId());
        var order = wechatPaymentService.createNativePayment(entry.getId());
        String body = """
                {"id":"%s-WX-MISMATCH","event_type":"TRANSACTION.SUCCESS","out_trade_no":"%s","transaction_id":"MOCK-TX-%s","trade_state":"SUCCESS","amount":{"total":9900}}
                """.formatted(testRun, order.getOutTradeNo(), order.getOutTradeNo());

        assertThatThrownBy(() -> wechatPaymentService.handlePaymentNotify(
                new WechatPayClient.WechatNotifyRequest(null, null, null, null, body)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("金额不一致");

        assertEntryStatus(entry.getId(), EntryStatus.PENDING_PAYMENT);
        assertPayment(entry.getId(), EntryPaymentStatus.UNPAID, EntryPayMethod.WECHAT);
    }

    private com.beercompetition.pojo.po.BeerEntry createPendingEntry(BeerCompetitionTestData.Fixture fixture,
                                                                     String name) {
        return testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-" + name, EntryStatus.PENDING_PAYMENT, false);
    }

    private void assertEntryStatus(Long entryId, EntryStatus expected) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class,
                entryId)).isEqualTo(expected.name());
    }

    private void assertPayment(Long entryId, EntryPaymentStatus status, EntryPayMethod payMethod) {
        var payment = jdbcTemplate.queryForMap("""
                SELECT status, pay_method
                FROM entry_payment
                WHERE beer_entry_id = ?
                """, entryId);
        assertThat(payment.get("status")).isEqualTo(status.name());
        assertThat(payment.get("pay_method")).isEqualTo(payMethod.name());
    }
}
