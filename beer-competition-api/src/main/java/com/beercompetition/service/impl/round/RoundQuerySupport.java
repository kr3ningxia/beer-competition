package com.beercompetition.service.impl.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 轮次评审模块的查询支撑类。
 *
 * <p>只承接数据库读取、基础上下文 require 和候选池过滤，不在这里执行跨表写入，避免查询细节继续膨胀主流程服务。</p>
 */
@Component
@RequiredArgsConstructor
public class RoundQuerySupport {

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final BreweryMapper breweryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final RoundResultMapper roundResultMapper;

    public List<CompetitionRound> listRounds(Long competitionId) {
        return competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .orderByAsc(CompetitionRound::getSortOrder)
                .orderByAsc(CompetitionRound::getRoundNo)
                .orderByAsc(CompetitionRound::getId));
    }

    public List<RoundTable> listRoundTables(Long roundId) {
        return roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, roundId)
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
    }

    public List<RoundTableEntry> listRoundEntries(Long roundId) {
        return roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, roundId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
    }

    public List<RoundTable> listRoundTablesByCompetition(Long competitionId) {
        return roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getCompetitionId, competitionId)
                .orderByAsc(RoundTable::getSortOrder)
                .orderByAsc(RoundTable::getId));
    }

    public List<RoundTableEntry> listRoundEntriesByCompetition(Long competitionId) {
        return roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getCompetitionId, competitionId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
    }

    public List<RoundResult> listResultsByCompetition(Long competitionId) {
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
    }

    public List<RoundResult> listCandidateResults(Long sourceRoundId) {
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, sourceRoundId)
                .in(RoundResult::getResultType, RoundResultType.ADVANCE.name(), RoundResultType.RANK.name(),
                        RoundResultType.AWARD_CANDIDATE.name(), RoundResultType.CHAMPION.name())
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
    }

    public List<RoundResult> resolveCandidateResultsForRound(CompetitionRound round, List<RoundTable> tables) {
        if (round.getSourceRoundId() == null) {
            return List.of();
        }
        // 候选池跟随当前轮次桌的目标模式：总冠军轮只接收上一轮各组金奖，其余轮次沿用全部晋级候选。
        String targetMode = tables.stream()
                .map(RoundTable::getTargetMode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(RoundTargetMode.TOP_N.name());
        return filterCandidatesForTargetMode(listCandidateResults(round.getSourceRoundId()), targetMode);
    }

    public List<RoundResult> filterCandidatesForTargetMode(List<RoundResult> candidates, String targetMode) {
        if (!RoundTargetMode.CHAMPION.name().equals(targetMode)) {
            return candidates;
        }
        Map<Long, RoundTable> sourceTables = new LinkedHashMap<>();
        candidates.stream()
                .map(RoundResult::getRoundTableId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(tableId -> {
                    RoundTable table = roundTableMapper.selectById(tableId);
                    if (table != null) {
                        sourceTables.put(tableId, table);
                    }
                });
        return candidates.stream()
                .filter(result -> {
                    RoundTable sourceTable = sourceTables.get(result.getRoundTableId());
                    return sourceTable != null
                            && RoundTargetMode.MEDALS.name().equals(sourceTable.getTargetMode())
                            && (Objects.equals(result.getRankNo(), 1) || RoundConstants.SLOT_GOLD.equals(result.getSlotLabel()));
                })
                .toList();
    }

    public List<JudgeTable> listBaseTables(Long competitionId) {
        return judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .orderByAsc(JudgeTable::getSortOrder)
                .orderByAsc(JudgeTable::getId));
    }

    public List<JudgeAssignment> listBaseAssignments(Long competitionId) {
        return judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
    }

    public List<BeerEntry> listStoredEntries(Long competitionId) {
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .eq(BeerEntry::getStoredFlag, RoundConstants.FLAG_TRUE)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())
                .orderByAsc(BeerEntry::getId));
    }

    public List<RoundTableMember> listMembers(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return List.of();
        }
        return roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tableIds));
    }

    public Map<Long, String> listCategoryNames(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, CompetitionCategory::getName, (left, right) -> left));
    }

    public Map<String, CompetitionStyleConfig> listStyleSnapshot(Long competitionId) {
        return competitionStyleConfigMapper.selectList(new LambdaQueryWrapper<CompetitionStyleConfig>()
                        .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionStyleConfig::getSortOrder)
                        .orderByAsc(CompetitionStyleConfig::getId))
                .stream()
                .collect(Collectors.toMap(CompetitionStyleConfig::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    public Map<Long, RoundResult> latestResultByEntry(Long competitionId) {
        Map<Long, RoundResult> map = new LinkedHashMap<>();
        // 按轮次和结果顺序覆盖写入，最终保留每个酒款最近一次轮次结果。
        roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .orderByAsc(RoundResult::getRoundId)
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByAsc(RoundResult::getId))
                .forEach(result -> map.put(result.getBeerEntryId(), result));
        return map;
    }

    public Map<Long, Brewery> loadBreweries(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return breweryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Brewery::getId, Function.identity()));
    }

    public Map<Long, EntryPayment> loadPayments(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        return entryPaymentMapper.selectList(new LambdaQueryWrapper<EntryPayment>()
                        .in(EntryPayment::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryPayment::getBeerEntryId, Function.identity(), (left, right) -> left));
    }

    public Map<Long, EntryDelivery> loadDeliveries(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        return entryDeliveryMapper.selectList(new LambdaQueryWrapper<EntryDelivery>()
                        .in(EntryDelivery::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryDelivery::getBeerEntryId, Function.identity(), (left, right) -> left));
    }

    public Map<Long, EntryRefund> loadLatestRefunds(Set<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EntryRefund> refundByEntryId = new LinkedHashMap<>();
        // 退款记录按 id 倒序读取，putIfAbsent 保留每个报名酒款最新的一条退款状态。
        entryRefundMapper.selectList(new LambdaQueryWrapper<EntryRefund>()
                        .in(EntryRefund::getBeerEntryId, entryIds)
                        .orderByDesc(EntryRefund::getId))
                .forEach(refund -> refundByEntryId.putIfAbsent(refund.getBeerEntryId(), refund));
        return refundByEntryId;
    }

    public Map<Long, BeerEntry> loadEntries(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return beerEntryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(BeerEntry::getId, Function.identity()));
    }

    public Map<String, BeerEntry> loadEntryByUuids(Long competitionId, Set<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Map.of();
        }
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .in(BeerEntry::getUuid, uuids));
        if (entries.size() != uuids.size()) {
            throw new BaseException("存在不属于当前比赛的酒款");
        }
        return entries.stream().collect(Collectors.toMap(BeerEntry::getUuid, Function.identity()));
    }

    public Map<Long, JudgeAccount> loadJudges(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return judgeAccountMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(JudgeAccount::getId, Function.identity()));
    }

    public Map<String, JudgeAccount> loadJudgeByPublicIds(Set<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Map.of();
        }
        List<JudgeAccount> judges = judgeAccountMapper.selectList(new LambdaQueryWrapper<JudgeAccount>()
                .in(JudgeAccount::getPublicId, publicIds));
        if (judges.size() != publicIds.size()) {
            throw new BaseException("存在无效桌长");
        }
        judges.forEach(judge -> {
            if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
                throw new BaseException("桌长必须是启用评审：" + judge.getName());
            }
        });
        return judges.stream().collect(Collectors.toMap(JudgeAccount::getPublicId, Function.identity()));
    }

    public Map<Long, RoundTable> loadRoundTables(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return roundTableMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
    }

    public Map<Long, CompetitionRound> loadRounds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return competitionRoundMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(CompetitionRound::getId, Function.identity()));
    }

    public Map<Long, Competition> loadCompetitions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return competitionMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Competition::getId, Function.identity()));
    }

    public Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    public CompetitionRound requireRound(Long competitionId, Long roundId) {
        CompetitionRound round = competitionRoundMapper.selectById(roundId);
        if (round == null || !round.getCompetitionId().equals(competitionId)) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        return round;
    }

    public RoundTable requireRoundTable(Long roundTableId) {
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            throw new ResourceNotFoundException("轮次桌不存在");
        }
        return table;
    }

    public JudgeAccount requireActiveJudge(Long judgeId) {
        JudgeAccount judge = judgeAccountMapper.selectById(judgeId);
        if (judge == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用");
        }
        return judge;
    }

    public CompetitionRound findLastLockedRound(Long competitionId) {
        return listRounds(competitionId).stream()
                .filter(round -> com.beercompetition.pojo.enums.RoundStatus.LOCKED.name().equals(round.getStatus()))
                .max(Comparator.comparing(CompetitionRound::getRoundNo))
                .orElse(null);
    }

    public Long findCaptainId(List<JudgeAssignment> assignments) {
        return assignments.stream()
                .filter(assignment -> JudgeRoleType.CAPTAIN.name().equals(assignment.getRole()))
                .map(JudgeAssignment::getJudgeAccountId)
                .findFirst()
                .orElse(null);
    }
}
