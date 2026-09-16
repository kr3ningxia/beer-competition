package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.JudgePerformanceSaveRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
class JudgePerformanceServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private JudgePerformanceService judgePerformanceService;

    @Test
    void confirmedEvaluationCanBeReeditedWithCurrentVersion() {
        var fixture = lockedScoreFixture();
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());
        insertConfirmedEvaluation(fixture.competition().getId(), fixture.professional().getId(), 0);
        asAdmin(1L);

        var saved = judgePerformanceService.savePerformance(
                fixture.competition().getId(), fixture.professional().getPublicId(), confirmedRequest(0, 3));

        assertThat(saved.getEvaluationStatus()).isEqualTo("CONFIRMED");
        assertThat(saved.getManualScore()).isEqualByComparingTo("52.5");
        assertThat(saved.getTotalScore()).isEqualByComparingTo("52.5");
        assertThat(saved.getVersion()).isEqualTo(1);
    }

    @Test
    void staleVersionCannotReeditConfirmedEvaluation() {
        var fixture = lockedScoreFixture();
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());
        insertConfirmedEvaluation(fixture.competition().getId(), fixture.professional().getId(), 2);
        asAdmin(1L);

        assertThatThrownBy(() -> judgePerformanceService.savePerformance(
                fixture.competition().getId(), fixture.professional().getPublicId(), confirmedRequest(1, 3)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已被其他管理员更新");
    }

    @Test
    void staleVersionCannotOverwriteDraft() {
        var fixture = lockedScoreFixture();
        insertEvaluation(fixture.competition().getId(), fixture.professional().getId(), "DRAFT", 2);
        asAdmin(1L);

        assertThatThrownBy(() -> judgePerformanceService.savePerformance(
                fixture.competition().getId(), fixture.professional().getPublicId(), draftRequest(1)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已被其他管理员更新");
    }

    @Test
    void evaluationCannotBeSavedBeforeScoreRoundIsLocked() {
        var fixture = testData.createFixture(testRun);
        asAdmin(1L);

        assertThatThrownBy(() -> judgePerformanceService.savePerformance(
                fixture.competition().getId(), fixture.professional().getPublicId(), draftRequest(0)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("评分轮尚未锁定");
    }

    @Test
    void accountHistoryWithoutConfirmedEvaluationReturnsEmptyHistory() {
        var fixture = testData.createFixture(testRun);
        asAdmin(1L);

        var history = judgePerformanceService.getAccountHistory(fixture.professional().getPublicId());

        assertThat(history.getEvaluatedCompetitionCount()).isZero();
        assertThat(history.getAverageScore()).isNull();
        assertThat(history.getHistory()).isEmpty();
    }

    @Test
    void incompleteJudgeCanBeConfirmedButReceivesNoCommentScore() {
        var fixture = lockedScoreFixture();
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());
        asAdmin(1L);
        JudgePerformanceSaveRequest request = confirmedRequest(0, 4);

        var saved = judgePerformanceService.savePerformance(
                fixture.competition().getId(), fixture.professional().getPublicId(), request);

        assertThat(saved.getEvaluationStatus()).isEqualTo("CONFIRMED");
        assertThat(saved.getTaskCompletedCount()).isZero();
        assertThat(saved.getTaskTotalCount()).isEqualTo(3);
        assertThat(saved.getCommentScore()).isZero();
        assertThat(saved.getTotalScore()).isEqualByComparingTo("63.0");
        assertThat(saved.getExcellentCandidate()).isFalse();
    }

    private BeerCompetitionTestData.Fixture lockedScoreFixture() {
        var fixture = testData.createFixture(testRun);
        var scoreRound = testData.createPublishedScoreRound(fixture,
                List.of(fixture.entryA1(), fixture.entryA2(), fixture.entryB1()), 1);
        jdbcTemplate.update("UPDATE competition_round SET status = ? WHERE id = ?",
                RoundStatus.LOCKED.name(), scoreRound.round().getId());
        return fixture;
    }

    private void insertEvaluation(Long competitionId, Long judgeId, String status, int version) {
        jdbcTemplate.update("""
                INSERT INTO competition_judge_evaluation
                    (competition_id, judge_account_id, status, version,
                     task_completed_count, task_total_count, completion_rate)
                VALUES (?, ?, ?, ?, 0, 3, 0.00)
                """, competitionId, judgeId, status, version);
    }

    private void insertConfirmedEvaluation(Long competitionId, Long judgeId, int version) {
        jdbcTemplate.update("""
                INSERT INTO competition_judge_evaluation
                    (competition_id, judge_account_id, status, version,
                     judgment_level, feedback_quality_level, rule_execution_level, professionalism_level,
                     manual_score, comment_score, total_score,
                     task_completed_count, task_total_count, completion_rate)
                VALUES (?, ?, 'CONFIRMED', ?, 4, 4, 4, 4, 63.0, 0.0, 63.0, 0, 3, 0.00)
                """, competitionId, judgeId, version);
    }

    private JudgePerformanceSaveRequest draftRequest(int version) {
        JudgePerformanceSaveRequest request = new JudgePerformanceSaveRequest();
        request.setStatus("DRAFT");
        request.setVersion(version);
        return request;
    }

    private JudgePerformanceSaveRequest confirmedRequest(int version, int level) {
        JudgePerformanceSaveRequest request = draftRequest(version);
        request.setStatus("CONFIRMED");
        request.setJudgmentLevel(level);
        request.setFeedbackQualityLevel(level);
        request.setRuleExecutionLevel(level);
        request.setProfessionalismLevel(level);
        return request;
    }
}
