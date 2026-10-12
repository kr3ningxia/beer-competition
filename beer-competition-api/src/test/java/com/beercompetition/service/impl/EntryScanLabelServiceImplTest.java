package com.beercompetition.service.impl;

import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryScanLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryScanLabelServiceImplTest {

    private static final int SAMPLE_SIZE = 200;
    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{5}");

    @Mock
    private EntryScanLabelMapper entryScanLabelMapper;

    @InjectMocks
    private EntryScanLabelServiceImpl service;

    @Test
    void createActiveLabelGeneratesShortCodesWithoutAmbiguousCharacters() {
        when(entryScanLabelMapper.selectCount(any())).thenReturn(0L);

        BeerEntry entry = new BeerEntry();
        entry.setId(1L);
        entry.setCompetitionId(2L);

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            service.createActiveLabel(entry, 3L);
        }

        ArgumentCaptor<EntryScanLabel> labelCaptor = ArgumentCaptor.forClass(EntryScanLabel.class);
        verify(entryScanLabelMapper, times(SAMPLE_SIZE)).insert(labelCaptor.capture());
        List<String> shortCodes = labelCaptor.getAllValues().stream()
                .map(EntryScanLabel::getShortCode)
                .toList();

        assertThat(shortCodes).allMatch(code -> SHORT_CODE_PATTERN.matcher(code).matches());
        assertThat(shortCodes).noneMatch(code -> code.contains("O")
                || code.contains("0")
                || code.contains("1")
                || code.contains("I"));
        verify(entryScanLabelMapper, times(SAMPLE_SIZE * 3)).selectCount(any());
    }
}
