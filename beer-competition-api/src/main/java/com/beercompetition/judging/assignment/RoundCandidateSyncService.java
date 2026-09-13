package com.beercompetition.judging.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoundCandidateSyncService {

    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundQuerySupport roundQuerySupport;

    public boolean isSourceReady(CompetitionRound sourceRound) {
        return sourceRound != null && (RoundStatus.SUBMITTED.name().equals(sourceRound.getStatus())
                || RoundStatus.LOCKED.name().equals(sourceRound.getStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncDependentDrafts(CompetitionRound sourceRound) {
        if (sourceRound == null) {
            return;
        }
        competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                        .eq(CompetitionRound::getCompetitionId, sourceRound.getCompetitionId())
                        .eq(CompetitionRound::getSourceRoundId, sourceRound.getId())
                        .eq(CompetitionRound::getStatus, RoundStatus.DRAFT.name())
                        .orderByAsc(CompetitionRound::getId)
                        .last("FOR UPDATE"))
                .forEach(this::syncDraftCandidatesAndRevision);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncDraftRound(Long competitionId, Long roundId) {
        CompetitionRound round = roundQuerySupport.requireRoundForUpdate(competitionId, roundId);
        if (!RoundType.RANKING.name().equals(round.getRoundType())) {
            throw new BaseException("只有后续排序轮需要更新候选酒款");
        }
        if (!RoundStatus.DRAFT.name().equals(round.getStatus())) {
            throw new BaseException("只有草稿轮次可以更新候选酒款");
        }
        roundQuerySupport.requireRound(competitionId, round.getSourceRoundId());
        syncDraftCandidatesAndRevision(round);
    }

    private void syncDraftCandidatesAndRevision(CompetitionRound round) {
        if (!syncDraftCandidates(round)) {
            return;
        }
        round.setAllocationRevision((round.getAllocationRevision() == null ? 0L : round.getAllocationRevision()) + 1);
        competitionRoundMapper.updateById(round);
    }

    private boolean syncDraftCandidates(CompetitionRound round) {
        List<RoundTable> tables = roundQuerySupport.listRoundTables(round.getId()).stream()
                .sorted(Comparator.comparing(RoundTable::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RoundTable::getId))
                .toList();
        if (tables.isEmpty()) {
            throw new BaseException("当前轮次至少需要 1 张桌");
        }
        List<RoundResult> candidates = roundQuerySupport.resolveCandidateResultsForRound(round, tables);
        Map<Long, RoundResult> candidateByEntryId = candidates.stream().collect(Collectors.toMap(
                RoundResult::getBeerEntryId, Function.identity(), (left, right) -> right, LinkedHashMap::new));
        Set<Long> candidateEntryIds = candidateByEntryId.keySet();
        List<RoundTableEntry> existingEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, round.getId()));
        List<Long> staleIds = existingEntries.stream()
                .filter(entry -> !candidateEntryIds.contains(entry.getBeerEntryId()))
                .map(RoundTableEntry::getId)
                .toList();
        if (!staleIds.isEmpty()) {
            roundTableEntryMapper.deleteBatchIds(staleIds);
        }
        boolean changed = !staleIds.isEmpty();

        List<RoundTableEntry> retainedEntries = existingEntries.stream()
                .filter(entry -> candidateEntryIds.contains(entry.getBeerEntryId()))
                .toList();
        Set<Long> assignedEntryIds = retainedEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, Integer> entryCountByTable = tables.stream().collect(Collectors.toMap(
                RoundTable::getId,
                table -> (int) retainedEntries.stream().filter(entry -> table.getId().equals(entry.getRoundTableId())).count()));
        Map<Long, Integer> nextSortOrderByTable = tables.stream().collect(Collectors.toMap(
                RoundTable::getId,
                table -> retainedEntries.stream()
                        .filter(entry -> table.getId().equals(entry.getRoundTableId()))
                        .map(RoundTableEntry::getSortOrder)
                        .filter(value -> value != null)
                        .max(Integer::compareTo)
                        .orElse(-1) + 1));

        for (RoundResult candidate : candidateByEntryId.values()) {
            if (assignedEntryIds.contains(candidate.getBeerEntryId())) {
                continue;
            }
            RoundTable targetTable = tables.stream()
                    .min(Comparator.comparingInt(table -> entryCountByTable.getOrDefault(table.getId(), 0)))
                    .orElseThrow();
            roundTableEntryMapper.insert(RoundTableEntry.builder()
                    .competitionId(round.getCompetitionId())
                    .roundId(round.getId())
                    .roundTableId(targetTable.getId())
                    .beerEntryId(candidate.getBeerEntryId())
                    .sourceRoundTableId(candidate.getRoundTableId())
                    .status(RoundEntryStatus.ASSIGNED.name())
                    .sortOrder(nextSortOrderByTable.compute(targetTable.getId(), (key, value) -> value + 1) - 1)
                    .build());
            entryCountByTable.compute(targetTable.getId(), (key, value) -> value + 1);
            changed = true;
        }
        return changed;
    }
}
