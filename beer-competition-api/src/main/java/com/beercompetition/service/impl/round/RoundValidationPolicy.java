package com.beercompetition.service.impl.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableAllocationRequest;
import com.beercompetition.pojo.dto.RoundTableMemberAllocationRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 轮次评审的业务规则校验器。
 *
 * <p>该类只做“能不能操作”的判断，不负责写入数据。主流程服务调用这里，可以把状态与分配规则从流程代码中剥离出来。</p>
 */
@Component
@RequiredArgsConstructor
public class RoundValidationPolicy {

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundQuerySupport roundQuerySupport;

    /**
     * 校验当前比赛阶段是否允许保存轮次编排。
     */
    public void validateCompetitionStageForRoundAllocation(Competition competition, CompetitionRound round) {
        CompetitionStatus status = parseCompetitionStatus(competition);
        if (RoundType.SCORE.name().equals(round.getRoundType()) && !isPreJudgingStage(status)) {
            throw new BaseException("评审已开始，不能再保存第一轮编排");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingRoundStage(status)) {
            throw new BaseException("当前阶段不能保存排序轮编排");
        }
    }

    /**
     * 校验当前比赛阶段是否允许发布轮次。
     */
    public void validateCompetitionStageForRoundPublish(Competition competition, CompetitionRound round) {
        CompetitionStatus status = parseCompetitionStatus(competition);
        if (RoundType.SCORE.name().equals(round.getRoundType()) && status != CompetitionStatus.JUDGING_PREP) {
            throw new BaseException("第一轮只能在评审准备阶段发布");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingRoundStage(status)) {
            throw new BaseException("当前阶段不能发布排序轮");
        }
    }

    public boolean isRankingRoundStage(CompetitionStatus status) {
        return status == CompetitionStatus.JUDGING
                || status == CompetitionStatus.RESULT_CONFIRMING;
    }

    /**
     * 校验轮次桌、评审和酒款分配的基础结构。
     */
    public void validateAllocationRequest(CompetitionRound round, RoundAllocationRequest request, boolean requireComplete) {
        if (request.getTables() == null || request.getTables().isEmpty()) {
            throw new BaseException("至少需要 1 张轮次桌");
        }
        Set<String> tableNames = new HashSet<>();
        Set<String> entryUuids = new HashSet<>();
        Set<String> roundJudgePublicIds = new HashSet<>();
        for (RoundTableAllocationRequest table : request.getTables()) {
            if (!tableNames.add(table.getName().trim())) {
                throw new BaseException("轮次桌名称不能重复：" + table.getName());
            }
            if (requireComplete && RoundType.SCORE.name().equals(round.getRoundType()) && !StringUtils.hasText(table.getCaptainPublicId())) {
                throw new BaseException(table.getName() + "缺少桌长");
            }
            if (StringUtils.hasText(table.getCaptainPublicId()) && !roundJudgePublicIds.add(table.getCaptainPublicId())) {
                throw new BaseException("同一轮同一评审只能分配到一张桌：" + table.getCaptainPublicId());
            }
            Set<String> tableParticipantIds = new HashSet<>();
            List<String> participantPublicIds = RoundType.SCORE.name().equals(round.getRoundType())
                    ? safeList(table.getMembers()).stream()
                    .map(RoundTableMemberAllocationRequest::getJudgePublicId)
                    .toList()
                    : safeList(table.getParticipantPublicIds());
            for (String participantPublicId : participantPublicIds) {
                if (!StringUtils.hasText(participantPublicId)) {
                    continue;
                }
                if (participantPublicId.equals(table.getCaptainPublicId())) {
                    throw new BaseException(table.getName() + "桌长不能同时作为参与评审");
                }
                if (!tableParticipantIds.add(participantPublicId)) {
                    throw new BaseException(table.getName() + "参与评审不能重复");
                }
                if (!roundJudgePublicIds.add(participantPublicId)) {
                    throw new BaseException("同一轮同一评审只能分配到一张桌：" + participantPublicId);
                }
            }
            if (table.getTargetCount() == null || table.getTargetCount() <= 0) {
                throw new BaseException(table.getName() + "目标数量必须大于 0");
            }
            String targetMode = resolveTargetMode(round, table.getTargetMode());
            validateTargetCountForMode(table.getName(), targetMode, table.getTargetCount());
            List<String> uuids = safeList(table.getEntryUuids()).stream()
                    .filter(StringUtils::hasText)
                    .toList();
            if (requireComplete && !RoundTargetMode.MEDALS.name().equals(targetMode) && !uuids.isEmpty() && table.getTargetCount() > uuids.size()) {
                throw new BaseException(table.getName() + "目标数量超过酒款数");
            }
            for (String uuid : uuids) {
                if (!entryUuids.add(uuid)) {
                    throw new BaseException("同一轮同一酒款只能分配到一张桌：" + uuid);
                }
            }
        }
        if (requireComplete && RoundType.SCORE.name().equals(round.getRoundType()) && entryUuids.isEmpty()) {
            throw new BaseException("第一轮必须分配已入库酒款");
        }
    }

