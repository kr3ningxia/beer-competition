package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetitionStateMachineIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private RoundService roundService;

    @Test
    void cannotPrepareJudgingBeforeRegistrationClosed() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);

        assertThatThrownBy(() -> competitionService.prepareJudging(fixture.competition().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("报名截止");
    }

    @Test
    void publishingFirstRoundMovesCompetitionToJudgingAndRejectsRepeatPublish() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.JUDGING_PREP.name(), fixture.competition().getId());
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);
        jdbcTemplate.update("UPDATE competition_round SET status = ? WHERE id = ?",
                RoundStatus.DRAFT.name(), scoreRound.round().getId());
        jdbcTemplate.update("UPDATE round_table SET status = ? WHERE id = ?",
                RoundStatus.DRAFT.name(), scoreRound.table().getId());

        asAdmin(1L);
        roundService.publishRound(fixture.competition().getId(), scoreRound.round().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition WHERE id = ?",
                String.class, fixture.competition().getId())).isEqualTo(CompetitionStatus.JUDGING.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.PUBLISHED.name());

        assertThatThrownBy(() -> roundService.publishRound(fixture.competition().getId(), scoreRound.round().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("草稿");
    }
}
