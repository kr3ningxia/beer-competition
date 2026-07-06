package com.beercompetition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.vo.CompetitionLiveBoardVO;
import com.beercompetition.pojo.vo.LiveBoardMetricVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiveBoardIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private LiveBoardService liveBoardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void liveBoardKeepsActiveRoundWhenNextRoundIsDraft() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);
        jdbcTemplate.update("""
                INSERT INTO competition_round (
                  competition_id, round_no, round_name, round_type, status, sort_order, create_time, update_time
                ) VALUES (?, 2, ?, ?, ?, 2, ?, ?)
                """,
                fixture.competition().getId(),
                "草稿排序轮",
                RoundType.RANKING.name(),
                RoundStatus.DRAFT.name(),
                LocalDateTime.now(),
                LocalDateTime.now());

        CompetitionLiveBoardVO board = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());

        assertThat(board.getRoundId()).isEqualTo(scoreRound.round().getId());
        assertThat(board.getRoundName()).isEqualTo("第一轮");
        assertThat(board.getStatusText()).isEqualTo("评审中");
    }

    @Test
    void scoreRoundTableStatusFollowsPublicFlow() throws Exception {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);

        CompetitionLiveBoardVO judging = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());
        assertThat(judging.getTables()).hasSize(1);
        assertThat(judging.getTables().get(0).getStatusText()).isEqualTo("评审中");
        assertThat(findMetric(judging, "pendingReview").getValue()).isEqualTo("2");

        insertPersonalScores(fixture, scoreRound);
        CompetitionLiveBoardVO finalizing = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());
        assertThat(finalizing.getTables().get(0).getStatusText()).isEqualTo("汇总中");
        assertThat(findMetric(finalizing, "completionRate").getValue()).isEqualTo("0%");

        insertFinalScores(fixture, scoreRound);
        CompetitionLiveBoardVO confirming = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());
        assertThat(confirming.getTables().get(0).getStatusText()).isEqualTo("确认中");
        assertThat(confirming.getSummary().getEyebrow()).isEqualTo("评审完成");
        assertThat(confirming.getSummary().getDone()).isZero();
        assertThat(confirming.getSummary().getTotal()).isEqualTo(2);

        insertConfirmation(scoreRound.table().getId(), fixture.professional().getId(), scoreRound.table().getResultVersion());
        insertConfirmation(scoreRound.table().getId(), fixture.cross().getId(), scoreRound.table().getResultVersion());
        CompetitionLiveBoardVO completed = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());
        assertThat(completed.getTables().get(0).getStatusText()).isEqualTo("本桌完成");
        assertThat(completed.getTables().get(0).getConfirmationProgress()).isEqualTo("2/2");
        assertThat(completed.getTables().get(0).getReviewedCount()).isEqualTo(2);
        assertThat(completed.getTables().get(0).getPendingCount()).isZero();
        assertThat(completed.getSummary().getDone()).isEqualTo(2);
        assertThat(findMetric(completed, "reviewed").getValue()).isEqualTo("2");
        assertThat(completed.getTables().get(0).getAverageCommentText()).endsWith("字");

        String publicJson = objectMapper.writeValueAsString(completed);
        assertThat(publicJson)
                .doesNotContain(fixture.captain().getName())
                .doesNotContain(fixture.professional().getName())
                .doesNotContain(fixture.entryA1().getName())
                .doesNotContain(fixture.entryA2().getName())
                .doesNotContain(fixture.portalA().brewery().getCompanyName());
    }

    @Test
    void liveBoardSponsorGroupsRespectSortingEnabledAndLogoFallback() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createPublishedScoreRound(fixture, List.of(fixture.entryA1()), 1);
        Long logoAssetId = insertSponsorLogoAsset(fixture.competition().getId(), "/uploads/sponsor/logo.png");
        insertSponsor(fixture.competition().getId(), "合作伙伴", "禁用品牌", null, 0, 0, 0);
        insertSponsor(fixture.competition().getId(), "战略合作", "有Logo品牌", logoAssetId, 1, 1, 1);
        insertSponsor(fixture.competition().getId(), "合作伙伴", "文字品牌", null, 2, 0, 1);
        insertSponsor(fixture.competition().getId(), "合作伙伴", "缺失Logo品牌", 999999999L, 3, 0, 1);

        CompetitionLiveBoardVO board = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());

        assertThat(board.getSponsorGroups()).hasSize(2);
        assertThat(board.getSponsorGroups().get(0).getTierLabel()).isEqualTo("战略合作");
        assertThat(board.getSponsorGroups().get(0).getFeatured()).isTrue();
        assertThat(board.getSponsorGroups().get(0).getSponsors().get(0).getSponsorName()).isEqualTo("有Logo品牌");
        assertThat(board.getSponsorGroups().get(0).getSponsors().get(0).getLogoUrl()).isEqualTo("/uploads/sponsor/logo.png");
        assertThat(board.getSponsorGroups().get(1).getSponsors())
                .extracting("sponsorName")
                .containsExactly("文字品牌", "缺失Logo品牌");
        assertThat(board.getSponsorGroups().get(1).getSponsors().get(1).getLogoUrl()).isNull();
        assertThat(objectMapper.valueToTree(board).toString()).doesNotContain("禁用品牌");
    }

    @Test
    void rankingRoundUsesRankingPublicProgressLabels() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture,
                List.of(fixture.entryA1(), fixture.entryA2()),
                RoundTargetMode.TOP_N,
                1,
                RoundStatus.PUBLISHED,
                1);
        insertRankingResult(fixture, rankingRound, fixture.entryA1().getId(), 1);

        CompetitionLiveBoardVO board = liveBoardService.getCompetitionLiveBoard(fixture.competition().getId());

        assertThat(board.getRoundType()).isEqualTo(RoundType.RANKING.name());
        assertThat(board.getSummary().getEyebrow()).isEqualTo("排序进度");
        assertThat(board.getTables()).hasSize(1);
        assertThat(board.getTables().get(0).getPersonalProgress()).isEqualTo("1/1");
        assertThat(board.getTables().get(0).getCaptainProgress()).isEqualTo("已提交");
        assertThat(board.getTables().get(0).getStatusText()).isEqualTo("确认中");
        assertThat(findMetric(board, "reviewed").getLabel()).isEqualTo("已评审");
        assertThat(findMetric(board, "pendingReview").getLabel()).isEqualTo("待评审");
    }

    private LiveBoardMetricVO findMetric(CompetitionLiveBoardVO board, String key) {
        return board.getMetrics().stream()
                .filter(metric -> key.equals(metric.getKey()))
                .findFirst()
                .orElseThrow();
    }

    private void insertPersonalScores(BeerCompetitionTestData.Fixture fixture, BeerCompetitionTestData.ScoreRound scoreRound) {
        List<Long> judgeIds = List.of(fixture.captain().getId(), fixture.professional().getId(), fixture.cross().getId());
        for (Long judgeId : judgeIds) {
            for (var entry : scoreRound.entries()) {
                insertScore(fixture, scoreRound, entry.getBeerEntryId(), judgeId, resolveRole(fixture, judgeId), false, 30);
            }
        }
    }

    private void insertFinalScores(BeerCompetitionTestData.Fixture fixture, BeerCompetitionTestData.ScoreRound scoreRound) {
        for (var entry : scoreRound.entries()) {
            insertScore(fixture, scoreRound, entry.getBeerEntryId(), fixture.captain().getId(), JudgeRoleType.CAPTAIN.name(), true, 35);
        }
    }

    private String resolveRole(BeerCompetitionTestData.Fixture fixture, Long judgeId) {
        if (fixture.captain().getId().equals(judgeId)) {
            return JudgeRoleType.CAPTAIN.name();
        }
        if (fixture.professional().getId().equals(judgeId)) {
            return JudgeRoleType.PROFESSIONAL.name();
        }
        return JudgeRoleType.CROSS.name();
    }

    private void insertScore(BeerCompetitionTestData.Fixture fixture, BeerCompetitionTestData.ScoreRound scoreRound,
                             Long beerEntryId, Long judgeId, String role, boolean finalFlag, int score) {
        jdbcTemplate.update("""
                INSERT INTO score_record (
                  competition_id, round_id, round_table_id, beer_entry_id, judge_account_id,
                  judge_role_type, dimensions_json, total_score, comments, is_final,
                  is_advanced, consensus_score, duration_seconds, comment_char_count,
                  create_time, update_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 90, 24, ?, ?)
                """,
                fixture.competition().getId(),
                scoreRound.round().getId(),
                scoreRound.table().getId(),
                beerEntryId,
                judgeId,
                role,
                "[{\"key\":\"overall\",\"label\":\"整体\",\"maxScore\":50,\"score\":" + score + "}]",
                new BigDecimal(score),
                "公开看板测试评语",
                finalFlag ? 1 : 0,
                finalFlag ? new BigDecimal(score) : null,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private void insertConfirmation(Long roundTableId, Long judgeId, Integer resultVersion) {
        jdbcTemplate.update("""
                INSERT INTO round_table_confirmation (
                  round_table_id, judge_account_id, result_version, status, confirmed_time
                ) VALUES (?, ?, ?, 'AGREED', ?)
                """, roundTableId, judgeId, resultVersion, LocalDateTime.now());
    }

    private void insertRankingResult(BeerCompetitionTestData.Fixture fixture,
                                     BeerCompetitionTestData.RankingRound rankingRound,
                                     Long beerEntryId,
                                     int rankNo) {
        jdbcTemplate.update("""
                INSERT INTO round_result (
                  competition_id, round_id, round_table_id, beer_entry_id, result_type,
                  rank_no, slot_label, submitted_by, submitted_time, locked_flag
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                fixture.competition().getId(),
                rankingRound.round().getId(),
                rankingRound.table().getId(),
                beerEntryId,
                RoundResultType.RANK.name(),
                rankNo,
                "排序第" + rankNo,
                fixture.captain().getId(),
                LocalDateTime.now());
    }

    private Long insertSponsorLogoAsset(Long competitionId, String publicUrl) {
        jdbcTemplate.update("""
                INSERT INTO file_asset (
                  business_type, owner_type, owner_id, storage_provider, file_name, storage_path, public_url, create_time
                ) VALUES ('COMPETITION_SPONSOR_LOGO', 'COMPETITION', ?, 'local', 'logo.png', 'uploads/sponsor/logo.png', ?, ?)
                """, competitionId, publicUrl, LocalDateTime.now());
        return jdbcTemplate.queryForObject("""
                SELECT id FROM file_asset
                WHERE business_type = 'COMPETITION_SPONSOR_LOGO'
                  AND owner_type = 'COMPETITION'
                  AND owner_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, competitionId);
    }

    private void insertSponsor(Long competitionId, String tierLabel, String sponsorName, Long logoAssetId,
                               int sortOrder, int featuredFlag, int enabledFlag) {
        jdbcTemplate.update("""
                INSERT INTO competition_sponsor (
                  competition_id, tier_label, sponsor_name, logo_asset_id, sort_order,
                  featured_flag, enabled_flag, create_time, update_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                competitionId,
                tierLabel,
                sponsorName,
                logoAssetId,
                sortOrder,
                featuredFlag,
                enabledFlag,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
