package com.beercompetition.service;

import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
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
    private CompetitionService competitionService;

    @Autowired
    private CompetitionMapper competitionMapper;

    @Test
    void listSummaryKeepsBusinessCountsAndLoadsQuickProgressOnDemand() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);

        CompetitionVO summary = findCompetition(
                competitionService.listCompetitions(false), fixture.competition().getId());

        assertThat(summary.getEntriesSummary().getTotal()).isEqualTo(3);
        assertThat(summary.getEntriesSummary().getRegistered()).isEqualTo(3);
        assertThat(summary.getEntriesSummary().getStored()).isEqualTo(3);
        assertThat(summary.getJudgeTableCount()).isEqualTo(1);
        assertThat(summary.getJudgeCount()).isEqualTo(3);
        assertThat(summary.getCheckTotal()).isEqualTo(8);
        assertThat(summary.getPrimaryAction()).isNotNull();
        assertThat(summary.getProgressSummary()).isNull();

        CompetitionQuickSummaryVO quickSummary = competitionService.getCompetitionQuickSummary(fixture.competition().getId());
        assertThat(quickSummary.getProgressSummary()).isNotNull();
        assertThat(quickSummary.getAlerts()).isNotNull();
        assertThat(quickSummary.getDataIntegrityIssues()).isNotNull();
    }

    @Test
    void archivedCompetitionsAreLoadedOnlyWhenRequested() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        fixture.competition().setStatus(CompetitionStatus.ARCHIVED.name());
        competitionMapper.updateById(fixture.competition());
        asAdmin(1L);

        assertThat(competitionService.listCompetitions(false))
                .noneMatch(item -> item.getId().equals(fixture.competition().getId()));
        assertThat(competitionService.listCompetitions(true))
                .anyMatch(item -> item.getId().equals(fixture.competition().getId()));
    }

    private CompetitionVO findCompetition(List<CompetitionVO> competitions, Long competitionId) {
        return competitions.stream()
                .filter(item -> item.getId().equals(competitionId))
                .findFirst()
                .orElseThrow();
    }
}
