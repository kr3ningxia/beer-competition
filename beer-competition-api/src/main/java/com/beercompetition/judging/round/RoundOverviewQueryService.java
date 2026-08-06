package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.RoundTableAllocationRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.RoundRankingSlotVO;
import com.beercompetition.pojo.vo.RoundTableMemberVO;
import com.beercompetition.pojo.vo.RoundTableJudgeEntryScoreVO;
import com.beercompetition.pojo.vo.RoundTableJudgeProgressVO;
import com.beercompetition.pojo.vo.RoundTableVO;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_CATEGORY;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_EMPTY;
import static com.beercompetition.service.impl.round.RoundConstants.CATEGORY_MODE_MIXED;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_FALSE;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import static com.beercompetition.service.impl.round.RoundConstants.FULL_PROGRESS;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_BRONZE;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_GOLD;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_SILVER;

/**
 * 批量聚合轮次、分桌、评委进度和候选酒款，供后台轮次总览使用。
 */
@Service
@RequiredArgsConstructor
public class RoundOverviewQueryService {

    private final BeerEntryMapper beerEntryMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    public List<CompetitionRoundVO> listCompetitionRounds(Long competitionId) {
        // 1) 查询轮次、桌、酒款与结果快照，避免组装 VO 时反复查库
        roundQuerySupport.requireCompetition(competitionId);
        List<CompetitionRound> rounds = roundQuerySupport.listRounds(competitionId);
        if (rounds.isEmpty()) {
            return List.of();
        }
        List<RoundTable> tables = roundQuerySupport.listRoundTablesByCompetition(competitionId);
        List<RoundTableEntry> tableEntries = roundQuerySupport.listRoundEntriesByCompetition(competitionId);
        List<RoundResult> results = roundQuerySupport.listResultsByCompetition(competitionId);
        Map<Long, List<RoundTable>> tablesByRound = tables.stream().collect(Collectors.groupingBy(RoundTable::getRoundId));
        Map<Long, List<RoundTableEntry>> entriesByTable = tableEntries.stream().collect(Collectors.groupingBy(RoundTableEntry::getRoundTableId));
        Map<Long, List<RoundResult>> resultsByTable = results.stream().collect(Collectors.groupingBy(RoundResult::getRoundTableId));
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(tableEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet()));
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(competitionId);
        List<RoundTableMember> members = roundQuerySupport.listMembers(tables.stream().map(RoundTable::getId).toList());
        Set<Long> judgeIds = new HashSet<>();
        tables.stream().map(RoundTable::getCaptainJudgeId).filter(Objects::nonNull).forEach(judgeIds::add);
        members.stream().map(RoundTableMember::getJudgeAccountId).filter(Objects::nonNull).forEach(judgeIds::add);
        Map<Long, JudgeAccount> judgeById = roundQuerySupport.loadJudges(judgeIds);
        Map<Long, List<RoundTableMember>> membersByTable = members.stream().collect(Collectors.groupingBy(RoundTableMember::getRoundTableId));

