package com.beercompetition.service;

import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.query.CompetitionFeedbackQueryService;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.vo.AdminFeedbackJudgeScoreVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalScoreDimensionVO;
import com.beercompetition.pojo.vo.PortalScoreRecordVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.result.PortalResultQueryService;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomScoreDimensionFlowIntegrationTest extends IntegrationTestBase {

    private static final List<String> PROFESSIONAL_LABELS = List.of("香气层次", "酒体与口感", "收口与余味");

    private static final List<String> CROSS_LABELS = List.of("整体喜好", "易饮程度", "记忆点");

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionConfigurationService competitionConfigurationService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CompetitionFeedbackQueryService competitionFeedbackQueryService;

    @Autowired
    private PortalResultQueryService portalResultQueryService;

    @Test
    void customProfessionalDimensionsFlowThroughScoringFeedbackAndPortalResult() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);

        asAdmin(1L);
        competitionConfigurationService.updateScoreConfigs(fixture.competition().getId(), scoreConfigRequest());

        asJudge(fixture.professional().getId());
        ScoreConfigVO config = scoreService.getCurrentScoreConfig(
                JudgeRoleType.PROFESSIONAL, fixture.competition().getId());
        assertThat(config.getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactlyElementsOf(PROFESSIONAL_LABELS);

        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);
        var score = scoreService.createScore(professionalScoreRequest(fixture.entryA1().getUuid()));
        assertThat(score.getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactlyElementsOf(PROFESSIONAL_LABELS);
        assertThat(score.getTotalScore()).isEqualByComparingTo(new BigDecimal("45"));

        asJudge(fixture.cross().getId());
        var crossScore = scoreService.createScore(crossScoreRequest(fixture.entryA1().getUuid()));
        assertThat(crossScore.getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactlyElementsOf(CROSS_LABELS);

        asAdmin(1L);
        List<AdminFeedbackJudgeScoreVO> feedbackJudges = competitionFeedbackQueryService
                .getFeedbackReviewEntries(fixture.competition().getId()).stream()
                .filter(entry -> fixture.entryA1().getId().equals(entry.getBeerEntryId()))
                .findFirst()
                .orElseThrow()
                .getJudges();
        assertThat(feedbackJudgeScore(feedbackJudges, JudgeRoleType.PROFESSIONAL).getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactlyElementsOf(PROFESSIONAL_LABELS);
        assertThat(feedbackJudgeScore(feedbackJudges, JudgeRoleType.CROSS).getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactlyElementsOf(CROSS_LABELS);

        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.PUBLISHED.name(), fixture.competition().getId());
        asPortal(fixture.portalA().account().getId());
        PortalResultDetailVO resultDetail = portalResultQueryService.getPortalResultDetail(fixture.entryA1().getId());
        PortalScoreRecordVO portalProfessionalScore = portalScore(resultDetail, JudgeRoleType.PROFESSIONAL);
        assertThat(portalProfessionalScore.getDimensions())
                .extracting(PortalScoreDimensionVO::getLabel)
                .containsExactlyElementsOf(PROFESSIONAL_LABELS);
        PortalScoreRecordVO portalCrossScore = portalScore(resultDetail, JudgeRoleType.CROSS);
        assertThat(portalCrossScore.getDimensions())
                .extracting(PortalScoreDimensionVO::getLabel)
                .containsExactlyElementsOf(CROSS_LABELS);
    }

    private AdminFeedbackJudgeScoreVO feedbackJudgeScore(List<AdminFeedbackJudgeScoreVO> judges, JudgeRoleType role) {
        return judges.stream()
                .filter(judge -> role.name().equals(judge.getRole()))
                .findFirst()
                .orElseThrow();
    }

    private PortalScoreRecordVO portalScore(PortalResultDetailVO resultDetail, JudgeRoleType role) {
        return resultDetail.getScores().stream()
                .filter(record -> role.name().equals(record.getJudgeRoleType()))
                .findFirst()
                .orElseThrow();
    }

    private JudgeScoreSaveRequest crossScoreRequest(String uuid) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.CROSS);
        request.setDimensions(List.of(
                dimension("cross_1", CROSS_LABELS.get(0), 20, 16, "整体讨喜"),
                dimension("cross_2", CROSS_LABELS.get(1), 20, 15, "容易入口"),
                dimension("cross_3", CROSS_LABELS.get(2), 10, 8, "花香有记忆点")));
        request.setTotalScore(new BigDecimal("39"));
        request.setComments("整体讨喜\n容易入口\n花香有记忆点");
        return request;
    }

    private ScoreConfigBatchUpdateRequest scoreConfigRequest() {
        ScoreConfigBatchUpdateRequest request = new ScoreConfigBatchUpdateRequest();
        request.setConfigs(List.of(
                config(JudgeRoleType.CROSS, List.of(
                        configDimension("cross_1", "整体喜好", 20),
                        configDimension("cross_2", "易饮程度", 20),
                        configDimension("cross_3", "记忆点", 10))),
                config(JudgeRoleType.PROFESSIONAL, List.of(
                        configDimension("pro_aroma", PROFESSIONAL_LABELS.get(0), 20),
                        configDimension("pro_body", PROFESSIONAL_LABELS.get(1), 20),
                        configDimension("pro_finish", PROFESSIONAL_LABELS.get(2), 10))),
                config(JudgeRoleType.CAPTAIN, List.of(configDimension("consensus", "共识分", 50)))));
        return request;
    }

    private ScoreConfigItemRequest config(JudgeRoleType role, List<DimensionRequest> dimensions) {
        ScoreConfigItemRequest config = new ScoreConfigItemRequest();
        config.setJudgeRoleType(role);
        config.setDimensions(dimensions);
        return config;
    }

    private DimensionRequest configDimension(String key, String label, int maxScore) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setMaxScore(BigDecimal.valueOf(maxScore));
        return dimension;
    }

    private JudgeScoreSaveRequest professionalScoreRequest(String uuid) {
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(uuid);
        request.setJudgeRoleType(JudgeRoleType.PROFESSIONAL);
        request.setDimensions(List.of(
                dimension("pro_aroma", PROFESSIONAL_LABELS.get(0), 20, 18, "香气层次清晰"),
                dimension("pro_body", PROFESSIONAL_LABELS.get(1), 20, 17, "酒体顺滑"),
                dimension("pro_finish", PROFESSIONAL_LABELS.get(2), 10, 10, "收口干净")));
        request.setTotalScore(new BigDecimal("45"));
        request.setComments("香气层次清晰\n酒体顺滑\n收口干净");
        return request;
    }

    private DimensionRequest dimension(String key, String label, int maxScore, int score, String note) {
        DimensionRequest dimension = configDimension(key, label, maxScore);
        dimension.setScore(BigDecimal.valueOf(score));
        dimension.setNote(note);
        return dimension;
    }
}