    public void validateAllocationReferences(CompetitionRound round,
                                             RoundAllocationRequest request,
                                             Map<String, JudgeAccount> captainMap,
                                             Map<String, JudgeAccount> scoreMemberMap,
                                             Map<String, JudgeAccount> participantMap,
                                             Map<String, BeerEntry> entryMap) {
        for (RoundTableAllocationRequest table : request.getTables()) {
            if (StringUtils.hasText(table.getCaptainPublicId()) && !captainMap.containsKey(table.getCaptainPublicId())) {
                throw new BaseException(table.getName() + "桌长不存在或已不可用");
            }
            if (RoundType.SCORE.name().equals(round.getRoundType())) {
                for (RoundTableMemberAllocationRequest member : safeList(table.getMembers())) {
                    if (member != null && StringUtils.hasText(member.getJudgePublicId()) && !scoreMemberMap.containsKey(member.getJudgePublicId())) {
                        throw new BaseException(table.getName() + "评审成员不存在或已不可用");
                    }
                }
            } else {
                for (String participantPublicId : safeList(table.getParticipantPublicIds())) {
                    if (StringUtils.hasText(participantPublicId) && !participantMap.containsKey(participantPublicId)) {
                        throw new BaseException(table.getName() + "参与评审不存在或已不可用");
                    }
                }
            }
            for (String uuid : safeList(table.getEntryUuids())) {
                if (StringUtils.hasText(uuid) && !entryMap.containsKey(uuid)) {
                    throw new BaseException(table.getName() + "酒款不存在或已不可用：" + uuid);
                }
            }
        }
    }

