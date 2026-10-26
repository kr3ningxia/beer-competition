package com.beercompetition.service;

import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.configuration.ScoreConfigExportService;
import com.beercompetition.competition.query.CompetitionFeedbackQueryService;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameItemRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.vo.AdminFeedbackJudgeScoreVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryScoreConfigFlowIntegrationTest extends IntegrationTestBase {

    private static final String SPECIAL_ENTRY_NAME = "-康普茶作品";

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionConfigurationService competitionConfigurationService;

    @Autowired
    private ScoreConfigExportService scoreConfigExportService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CompetitionFeedbackQueryService competitionFeedbackQueryService;

    @Test
    void specialCategoryFormAppliesToMixedTableAndPersistsRecordItems() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        CompetitionCategory specialCategory = testData.createCategory(fixture.competition().getId(), testRun + "-康普茶");
        BeerEntry specialEntry = testData.createEntry(testRun + "K", fixture.competition().getId(),
                fixture.portalA().brewery().getId(), specialCategory.getId(),
                testRun + SPECIAL_ENTRY_NAME, EntryStatus.STORED, true);

        asAdmin(1L);
        competitionConfigurationService.updateScoreConfigs(fixture.competition().getId(), scoreConfigRequest(specialCategory.getId()));

        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1(), specialEntry), 1);

        asJudge(fixture.cross().getId());
        assertThat(scoreService.getCurrentScoreConfig(JudgeRoleType.CROSS,
                fixture.competition().getId(), fixture.entryA1().getUuid()).getDimensions())
                .extracting(DimensionRequest::getLabel)
                .containsExactly("整体感受", "适饮性", "记忆点");
        ScoreConfigVO specialConfig = scoreService.getCurrentScoreConfig(JudgeRoleType.CROSS,
                fixture.competition().getId(), specialEntry.getUuid());
        assertThat(specialConfig.getDimensions())
                .extracting(DimensionRequest::getKind)
                .containsExactly("SCORE", "SCORE", "CHECK", "TEXT", "CHECK");

        ScoreRecordVO crossScore = scoreService.createScore(scoreRequest(specialEntry, JudgeRoleType.CROSS));
        assertThat(crossScore.getCategoryId()).isEqualTo(specialCategory.getId());
        assertThat(crossScore.getTotalScore()).isEqualByComparingTo(new BigDecimal("50"));
        DimensionRequest scoredCheck = crossScore.getDimensions().stream()
                .filter(dimension -> "主要缺陷".equals(dimension.getLabel()))
                .findFirst()
                .orElseThrow();
        assertThat(scoredCheck.getScored()).isTrue();
        assertThat(scoredCheck.getCheckbox()).isTrue();
        assertThat(scoredCheck.getScore()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(scoredCheck.getSelectedOptions()).containsExactly("氧化味");
        DimensionRequest checkboxOnly = crossScore.getDimensions().stream()
                .filter(dimension -> "风味标记".equals(dimension.getLabel()))
                .findFirst()
                .orElseThrow();
        assertThat(checkboxOnly.getScored()).isFalse();
        assertThat(checkboxOnly.getCheckbox()).isTrue();
        assertThat(checkboxOnly.getNoteEnabled()).isFalse();
        assertThat(checkboxOnly.getNote()).isNull();
        assertThat(checkboxOnly.getSelectedOptions()).containsExactly("花香");

        asJudge(fixture.professional().getId());
        scoreService.createScore(scoreRequest(specialEntry, JudgeRoleType.PROFESSIONAL));

        asJudge(fixture.captain().getId());
        scoreService.createScore(scoreRequest(specialEntry, JudgeRoleType.PROFESSIONAL));
        ScoreRecordVO finalScore = scoreService.finalizeTableScore(specialEntry.getUuid(), finalizeRequest());
        assertThat(finalScore.getConsensusScore()).isEqualByComparingTo(new BigDecimal("46"));

        asAdmin(1L);
        List<AdminFeedbackJudgeScoreVO> judges = competitionFeedbackQueryService
                .getFeedbackReviewEntries(fixture.competition().getId()).stream()
                .filter(entry -> specialEntry.getId().equals(entry.getBeerEntryId()))
                .findFirst()
                .orElseThrow()
                .getJudges();
        AdminFeedbackJudgeScoreVO crossFeedback = judges.stream()
                .filter(judge -> JudgeRoleType.CROSS.name().equals(judge.getRole()))
                .findFirst()
                .orElseThrow();
        assertThat(crossFeedback.getDimensions())
                .extracting(DimensionRequest::getKind)
                .containsExactly("SCORE", "SCORE", "CHECK", "TEXT", "CHECK");
        assertThat(crossFeedback.getMaxTotal()).isEqualByComparingTo(new BigDecimal("50"));
    }

    @Test
    void exportsAllConfiguredScoreForms() throws IOException {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        CompetitionCategory specialCategory = testData.createCategory(fixture.competition().getId(), testRun + "-康普茶");

        asAdmin(1L);
        competitionConfigurationService.updateScoreConfigs(fixture.competition().getId(), scoreConfigRequest(specialCategory.getId()));

        FileDownloadVO file = scoreConfigExportService.export(fixture.competition().getId());
        assertThat(file.getFileName()).endsWith("-评分表配置.xlsx");
        assertThat(file.getContent()).isNotEmpty();
        Map<String, String> entries = readXlsx(file.getContent());
        assertThat(entries.get("xl/worksheets/sheet1.xml"))
                .contains("适用组别", "通用评分表", specialCategory.getName(), "评语字数下限")
                .contains("state=\"frozen\"")
                .contains("<cols>");
        assertThat(entries.get("xl/worksheets/sheet2.xml"))
                .contains("维度名称", "候选项数", "补充评语", "评分 + 勾选", "勾选项", "风味标记");
        assertThat(entries.get("xl/worksheets/sheet3.xml"))
                .contains("维度序号", "候选项", "评分 + 勾选", "风味标记", "氧化味");
    }

    private Map<String, String> readXlsx(byte[] content) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    @Test
    void categoryIdRemapKeepsSpecialFormAndBlocksDeletion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        CompetitionCategory specialCategory = testData.createCategory(fixture.competition().getId(), testRun + "-康普茶");

        asAdmin(1L);
        competitionConfigurationService.updateScoreConfigs(fixture.competition().getId(), scoreConfigRequest(specialCategory.getId()));

        competitionConfigurationService.updateCategories(fixture.competition().getId(),
                categoryRequest(fixture.category().getName(), specialCategory.getName()));

        Long remappedCategoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM competition_category WHERE competition_id = ? AND name = ?",
                Long.class, fixture.competition().getId(), specialCategory.getName());
        assertThat(competitionConfigurationService.getScoreConfigs(fixture.competition().getId()))
                .filteredOn(config -> config.getCategoryId() > 0)
                .hasSize(3)
                .allMatch(config -> remappedCategoryId.equals(config.getCategoryId()));

        assertThatThrownBy(() -> competitionConfigurationService.updateCategories(fixture.competition().getId(),
                categoryRequest(fixture.category().getName())))
                .hasMessageContaining("专属评分表");
    }

    private ConfigNameBatchUpdateRequest categoryRequest(String... names) {
        ConfigNameBatchUpdateRequest request = new ConfigNameBatchUpdateRequest();
        request.setItems(java.util.Arrays.stream(names).map(name -> {
            ConfigNameItemRequest item = new ConfigNameItemRequest();
            item.setName(name);
            return item;
        }).toList());
        return request;
    }

    private ScoreConfigBatchUpdateRequest scoreConfigRequest(Long specialCategoryId) {
        ScoreConfigBatchUpdateRequest request = new ScoreConfigBatchUpdateRequest();
        request.setConfigs(List.of(
                config(0L, JudgeRoleType.CROSS, List.of(
                        scoreDimension("cross_1", "整体感受", 20),
                        scoreDimension("cross_2", "适饮性", 20),
                        scoreDimension("cross_3", "记忆点", 10))),
                config(0L, JudgeRoleType.PROFESSIONAL, List.of(
                        scoreDimension("pro_1", "香气", 20),
                        scoreDimension("pro_2", "味道", 20),
                        scoreDimension("pro_3", "口感", 10))),
                config(0L, JudgeRoleType.CAPTAIN, List.of(scoreDimension("consensus", "共识分", 50))),
                config(specialCategoryId, JudgeRoleType.CROSS, specialDimensions("cross")),
                config(specialCategoryId, JudgeRoleType.PROFESSIONAL, specialDimensions("pro")),
                config(specialCategoryId, JudgeRoleType.CAPTAIN, List.of(
                        scoreDimension("consensus_kombucha", "康普茶共识分", 50)))));
        return request;
    }

    private List<DimensionRequest> specialDimensions(String prefix) {
        DimensionRequest appearance = scoreDimension(prefix + "_appearance", "外观", 10);
        DimensionRequest overall = scoreDimension(prefix + "_overall", "整体表现", 30);
        DimensionRequest defect = new DimensionRequest();
        defect.setKey(prefix + "_defect");
        defect.setLabel("主要缺陷");
        defect.setKind("CHECK");
        defect.setScored(true);
        defect.setCheckbox(true);
        defect.setMaxScore(BigDecimal.TEN);
        defect.setSelectionMode("SINGLE");
        defect.setRequired(true);
        defect.setOptions(List.of("无", "氧化味", "金属味"));
        DimensionRequest flavorNote = new DimensionRequest();
        flavorNote.setKey(prefix + "_flavor");
        flavorNote.setLabel("风味描述");
        flavorNote.setKind("TEXT");
        flavorNote.setScored(false);
        flavorNote.setCheckbox(false);
        flavorNote.setRequired(true);
        DimensionRequest flavorFlags = new DimensionRequest();
        flavorFlags.setKey(prefix + "_flags");
        flavorFlags.setLabel("风味标记");
        flavorFlags.setKind("CHECK");
        flavorFlags.setScored(false);
        flavorFlags.setCheckbox(true);
        flavorFlags.setNoteEnabled(false);
        flavorFlags.setSelectionMode("MULTI");
        flavorFlags.setRequired(true);
        flavorFlags.setOptions(List.of("花香", "果香", "香料"));
        return List.of(appearance, overall, defect, flavorNote, flavorFlags);
    }

    private ScoreConfigItemRequest config(Long categoryId, JudgeRoleType role, List<DimensionRequest> dimensions) {
        ScoreConfigItemRequest config = new ScoreConfigItemRequest();
        config.setCategoryId(categoryId);
        config.setJudgeRoleType(role);
        config.setDimensions(dimensions);
        config.setMinCommentLength(0);
        return config;
    }

    private DimensionRequest scoreDimension(String key, String label, int maxScore) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setKind("SCORE");
        dimension.setScored(true);
        dimension.setCheckbox(false);
        dimension.setMaxScore(BigDecimal.valueOf(maxScore));
        return dimension;
    }

    private JudgeScoreSaveRequest scoreRequest(BeerEntry entry, JudgeRoleType role) {
        String prefix = role == JudgeRoleType.CROSS ? "cross" : "pro";
        JudgeScoreSaveRequest request = new JudgeScoreSaveRequest();
        request.setBeerUuid(entry.getUuid());
        request.setJudgeRoleType(role);
        DimensionRequest scoredCheck = scoredDimension(prefix + "_defect", "主要缺陷", 10, 10, "轻微氧化");
        scoredCheck.setCheckbox(true);
        scoredCheck.setSelectedOptions(List.of("氧化味"));
        request.setDimensions(List.of(
                scoredDimension(prefix + "_appearance", "外观", 10, 10, "透亮"),
                scoredDimension(prefix + "_overall", "整体表现", 30, 30, "协调"),
                scoredCheck,
                textDimension(prefix + "_flavor", "风味描述", "柑橘与轻酸，发酵干净"),
                checkDimension(prefix + "_flags", "风味标记", "花香")));
        request.setTotalScore(new BigDecimal("50"));
        request.setComments("透亮\n协调\n柑橘与轻酸，发酵干净");
        return request;
    }

    private DimensionRequest scoredDimension(String key, String label, int maxScore, int score, String note) {
        DimensionRequest dimension = scoreDimension(key, label, maxScore);
        dimension.setScore(BigDecimal.valueOf(score));
        dimension.setNote(note);
        return dimension;
    }

    private DimensionRequest textDimension(String key, String label, String note) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setKind("TEXT");
        dimension.setScored(false);
        dimension.setCheckbox(false);
        dimension.setRequired(true);
        dimension.setNote(note);
        return dimension;
    }

    private DimensionRequest checkDimension(String key, String label, String selectedOption) {
        DimensionRequest dimension = new DimensionRequest();
        dimension.setKey(key);
        dimension.setLabel(label);
        dimension.setKind("CHECK");
        dimension.setScored(false);
        dimension.setCheckbox(true);
        dimension.setSelectionMode("SINGLE");
        dimension.setRequired(true);
        dimension.setSelectedOptions(List.of(selectedOption));
        dimension.setNote("后段轻微氧化");
        return dimension;
    }

    private TableScoreFinalizeRequest finalizeRequest() {
        TableScoreFinalizeRequest request = new TableScoreFinalizeRequest();
        DimensionRequest dimension = scoreDimension("consensus_kombucha", "康普茶共识分", 50);
        dimension.setScore(new BigDecimal("46"));
        request.setDimensions(List.of(dimension));
        request.setConsensusScore(new BigDecimal("46"));
        request.setComments("整体协调，后段略有氧化");
        request.setAdvanced(true);
        return request;
    }
}
