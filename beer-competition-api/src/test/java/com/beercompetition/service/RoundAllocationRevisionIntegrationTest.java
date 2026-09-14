package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.judging.assignment.RoundAllocationService;
import com.beercompetition.judging.assignment.RoundCandidateSyncService;
import com.beercompetition.judging.round.RoundQueryService;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableAllocationRequest;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundAllocationRevisionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private RoundAllocationService roundAllocationService;

    @Autowired
    private RoundCandidateSyncService roundCandidateSyncService;

    @Autowired
    private RoundQueryService roundQueryService;

    @Test
    void consecutiveAllocationSavesIncrementRevision() {
        DraftFixture draft = createDraftRound();

        roundAllocationService.saveRoundAllocation(draft.competitionId(), draft.roundId(),
                allocationRequest(draft, 0L, List.of()));
        assertThat(allocationRevision(draft.roundId())).isEqualTo(1L);

        roundAllocationService.saveRoundAllocation(draft.competitionId(), draft.roundId(),
                allocationRequest(draft, 1L, List.of()));

        assertThat(allocationRevision(draft.roundId())).isEqualTo(2L);
        CompetitionRoundVO round = roundQueryService.listCompetitionRounds(draft.competitionId()).stream()
                .filter(item -> item.getId().equals(draft.roundId()))
                .findFirst()
                .orElseThrow();
        assertThat(round.getAllocationRevision()).isEqualTo(2L);
    }

    @Test
    void staleAllocationRevisionIsRejectedWithoutChangingSavedDraft() {
        DraftFixture draft = createDraftRound();
        RoundAllocationRequest staleRequest = allocationRequest(draft, 0L, List.of());
        roundAllocationService.saveRoundAllocation(draft.competitionId(), draft.roundId(), staleRequest);

        assertThatThrownBy(() -> roundAllocationService.saveRoundAllocation(
                draft.competitionId(), draft.roundId(), staleRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("轮次草稿已被更新");

        assertThat(allocationRevision(draft.roundId())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM round_table WHERE round_id = ?", Integer.class, draft.roundId()))
                .isEqualTo(1);
    }

    @Test
    void candidateSyncInvalidatesOldDraftAndCurrentSaveCannotOmitCandidate() {
        DraftFixture draft = createDraftRound();
        RoundAllocationRequest staleRequest = allocationRequest(draft, 0L, List.of());
        jdbcTemplate.update("UPDATE round_table SET status = 'SUBMITTED' WHERE id = ?", draft.sourceTableId());
        jdbcTemplate.update("""
                INSERT INTO round_result
                    (competition_id, round_id, round_table_id, beer_entry_id, result_type, slot_label, locked_flag)
                VALUES (?, ?, ?, ?, 'ADVANCE', '晋级', 0)
                """, draft.competitionId(), draft.sourceRoundId(), draft.sourceTableId(), draft.candidateEntryId());

        roundCandidateSyncService.syncDependentDrafts(draft.sourceRound());

        assertThat(allocationRevision(draft.roundId())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM round_table_entry WHERE round_id = ?", Integer.class, draft.roundId()))
                .isEqualTo(1);
        assertThatThrownBy(() -> roundAllocationService.saveRoundAllocation(
                draft.competitionId(), draft.roundId(), staleRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("轮次草稿已被更新");

        RoundAllocationRequest missingCandidateRequest = allocationRequest(draft, 1L, List.of());
        assertThatThrownBy(() -> roundAllocationService.saveRoundAllocation(
                draft.competitionId(), draft.roundId(), missingCandidateRequest))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("晋级候选已更新");

        roundAllocationService.saveRoundAllocation(draft.competitionId(), draft.roundId(),
                allocationRequest(draft, 1L, List.of(draft.candidateEntryUuid())));
        assertThat(allocationRevision(draft.roundId())).isEqualTo(2L);
    }

    @Test
    void medalRoundCanOnlyCreateChampionRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.RankingRound medalRound = testData.createRankingRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2(), fixture.entryB1()),
                RoundTargetMode.MEDALS, 3, RoundStatus.LOCKED, 1);
        jdbcTemplate.update("UPDATE competition SET status = 'JUDGING' WHERE id = ?", fixture.competition().getId());
        NextRoundCreateRequest request = nextRoundRequest(medalRound.round().getId());
        request.setRoundName("决赛轮");
        request.setTargetMode(RoundTargetMode.MEDALS.name());
        request.setTargetCount(3);

        asAdmin(1L);
        assertThatThrownBy(() -> roundAllocationService.createNextRound(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("只能创建决赛轮");
    }

    private DraftFixture createDraftRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        BeerCompetitionTestData.ScoreRound source = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1()), 1);
        jdbcTemplate.update("UPDATE competition SET status = 'JUDGING' WHERE id = ?", fixture.competition().getId());
        asAdmin(1L);
        roundAllocationService.createNextRound(fixture.competition().getId(), nextRoundRequest(source.round().getId()));
        Long roundId = jdbcTemplate.queryForObject(
                "SELECT id FROM competition_round WHERE source_round_id = ?", Long.class, source.round().getId());
        Long tableId = jdbcTemplate.queryForObject(
                "SELECT id FROM round_table WHERE round_id = ?", Long.class, roundId);
        return new DraftFixture(fixture.competition().getId(), source.round().getId(), source.table().getId(),
                source.round(), roundId, tableId, fixture.entryA1().getId(), fixture.entryA1().getUuid());
    }

    private NextRoundCreateRequest nextRoundRequest(Long sourceRoundId) {
        NextRoundCreateRequest request = new NextRoundCreateRequest();
        request.setSourceRoundId(sourceRoundId);
        request.setRoundName("第二轮");
        request.setStrategy("MANUAL");
        request.setTableCount(1);
        request.setTargetMode("TOP_N");
        request.setTargetCount(1);
        request.setCaptainPublicIds(List.of());
        return request;
    }

    private RoundAllocationRequest allocationRequest(DraftFixture draft, Long revision, List<String> entryUuids) {
        RoundTableAllocationRequest table = new RoundTableAllocationRequest();
        table.setId(draft.tableId());
        table.setName("第二轮一号桌");
        table.setCaptainPublicId("");
        table.setTargetMode("TOP_N");
        table.setTargetCount(1);
        table.setSortOrder(0);
        table.setParticipantPublicIds(List.of());
        table.setMembers(List.of());
        table.setEntryUuids(entryUuids);
        RoundAllocationRequest request = new RoundAllocationRequest();
        request.setAllocationRevision(revision);
        request.setTables(List.of(table));
        return request;
    }

    private Long allocationRevision(Long roundId) {
        return jdbcTemplate.queryForObject(
                "SELECT allocation_revision FROM competition_round WHERE id = ?", Long.class, roundId);
    }

    private record DraftFixture(Long competitionId,
                                Long sourceRoundId,
                                Long sourceTableId,
                                CompetitionRound sourceRound,
                                Long roundId,
                                Long tableId,
                                Long candidateEntryId,
                                String candidateEntryUuid) {
    }
}
