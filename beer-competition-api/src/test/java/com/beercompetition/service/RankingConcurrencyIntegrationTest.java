package com.beercompetition.service;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.judging.scoring.RankingService;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankingConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired private BeerCompetitionTestData testData;
    @Autowired private RankingService rankingService;

    @Test
    void concurrentAutosavesKeepOneNullCategoryDraft() throws Exception {
        var fixture = testData.createFixture(testRun);
        var round = createRound(fixture);
        RankingDraftSaveRequest request = new RankingDraftSaveRequest();
        request.setResults(List.of(result(fixture.entryA1().getId())));

        runConcurrently(java.util.Collections.nCopies(8, fixture.professional().getId()),
                () -> rankingService.saveRankingDraft(round.table().getId(), request));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_judge_ranking_draft
                WHERE round_table_id = ? AND category_id IS NULL AND judge_account_id = ?
                """, Integer.class, round.table().getId(), fixture.professional().getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT rankings_json FROM round_judge_ranking_draft WHERE round_table_id = ?
                """, String.class, round.table().getId())).contains(fixture.entryA1().getId().toString());
    }

    @Test
    void concurrentDuplicateConfirmationsCountJudgeOnce() throws Exception {
        var fixture = testData.createFixture(testRun);
        var round = createRound(fixture);
        var request = submitAndBuildConfirmation(fixture, round);

        runConcurrently(java.util.Collections.nCopies(4, fixture.professional().getId()),
                () -> rankingService.confirmRankingRoundTable(round.table().getId(), request));

        asJudge(fixture.professional().getId());
        var confirmation = rankingService.getRankingConfirmation(round.table().getId());
        assertThat(confirmation.getConfirmedCount()).isEqualTo(1);
        assertThat(confirmation.getMineConfirmed()).isTrue();
        assertThat(confirmation.getStatus()).isEqualTo(RoundStatus.IN_PROGRESS.name());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM round_table_confirmation WHERE round_table_id = ?
                """, Integer.class, round.table().getId())).isEqualTo(1);
    }

    @Test
    void simultaneousPeerConfirmationsSubmitTableAndRound() throws Exception {
        var fixture = testData.createFixture(testRun);
        var round = createRound(fixture);
        var request = submitAndBuildConfirmation(fixture, round);

        runConcurrently(List.of(fixture.professional().getId(), fixture.cross().getId()),
                () -> rankingService.confirmRankingRoundTable(round.table().getId(), request));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM round_table WHERE id = ?",
                String.class, round.table().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, round.round().getId())).isEqualTo(RoundStatus.SUBMITTED.name());
    }

    @Test
    void nullCategoryUniqueKeysRejectDirectDuplicateWrites() {
        var fixture = testData.createFixture(testRun);
        var round = createRound(fixture);
        String insertDraft = """
                INSERT INTO round_judge_ranking_draft
                    (competition_id, round_id, round_table_id, category_id, judge_account_id, rankings_json)
                VALUES (?, ?, ?, NULL, ?, '[]')
                """;
        Object[] draftArgs = {fixture.competition().getId(), round.round().getId(), round.table().getId(),
                fixture.professional().getId()};
        jdbcTemplate.update(insertDraft, draftArgs);
        assertThatThrownBy(() -> jdbcTemplate.update(insertDraft, draftArgs)).isInstanceOf(DuplicateKeyException.class);

        String insertConfirmation = """
                INSERT INTO round_table_confirmation (round_table_id, category_id, judge_account_id, result_version)
                VALUES (?, NULL, ?, 1)
                """;
        jdbcTemplate.update(insertConfirmation, round.table().getId(), fixture.professional().getId());
        assertThatThrownBy(() -> jdbcTemplate.update(insertConfirmation, round.table().getId(),
                fixture.professional().getId())).isInstanceOf(DuplicateKeyException.class);
    }

    private BeerCompetitionTestData.RankingRound createRound(BeerCompetitionTestData.Fixture fixture) {
        return testData.createRankingRound(fixture, List.of(fixture.entryA1(), fixture.entryA2()),
                RoundTargetMode.TOP_N, 1, RoundStatus.IN_PROGRESS, 1);
    }

    private RoundTableConfirmationRequest submitAndBuildConfirmation(BeerCompetitionTestData.Fixture fixture,
                                                                    BeerCompetitionTestData.RankingRound round) {
        asJudge(fixture.captain().getId());
        RankingSubmitRequest submit = new RankingSubmitRequest();
        submit.setResults(List.of(result(fixture.entryA1().getId())));
        rankingService.submitRanking(round.table().getId(), submit);
        RoundTableConfirmationRequest request = new RoundTableConfirmationRequest();
        request.setResultVersion(rankingService.getRankingConfirmation(round.table().getId()).getResultVersion());
        return request;
    }

    private RankingResultItemRequest result(Long entryId) {
        RankingResultItemRequest result = new RankingResultItemRequest();
        result.setBeerEntryId(entryId);
        result.setRankNo(1);
        return result;
    }

    private void runConcurrently(List<Long> judgeIds, Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(judgeIds.size());
        CountDownLatch ready = new CountDownLatch(judgeIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Long judgeId : judgeIds) {
                futures.add(executor.submit(() -> {
                    asJudge(judgeId);
                    try {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("并发测试启动超时");
                        action.run();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    } finally {
                        BaseContext.clear();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
