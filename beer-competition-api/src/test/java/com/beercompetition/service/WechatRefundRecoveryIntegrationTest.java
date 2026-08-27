package com.beercompetition.service;

import com.beercompetition.registration.refund.EntryRefundService;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatRefundRecoveryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private EntryRefundService entryRefundService;

    @Autowired
    private WechatPaymentService wechatPaymentService;

    @MockitoBean
    private WechatPayClient wechatPayClient;

    @Test
    void applicationUsesShanghaiBusinessTimeZone() {
        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void manualApprovalFailureBecomesRetryableWithoutChangingEntryOrPayment() {
        RefundFixture fixture = createRequestedWechatRefund("余额不足");
        when(wechatPayClient.createRefund(any())).thenThrow(new RuntimeException(
                "Wrong HttpStatusCode[403] response={\"code\":\"NOT_ENOUGH\"}"));

        asAdmin(1L);
        AdminEntryStatusRequest request = new AdminEntryStatusRequest();
        request.setReason("同意退款");

        assertThatThrownBy(() -> entryRefundService.approveRefund(fixture.refundId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("余额不足");

        Map<String, Object> refund = loadRefund(fixture.refundId());
        assertThat(refund.get("status")).isEqualTo(EntryRefundStatus.FAILED.name());
        assertThat(refund.get("fail_reason")).isEqualTo("微信退款发起失败：商户基本账户余额不足");
        assertEntryAndPaymentRemainEffective(fixture.entryId());
    }

    @Test
    void retrySynchronizesExistingWechatRefundBeforeCreatingAgain() {
        RefundFixture fixture = createRequestedWechatRefund("响应丢失");
        jdbcTemplate.update("""
                UPDATE entry_refund
                SET status = 'FAILED', out_refund_no = ?, fail_reason = '微信响应未收到'
                WHERE id = ?
                """, testRun + "-OUT-REFUND", fixture.refundId());
        when(wechatPayClient.queryRefund(testRun + "-OUT-REFUND"))
                .thenReturn(new WechatPayClient.RefundResult(
                        testRun + "-OUT-TRADE", testRun + "-OUT-REFUND", testRun + "-WX-REFUND",
                        "SUCCESS", LocalDateTime.now()));

        asAdmin(1L);
        wechatPaymentService.retryRefund(fixture.refundId(), "重新查询", 1L);

        assertThat(loadRefund(fixture.refundId()).get("status")).isEqualTo(EntryRefundStatus.SUCCESS.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, fixture.entryId())).isEqualTo(EntryStatus.CANCELED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, fixture.entryId())).isEqualTo(EntryPaymentStatus.REFUNDED.name());
        verify(wechatPayClient, never()).createRefund(any());
    }

    @Test
    void reconciliationConvergesStaleProcessingRefundWhenCallbackIsMissing() {
        RefundFixture fixture = createRequestedWechatRefund("回调丢失");
        jdbcTemplate.update("""
                UPDATE entry_refund
                SET status = 'PROCESSING', out_refund_no = ?, processed_time = ?
                WHERE id = ?
                """, testRun + "-STALE-REFUND", LocalDateTime.now().minusMinutes(10), fixture.refundId());
        when(wechatPayClient.queryRefund(testRun + "-STALE-REFUND"))
                .thenReturn(new WechatPayClient.RefundResult(
                        testRun + "-OUT-TRADE", testRun + "-STALE-REFUND", testRun + "-WX-REFUND",
                        "SUCCESS", LocalDateTime.now()));

        int reconciled = wechatPaymentService.reconcileProcessingRefunds();

        assertThat(reconciled).isEqualTo(1);
        assertThat(loadRefund(fixture.refundId()).get("status")).isEqualTo(EntryRefundStatus.SUCCESS.name());
    }

    private RefundFixture createRequestedWechatRefund(String suffix) {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET refund_approval_mode = ? WHERE id = ?",
                RefundApprovalMode.MANUAL_REVIEW.name(), fixture.competition().getId());
        var entry = testData.createEntry(testRun, fixture.competition().getId(), fixture.portalA().brewery().getId(),
                fixture.category().getId(), testRun + "-" + suffix, EntryStatus.REGISTERED, true);
        jdbcTemplate.update("""
                UPDATE entry_payment
                SET pay_method = ?, out_trade_no = ?, wechat_transaction_id = ?, paid_amount = amount
                WHERE beer_entry_id = ?
                """, EntryPayMethod.WECHAT.name(), testRun + "-OUT-TRADE", testRun + "-WX-TRADE", entry.getId());

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("申请退款");
        entryRefundService.requestPortalEntryRefund(entry.getId(), request);
        Long refundId = jdbcTemplate.queryForObject("""
                SELECT id FROM entry_refund
                WHERE beer_entry_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, entry.getId());
        return new RefundFixture(entry.getId(), refundId);
    }

    private Map<String, Object> loadRefund(Long refundId) {
        return jdbcTemplate.queryForMap("""
                SELECT status, fail_reason, out_refund_no, wechat_refund_id, wechat_refund_status
                FROM entry_refund
                WHERE id = ?
                """, refundId);
    }

    private void assertEntryAndPaymentRemainEffective(Long entryId) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, entryId)).isEqualTo(EntryStatus.REGISTERED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM entry_payment WHERE beer_entry_id = ?",
                String.class, entryId)).isEqualTo(EntryPaymentStatus.PAID.name());
    }

    private record RefundFixture(Long entryId, Long refundId) {
    }
}
