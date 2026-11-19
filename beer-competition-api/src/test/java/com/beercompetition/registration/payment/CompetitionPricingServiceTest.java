package com.beercompetition.registration.payment;

import com.beercompetition.mapper.CompetitionFeeTierMapper;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionFeeTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 阶梯价改为按折后金额维护后，计价仍按“基础单价 × 档位比例”执行。
 */
@ExtendWith(MockitoExtension.class)
class CompetitionPricingServiceTest {

    @Mock
    private CompetitionFeeTierMapper tierMapper;

    @InjectMocks
    private CompetitionPricingService pricingService;

    @Test
    void chargesTierPriceAsFixedAmount() {
        Competition competition = competition(new BigDecimal("199.00"), null, null);
        when(tierMapper.selectList(any())).thenReturn(List.of(tier(3, "0.800000", "159.20")));

        CompetitionPricingService.PricingQuote quote = pricingService.quote(competition, 0, 3, LocalDateTime.now());

        assertEquals(new BigDecimal("159.20"), quote.items().get(2).getAmount());
        assertEquals(new BigDecimal("557.20"), quote.totalAmount());
    }

    @Test
    void stacksTierPriceOnEarlyBirdFee() {
        Competition competition = competition(new BigDecimal("199.00"), new BigDecimal("99.00"),
                LocalDateTime.now().plusDays(3));
        when(tierMapper.selectList(any())).thenReturn(List.of(tier(3, "0.800000", "159.20")));

        CompetitionPricingService.PricingQuote quote = pricingService.quote(competition, 0, 3, LocalDateTime.now());

        assertEquals(new BigDecimal("79.20"), quote.items().get(2).getAmount());
        assertEquals(new BigDecimal("277.20"), quote.totalAmount());
    }

    @Test
    void fallsBackToLegacyDiscountRateWhenPriceMissing() {
        Competition competition = competition(new BigDecimal("199.00"), null, null);
        when(tierMapper.selectList(any())).thenReturn(List.of(tier(3, "0.800000", null)));

        CompetitionPricingService.PricingQuote quote = pricingService.quote(competition, 0, 3, LocalDateTime.now());

        assertEquals(new BigDecimal("159.20"), quote.items().get(2).getAmount());
    }

    @Test
    void derivesRateFromPriceWhenEntryFeeChanges() {
        Competition competition = competition(new BigDecimal("219.00"), null, null);
        when(tierMapper.selectList(any())).thenReturn(List.of(tier(3, "0.800000", "159.20")));

        CompetitionPricingService.PricingQuote quote = pricingService.quote(competition, 0, 3, LocalDateTime.now());

        assertEquals(new BigDecimal("159.20"), quote.items().get(2).getAmount());
    }

    private Competition competition(BigDecimal entryFee, BigDecimal earlyBirdFee, LocalDateTime earlyBirdDeadline) {
        return Competition.builder()
                .id(1L)
                .entryFee(entryFee)
                .earlyBirdFee(earlyBirdFee)
                .earlyBirdDeadline(earlyBirdDeadline)
                .tierPricingEnabled(1)
                .build();
    }

    private CompetitionFeeTier tier(int startQuantity, String rate, String price) {
        return CompetitionFeeTier.builder()
                .startQuantity(startQuantity)
                .discountRate(rate == null ? null : new BigDecimal(rate))
                .tierPrice(price == null ? null : new BigDecimal(price))
                .build();
    }
}
