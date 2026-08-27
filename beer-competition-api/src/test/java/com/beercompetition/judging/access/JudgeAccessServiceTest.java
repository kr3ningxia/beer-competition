package com.beercompetition.judging.access;

import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JudgeAccessServiceTest {

    private final CompetitionAccessService competitionAccessService = mock(CompetitionAccessService.class);
    private final JudgeAssignmentMapper judgeAssignmentMapper = mock(JudgeAssignmentMapper.class);
    private final JudgeAccessService judgeAccessService = new JudgeAccessService(
            competitionAccessService, judgeAssignmentMapper);

    @Test
    void superAdminUsesUnrestrictedScope() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(true);

        JudgeAccessService.JudgeScope scope = judgeAccessService.currentScope();

        assertThat(scope.all()).isTrue();
        verifyNoInteractions(judgeAssignmentMapper);
    }

    @Test
    void organizerScopeUsesAssignedJudgeIds() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(8L);
        when(judgeAssignmentMapper.selectJudgeIdsByOrganizer(8L)).thenReturn(List.of(11L, 12L));

        assertThat(judgeAccessService.currentScope().judgeIds()).containsExactly(11L, 12L);
    }

    @Test
    void organizerCannotReadJudgeFromAnotherOrganizer() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(8L);
        when(judgeAssignmentMapper.selectJudgeIdsByOrganizer(8L)).thenReturn(List.of(11L));

        assertThatThrownBy(() -> judgeAccessService.requireJudgeAccess(22L))
                .isInstanceOf(ForbiddenException.class);
    }
}
