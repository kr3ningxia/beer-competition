package com.beercompetition.service;

import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionProgressVO;
import com.beercompetition.pojo.vo.CompetitionQuickSummaryVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionListSummaryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionQueryService competitionQueryService;

    @Autowired
    private CompetitionMapper competitionMapper;

    @Test
    void listSummaryKeepsBusinessCountsAndLoadsQuickProgressOnDemand() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);

        CompetitionVO summary = findCompetition(
                competitionQueryService.listCompetitions(false), fixture.competition().getId());

        assertThat(summary.getEntriesSummary().getTotal()).isEqualTo(3);
        assertThat(summary.getEntriesSummary().getRegistered()).isEqualTo(3);
        assertThat(summary.getEntriesSummary().getStored()).isEqualTo(3);
        assertThat(summary.getJudgeTableCount()).isEqualTo(1);
        assertThat(summary.getJudgeCount()).isEqualTo(3);
        assertThat(summary.getCheckTotal()).isEqualTo(8);
        assertThat(summary.getPrimaryAction()).isNotNull();
        assertThat(summary.getProgressSummary()).isNull();

        CompetitionQuickSummaryVO quickSummary = competitionQueryService.getCompetitionQuickSummary(fixture.competition().getId());
        assertThat(quickSummary.getProgressSummary()).isNotNull();
        assertThat(quickSummary.getAlerts()).isNotNull();
        assertThat(quickSummary.getDataIntegrityIssues()).isNotNull();

        CompetitionDetailVO overview = competitionQueryService.getCompetitionOverview(fixture.competition().getId());
        assertThat(overview.getEntriesSummary().getTotal()).isEqualTo(3);
        assertThat(overview.getEntryPool()).isEmpty();
        assertThat(overview.getRounds()).isEmpty();
        assertThat(overview.getResultDrafts()).isEmpty();
        assertThat(overview.getAwardRules()).isEmpty();
        assertThat(overview.getAwardResults()).isEmpty();

        assertThat(competitionQueryService.getCompetitionEntryPool(fixture.competition().getId())).hasSize(3);

        CompetitionProgressVO progress = competitionQueryService.getCompetitionProgress(fixture.competition().getId());
        assertThat(progress.getProgressSummary()).isNotNull();
        assertThat(progress.getRounds()).isNotNull();

        CompetitionDetailVO fullDetail = competitionQueryService.getCompetitionDetail(fixture.competition().getId());
        assertThat(fullDetail.getEntryPool()).hasSize(3);
        assertThat(fullDetail.getRounds()).isNotNull();
    }

    @Test
    void archivedCompetitionsAreLoadedOnlyWhenRequested() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        fixture.competition().setStatus(CompetitionStatus.ARCHIVED.name());
        competitionMapper.updateById(fixture.competition());
        asAdmin(1L);

        assertThat(competitionQueryService.listCompetitions(false))
                .noneMatch(item -> item.getId().equals(fixture.competition().getId()));
        assertThat(competitionQueryService.listCompetitions(true))
                .anyMatch(item -> item.getId().equals(fixture.competition().getId()));
    }

    private CompetitionVO findCompetition(List<CompetitionVO> competitions, Long competitionId) {
        return competitions.stream()
                .filter(item -> item.getId().equals(competitionId))
                .findFirst()
                .orElseThrow();
    }
}
