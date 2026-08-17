package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.ScoreConfirmationEntryVO;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import com.beercompetition.judging.assignment.RoundCandidateSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import com.beercompetition.judging.scoring.ScoreConfirmationService;

/**
 * 维护评分确认版本，防止过期页面覆盖最新确认结果。
 */
@Service
@RequiredArgsConstructor
public class ScoreConfirmationServiceImpl implements ScoreConfirmationService {

    private final CompetitionMapper competitionMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    private final RoundCandidateSyncService roundCandidateSyncService;

    @Override
    public ScoreConfirmationVO getScoreConfirmation(Long roundTableId) {
        // 1) 校验评审身份、轮次可见状态和本桌成员关系
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.SCORE.name().equals(round.getRoundType()) || !isScoreRoundVisibleStatus(round.getStatus())) {
            throw new BaseException("当前评分轮次不可查看");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);

        // 2) 返回评分桌的当前汇总与确认进度
        return buildScoreConfirmation(table, member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreConfirmationVO confirmScoreRoundTable(Long roundTableId, RoundTableConfirmationRequest request) {
        // 1) 校验非桌长评审的确认权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !RoundType.SCORE.name().equals(round.getRoundType()) || !RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            throw new BaseException("当前轮次不能确认本桌结果");
        }
        if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        if (JudgeRoleType.CAPTAIN.name().equals(member.getRole()) || !Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE)) {
            throw new ForbiddenException("当前账号不需要确认本桌结果");
        }
        validateScoreRoundTableReady(table);
        int version = currentResultVersion(table);
        validateConfirmationVersion(request, version);

