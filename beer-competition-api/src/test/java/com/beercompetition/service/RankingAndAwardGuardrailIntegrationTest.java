package com.beercompetition.service;

import com.beercompetition.judging.round.RoundLifecycleService;
import com.beercompetition.judging.scoring.RankingService;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.RoundTableMapper;
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
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.service.impl.round.RoundQuerySupport;
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
    private RankingService rankingService;

    @Autowired
    private RoundLifecycleService roundLifecycleService;

    @Autowired
    private AwardService awardService;

    @Autowired
    private RoundQuerySupport roundQuerySupport;

    @Autowired
    private RoundTableMapper roundTableMapper;

    @Test
    void championCandidatesUseHighestAvailableMedalFromEachTable() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound goldTable = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2(), fixture.entryB1()),
                RoundTargetMode.MEDALS, 3, RoundStatus.SUBMITTED, 1);
        RoundTable silverTable = insertTable(fixture, goldTable.round().getId(), RoundTargetMode.MEDALS, 2);
        RoundTable bronzeTable = insertTable(fixture, goldTable.round().getId(), RoundTargetMode.MEDALS, 3);
        RoundTable topNTable = insertTable(fixture, goldTable.round().getId(), RoundTargetMode.TOP_N, 4);

        RoundResult gold = candidate(goldTable.table().getId(), fixture.entryA1().getId(), 1, "自定义奖项");
        RoundResult goldTableSilver = candidate(goldTable.table().getId(), fixture.entryA2().getId(), 2, "金奖");
        RoundResult silver = candidate(silverTable.getId(), fixture.entryA2().getId(), 2, "自定义奖项");
        RoundResult silverTableBronze = candidate(silverTable.getId(), fixture.entryB1().getId(), 3, "铜奖");
        RoundResult bronze = candidate(bronzeTable.getId(), fixture.entryB1().getId(), 3, "自定义奖项");
        RoundResult invalid = candidate(bronzeTable.getId(), fixture.entryA1().getId(), 0, "金奖");
        RoundResult nonMedal = candidate(topNTable.getId(), fixture.entryA1().getId(), 1, "金奖");

        List<RoundResult> selected = roundQuerySupport.filterCandidatesForTargetMode(List.of(
                goldTableSilver, silverTableBronze, bronze, invalid, nonMedal, silver, gold),
                RoundTargetMode.CHAMPION.name());

        assertThat(selected).containsExactlyInAnyOrder(gold, silver, bronze);
    }

    @Test
    void championCanUseHighestAvailableMedalWhenCategoryHasNoGold() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());
        AwardConfirmRequest request = new AwardConfirmRequest();
        request.setAwards(List.of(
                award(fixture.category().getId(), fixture.entryA2().getId(), AwardType.MEDAL, "候选奖项", 2),
                award(fixture.category().getId(), fixture.entryB1().getId(), AwardType.MEDAL, "金奖", 3),
                award(null, fixture.entryA2().getId(), AwardType.CHAMPION, "总冠军", 1)
        ));

        asAdmin(1L);
        awardService.confirmAwards(fixture.competition().getId(), request);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM award_result
                WHERE competition_id = ? AND award_type = 'CHAMPION' AND beer_entry_id = ?
                """, Integer.class, fixture.competition().getId(), fixture.entryA2().getId())).isEqualTo(1);
    }

    @Test
    void championCannotUseLowerMedalWhenHigherRankExists() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());
        AwardConfirmRequest request = new AwardConfirmRequest();
        request.setAwards(List.of(
                award(fixture.category().getId(), fixture.entryA2().getId(), AwardType.MEDAL, "候选奖项", 2),
                award(fixture.category().getId(), fixture.entryB1().getId(), AwardType.MEDAL, "金奖", 3),
                award(null, fixture.entryB1().getId(), AwardType.CHAMPION, "总冠军", 1)
        ));

        asAdmin(1L);
        assertThatThrownBy(() -> awardService.confirmAwards(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("各组最高奖项");
    }

    @Test
    void onlyCaptainCanSubmitRankingAndResultsMustBelongToTable() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.professional().getId());
        assertThatThrownBy(() -> rankingService.submitRanking(rankingRound.table().getId(),
                rankingRequest(result(fixture.entryA1().getId(), 1))))
                .isInstanceOf(ForbiddenException.class);

        asJudge(fixture.captain().getId());
        assertThatThrownBy(() -> rankingService.submitRanking(rankingRound.table().getId(),
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
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));

        assertThatThrownBy(() -> rankingService.finalizeRanking(rankingRound.table().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("确认未完成");
    }

    @Test
    void rankingAutoSubmitsWhenAllPeersConfirmCurrentVersion() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.captain().getId());
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));

        asJudge(fixture.professional().getId());
        RankingConfirmationVO confirmation = rankingService.getRankingConfirmation(rankingRound.table().getId());
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, rankingRound.table().getId())).isEqualTo(RoundStatus.IN_PROGRESS.name());

        asJudge(fixture.cross().getId());
        confirmation = rankingService.getRankingConfirmation(rankingRound.table().getId());
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));

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
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));
        Integer staleVersion = rankingService.getRankingConfirmation(rankingRound.table().getId()).getResultVersion();
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA2().getId(), 1)));

        asJudge(fixture.professional().getId());
        assertThatThrownBy(() -> rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(staleVersion)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("已更新");
    }

    @Test
    void awardPublishRequiresConfirmedAwardsAndMarksEntriesPublished() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.RESULT_CONFIRMING.name(), fixture.competition().getId());

        asAdmin(1L);
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

        assertThatThrownBy(() -> roundLifecycleService.publishResults(fixture.competition().getId()))
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

    private RoundResult candidate(Long tableId, Long entryId, int rankNo, String slotLabel) {
        return RoundResult.builder()
                .roundTableId(tableId)
                .beerEntryId(entryId)
                .rankNo(rankNo)
                .slotLabel(slotLabel)
                .build();
    }

    private RoundTable insertTable(BeerCompetitionTestData.Fixture fixture, Long roundId,
                                   RoundTargetMode targetMode, int sortOrder) {
        RoundTable table = RoundTable.builder()
                .competitionId(fixture.competition().getId())
                .roundId(roundId)
                .tableName("排序" + sortOrder + "号桌")
                .captainJudgeId(fixture.captain().getId())
                .categoryId(fixture.category().getId())
                .categoryMode("SINGLE")
                .targetCount(3)
                .targetMode(targetMode.name())
                .status(RoundStatus.SUBMITTED.name())
                .resultVersion(1)
                .confirmationOverrideFlag(0)
                .sortOrder(sortOrder)
                .build();
        roundTableMapper.insert(table);
        return table;
    }
}
