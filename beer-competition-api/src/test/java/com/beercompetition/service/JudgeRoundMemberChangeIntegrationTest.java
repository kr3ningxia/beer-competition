package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.judging.round.JudgeRoundTaskService;
import com.beercompetition.judging.scoring.RankingService;
import com.beercompetition.judging.scoring.ScoreConfirmationService;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeRoundMemberChangeRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JudgeRoundMemberChangeIntegrationTest extends IntegrationTestBase {

    @Autowired private BeerCompetitionTestData testData;
    @Autowired private JudgeService judgeService;
    @Autowired private ScoreService scoreService;
    @Autowired private ScoreConfirmationService scoreConfirmationService;
    @Autowired private RankingService rankingService;
    @Autowired private JudgeRoundTaskService judgeRoundTaskService;
    @Autowired private RoundTableMapper roundTableMapper;

    @Test
    void scoreJudgeCanLeaveAfterPartialScoringAndRejoinWithoutLosingScores() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), scoreRound.round().getId(),
                scoreRound.table().getId(), removeRequest(fixture.professional().getPublicId()));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM score_record
                WHERE round_table_id = ? AND judge_account_id = ?
                """, Integer.class, scoreRound.table().getId(), fixture.professional().getId())).isEqualTo(1);
        assertThat(memberValue(scoreRound.table().getId(), fixture.professional().getId(), "status"))
                .isEqualTo("REMOVED");
        assertThat(memberValue(scoreRound.table().getId(), fixture.professional().getId(), "system_task_required"))
                .isEqualTo("0");

        asJudge(fixture.professional().getId());
        assertThat(judgeRoundTaskService.listMyTasks())
                .noneMatch(task -> scoreRound.table().getId().equals(task.getRoundTableId()));
        assertThatThrownBy(() -> scoreService.createScore(
                professionalScoreRequest(fixture.entryA2().getUuid(), 17, 26)))
                .isInstanceOf(ForbiddenException.class);

        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), scoreRound.round().getId(),
                scoreRound.table().getId(), addRequest(fixture.professional().getPublicId(), JudgeRoleType.PROFESSIONAL));

        assertThat(memberValue(scoreRound.table().getId(), fixture.professional().getId(), "status"))
                .isEqualTo("ACTIVE");
        assertThat(memberValue(scoreRound.table().getId(), fixture.professional().getId(), "system_task_required"))
                .isEqualTo("1");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT removed_time IS NULL FROM round_table_member
                WHERE round_table_id = ? AND judge_account_id = ?
                """, Boolean.class, scoreRound.table().getId(), fixture.professional().getId())).isTrue();

        asJudge(fixture.professional().getId());
        assertThat(judgeRoundTaskService.listMyTasks())
                .anyMatch(task -> scoreRound.table().getId().equals(task.getRoundTableId()));
        scoreService.createScore(professionalScoreRequest(fixture.entryA2().getUuid(), 17, 26));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM score_record
                WHERE round_table_id = ? AND judge_account_id = ?
                """, Integer.class, scoreRound.table().getId(), fixture.professional().getId())).isEqualTo(2);
    }

    @Test
    void leavingJudgeIsRemovedFromPendingConfirmationsAndTableCanAutoSubmit() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46));

        asJudge(fixture.professional().getId());
        var confirmation = scoreConfirmationService.getScoreConfirmation(scoreRound.table().getId());
        scoreConfirmationService.confirmScoreRoundTable(scoreRound.table().getId(),
                confirmationRequest(confirmation.getResultVersion()));

        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), scoreRound.round().getId(),
                scoreRound.table().getId(), removeRequest(fixture.cross().getPublicId()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, scoreRound.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_table_confirmation
                WHERE round_table_id = ? AND judge_account_id = ?
                """, Integer.class, scoreRound.table().getId(), fixture.cross().getId())).isZero();
    }

    @Test
    void rankingParticipantWithNoSystemTaskFlagCanLeaveAndRejoin() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()),
                RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 0);

        asJudge(fixture.captain().getId());
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(fixture.entryA1().getId()));

        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), rankingRound.round().getId(),
                rankingRound.table().getId(), removeRequest(fixture.professional().getPublicId()));

        asJudge(fixture.professional().getId());
        assertThat(judgeRoundTaskService.listMyTasks())
                .noneMatch(task -> rankingRound.table().getId().equals(task.getRoundTableId()));
        assertThatThrownBy(() -> rankingService.confirmRankingRoundTable(rankingRound.table().getId(),
                confirmationRequest(1)))
                .isInstanceOf(ForbiddenException.class);

        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), rankingRound.round().getId(),
                rankingRound.table().getId(), addRequest(fixture.professional().getPublicId(), JudgeRoleType.PROFESSIONAL));

        assertThat(memberValue(rankingRound.table().getId(), fixture.professional().getId(), "status"))
                .isEqualTo("ACTIVE");
        assertThat(memberValue(rankingRound.table().getId(), fixture.professional().getId(), "system_task_required"))
                .isEqualTo("0");
        asJudge(fixture.professional().getId());
        assertThat(judgeRoundTaskService.listMyTasks())
                .anyMatch(task -> rankingRound.table().getId().equals(task.getRoundTableId()));
    }

    @Test
    void activeJudgeCannotBeAddedToAnotherTableInTheSameRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 0);
        RoundTable otherTable = RoundTable.builder()
                .competitionId(fixture.competition().getId())
                .roundId(rankingRound.round().getId())
                .tableName("排序二号桌")
                .targetCount(1)
                .targetMode(RoundTargetMode.TOP_N.name())
                .status(RoundStatus.IN_PROGRESS.name())
                .resultVersion(1)
                .confirmationOverrideFlag(0)
                .sortOrder(2)
                .build();
        roundTableMapper.insert(otherTable);

        asAdmin(1L);
        assertThatThrownBy(() -> judgeService.changeRoundTableMembers(fixture.competition().getId(),
                rankingRound.round().getId(), otherTable.getId(),
                addRequest(fixture.professional().getPublicId(), JudgeRoleType.PROFESSIONAL)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("只能分配到一张桌");
    }

    @Test
    void replacingCaptainKeepsExistingFinalScoreAndLeavesOnlyOneActiveCaptain() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46));

        JudgeRoundMemberChangeRequest request = addRequest(fixture.outsider().getPublicId(), JudgeRoleType.CAPTAIN);
        request.setRemoveJudgePublicIds(List.of(fixture.captain().getPublicId()));
        request.setCaptainJudgePublicId(fixture.outsider().getPublicId());
        request.setReason("桌长临时离场");
        asAdmin(1L);
        judgeService.changeRoundTableMembers(fixture.competition().getId(), scoreRound.round().getId(),
                scoreRound.table().getId(), request);

        assertThat(jdbcTemplate.queryForObject("SELECT captain_judge_id FROM round_table WHERE id = ?",
                Long.class, scoreRound.table().getId())).isEqualTo(fixture.outsider().getId());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_table_member
                WHERE round_table_id = ? AND role = 'CAPTAIN' AND status = 'ACTIVE'
                """, Integer.class, scoreRound.table().getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM score_record
                WHERE round_table_id = ? AND is_final = 1
                """, Integer.class, scoreRound.table().getId())).isEqualTo(1);

        asJudge(fixture.outsider().getId());
        assertThatThrownBy(() -> scoreService.finalizeTableScore(
                fixture.entryA1().getUuid(), finalizeRequest(45)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("原桌长");
    }

    private JudgeRoundMemberChangeRequest removeRequest(String judgePublicId) {
        JudgeRoundMemberChangeRequest request = new JudgeRoundMemberChangeRequest();
        request.setRemoveJudgePublicIds(List.of(judgePublicId));
        request.setReason("临时离场");
        return request;
    }

    private JudgeRoundMemberChangeRequest addRequest(String judgePublicId, JudgeRoleType role) {
        JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest item =
                new JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest();
        item.setJudgePublicId(judgePublicId);
        item.setRole(role);
        JudgeRoundMemberChangeRequest request = new JudgeRoundMemberChangeRequest();
        request.setAdd(List.of(item));
        request.setReason("恢复参与评审");
        return request;
    }

    private String memberValue(Long tableId, Long judgeId, String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM round_table_member "
                + "WHERE round_table_id = ? AND judge_account_id = ?", String.class, tableId, judgeId);
    }

    private JudgeScoreSaveRequest professionalScoreRequest(String uuid, int aroma, int taste) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.PROFESSIONAL);
        request.setDimensions(List.of(
                dimension("aroma", "香气", 20, aroma, "香气清晰"),
                dimension("taste", "口味", 30, taste, "口味平衡")));
        request.setTotalScore(new BigDecimal(aroma + taste));
        request.setComments("专业评语");
        return request;
    }

    private JudgeScoreSaveRequest crossScoreRequest(String uuid, int overall) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.CROSS);
        request.setDimensions(List.of(dimension("overall", "整体印象", 50, overall, null)));
        request.setTotalScore(new BigDecimal(overall));
        request.setComments("跨界评语");
        return request;
    }

    private TableScoreFinalizeRequest finalizeRequest(int score) {
        TableScoreFinalizeRequest request = new TableScoreFinalizeRequest();
        request.setDimensions(List.of(dimension("consensus", "共识分", 50, score, "桌长共识")));
        request.setConsensusScore(new BigDecimal(score));
        request.setComments("桌长总结");
        request.setAdvanced(true);
        return request;
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

    private RankingSubmitRequest rankingRequest(Long entryId) {
        RankingResultItemRequest item = new RankingResultItemRequest();
        item.setBeerEntryId(entryId);
        item.setRankNo(1);
        RankingSubmitRequest request = new RankingSubmitRequest();
        request.setResults(List.of(item));
        return request;
    }

    private RoundTableConfirmationRequest confirmationRequest(Integer resultVersion) {
        RoundTableConfirmationRequest request = new RoundTableConfirmationRequest();
        request.setResultVersion(resultVersion);
        return request;
    }
}
