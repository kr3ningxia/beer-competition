package com.beercompetition.registration.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionFeeTierMapper;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionFeeTier;
import com.beercompetition.pojo.vo.QuotePriceItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 集中计算报名基础价、早鸟价和累计阶梯价。 */
@Service
@RequiredArgsConstructor
public class CompetitionPricingService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private final CompetitionFeeTierMapper tierMapper;

    public PricingQuote quote(Competition competition, int existingCount, int newCount, LocalDateTime at) {
        if (competition == null || newCount < 1) {
            throw new BaseException("报名数量不正确");
        }
        if (existingCount < 0) {
            existingCount = 0;
        }
        BigDecimal base = resolveBaseAmount(competition, at);
        List<CompetitionFeeTier> tiers = loadTiers(competition.getId(), competition.getTierPricingEnabled());
        List<QuotePriceItemVO> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal tierDiscount = BigDecimal.ZERO;
        for (int i = 1; i <= newCount; i++) {
            int sequence = existingCount + i;
            BigDecimal rate = resolveRate(sequence, tiers);
            BigDecimal amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            total = total.add(amount);
            tierDiscount = tierDiscount.add(base.subtract(amount));
            items.add(QuotePriceItemVO.builder().sequence(sequence).baseAmount(base).discountRate(rate).amount(amount).build());
        }
        BigDecimal standardTotal = defaultAmount(competition.getEntryFee()).multiply(BigDecimal.valueOf(newCount));
        return new PricingQuote(existingCount, base, isEarlyBirdActive(competition, at), total.setScale(2, RoundingMode.HALF_UP),
                standardTotal.subtract(total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                tierDiscount.setScale(2, RoundingMode.HALF_UP), items);
    }

    public BigDecimal priceForSequence(Competition competition, BigDecimal baseAmount, int sequence) {
        List<CompetitionFeeTier> tiers = loadTiers(competition.getId(), competition.getTierPricingEnabled());
        BigDecimal rate = resolveRate(sequence, tiers);
        return defaultAmount(baseAmount).multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveBaseAmount(Competition competition, LocalDateTime at) {
        if (isEarlyBirdActive(competition, at) && competition.getEarlyBirdFee() != null) {
            return competition.getEarlyBirdFee();
        }
        return defaultAmount(competition.getEntryFee());
    }

    public boolean isEarlyBirdActive(Competition competition, LocalDateTime at) {
        return competition != null && competition.getEarlyBirdFee() != null && competition.getEarlyBirdDeadline() != null
                && !at.isAfter(competition.getEarlyBirdDeadline());
    }

    private List<CompetitionFeeTier> loadTiers(Long competitionId, Integer enabled) {
        if (!Integer.valueOf(1).equals(enabled) || competitionId == null) {
            return List.of();
        }
        return tierMapper.selectList(new LambdaQueryWrapper<CompetitionFeeTier>()
                .eq(CompetitionFeeTier::getCompetitionId, competitionId)
                .eq(CompetitionFeeTier::getEnabled, 1)
                .orderByAsc(CompetitionFeeTier::getStartQuantity)
                .orderByAsc(CompetitionFeeTier::getId));
    }

    private BigDecimal resolveRate(int sequence, List<CompetitionFeeTier> tiers) {
        return tiers.stream().filter(tier -> tier.getStartQuantity() != null && tier.getStartQuantity() <= sequence)
                .max(Comparator.comparing(CompetitionFeeTier::getStartQuantity))
                .map(CompetitionFeeTier::getDiscountRate).orElse(ONE);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record PricingQuote(int existingCount, BigDecimal baseUnitAmount, boolean earlyBirdActive,
                               BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal tierDiscountAmount,
                               List<QuotePriceItemVO> items) {
    }
}
