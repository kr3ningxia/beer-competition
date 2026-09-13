package com.beercompetition.billing.beercoin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeerCoinServiceImplTest {

    @Test
    void chargesOneCoinPerEffectiveEntryWithCompetitionMinimum() {
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(0)).isEqualTo(1L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(1)).isEqualTo(1L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(10)).isEqualTo(10L);
        assertThat(BeerCoinServiceImpl.requiredQuantityForEntryCount(11)).isEqualTo(11L);
    }
}
