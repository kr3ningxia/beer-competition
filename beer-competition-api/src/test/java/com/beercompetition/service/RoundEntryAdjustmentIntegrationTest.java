package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.judging.assignment.RoundEntryAdjustmentService;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundEntryAdjustmentIntegrationTest extends IntegrationTestBase {

    @Autowired private BeerCompetitionTestData testData;
    @Autowired private RoundEntryAdjustmentService roundEntryAdjustmentService;
    @Autowired private ScoreService scoreService;

    @Test
    void removingEntryKeepsScoresAndRestoresOnReAdd() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound round = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 2);

        // 桌长完成 A1 汇总并标记晋级
        asJudge(fixture.professional().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));
        asJudge(fixture.cross().getId());
        scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid(), 44));
        asJudge(fixture.captain().getId());
        scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 28));
        scoreService.finalizeTableScore(fixture.entryA1().getUuid(), finalizeRequest(46));

        assertThat(entryStatus(round.round().getId(), fixture.entryA1().getId())).isEqualTo("ADVANCED");
        assertThat(advanceResultCount(round.round().getId(), fixture.entryA1().getId())).isEqualTo(1);
        int versionBefore = tableResultVersion(round.table().getId());

        asAdmin(1L);
        roundEntryAdjustmentService.removeEntryFromRoundTable(
                fixture.competition().getId(), round.round().getId(), round.table().getId(),
                fixture.entryA1().getUuid(), "样品重复录入");

        // 摘除：分配行移除、晋级结果作废，评分记录保留，桌结果版本递增
        assertThat(roundEntryCount(round.round().getId(), fixture.entryA1().getId())).isZero();
        assertThat(advanceResultCount(round.round().getId(), fixture.entryA1().getId())).isZero();
        assertThat(finalScoreCount(fixture.entryA1().getId())).isEqualTo(1);
        assertThat(tableResultVersion(round.table().getId())).isEqualTo(versionBefore + 1);
        assertThat(tableStatus(round.table().getId())).isEqualTo("PUBLISHED");

        asAdmin(1L);
        roundEntryAdjustmentService.addEntryToRoundTable(
                fixture.competition().getId(), round.round().getId(), round.table().getId(),
                fixture.entryA1().getUuid(), "误摘回补");

        // 回补：按保留的桌长汇总重建分配行与晋级状态
        assertThat(roundEntryCount(round.round().getId(), fixture.entryA1().getId())).isEqualTo(1);
        assertThat(entryStatus(round.round().getId(), fixture.entryA1().getId())).isEqualTo("ADVANCED");
        assertThat(advanceResultCount(round.round().getId(), fixture.entryA1().getId())).isEqualTo(1);
    }

    @Test
    void canAddNewlyStoredEntryMidRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound round = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 2);

        asAdmin(1L);
        roundEntryAdjustmentService.addEntryToRoundTable(
                fixture.competition().getId(), round.round().getId(), round.table().getId(),
                fixture.entryB1().getUuid(), "现场补录新到样品");

        assertThat(roundEntryCount(round.round().getId(), fixture.entryB1().getId())).isEqualTo(1);
        assertThat(entryStatus(round.round().getId(), fixture.entryB1().getId())).isEqualTo("ASSIGNED");
        // 新加入的入库酒款尚未汇总，不产生晋级结果
        assertThat(advanceResultCount(round.round().getId(), fixture.entryB1().getId())).isZero();
    }

    @Test
    void rejectsNonStoredEntryAndLockedRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound round = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 2);

        BeerEntry registered = testData.createEntry(testRun, fixture.competition().getId(),
                fixture.portalA().brewery().getId(), fixture.category().getId(),
                testRun + "-未入库", EntryStatus.REGISTERED, true);

        asAdmin(1L);
        assertThatThrownBy(() -> roundEntryAdjustmentService.addEntryToRoundTable(
                fixture.competition().getId(), round.round().getId(), round.table().getId(),
                registered.getUuid(), null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已入库");

        jdbcTemplate.update("UPDATE competition_round SET status = 'LOCKED' WHERE id = ?", round.round().getId());
        assertThatThrownBy(() -> roundEntryAdjustmentService.removeEntryFromRoundTable(
                fixture.competition().getId(), round.round().getId(), round.table().getId(),
                fixture.entryA1().getUuid(), null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未锁定");
    }

    private int roundEntryCount(Long roundId, Long beerEntryId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM round_table_entry WHERE round_id = ? AND beer_entry_id = ?",
                Integer.class, roundId, beerEntryId);
    }

    private String entryStatus(Long roundId, Long beerEntryId) {
        return jdbcTemplate.queryForObject("SELECT status FROM round_table_entry WHERE round_id = ? AND beer_entry_id = ?",
                String.class, roundId, beerEntryId);
    }

    private int advanceResultCount(Long roundId, Long beerEntryId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_result
                WHERE round_id = ? AND beer_entry_id = ? AND result_type = 'ADVANCE'
                """, Integer.class, roundId, beerEntryId);
    }

    private int finalScoreCount(Long beerEntryId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM score_record WHERE beer_entry_id = ? AND is_final = 1",
                Integer.class, beerEntryId);
    }

    private int tableResultVersion(Long tableId) {
        return jdbcTemplate.queryForObject("SELECT result_version FROM round_table WHERE id = ?", Integer.class, tableId);
    }

    private String tableStatus(Long tableId) {
        return jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?", String.class, tableId);
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
}
