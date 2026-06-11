package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
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

    @Test
    void assignedJudgeCanScoreOnceAndDuplicateCreateIsRejected() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);

        asJudge(fixture.professional().getId());
        var score = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 18, 27));

        assertThat(score.getBeerUuid()).isEqualTo(fixture.entryA1().getUuid());
        assertThat(score.getJudgeRoleType()).isEqualTo(JudgeRoleType.PROFESSIONAL.name());
        assertThat(score.getTotalScore()).isEqualByComparingTo(new BigDecimal("45"));

        assertThatThrownBy(() -> scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid(), 17, 26)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已经提交过评分");
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
                dimension("aroma", "香气", 20, aroma),
                dimension("taste", "口味", 30, taste)
        );
    }

    private TableScoreFinalizeRequest finalizeRequest(int score, boolean advanced) {
        TableScoreFinalizeRequest request = new TableScoreFinalizeRequest();
        request.setDimensions(List.of(dimension("consensus", "共识分", 50, score)));
        request.setConsensusScore(new BigDecimal(score));
        request.setComments("桌长总结");
        request.setAdvanced(advanced);
        return request;
    }

    private DimensionRequest dimension(String key, String label, int maxScore, int score) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setMaxScore(new BigDecimal(maxScore));
        dimension.setScore(new BigDecimal(score));
        return dimension;
    }
}
