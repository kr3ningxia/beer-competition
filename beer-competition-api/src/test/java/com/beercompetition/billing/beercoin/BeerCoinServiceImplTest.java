package com.beercompetition.billing.beercoin;

import com.beercompetition.mapper.BeerCoinConsumptionAllocationMapper;
import com.beercompetition.mapper.BeerCoinLedgerMapper;
import com.beercompetition.mapper.BeerCoinLotMapper;
import com.beercompetition.mapper.BeerCoinProductMapper;
import com.beercompetition.mapper.BeerCoinProductTierMapper;
import com.beercompetition.mapper.BeerCoinPurchaseOrderMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCoinSettlementEntryMapper;
import com.beercompetition.mapper.CompetitionCoinSettlementMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EnterpriseAccountMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.enums.BeerCoinPurchaseOrderStatus;
import com.beercompetition.pojo.po.BeerCoinLedger;
import com.beercompetition.pojo.po.BeerCoinLot;
import com.beercompetition.pojo.po.BeerCoinPurchaseOrder;
import com.beercompetition.properties.BeerCoinPaymentProperties;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.service.WechatOAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeerCoinServiceImplTest {

    private static final String OUT_TRADE_NO = "BCP2026091400001";

    private final BeerCoinProductMapper productMapper = mock(BeerCoinProductMapper.class);
    private final BeerCoinProductTierMapper tierMapper = mock(BeerCoinProductTierMapper.class);
    private final BeerCoinPurchaseOrderMapper purchaseOrderMapper = mock(BeerCoinPurchaseOrderMapper.class);
    private final BeerCoinLotMapper lotMapper = mock(BeerCoinLotMapper.class);
    private final BeerCoinLedgerMapper ledgerMapper = mock(BeerCoinLedgerMapper.class);
    private final BeerCoinServiceImpl service = new BeerCoinServiceImpl(
            productMapper,
            tierMapper,
            purchaseOrderMapper,
            lotMapper,
            ledgerMapper,
            mock(BeerCoinConsumptionAllocationMapper.class),
            mock(CompetitionCoinSettlementMapper.class),
            mock(CompetitionCoinSettlementEntryMapper.class),
            mock(EnterpriseAccountMapper.class),
            mock(OrganizerMapper.class),
            mock(CompetitionMapper.class),
            mock(BeerEntryMapper.class),
            mock(AdminIdentityService.class),
            mock(WechatPayClient.class),
            mock(WechatOAuthService.class),
            new WechatPayProperties(),
            new BeerCoinPaymentProperties(),
            new ObjectMapper()
    );

    @Test
    void chargesOneCoinPerEffectiveEntryWithCompetitionMinimum() {
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(0)).isEqualTo(1L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(1)).isEqualTo(1L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(10)).isEqualTo(10L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(11)).isEqualTo(11L);
    }

    @Test
    void creditsWalletWhenWechatConfirmsPaymentOfLocallyExpiredOrder() {
        BeerCoinPurchaseOrder order = expiredOrder();
        // 第一次按商户订单号加锁查询，第二次排查微信交易号是否已关联其他订单。
        when(purchaseOrderMapper.selectOne(any())).thenReturn(order, null);
        when(lotMapper.selectOne(any())).thenReturn(null);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        boolean applied = service.applyWechatPaymentSuccess(successNotify(order.getAmount()));

        assertThat(applied).isTrue();
        assertThat(order.getStatus()).isEqualTo(BeerCoinPurchaseOrderStatus.PAID.name());
        assertThat(order.getWechatTransactionId()).isEqualTo("4200009999202609140000000001");
        verify(lotMapper).insert(any(BeerCoinLot.class));
        verify(ledgerMapper).insert(org.mockito.ArgumentMatchers.argThat((BeerCoinLedger ledger) ->
                "微信支付到账（订单过期后补入账）".equals(ledger.getReason())));
    }

    @Test
    void ignoresDuplicateNotifyForAlreadyPaidOrder() {
        BeerCoinPurchaseOrder order = expiredOrder();
        order.setStatus(BeerCoinPurchaseOrderStatus.PAID.name());
        order.setWechatTransactionId("4200009999202609140000000001");
        when(purchaseOrderMapper.selectOne(any())).thenReturn(order);

        boolean applied = service.applyWechatPaymentSuccess(successNotify(order.getAmount()));

        assertThat(applied).isTrue();
        verify(lotMapper, never()).insert(any(BeerCoinLot.class));
        verify(ledgerMapper, never()).insert(any(BeerCoinLedger.class));
    }

    private BeerCoinPurchaseOrder expiredOrder() {
        return BeerCoinPurchaseOrder.builder()
                .id(7L)
                .orderNo("BCP2026091400001")
                .enterpriseAccountId(3L)
                .quantity(50L)
                .amount(new BigDecimal("199.00"))
                .status(BeerCoinPurchaseOrderStatus.EXPIRED.name())
                .outTradeNo(OUT_TRADE_NO)
                .expireTime(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    private WechatPayClient.PaymentNotifyResult successNotify(BigDecimal paidAmount) {
        return new WechatPayClient.PaymentNotifyResult(
                "NOTIFY-1", "TRANSACTION.SUCCESS", OUT_TRADE_NO,
                "4200009999202609140000000001", "SUCCESS", "支付成功",
                paidAmount, LocalDateTime.now(), "{}");
    }
}