        // 2) 按结果版本幂等写入确认记录
        RoundTableConfirmation existing = roundTableConfirmationMapper.selectOne(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, roundTableId)
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version));
        if (existing == null) {
            roundTableConfirmationMapper.insert(RoundTableConfirmation.builder()
                    .roundTableId(roundTableId)
                    .judgeAccountId(judgeId)
                    .resultVersion(version)
                    .status("AGREED")
                    .confirmedTime(LocalDateTime.now())
                    .build());
        }

        // 3) 当前版本确认完成后自动提交本桌结果
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), round);
        return buildScoreConfirmation(roundTableMapper.selectById(roundTableId), member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideScoreConfirmation(Long competitionId, Long roundTableId, AdminConfirmationOverrideRequest request) {
        // 1) 校验管理员现场确认的比赛、轮次和桌状态
        roundQuerySupport.requireCompetition(competitionId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        if (!Objects.equals(table.getCompetitionId(), competitionId)) {
            throw new ResourceNotFoundException("评审桌不存在");
        }
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null || !canOverrideConfirmation(round)) {
            throw new BaseException("当前轮次不能现场确认");
        }
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            validateScoreRoundTableReady(table);
        } else {
            roundValidationPolicy.validateRankingRoundTableReady(table);
        }

        // 2) 记录人工确认原因并自动提交本桌结果
        table.setConfirmationOverrideFlag(FLAG_TRUE);
        table.setConfirmationOverrideReason(request.getReason().trim());
        table.setConfirmationOverrideBy(BaseContext.getCurrentId());
        table.setConfirmationOverrideTime(LocalDateTime.now());
        roundTableMapper.updateById(table);
        autoSubmitRoundTableIfReady(roundTableMapper.selectById(roundTableId), round);
    }

    private RoundTableMember requireRoundTableMember(Long roundTableId, Long judgeId) {
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId));
        if (member == null) {
            throw new ForbiddenException("无权查看该轮次桌");
        }
        return member;
    }

    private boolean isScoreRoundVisibleStatus(String status) {
        return RoundStatus.PUBLISHED.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new BaseException("比赛已归档");
        }
    }

    private boolean isRankingEditableStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status);
    }

    private boolean canOverrideConfirmation(CompetitionRound round) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return RoundStatus.PUBLISHED.name().equals(round.getStatus());
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return isRankingEditableStatus(round.getStatus());
        }
        return false;
    }

    private ScoreConfirmationVO buildScoreConfirmation(RoundTable table, RoundTableMember currentMember) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId())
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        Set<Long> entryIds = tableEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entryIds);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Map<Long, ScoreRecord> finalScoreByEntry = entryIds.isEmpty()
                ? Map.of()
                : scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        int version = currentResultVersion(table);
        Long judgeId = BaseContext.getCurrentId();
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version)) > 0;
        List<ScoreConfirmationEntryVO> entries = tableEntries.stream()
                .map(item -> {
                    BeerEntry entry = entryById.get(item.getBeerEntryId());
                    ScoreRecord finalScore = finalScoreByEntry.get(item.getBeerEntryId());
                    EntryScanLabel label = labelByEntryId.get(item.getBeerEntryId());
                    return ScoreConfirmationEntryVO.builder()
                            .beerEntryId(item.getBeerEntryId())
                            .uuid(entry == null ? "" : entry.getUuid())
                            .shortCode(label == null ? "" : label.getShortCode())
                            .categoryName(entry == null ? "" : categoryNameById.getOrDefault(entry.getCategoryId(), ""))
                            .style(entry == null ? "" : entry.getStyle())
                            .consensusScore(finalScore == null ? null : finalScore.getConsensusScore())
                            .comments(finalScore == null ? "" : finalScore.getComments())
                            .advanced(finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE))
                            .build();
                })
                .toList();
        return ScoreConfirmationVO.builder()
                .roundTableId(table.getId())
                .competitionType(resolveCompetitionType(competitionMapper.selectById(table.getCompetitionId())).name())
                .tableName(table.getTableName())
                .status(table.getStatus())
                .resultVersion(version)
                .confirmedCount(resolveConfirmationConfirmedCount(table))
                .requiredCount(resolveConfirmationRequiredCount(table))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(isScoreRoundTableReady(table))
                .overrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .overrideReason(table.getConfirmationOverrideReason())
                .overrideTime(table.getConfirmationOverrideTime())
                .entries(entries)
                .build();
    }

    private void validateScoreRoundTableReady(RoundTable table) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId()));
        if (tableEntries.isEmpty()) {
            throw new BaseException("本桌没有酒款，不能提交结果");
        }
        Set<Long> entryIds = tableEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        List<Long> missing = entryIds.stream()
                .filter(entryId -> !finalScoreByEntry.containsKey(entryId))
                .toList();
        if (!missing.isEmpty()) {
            throw new BaseException("还有酒款未完成桌长汇总");
        }
        if (isFeedbackOnlyCompetition(table.getCompetitionId())) {
            return;
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        if (advancedCount != targetCount) {
            throw new BaseException("晋级数量必须等于目标数量 " + targetCount);
        }
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

    private void validateConfirmationVersion(RoundTableConfirmationRequest request, int currentVersion) {
        if (request == null || request.getResultVersion() == null || !Objects.equals(request.getResultVersion(), currentVersion)) {
            throw new BaseException("本桌结果已更新，请重新核对后确认");
        }
    }

    private void autoSubmitRoundTableIfReady(RoundTable table, CompetitionRound round) {
        if (table == null || round == null) {
            return;
        }
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            return;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            validateScoreRoundTableReady(table);
            if (!isScoreConfirmationReady(table)) {
                return;
            }
        } else if (RoundType.RANKING.name().equals(round.getRoundType())) {
            roundValidationPolicy.validateRankingRoundTableReady(table);
            if (!isRankingConfirmationReady(table)) {
                return;
            }
        } else {
            return;
        }
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (roundQuerySupport.listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
        roundCandidateSyncService.syncDependentDrafts(round);
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

    private boolean isScoreRoundTableReady(RoundTable table) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId()));
        if (tableEntries.isEmpty()) {
            return false;
        }
        Set<Long> entryIds = tableEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        if (entryIds.stream().anyMatch(entryId -> !finalScoreByEntry.containsKey(entryId))) {
            return false;
        }
        if (isFeedbackOnlyCompetition(table.getCompetitionId())) {
            return true;
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        return advancedCount == targetCount;
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isFeedbackOnlyCompetition(Long competitionId) {
        return resolveCompetitionType(competitionMapper.selectById(competitionId)) == CompetitionType.FEEDBACK_ONLY;
    }
}
