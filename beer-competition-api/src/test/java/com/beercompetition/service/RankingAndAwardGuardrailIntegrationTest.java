package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.AwardConfirmItemRequest;
import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.enums.AwardType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankingAndAwardGuardrailIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private RoundService roundService;

    @Autowired
    private AwardService awardService;

    @Test
    void onlyCaptainCanSubmitRankingAndResultsMustBelongToTable() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.professional().getId());
        assertThatThrownBy(() -> roundService.submitRanking(rankingRound.table().getId(),
                rankingRequest(result(fixture.entryA1().getId(), 1))))
                .isInstanceOf(ForbiddenException.class);

        asJudge(fixture.captain().getId());
        assertThatThrownBy(() -> roundService.submitRanking(rankingRound.table().getId(),
                rankingRequest(result(fixture.entryB1().getId(), 1))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("不属于当前桌");
    }

    @Test
    void rankingCannotBeFinalizedBeforePeerConfirmation() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.captain().getId());
        roundService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));

        assertThatThrownBy(() -> roundService.finalizeRanking(rankingRound.table().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("确认未完成");
    }

    @Test
    void rankingAutoSubmitsWhenAllPeersConfirmCurrentVersion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.captain().getId());
        roundService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));

        asJudge(fixture.professional().getId());
        RankingConfirmationVO confirmation = roundService.getRankingConfirmation(rankingRound.table().getId());
        roundService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, rankingRound.table().getId())).isEqualTo(RoundStatus.IN_PROGRESS.name());

        asJudge(fixture.cross().getId());
        confirmation = roundService.getRankingConfirmation(rankingRound.table().getId());
        roundService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, rankingRound.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, rankingRound.round().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
    }

    @Test
    void staleRankingConfirmationVersionIsRejected() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.captain().getId());
        roundService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));
        Integer staleVersion = roundService.getRankingConfirmation(rankingRound.table().getId()).getResultVersion();
        roundService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA2().getId(), 1)));

        asJudge(fixture.professional().getId());
        assertThatThrownBy(() -> roundService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(staleVersion)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已更新");
    }

    @Test
    void awardPublishRequiresConfirmedAwardsAndMarksEntriesPublished() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());

        assertThatThrownBy(() -> awardService.publishAwards(fixture.competition().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("生成并确认");

        asAdmin(1L);
        awardService.confirmAwards(fixture.competition().getId(), confirmRequest(fixture));
        awardService.publishAwards(fixture.competition().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM award_result WHERE competition_id = ? AND status = 'PUBLISHED'",
                Integer.class, fixture.competition().getId())).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM beer_entry WHERE id = ?",
                String.class, fixture.entryA1().getId())).isEqualTo(EntryStatus.RESULT_PUBLISHED.name());
    }

    @Test
    void publishedCompetitionCannotPublishResultsWithoutTerminalLockedRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());

        asAdmin(1L);

        assertThatThrownBy(() -> roundService.publishResults(fixture.competition().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("决赛轮结果未锁定");
    }

    private AwardConfirmRequest confirmRequest(BeerCompetitionTestData.Fixture fixture) {
        AwardConfirmRequest request = new AwardConfirmRequest();
        request.setAwards(List.of(
                award(fixture.category().getId(), fixture.entryA1().getId(), AwardType.MEDAL, "金奖", 1),
                award(fixture.category().getId(), fixture.entryA2().getId(), AwardType.MEDAL, "银奖", 2),
                award(fixture.category().getId(), fixture.entryB1().getId(), AwardType.MEDAL, "铜奖", 3),
                award(null, fixture.entryA1().getId(), AwardType.CHAMPION, "总冠军", 1)
        ));
        return request;
    }

    private AwardConfirmItemRequest award(Long categoryId, Long entryId, AwardType type, String name, int rank) {
        AwardConfirmItemRequest item = new AwardConfirmItemRequest();
        item.setCategoryId(categoryId);
        item.setBeerEntryId(entryId);
        item.setAwardType(type.name());
        item.setAwardName(name);
        item.setRankNo(rank);
        return item;
    }

    private RankingSubmitRequest rankingRequest(RankingResultItemRequest... results) {
        RankingSubmitRequest request = new RankingSubmitRequest();
        request.setResults(List.of(results));
        return request;
    }

    private RoundTableConfirmationRequest confirmationRequest(Integer resultVersion) {
        RoundTableConfirmationRequest request = new RoundTableConfirmationRequest();
        request.setResultVersion(resultVersion);
        return request;
    }

    private RankingResultItemRequest result(Long entryId, int rankNo) {
        RankingResultItemRequest item = new RankingResultItemRequest();
        item.setBeerEntryId(entryId);
        item.setRankNo(rankNo);
        return item;
    }
}
