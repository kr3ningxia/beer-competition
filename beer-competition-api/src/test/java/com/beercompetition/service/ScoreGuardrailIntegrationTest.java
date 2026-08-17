package com.beercompetition.service;

import com.beercompetition.judging.scoring.ScoreConfirmationService;
import com.beercompetition.judging.assignment.RoundAllocationService;
import com.beercompetition.judging.round.JudgeRoundTaskService;
import com.beercompetition.judging.round.RoundQueryService;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreStartRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreGuardrailIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private ScoreConfirmationService scoreConfirmationService;

    @Autowired
    private RoundAllocationService roundAllocationService;

    @Autowired
    private JudgeRoundTaskService judgeRoundTaskService;

    @Autowired
    private RoundQueryService roundQueryService;

    @Autowired
    private AuthService authService;

    @Test
    void assignedJudgeCanScoreOnceAndDuplicateCreateIsRejected() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        var score = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        assertThat(score.getBeerUuid()).isEqualTo(fixture.entryA1().getUuid());
        assertThat(score.getJudgeRoleType()).isEqualTo(JudgeRoleType.PROFESSIONAL.name());
        assertThat(score.getTotalScore()).isEqualByComparingTo(new BigDecimal("45"));
        assertThat(score.getCommentCharCount()).isEqualTo(8);
        assertThat(score.getDurationSeconds()).isNotNull();

        assertThatThrownBy(() -> scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 26)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已经提交过评分");
    }

    @Test
    void scoreStartIsIdempotentAndUpdateKeepsDurationButRefreshesCommentCount() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        JudgeScoreStartRequest startRequest = new JudgeScoreStartRequest();
        startRequest.setBeerUuid(fixture.entryA1().getUuid());
        startRequest.setJudgeRoleType(JudgeRoleType.PROFESSIONAL);
        scoreService.startScore(startRequest);
        scoreService.startScore(startRequest);

        var score = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        Integer originalDuration = score.getDurationSeconds();

        JudgeScoreUpdateRequest update = new JudgeScoreUpdateRequest();
        update.setDimensions(List.of(
                dimension("aroma", "香气", 20, 17, "香气清晰持久"),
                dimension("taste", "口味", 30, 26, "口味平衡干净")
        ));
        update.setTotalScore(new BigDecimal("43"));
        update.setComments("修改评分");
        var updated = scoreService.updateScore(score.getId(), update);

        assertThat(updated.getDurationSeconds()).isEqualTo(originalDuration);
        assertThat(updated.getCommentCharCount()).isEqualTo(12);
    }

    @Test
    void unassignedJudgeCannotScoreEntry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.outsider().getId());

        assertThatThrownBy(() -> scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("未分配");
    }

    @Test
    void visibleRoundMemberCanScoreWhenBaseAssignmentIsMissing() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound round = testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);
        jdbcTemplate.update("""
                DELETE FROM competition_judge_assignment
                WHERE competition_id = ?
                  AND judge_account_id = ?
                """, fixture.competition().getId(), fixture.professional().getId());

        asJudge(fixture.professional().getId());
        var currentUser = authService.getCurrentUser(UserRole.JUDGE);
        assertThat(currentUser.getCurrentCompetitionId()).isEqualTo(fixture.competition().getId());
        assertThat(currentUser.getTableName()).isEqualTo(round.table().getTableName());
        assertThat(currentUser.getJudgeRoleType()).isEqualTo(JudgeRoleType.PROFESSIONAL.name());

        var score = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        assertThat(score.getBeerUuid()).isEqualTo(fixture.entryA1().getUuid());
        Integer assignmentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM competition_judge_assignment
                WHERE competition_id = ?
                  AND judge_account_id = ?
                """, Integer.class, fixture.competition().getId(), fixture.professional().getId());
        assertThat(assignmentCount).isEqualTo(1);
    }

    @Test
    void normalJudgeCannotUpdateAfterCaptainFinalized() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        var professionalScore = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));

        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46, true));

        asJudge(fixture.professional().getId());
        JudgeScoreUpdateRequest update = new JudgeScoreUpdateRequest();
        update.setDimensions(professionalDimensions(16, 27));
        update.setTotalScore(new BigDecimal("43"));
        update.setComments("修改评分");

        assertThatThrownBy(() -> scoreService.updateScore(professionalScore.getId(), update))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("桌长已汇总");
    }

    @Test
    void captainTableScoresIncludeOwnPersonalScoreSeparatelyFromFinalOpinion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        ScoreRecordVO captainPersonalScore = scoreService.createScore(
                professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));

        List<ScoreRecordVO> personalScores = scoreService.listTableScores(fixture.entryA1().getUuid());

        assertThat(personalScores).hasSize(3);
        assertThat(personalScores).extracting(ScoreRecordVO::getId).contains(captainPersonalScore.getId());
        assertThat(personalScores)
                .filteredOn(score -> Boolean.TRUE.equals(score.getMine()))
                .singleElement()
                .satisfies(score -> {
                    assertThat(score.getId()).isEqualTo(captainPersonalScore.getId());
                    assertThat(score.getIsFinal()).isZero();
                    assertThat(score.getJudgeRoleType()).isEqualTo(JudgeRoleType.PROFESSIONAL.name());
                });

        ScoreRecordVO finalOpinion = scoreService.finalizeTableScore(
                fixture.entryA1().getUuid(), finalizeRequest(46, true));
        List<ScoreRecordVO> scoresWithFinalOpinion = scoreService.listTableScores(fixture.entryA1().getUuid());

        assertThat(scoresWithFinalOpinion).hasSize(4);
        assertThat(scoresWithFinalOpinion)
                .filteredOn(score -> Integer.valueOf(0).equals(score.getIsFinal()))
                .hasSize(3);
        assertThat(scoresWithFinalOpinion)
                .filteredOn(score -> Integer.valueOf(1).equals(score.getIsFinal()))
                .extracting(ScoreRecordVO::getId)
                .containsExactly(finalOpinion.getId());
    }

    @Test
    void captainCannotFinalizeBeforeAllRequiredScoresSubmitted() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));

        assertThatThrownBy(() -> scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46, true)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("同桌评分未完成");
    }

    @Test
    void awardScoreRoundAutoSubmitsAfterAllPeersConfirmCurrentVersion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46, true));

        asJudge(fixture.professional().getId());
        ScoreConfirmationVO confirmation = scoreConfirmationService.getScoreConfirmation(scoreRound.table().getId());
        scoreConfirmationService.confirmScoreRoundTable(scoreRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.PUBLISHED.name());

        asJudge(fixture.cross().getId());
        confirmation = scoreConfirmationService.getScoreConfirmation(scoreRound.table().getId());
        scoreConfirmationService.confirmScoreRoundTable(scoreRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
    }

    @Test
    void staleScoreConfirmationVersionIsRejectedAfterCaptainUpdate() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        ScoreConfirmationVO staleConfirmation = scoreConfirmationService.getScoreConfirmation(scoreRound.table().getId());
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46, true));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(45, true));

        asJudge(fixture.professional().getId());
        assertThatThrownBy(() -> scoreConfirmationService.confirmScoreRoundTable(scoreRound.table().getId(), confirmationRequest(staleConfirmation.getResultVersion())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已更新");
    }

    @Test
    void adminOverrideAutoSubmitsScoreRoundTable() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46, true));

        AdminConfirmationOverrideRequest request = new AdminConfirmationOverrideRequest();
        request.setReason("现场纸面确认");
        asAdmin(1L);
        scoreConfirmationService.overrideScoreConfirmation(fixture.competition().getId(), scoreRound.table().getId(), request);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
    }

    @Test
    void captainCanReopenSubmittedTableAndDraftCandidatesFollowConfirmedResults() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);
        jdbcTemplate.update("UPDATE competition SET status = 'JUDGING' WHERE id = ?", fixture.competition().getId());

        NextRoundCreateRequest nextRoundRequest = nextRoundRequest(fixture, scoreRound.round().getId(), 1);
        asAdmin(1L);
        roundAllocationService.createNextRound(fixture.competition().getId(), nextRoundRequest);
        Long draftRoundId = jdbcTemplate.queryForObject(
                "SELECT id FROM competition_round WHERE source_round_id = ?", Long.class, scoreRound.round().getId());

        submitScoreRound(fixture, scoreRound, fixture.entryA1().getUuid(), fixture.entryA2().getUuid());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT beer_entry_id FROM round_table_entry WHERE round_id = ?",
                Long.class, draftRoundId)).isEqualTo(fixture.entryA1().getId());

        asJudge(fixture.captain().getId());
        judgeRoundTaskService.reopenScoreRoundTable(scoreRound.table().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.PUBLISHED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.PUBLISHED.name());

        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(44, false));
        scoreService.finalizeTableScore(fixture.entryA2().getUuid(), finalizeRequest(47, true));
        confirmScoreTable(fixture, scoreRound.table().getId());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT beer_entry_id FROM round_table_entry WHERE round_id = ?",
                Long.class, draftRoundId)).isEqualTo(fixture.entryA2().getId());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
    }

    @Test
    void submittedSourceTableCandidatesSyncBeforeWholeRoundCompletesAndReopenRemovesThem() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound firstTable = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 1);
        BeerCompetitionTestData.ScoreRound secondTable = testData.addPublishedScoreTable(
                fixture, firstTable.round(), "第一轮二号桌", List.of(fixture.entryB1()), 1, 2);
        jdbcTemplate.update("UPDATE competition SET status = 'JUDGING' WHERE id = ?", fixture.competition().getId());

        asAdmin(1L);
        roundAllocationService.createNextRound(fixture.competition().getId(), nextRoundRequest(fixture, firstTable.round().getId(), 2));
        Long draftRoundId = jdbcTemplate.queryForObject(
                "SELECT id FROM competition_round WHERE source_round_id = ?", Long.class, firstTable.round().getId());

        finalizeScoreEntry(fixture, fixture.entryA1().getUuid(), true);
        finalizeScoreEntry(fixture, fixture.entryB1().getUuid(), true);
        confirmScoreTable(fixture, firstTable.table().getId());

        assertThat(jdbcTemplate.queryForList(
                "SELECT beer_entry_id FROM round_table_entry WHERE round_id = ? ORDER BY beer_entry_id",
                Long.class, draftRoundId)).containsExactly(fixture.entryA1().getId());
        CompetitionRoundVO draftRound = roundQueryService.listCompetitionRounds(fixture.competition().getId()).stream()
                .filter(round -> round.getId().equals(draftRoundId))
                .findFirst()
                .orElseThrow();
        assertThat(draftRound.getSourceSubmittedTableCount()).isEqualTo(1);
        assertThat(draftRound.getSourceTableCount()).isEqualTo(2);
        assertThat(draftRound.getSourceReady()).isFalse();
        assertThat(draftRound.getCandidatesSynced()).isTrue();
        assertThat(draftRound.getSourceEntryUuids()).containsExactly(fixture.entryA1().getUuid());

        asJudge(fixture.captain().getId());
        judgeRoundTaskService.reopenScoreRoundTable(firstTable.table().getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM round_table_entry WHERE round_id = ?",
                Integer.class, draftRoundId)).isZero();

        confirmScoreTable(fixture, firstTable.table().getId());
        confirmScoreTable(fixture, secondTable.table().getId());
        assertThat(jdbcTemplate.queryForList(
                "SELECT beer_entry_id FROM round_table_entry WHERE round_id = ? ORDER BY beer_entry_id",
                Long.class, draftRoundId)).containsExactly(fixture.entryA1().getId(), fixture.entryB1().getId());
    }

    private NextRoundCreateRequest nextRoundRequest(BeerCompetitionTestData.Fixture fixture, Long sourceRoundId, int targetCount) {
        NextRoundCreateRequest request = new NextRoundCreateRequest();
        request.setSourceRoundId(sourceRoundId);
        request.setRoundName("第二轮");
        request.setStrategy("MANUAL");
        request.setTableCount(1);
        request.setTargetMode("TOP_N");
        request.setTargetCount(targetCount);
        request.setCaptainPublicIds(List.of(fixture.captain().getPublicId()));
        return request;
    }

    private void submitScoreRound(BeerCompetitionTestData.Fixture fixture,
                                  BeerCompetitionTestData.ScoreRound scoreRound,
                                  String advancedUuid,
                                  String eliminatedUuid) {
        finalizeScoreEntry(fixture, advancedUuid, true);
        finalizeScoreEntry(fixture, eliminatedUuid, false);
        confirmScoreTable(fixture, scoreRound.table().getId());
    }

    private void finalizeScoreEntry(BeerCompetitionTestData.Fixture fixture, String entryUuid, boolean advanced) {
        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(entryUuid, 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(entryUuid, 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(entryUuid, 17, 28));
        scoreService.finalizeTableScore(entryUuid, finalizeRequest(46, advanced));
    }

    private void confirmScoreTable(BeerCompetitionTestData.Fixture fixture, Long roundTableId) {
        asJudge(fixture.professional().getId());
        ScoreConfirmationVO confirmation = scoreConfirmationService.getScoreConfirmation(roundTableId);
        scoreConfirmationService.confirmScoreRoundTable(roundTableId, confirmationRequest(confirmation.getResultVersion()));
        asJudge(fixture.cross().getId());
        confirmation = scoreConfirmationService.getScoreConfirmation(roundTableId);
        scoreConfirmationService.confirmScoreRoundTable(roundTableId, confirmationRequest(confirmation.getResultVersion()));
    }

    private JudgeScoreSaveRequest professionalScoreRequest(String uuid, int aroma, int taste) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.PROFESSIONAL);
        request.setDimensions(professionalDimensions(aroma, taste));
        request.setTotalScore(new BigDecimal(aroma + taste));
        request.setComments("专业评语");
        return request;
    }

    private JudgeScoreSaveRequest crossScoreRequest(String uuid, int overall) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.CROSS);
        request.setDimensions(List.of(dimension("overall", "整体印象", 50, overall)));
        request.setTotalScore(new BigDecimal(overall));
        request.setComments("跨界评语");
        return request;
    }

    private List<DimensionRequest> professionalDimensions(int aroma, int taste) {
        return List.of(
                dimension("aroma", "香气", 20, aroma, "香气清晰"),
                dimension("taste", "口味", 30, taste, "口味平衡")
        );
    }

    private TableScoreFinalizeRequest finalizeRequest(int score, boolean advanced) {
        TableScoreFinalizeRequest request = new TableScoreFinalizeRequest();
        request.setDimensions(List.of(dimension("consensus", "共识分", 50, score, "桌长共识")));
        request.setConsensusScore(new BigDecimal(score));
        request.setComments("桌长总结");
        request.setAdvanced(advanced);
        return request;
    }

    private RoundTableConfirmationRequest confirmationRequest(Integer resultVersion) {
        RoundTableConfirmationRequest request = new RoundTableConfirmationRequest();
        request.setResultVersion(resultVersion);
        return request;
    }

    private DimensionRequest dimension(String key, String label, int maxScore, int score) {
        return dimension(key, label, maxScore, score, null);
    }

    private DimensionRequest dimension(String key, String label, int maxScore, int score, String note) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setMaxScore(new BigDecimal(maxScore));
        dimension.setScore(new BigDecimal(score));
        dimension.setNote(note);
        return dimension;
    }
}
