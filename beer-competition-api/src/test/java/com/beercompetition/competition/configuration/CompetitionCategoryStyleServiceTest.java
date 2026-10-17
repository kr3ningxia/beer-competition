package com.beercompetition.competition.configuration;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionCategoryStyleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionCategoryStyleServiceTest {

    @Mock
    private CompetitionCategoryStyleMapper competitionCategoryStyleMapper;

    @InjectMocks
    private CompetitionCategoryStyleService competitionCategoryStyleService;

    @Test
    void unrestrictedCategoryAllowsAnyStyle() {
        when(competitionCategoryStyleMapper.selectCount(any())).thenReturn(0L);

        assertThatCode(() -> competitionCategoryStyleService.requireStyleAllowed(1L, "美式IPA"))
                .doesNotThrowAnyException();
    }

    @Test
    void restrictedCategoryAllowsConfiguredStyle() {
        when(competitionCategoryStyleMapper.selectCount(any())).thenReturn(1L, 1L);

        assertThatCode(() -> competitionCategoryStyleService.requireStyleAllowed(1L, "美式IPA"))
                .doesNotThrowAnyException();
    }

    @Test
    void restrictedCategoryRejectsOtherStyle() {
        when(competitionCategoryStyleMapper.selectCount(any())).thenReturn(1L, 0L);

        assertThatThrownBy(() -> competitionCategoryStyleService.requireStyleAllowed(1L, "德式皮尔森"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("不在所选投递组别的可选范围内");
    }
}
