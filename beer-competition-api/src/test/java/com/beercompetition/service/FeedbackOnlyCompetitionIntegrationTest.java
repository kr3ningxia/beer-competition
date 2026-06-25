package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackOnlyCompetitionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private RoundService roundService;

    @Test
    void feedbackOnlyFirstRoundLocksEvaluatedResultsAndPublishesWithoutAwards() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET competition_type = ?, status = ? WHERE id = ?",
                CompetitionType.FEEDBACK_ONLY.name(), CompetitionStatus.JUDGING.name(), fixture.competition().getId());
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);

        insertFinalScore(fixture, scoreRound, fixture.entryA1().getId(), 46);
        insertFinalScore(fixture, scoreRound, fixture.entryA2().getId(), 44);

        asJudge(fixture.professional().getId());
        roundService.confirmScoreRoundTable(scoreRound.table().getId());
        asJudge(fixture.cross().getId());
        roundService.confirmScoreRoundTable(scoreRound.table().getId());

        asAdmin(1L);
        roundService.completeFirstRound(fixture.competition().getId(), scoreRound.round().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.LOCKED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.LOCKED.name());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_result
                WHERE competition_id = ? AND result_type = ?
                """, Integer.class, fixture.competition().getId(), RoundResultType.EVALUATED.name())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_result
                WHERE competition_id = ? AND result_type = ?
                """, Integer.class, fixture.competition().getId(), RoundResultType.ADVANCE.name())).isZero();

        NextRoundCreateRequest request = new NextRoundCreateRequest();
        request.setSourceRoundId(scoreRound.round().getId());
        request.setRoundName("第二轮");
        assertThatThrownBy(() -> roundService.createNextRound(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("风格对齐会不创建后续轮");

        roundService.publishResults(fixture.competition().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition WHERE id = ?",
                String.class, fixture.competition().getId())).isEqualTo(CompetitionStatus.PUBLISHED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, fixture.entryA1().getId())).isEqualTo(EntryStatus.RESULT_PUBLISHED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM award_result WHERE competition_id = ?",
                Integer.class, fixture.competition().getId())).isZero();
    }

    private void insertFinalScore(BeerCompetitionTestData.Fixture fixture,
                                  BeerCompetitionTestData.ScoreRound scoreRound,
                                  Long beerEntryId,
                                  int score) {
        jdbcTemplate.update("""
                INSERT INTO score_record (
                  competition_id, round_id, round_table_id, beer_entry_id, judge_account_id,
                  judge_role_type, dimensions_json, total_score, comments, is_final,
                  is_advanced, consensus_score, duration_seconds, comment_char_count,
                  create_time, update_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?, 60, ?, ?, ?)
                """,
                fixture.competition().getId(),
                scoreRound.round().getId(),
                scoreRound.table().getId(),
                beerEntryId,
                fixture.captain().getId(),
                "CAPTAIN",
                "[{\"key\":\"consensus\",\"label\":\"共识分\",\"maxScore\":50,\"score\":" + score + ",\"note\":\"桌长诊断\"}]",
                new BigDecimal(score),
                "适合当前投报组别，建议补充报名信息",
                new BigDecimal(score),
                18,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