    public void validateEntrySource(CompetitionRound round, List<BeerEntry> entries) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            entries.stream()
                    .filter(entry -> !Objects.equals(entry.getStoredFlag(), RoundConstants.FLAG_TRUE)
                            || EntryStatus.CANCELED.name().equals(entry.getStatus()))
                    .findFirst()
                    .ifPresent(entry -> {
                        throw new BaseException("第一轮只能分配已入库且未取消的酒款：" + entry.getUuid());
                    });
            return;
        }
        Set<Long> allowedEntryIds = roundQuerySupport.listCandidateResults(round.getSourceRoundId()).stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        entries.stream()
                .filter(entry -> !allowedEntryIds.contains(entry.getId()))
                .findFirst()
                .ifPresent(entry -> {
                    throw new BaseException("后续轮只能分配上一轮晋级酒款：" + entry.getUuid());
                });
    }

    /**
     * 发布轮次前的完整性校验，覆盖评分表、候选池、桌长和目标数量。
     */
    public void validateRoundReady(CompetitionRound round) {
        List<RoundTable> tables = roundQuerySupport.listRoundTables(round.getId());
        if (tables.isEmpty()) {
            throw new BaseException("当前轮次至少需要 1 张桌");
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            validateScoreFormsReady(round.getCompetitionId());
        } else {
            CompetitionRound sourceRound = roundQuerySupport.requireRound(round.getCompetitionId(), round.getSourceRoundId());
            if (!RoundStatus.LOCKED.name().equals(sourceRound.getStatus())) {
                throw new BaseException("请先锁定上一轮，再发布后续轮");
            }
            validateSourceIsLatestLockedRound(round.getCompetitionId(), sourceRound);
            validateCandidatePoolSynced(round, tables);
        }
        Set<Long> captainJudgeIds = new HashSet<>();
        for (RoundTable table : tables) {
            if (table.getCaptainJudgeId() == null) {
                throw new BaseException(table.getTableName() + "缺少桌长");
            }
            if (!captainJudgeIds.add(table.getCaptainJudgeId())) {
                throw new BaseException("同一轮同一桌长只能负责一张桌");
            }
            List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                    .eq(RoundTableEntry::getRoundTableId, table.getId()));
            if (entries.isEmpty()) {
                throw new BaseException(table.getTableName() + "尚未分配酒款");
            }
            boolean allowEmptyMedalSlots = RoundTargetMode.MEDALS.name().equals(table.getTargetMode());
            if (table.getTargetCount() == null || table.getTargetCount() <= 0
                    || (!allowEmptyMedalSlots && table.getTargetCount() > entries.size())) {
                throw new BaseException(table.getTableName() + "目标数量不合法");
            }
            validateTargetCountForMode(table.getTableName(), table.getTargetMode(), table.getTargetCount());
            if (RoundTargetMode.MEDALS.name().equals(table.getTargetMode())
                    && (table.getCategoryId() == null || !RoundConstants.CATEGORY_MODE_CATEGORY.equals(table.getCategoryMode()))) {
                throw new BaseException(table.getTableName() + "奖牌轮必须只包含一个投递组别");
            }
        }
    }

    public void validateTargetCountForMode(String tableName, String targetMode, Integer targetCount) {
        if (RoundTargetMode.MEDALS.name().equals(targetMode) && !Objects.equals(targetCount, 3)) {
            throw new BaseException(tableName + "奖牌排序目标必须为 3");
        }
        if (RoundTargetMode.CHAMPION.name().equals(targetMode) && !Objects.equals(targetCount, 1)) {
            throw new BaseException(tableName + "总冠军排序目标必须为 1");
        }
    }

    public void validateRankingSubmit(RoundTable table, RankingSubmitRequest request) {
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        boolean allowEmptyMedalSlots = RoundTargetMode.MEDALS.name().equals(table.getTargetMode());
        // 奖牌排序允许个别奖项空缺；普通 TopN 和总冠军排序必须正好填满目标数量。
        if (allowEmptyMedalSlots) {
            if (request.getResults().size() > targetCount) {
                throw new BaseException("奖项结果数量不能超过奖项槽位");
            }
        } else if (request.getResults().size() != targetCount) {
            throw new BaseException("排序结果数量必须等于目标数量");
        }
        Set<Long> tableEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        validateRankingItems(request.getResults(), tableEntryIds, targetCount, "排序");
    }

    public void validateRankingDraft(RoundTable table, List<RankingResultItemRequest> results) {
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        if (results.size() > targetCount) {
            throw new BaseException("参考排序数量不能超过槽位数量");
        }
        Set<Long> tableEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        validateRankingItems(results, tableEntryIds, targetCount, "参考排序");
    }

    public boolean isRankingRoundTableReady(RoundTable table) {
        int resultCount = Math.toIntExact(roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, table.getId())));
        // 奖牌桌允许空奖项，只要已有有效排序结果即可进入确认；其他模式必须达到目标数量。
        if (RoundTargetMode.MEDALS.name().equals(table.getTargetMode())) {
            return resultCount > 0;
        }
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        return targetCount > 0 && resultCount == targetCount;
    }

    public void validateRankingRoundTableReady(RoundTable table) {
        if (!isRankingRoundTableReady(table)) {
            throw new BaseException("请先提交本桌排序结果");
        }
    }

    public void validateSourceIsLatestLockedRound(Long competitionId, CompetitionRound sourceRound) {
        CompetitionRound latestLocked = roundQuerySupport.findLastLockedRound(competitionId);
        if (latestLocked == null || !latestLocked.getId().equals(sourceRound.getId())) {
            throw new BaseException("只能基于最新锁定轮次创建下一轮");
        }
    }

    public String resolveTargetMode(CompetitionRound round, String requested) {
        if (StringUtils.hasText(requested)) {
            return RoundTargetMode.of(requested).name();
        }
        return RoundType.SCORE.name().equals(round.getRoundType())
                ? RoundTargetMode.ADVANCE_COUNT.name()
                : RoundTargetMode.TOP_N.name();
    }

    public CompetitionStatus parseCompetitionStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private void validateCandidatePoolSynced(CompetitionRound round, List<RoundTable> tables) {
        List<RoundResult> candidates = roundQuerySupport.resolveCandidateResultsForRound(round, tables);
        if (candidates.isEmpty()) {
            throw new BaseException("上一轮没有可发布的候选酒款");
        }
        // 发布后续轮前要求分桌酒款与上一轮候选池完全一致，避免旧草稿漏掉新增晋级结果。
        Set<Long> candidateEntryIds = candidates.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> assignedEntryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundId, round.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        if (!assignedEntryIds.equals(candidateEntryIds)) {
            throw new BaseException("晋级酒款已变化，请重新载入后核对分桌");
        }
    }

    private void validateScoreFormsReady(Long competitionId) {
        Set<String> roles = competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId))
                .stream()
                .map(CompetitionScoreConfig::getJudgeRoleType)
                .collect(Collectors.toSet());
        if (!roles.containsAll(Set.of(JudgeRoleType.CAPTAIN.name(), JudgeRoleType.PROFESSIONAL.name(), JudgeRoleType.CROSS.name()))) {
            throw new BaseException("第一轮发布前需要完整评分表");
        }
    }

    private void validateRankingItems(List<RankingResultItemRequest> results, Set<Long> tableEntryIds, int targetCount, String label) {
        Set<Long> resultEntryIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (RankingResultItemRequest item : results) {
            if (item == null) {
                throw new BaseException(label + "项不能为空");
            }
            if (!tableEntryIds.contains(item.getBeerEntryId())) {
                throw new BaseException(label + "酒款不属于当前桌");
            }
            if (!resultEntryIds.add(item.getBeerEntryId())) {
                throw new BaseException(label + "酒款不能重复");
            }
            if (!ranks.add(item.getRankNo())) {
                throw new BaseException(label + "名次不能重复");
            }
            if (item.getRankNo() < 1 || item.getRankNo() > targetCount) {
                throw new BaseException(label + "名次超出" + (label.equals("排序") ? "目标范围" : "槽位范围"));
            }
        }
    }

    private boolean isPreJudgingStage(CompetitionStatus status) {
        return status == CompetitionStatus.DRAFT
                || status == CompetitionStatus.REGISTRATION_OPEN
                || status == CompetitionStatus.REGISTRATION_CLOSED
                || status == CompetitionStatus.JUDGING_PREP;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