        // 2) 按轮次聚合桌、评审、酒款和结果信息，返回后台编排视图
        return rounds.stream()
                .map(round -> toRoundVO(round, tablesByRound.getOrDefault(round.getId(), List.of()), entriesByTable,
                        resultsByTable, entryById, judgeById, membersByTable, categoryNameById))
                .toList();
    }

    private CompetitionRoundVO toRoundVO(CompetitionRound round,
                                         List<RoundTable> tables,
                                         Map<Long, List<RoundTableEntry>> entriesByTable,
                                         Map<Long, List<RoundResult>> resultsByTable,
                                         Map<Long, BeerEntry> entryById,
                                         Map<Long, JudgeAccount> judgeById,
                                         Map<Long, List<RoundTableMember>> membersByTable,
                                         Map<Long, String> categoryNameById) {
        Set<String> sourceEntryUuids = new LinkedHashSet<>();
        List<RoundResult> sourceCandidates = round.getSourceRoundId() == null ? List.of() : roundQuerySupport.resolveCandidateResultsForRound(round, tables);
        if (!sourceCandidates.isEmpty()) {
            sourceCandidates.forEach(result -> {
                BeerEntry entry = entryById.get(result.getBeerEntryId());
                if (entry == null) {
                    entry = beerEntryMapper.selectById(result.getBeerEntryId());
                }
                if (entry != null) {
                    sourceEntryUuids.add(entry.getUuid());
                }
            });
        }
        CompetitionRound sourceRound = round.getSourceRoundId() == null ? null : competitionRoundMapper.selectById(round.getSourceRoundId());
        boolean sourceLocked = sourceRound != null && RoundStatus.LOCKED.name().equals(sourceRound.getStatus());
        Set<Long> sourceCandidateEntryIds = sourceCandidates.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> assignedEntryIds = tables.stream()
                .flatMap(table -> entriesByTable.getOrDefault(table.getId(), List.of()).stream())
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        boolean candidatesSynced = RoundType.SCORE.name().equals(round.getRoundType())
                || (sourceLocked && !sourceCandidateEntryIds.isEmpty() && assignedEntryIds.equals(sourceCandidateEntryIds));
        return CompetitionRoundVO.builder()
                .id(round.getId())
                .roundNo(round.getRoundNo())
                .name(round.getRoundName())
                .type(round.getRoundType())
                .status(round.getStatus())
                .sourceRoundId(round.getSourceRoundId())
                .sourceEntryUuids(new ArrayList<>(sourceEntryUuids))
                .sourceLocked(sourceRound == null ? null : sourceLocked)
                .candidatesSynced(candidatesSynced)
                .preparationDraft(RoundType.RANKING.name().equals(round.getRoundType())
                        && RoundStatus.DRAFT.name().equals(round.getStatus())
                        && (sourceRound == null || !sourceLocked || !candidatesSynced))
                .tables(tables.stream()
                        .sorted(Comparator.comparing(RoundTable::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTable::getId))
                        .map(table -> toRoundTableVO(table, entriesByTable.getOrDefault(table.getId(), List.of()),
                                resultsByTable.getOrDefault(table.getId(), List.of()), entryById, judgeById,
                                membersByTable.getOrDefault(table.getId(), List.of()), categoryNameById, round))
                        .toList())
                .build();
    }

    private JudgeAccount getJudgeById(Map<Long, JudgeAccount> judgeById, Long judgeId) {
        if (judgeId == null) {
            return null;
        }
        return judgeById.get(judgeId);
    }

    private RoundTableVO toRoundTableVO(RoundTable table,
                                        List<RoundTableEntry> entries,
                                        List<RoundResult> results,
                                        Map<Long, BeerEntry> entryById,
                                        Map<Long, JudgeAccount> judgeById,
                                        List<RoundTableMember> members,
                                        Map<Long, String> categoryNameById,
                                        CompetitionRound currentRound) {
        int advancedCount = (int) entries.stream()
                .filter(entry -> RoundEntryStatus.ADVANCED.name().equals(entry.getStatus()) || RoundEntryStatus.RANKED.name().equals(entry.getStatus()))
                .count();
        int finalCount = (int) scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .filter(record -> entries.stream().anyMatch(entry -> entry.getBeerEntryId().equals(record.getBeerEntryId())))
                .count();
        int evaluatedCount = (int) results.stream()
                .filter(result -> RoundResultType.EVALUATED.name().equals(result.getResultType()))
                .count();
        List<ScoreRecord> tableJudgeScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getRoundTableId, table.getId())
                .eq(ScoreRecord::getFinalFlag, FLAG_FALSE));
        return RoundTableVO.builder()
                .id(table.getId())
                .name(table.getTableName())
                .captainPublicId(getJudgeById(judgeById, table.getCaptainJudgeId()) == null ? "" : getJudgeById(judgeById, table.getCaptainJudgeId()).getPublicId())
                .professionalCount(countMembers(members, JudgeRoleType.PROFESSIONAL.name()))
                .crossCount(countMembers(members, JudgeRoleType.CROSS.name()))
                .categoryId(table.getCategoryId())
                .categoryMode(resolveCategoryMode(table))
                .categoryName(resolveCategoryName(table, categoryNameById))
                .targetCount(table.getTargetCount())
                .targetMode(table.getTargetMode())
                .status(table.getStatus())
                .entryUuids(entries.stream()
                        .sorted(Comparator.comparing(RoundTableEntry::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTableEntry::getId))
                        .map(entry -> entryById.get(entry.getBeerEntryId()))
                        .filter(Objects::nonNull)
                        .map(BeerEntry::getUuid)
                        .toList())
                .advancedCount(advancedCount)
                .finalCount(finalCount)
                .evaluatedCount(evaluatedCount)
                .judgeProgress(resolveJudgeProgress(table, entries))
                .captainProgress(entries.isEmpty() ? 0 : finalCount * FULL_PROGRESS / entries.size())
                .averageDurationSeconds(averageInt(tableJudgeScores.stream()
                        .map(ScoreRecord::getDurationSeconds)
                        .filter(Objects::nonNull)
                        .toList()))
                .averageCommentChars(averageInt(tableJudgeScores.stream()
                        .map(ScoreRecord::getCommentCharCount)
                        .filter(Objects::nonNull)
                        .toList()))
                .resultVersion(currentResultVersion(table))
                .confirmationConfirmedCount(RoundType.RANKING.name().equals(currentRound.getRoundType())
                        ? resolveRankingConfirmationConfirmedCount(table)
                        : resolveConfirmationConfirmedCount(table))
                .confirmationRequiredCount(RoundType.RANKING.name().equals(currentRound.getRoundType())
                        ? resolveRankingConfirmationRequiredCount(table)
                        : resolveConfirmationRequiredCount(table))
                .confirmationReady(RoundType.RANKING.name().equals(currentRound.getRoundType()) ? isRankingConfirmationReady(table) : isScoreConfirmationReady(table))
                .confirmationOverrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .confirmationOverrideReason(table.getConfirmationOverrideReason())
                .confirmationOverrideTime(table.getConfirmationOverrideTime())
                .members(buildRoundTableMembers(table, judgeById, members))
                .judgeDetails(buildJudgeDetails(table, entries, entryById, judgeById, members))
                .rankings(buildRankings(table, results, entryById))
                .build();
    }

    private List<RoundTableMemberVO> buildRoundTableMembers(RoundTable table,
                                                            Map<Long, JudgeAccount> judgeById,
                                                            List<RoundTableMember> members) {
        List<RoundTableMember> normalized = new ArrayList<>(members);
        if (table.getCaptainJudgeId() != null && normalized.stream().noneMatch(member -> table.getCaptainJudgeId().equals(member.getJudgeAccountId()))) {
            normalized.add(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(table.getCaptainJudgeId())
                    .role(JudgeRoleType.CAPTAIN.name())
                    .systemTaskRequired(FLAG_TRUE)
                    .build());
        }
        return normalized.stream()
                .sorted(Comparator.comparing(RoundTableMember::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(member -> {
                            JudgeAccount judge = getJudgeById(judgeById, member.getJudgeAccountId());
                            return judge == null ? "" : judge.getName();
                        }, Comparator.nullsLast(String::compareTo)))
                .map(member -> {
                    JudgeAccount judge = getJudgeById(judgeById, member.getJudgeAccountId());
                    return RoundTableMemberVO.builder()
                            .judgePublicId(judge == null ? "" : judge.getPublicId())
                            .name(judge == null ? "未知评审" : judge.getName())
                            .role(member.getRole())
                            .roleLabel(roleLabel(member))
                            .systemTaskRequired(Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                            .build();
                })
                .toList();
    }

    private List<RoundTableJudgeProgressVO> buildJudgeDetails(RoundTable table,
                                                              List<RoundTableEntry> entries,
                                                              Map<Long, BeerEntry> entryById,
                                                              Map<Long, JudgeAccount> judgeById,
                                                              List<RoundTableMember> members) {
        List<RoundTableEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(RoundTableEntry::getSortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(RoundTableEntry::getId))
                .toList();
        if (sortedEntries.isEmpty()) {
            return List.of();
        }
        List<RoundTableMember> taskJudges = members.stream()
                .filter(member -> Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE))
                .sorted(Comparator.comparing(RoundTableMember::getRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(member -> {
                            JudgeAccount judge = getJudgeById(judgeById, member.getJudgeAccountId());
                            return judge == null ? "" : judge.getName();
                        }, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (table.getCaptainJudgeId() != null && taskJudges.stream().noneMatch(member -> table.getCaptainJudgeId().equals(member.getJudgeAccountId()))) {
            taskJudges.add(RoundTableMember.builder()
                    .roundTableId(table.getId())
                    .judgeAccountId(table.getCaptainJudgeId())
                    .role(JudgeRoleType.CAPTAIN.name())
                    .systemTaskRequired(FLAG_TRUE)
                    .build());
        }
        if (taskJudges.isEmpty()) {
            return List.of();
        }
        List<Long> entryIds = sortedEntries.stream().map(RoundTableEntry::getBeerEntryId).toList();
        List<Long> judgeIds = taskJudges.stream().map(RoundTableMember::getJudgeAccountId).toList();
        Map<String, ScoreRecord> scoreByJudgeAndEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .in(ScoreRecord::getJudgeAccountId, judgeIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_FALSE))
                .stream()
                .collect(Collectors.toMap(
                        record -> scoreKey(record.getJudgeAccountId(), record.getBeerEntryId()),
                        Function.identity(),
                        (first, second) -> first.getUpdateTime() != null
                                && (second.getUpdateTime() == null || first.getUpdateTime().isAfter(second.getUpdateTime())) ? first : second));
        return taskJudges.stream().map(member -> {
            JudgeAccount judge = getJudgeById(judgeById, member.getJudgeAccountId());
            List<RoundTableJudgeEntryScoreVO> entryScores = sortedEntries.stream().map(entry -> {
                BeerEntry beerEntry = entryById.get(entry.getBeerEntryId());
                ScoreRecord score = scoreByJudgeAndEntry.get(scoreKey(member.getJudgeAccountId(), entry.getBeerEntryId()));
                return RoundTableJudgeEntryScoreVO.builder()
                        .beerUuid(beerEntry == null ? "" : beerEntry.getUuid())
                        .scored(score != null)
                        .totalScore(score == null ? null : score.getTotalScore())
                        .submittedAt(score == null ? null : score.getUpdateTime())
                        .build();
            }).toList();
            int total = entryScores.size();
            int submitted = (int) entryScores.stream().filter(score -> Boolean.TRUE.equals(score.getScored())).count();
            return RoundTableJudgeProgressVO.builder()
                    .judgePublicId(judge == null ? "" : judge.getPublicId())
                    .judgeName(judge == null ? "未知评审" : judge.getName())
                    .role(member.getRole())
                    .roleLabel(roleLabel(member))
                    .submittedCount(submitted)
                    .totalCount(total)
                    .progress(total == 0 ? 0 : submitted * FULL_PROGRESS / total)
                    .missingEntryUuids(entryScores.stream()
                            .filter(score -> !Boolean.TRUE.equals(score.getScored()))
                            .map(RoundTableJudgeEntryScoreVO::getBeerUuid)
                            .filter(StringUtils::hasText)
                            .toList())
                    .entryScores(entryScores)
                    .build();
        }).toList();
    }

    private String scoreKey(Long judgeAccountId, Long beerEntryId) {
        return judgeAccountId + ":" + beerEntryId;
    }

    private String resolveCategoryMode(RoundTableAllocationRequest table, Map<String, BeerEntry> entryMap) {
        List<Long> categoryIds = safeList(table.getEntryUuids()).stream()
                .map(entryMap::get)
                .filter(Objects::nonNull)
                .map(BeerEntry::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return CATEGORY_MODE_EMPTY;
        }
        if (categoryIds.size() == 1) {
            return CATEGORY_MODE_CATEGORY;
        }
        return CATEGORY_MODE_MIXED;
    }

    private String resolveCategoryMode(RoundTable table) {
        if (table.getCategoryId() != null) {
            return CATEGORY_MODE_CATEGORY;
        }
        if (StringUtils.hasText(table.getCategoryMode()) && CATEGORY_MODE_MIXED.equals(table.getCategoryMode())) {
            return CATEGORY_MODE_MIXED;
        }
        return CATEGORY_MODE_EMPTY;
    }

    private String resolveCategoryName(RoundTable table, Map<Long, String> categoryNameById) {
        if (table.getCategoryId() != null) {
            return categoryNameById.getOrDefault(table.getCategoryId(), "");
        }
        return CATEGORY_MODE_MIXED.equals(resolveCategoryMode(table)) ? "混合" : "";
    }

    private int resolveConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersion(table))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds)));
    }

    private int resolveRankingConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveRankingConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersion(table))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds)));
    }

    private boolean isRankingConfirmationReady(RoundTable table) {
        if (!roundValidationPolicy.isRankingRoundTableReady(table)) {
            return false;
        }
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveRankingConfirmationRequiredCount(table);
        return required <= 0 || resolveRankingConfirmationConfirmedCount(table) >= required;
    }

    private boolean isScoreConfirmationReady(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveConfirmationRequiredCount(table);
        return required <= 0 || resolveConfirmationConfirmedCount(table) >= required;
    }

    private int currentResultVersion(RoundTable table) {
        return table.getResultVersion() == null ? 0 : table.getResultVersion();
    }

    private List<RoundRankingSlotVO> buildRankings(RoundTable table, List<RoundResult> results, Map<Long, BeerEntry> entryById) {
        Map<Integer, RoundResult> resultByRank = results.stream()
                .filter(result -> result.getRankNo() != null)
                .collect(Collectors.toMap(RoundResult::getRankNo, Function.identity(), (left, right) -> right));
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            RoundResult result = resultByRank.get(rank);
            BeerEntry entry = result == null ? null : entryById.get(result.getBeerEntryId());
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(result == null || !StringUtils.hasText(result.getSlotLabel()) ? defaultSlotLabel(table.getTargetMode(), rank) : result.getSlotLabel())
                    .uuid(entry == null ? "" : entry.getUuid())
                    .beerEntryId(result == null ? null : result.getBeerEntryId())
                    .build());
        }
        return slots;
    }

    private String defaultSlotLabel(String targetMode, int rank) {
        if (RoundTargetMode.MEDALS.name().equals(targetMode)) {
            return switch (rank) {
                case 1 -> SLOT_GOLD;
                case 2 -> SLOT_SILVER;
                case 3 -> SLOT_BRONZE;
                default -> "第 " + rank + " 名";
            };
        }
        if (RoundTargetMode.CHAMPION.name().equals(targetMode)) {
            return "总冠军";
        }
        return "第 " + rank + " 名";
    }

    private int resolveJudgeProgress(RoundTable table, List<RoundTableEntry> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        List<Long> entryIds = entries.stream().map(RoundTableEntry::getBeerEntryId).toList();
        int taskJudges = Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)));
        if (table.getCaptainJudgeId() != null) {
            boolean captainCounted = roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                    .eq(RoundTableMember::getRoundTableId, table.getId())
                    .eq(RoundTableMember::getJudgeAccountId, table.getCaptainJudgeId())
                    .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)) > 0;
            if (!captainCounted) {
                taskJudges++;
            }
        }
        if (taskJudges <= 0) {
            return 0;
        }
        int submitted = Math.toIntExact(scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .in(ScoreRecord::getBeerEntryId, entryIds)
                .eq(ScoreRecord::getFinalFlag, FLAG_FALSE)));
        int total = entries.size() * taskJudges;
        return Math.min(FULL_PROGRESS, submitted * FULL_PROGRESS / total);
    }

    private int countMembers(List<RoundTableMember> members, String role) {
        return (int) members.stream().filter(member -> role.equals(member.getRole())).count();
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String roleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return role;
    }

    private String roleLabel(RoundTableMember member) {
        if (!JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_FALSE)) {
            return "参与评审";
        }
        return roleLabel(member.getRole());
    }

    private String roleLabel(CompetitionRound round, RoundTableMember member) {
        if (RoundType.RANKING.name().equals(round.getRoundType())
                && !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return "参与评审";
        }
        return roleLabel(member);
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
