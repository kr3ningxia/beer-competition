package com.beercompetition.service;

import com.beercompetition.judging.round.RoundLifecycleService;
import com.beercompetition.judging.round.JudgeRoundTaskService;
import com.beercompetition.judging.scoring.RankingService;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.pojo.dto.AwardConfirmItemRequest;
import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
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
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
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
    private JudgeRoundTaskService judgeRoundTaskService;

    @Autowired
    private RoundLifecycleService roundLifecycleService;

    @Autowired
    private AwardService awardService;

    @Autowired
    private RoundQuerySupport roundQuerySupport;

    @Autowired
    private RoundTableMapper roundTableMapper;

    @Autowired
    private CompetitionRoundMapper competitionRoundMapper;

    @Autowired
    private RoundValidationPolicy roundValidationPolicy;

    @Test
    void championCandidatesOnlyTakeGoldMedalFromEachMedalTable() {
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

        // 只有奖牌桌 rank 1 的金奖晋级总冠军轮；银奖、铜奖、非奖牌桌与非法名次均不晋级。
        assertThat(selected).containsExactly(gold);
    }

    @Test
    void championCandidatesSkipCategoryWhoseOnlyMedalIsNotGold() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound medalTable = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2(), fixture.entryB1()),
                RoundTargetMode.MEDALS, 3, RoundStatus.SUBMITTED, 1);
        Long goldCategory = 9001L;
        Long bronzeOnlyCategory = 9002L;

        RoundResult goldWinner = candidateWithCategory(
                medalTable.table().getId(), fixture.entryA1().getId(), 1, "金奖", goldCategory);
        RoundResult bronzeOnly = candidateWithCategory(
                medalTable.table().getId(), fixture.entryB1().getId(), 3, "铜奖", bronzeOnlyCategory);

        List<RoundResult> selected = roundQuerySupport.filterCandidatesForTargetMode(
                List.of(goldWinner, bronzeOnly), RoundTargetMode.CHAMPION.name());

        // 同一奖牌桌内按组别取金奖：只有铜奖、没有金奖的组别不产生冠军轮候选。
        assertThat(selected).containsExactly(goldWinner);
    }

    @Test
    void championAllocationSaveRejectsNonGoldEntry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound medalRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2(), fixture.entryB1()),
                RoundTargetMode.MEDALS, 3, RoundStatus.LOCKED, 1);
        insertStandaloneResult(fixture, medalRound, fixture.entryA1(), 1, "金奖");
        insertStandaloneResult(fixture, medalRound, fixture.entryA2(), 2, "银奖");
        insertStandaloneResult(fixture, medalRound, fixture.entryB1(), 3, "铜奖");

        CompetitionRound championRound = insertRankingRound(fixture, medalRound.round().getId());
        insertRoundTable(fixture, championRound.getId(), RoundTargetMode.CHAMPION, 1, "决赛桌");

        // 保存分桌时只有各组别金奖可进入决赛轮，银奖、铜奖应被拒绝。
        assertThatThrownBy(() -> roundValidationPolicy.validateEntrySource(championRound, List.of(fixture.entryB1())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("晋级酒款");
        assertThatThrownBy(() -> roundValidationPolicy.validateEntrySource(championRound, List.of(fixture.entryA2())))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("晋级酒款");
        roundValidationPolicy.validateEntrySource(championRound, List.of(fixture.entryA1()));
    }

    @Test
    void multiCategoryMedalTableDoesNotAutoSubmitUntilEveryCategoryConfirmed() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound medalRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()),
                RoundTargetMode.MEDALS, 3, RoundStatus.IN_PROGRESS, 1);
        Long tableId = medalRound.table().getId();
        Long firstCategory = fixture.category().getId();
        Long secondCategory = testData.createCategory(fixture.competition().getId(), "第二组别").getId();
        insertCategoryState(tableId, firstCategory, 1);
        insertCategoryState(tableId, secondCategory, 1);
        insertCategoryResult(fixture, medalRound, fixture.entryA1(), firstCategory, 1);
        insertCategoryResult(fixture, medalRound, fixture.entryA2(), secondCategory, 1);

        // 两个非桌长评委各只确认了一个组别，整桌不应视为完成。
        insertConfirmation(tableId, firstCategory, fixture.professional().getId(), 1);
        insertConfirmation(tableId, secondCategory, fixture.cross().getId(), 1);
        rankingService.refreshRankingTableReadiness(tableId);
        assertThat(tableStatus(tableId)).isEqualTo(RoundStatus.IN_PROGRESS.name());

        // 两个组别分别集齐所有评委确认后才自动提交。
        insertConfirmation(tableId, firstCategory, fixture.cross().getId(), 1);
        insertConfirmation(tableId, secondCategory, fixture.professional().getId(), 1);
        rankingService.refreshRankingTableReadiness(tableId);
        assertThat(tableStatus(tableId)).isEqualTo(RoundStatus.SUBMITTED.name());
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
    void ordinaryRankingIgnoresCategoryIdAndDraftSaveIsIdempotent() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        RankingDraftSaveRequest draft = new RankingDraftSaveRequest();
        draft.setCategoryId(fixture.category().getId());
        draft.setResults(List.of(result(fixture.entryA1().getId(), 1)));
        asJudge(fixture.professional().getId());
        rankingService.saveRankingDraft(rankingRound.table().getId(), draft);
        draft.setResults(List.of(result(fixture.entryA2().getId(), 1)));
        rankingService.saveRankingDraft(rankingRound.table().getId(), draft);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_judge_ranking_draft
                WHERE round_table_id = ? AND judge_account_id = ? AND category_id IS NULL
                """, Integer.class, rankingRound.table().getId(), fixture.professional().getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_judge_ranking_draft
                WHERE round_table_id = ? AND judge_account_id = ? AND category_id IS NOT NULL
                """, Integer.class, rankingRound.table().getId(), fixture.professional().getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT rankings_json FROM round_judge_ranking_draft
                WHERE round_table_id = ? AND judge_account_id = ? AND category_id IS NULL
                """, String.class, rankingRound.table().getId(), fixture.professional().getId()))
                .contains(String.valueOf(fixture.entryA2().getId()));

        asJudge(fixture.captain().getId());
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));
        asJudge(fixture.professional().getId());
        RankingConfirmationVO confirmation = rankingService.getRankingConfirmation(rankingRound.table().getId());
        RoundTableConfirmationRequest confirmationRequest = confirmationRequest(confirmation.getResultVersion());
        confirmationRequest.setCategoryId(fixture.category().getId());
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest);
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_table_confirmation
                WHERE round_table_id = ? AND judge_account_id = ? AND category_id IS NULL AND result_version = ?
                """, Integer.class, rankingRound.table().getId(), fixture.professional().getId(), confirmation.getResultVersion())).isEqualTo(1);
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
    void captainCanResubmitRankingAfterAutoSubmitUntilLocked() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound rankingRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);

        asJudge(fixture.captain().getId());
        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA1().getId(), 1)));

        asJudge(fixture.professional().getId());
        RankingConfirmationVO confirmation = rankingService.getRankingConfirmation(rankingRound.table().getId());
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));

        asJudge(fixture.cross().getId());
        confirmation = rankingService.getRankingConfirmation(rankingRound.table().getId());
        rankingService.confirmRankingRoundTable(rankingRound.table().getId(), confirmationRequest(confirmation.getResultVersion()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, rankingRound.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());

        // 主办方锁定前桌长仍能改：可见性字段保持 true，重提会作废本轮确认并把本桌拉回进行中。
        asJudge(fixture.captain().getId());
        assertThat(judgeRoundTaskService.getMyRoundTable(rankingRound.table().getId()).getCanSubmitRanking()).isTrue();

        rankingService.submitRanking(rankingRound.table().getId(), rankingRequest(result(fixture.entryA2().getId(), 1)));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, rankingRound.table().getId())).isEqualTo(RoundStatus.IN_PROGRESS.name());
        assertThat(jdbcTemplate.queryForObject("SELECT result_version FROM round_table WHERE id = ?",
                Integer.class, rankingRound.table().getId())).isEqualTo(3);
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

    private RoundResult candidateWithCategory(Long tableId, Long entryId, int rankNo, String slotLabel, Long categoryId) {
        return RoundResult.builder()
                .roundTableId(tableId)
                .beerEntryId(entryId)
                .rankNo(rankNo)
                .slotLabel(slotLabel)
                .categoryId(categoryId)
                .build();
    }

    private void insertStandaloneResult(BeerCompetitionTestData.Fixture fixture,
                                        BeerCompetitionTestData.RankingRound rankingRound,
                                        BeerEntry entry, int rankNo, String slotLabel) {
        jdbcTemplate.update("""
                INSERT INTO round_result (competition_id, round_id, round_table_id, category_id, beer_entry_id, result_type, rank_no, slot_label)
                VALUES (?, ?, ?, NULL, ?, 'RANK', ?, ?)
                """, fixture.competition().getId(), rankingRound.round().getId(),
                rankingRound.table().getId(), entry.getId(), rankNo, slotLabel);
    }

    private void insertCategoryResult(BeerCompetitionTestData.Fixture fixture,
                                      BeerCompetitionTestData.RankingRound rankingRound,
                                      BeerEntry entry, Long categoryId, int rankNo) {
        jdbcTemplate.update("""
                INSERT INTO round_result (competition_id, round_id, round_table_id, category_id, beer_entry_id, result_type, rank_no, slot_label)
                VALUES (?, ?, ?, ?, ?, 'RANK', ?, '金奖')
                """, fixture.competition().getId(), rankingRound.round().getId(),
                rankingRound.table().getId(), categoryId, entry.getId(), rankNo);
    }

    private void insertCategoryState(Long roundTableId, Long categoryId, int resultVersion) {
        jdbcTemplate.update("""
                INSERT INTO round_table_category_state (round_table_id, category_id, result_version, status)
                VALUES (?, ?, ?, 'IN_PROGRESS')
                """, roundTableId, categoryId, resultVersion);
    }

    private void insertConfirmation(Long roundTableId, Long categoryId, Long judgeId, int resultVersion) {
        jdbcTemplate.update("""
                INSERT INTO round_table_confirmation (round_table_id, category_id, judge_account_id, result_version, status)
                VALUES (?, ?, ?, ?, 'AGREED')
                """, roundTableId, categoryId, judgeId, resultVersion);
    }

    private String tableStatus(Long roundTableId) {
        return jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?", String.class, roundTableId);
    }

    private CompetitionRound insertRankingRound(BeerCompetitionTestData.Fixture fixture, Long sourceRoundId) {
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(fixture.competition().getId())
                .roundNo(3)
                .roundName("决赛轮")
                .roundType(RoundType.RANKING.name())
                .sourceRoundId(sourceRoundId)
                .status(RoundStatus.DRAFT.name())
                .sortOrder(3)
                .build();
        competitionRoundMapper.insert(round);
        return round;
    }

    private RoundTable insertRoundTable(BeerCompetitionTestData.Fixture fixture, Long roundId,
                                        RoundTargetMode targetMode, int targetCount, String name) {
        RoundTable table = RoundTable.builder()
                .competitionId(fixture.competition().getId())
                .roundId(roundId)
                .tableName(name)
                .captainJudgeId(fixture.captain().getId())
                .categoryId(fixture.category().getId())
                .categoryMode("SINGLE")
                .targetCount(targetCount)
                .targetMode(targetMode.name())
                .status(RoundStatus.DRAFT.name())
                .resultVersion(1)
                .confirmationOverrideFlag(0)
                .sortOrder(0)
                .build();
        roundTableMapper.insert(table);
        return table;
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
