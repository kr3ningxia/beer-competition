package com.beercompetition.registration.entry;

import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEntryServiceImplTest {

    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;
    @Mock
    private BeerEntryMapper beerEntryMapper;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private CompetitionCategoryMapper competitionCategoryMapper;
    @Mock
    private CompetitionStyleConfigMapper competitionStyleConfigMapper;
    @Mock
    private EntryFieldConfigMapper entryFieldConfigMapper;
    @Mock
    private BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AdminEntryDetailAssembler adminEntryDetailAssembler;
    @Mock
    private AdminEntryDeletionService adminEntryDeletionService;
    @Mock
    private AdminEntryStatusService adminEntryStatusService;
    @Mock
    private CompetitionAccessService competitionAccessService;

    @InjectMocks
    private AdminEntryServiceImpl adminEntryService;

    @Test
    void listWithoutCompetitionUsesCurrentOrganizerScope() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(8L);
        when(beerEntryMapper.countAdminEntries(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);

        var result = adminEntryService.listAdminEntries(null, null, null, null,
                null, null, null, null, 1, 30);

        assertThat(result.getTotal()).isZero();
        verify(beerEntryMapper).countAdminEntries(isNull(), eq(8L), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull());
        verifyNoInteractions(competitionMapper);
    }
}
