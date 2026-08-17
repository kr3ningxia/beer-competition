package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.JudgeTaskType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.service.impl.round.RoundQuerySupport;
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
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_FALSE;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import com.beercompetition.judging.round.JudgeRoundTaskService;

/**
 * 查询评委任务并提交当前评分桌，严格使用当前登录评委身份。
 */
@Service
@RequiredArgsConstructor
public class JudgeRoundTaskServiceImpl implements JudgeRoundTaskService {

    private final CompetitionMapper competitionMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final JudgeRoundTableQueryService judgeRoundTableQueryService;

    private final RoundCandidateSyncService roundCandidateSyncService;

    @Override
    public List<JudgeTaskVO> listMyTasks() {
        // 1) 读取当前评审参与的轮次桌
        Long judgeId = BaseContext.getCurrentId();
        JudgeAccount judge = roundQuerySupport.requireActiveJudge(judgeId);
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeId));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, RoundTable> tableById = roundQuerySupport.loadRoundTables(members.stream().map(RoundTableMember::getRoundTableId).collect(Collectors.toSet()));
        Map<Long, CompetitionRound> roundById = roundQuerySupport.loadRounds(tableById.values().stream().map(RoundTable::getRoundId).collect(Collectors.toSet()));
        Map<Long, Competition> competitionById = roundQuerySupport.loadCompetitions(roundById.values().stream().map(CompetitionRound::getCompetitionId).collect(Collectors.toSet()));

        // 2) 按评审身份过滤不可见任务，返回评委端任务列表
        return members.stream()
                .map(member -> buildJudgeTask(judge, member, tableById.get(member.getRoundTableId()), roundById, competitionById))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScoreRoundTable(Long roundTableId) {
        // 1) 查询评分桌并校验桌长权限
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        if (!RoundType.SCORE.name().equals(round.getRoundType()) || !RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
            throw new BaseException("当前轮次不能提交本桌结果");
        }
        if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果已提交或已锁定");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        requireRankingCaptainMember(roundTableId, judgeId);
        validateScoreRoundTableReady(table);
        validateScoreRoundTableConfirmations(table);

        // 2) 提交本桌汇总；全部桌提交后推进轮次到待管理员确认
        table.setStatus(RoundStatus.SUBMITTED.name());
        roundTableMapper.updateById(table);
        if (roundQuerySupport.listRoundTables(round.getId()).stream().allMatch(item -> RoundStatus.SUBMITTED.name().equals(item.getStatus()))) {
            round.setStatus(RoundStatus.SUBMITTED.name());
            round.setSubmittedTime(LocalDateTime.now());
            competitionRoundMapper.updateById(round);
        }
        roundCandidateSyncService.syncDependentDrafts(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenScoreRoundTable(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = roundQuerySupport.requireRoundForUpdate(table.getCompetitionId(), table.getRoundId());
        if (!RoundType.SCORE.name().equals(round.getRoundType())) {
            throw new BaseException("只有首轮评分桌可以重新修改");
        }
        if (RoundStatus.LOCKED.name().equals(round.getStatus()) || RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("首轮已锁定，不能再修改本桌结果");
        }
        if (!RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
            throw new BaseException("本桌结果尚未提交，无需重新修改");
        }
        requireRankingCaptainMember(roundTableId, judgeId);

        table.setStatus(RoundStatus.PUBLISHED.name());
        table.setResultVersion(currentResultVersion(table) + 1);
        table.setConfirmationOverrideFlag(FLAG_FALSE);
        table.setConfirmationOverrideReason(null);
        table.setConfirmationOverrideBy(null);
        table.setConfirmationOverrideTime(null);
        roundTableMapper.updateById(table);
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            round.setStatus(RoundStatus.PUBLISHED.name());
            round.setSubmittedTime(null);
            competitionRoundMapper.updateById(round);
        }
        roundCandidateSyncService.syncDependentDrafts(round);
    }

    private JudgeTaskVO buildJudgeTask(JudgeAccount judge,
                                       RoundTableMember member,
                                       RoundTable table,
                                       Map<Long, CompetitionRound> roundById,
                                       Map<Long, Competition> competitionById) {
        if (table == null) {
            return null;
        }
        CompetitionRound round = roundById.get(table.getRoundId());
        if (round == null) {
            return null;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            if (!isScoreRoundVisibleStatus(round.getStatus())) {
                return null;
            }
            if (!RoundStatus.PUBLISHED.name().equals(table.getStatus())
                    && !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
                return null;
            }
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingVisibleStatus(round.getStatus())) {
            return null;
        }
        Competition competition = competitionById.get(round.getCompetitionId());
        if (competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            return null;
        }
        String taskType = resolveTaskType(round, member);
        if (taskType == null) {
            return null;
        }
        int totalEntries = Math.toIntExact(roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId())));
        return JudgeTaskVO.builder()
                .taskType(taskType)
                .competitionId(round.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .roundTableId(table.getId())
                .tableName(table.getTableName())
                .judgeRoleType(member.getRole())
                .roleLabel(roleLabel(round, member))
                .totalEntries(totalEntries)
                .completedCount(resolveCompletedCount(judge.getId(), table, taskType))
                .build();
    }

    private String resolveTaskType(CompetitionRound round, RoundTableMember member) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                    ? JudgeTaskType.CAPTAIN_FINALIZE.name()
                    : JudgeTaskType.SCORE_ENTRY.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return JudgeTaskType.RANKING_ROUND.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return JudgeTaskType.RANKING_PARTICIPANT.name();
        }
        return null;
    }

    private int resolveCompletedCount(Long judgeId, RoundTable table, String taskType) {
        if (JudgeTaskType.RANKING_ROUND.name().equals(taskType) || JudgeTaskType.RANKING_PARTICIPANT.name().equals(taskType)) {
            return Math.toIntExact(roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                    .eq(RoundResult::getRoundTableId, table.getId())));
        }
        List<Long> entryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .toList();
        if (entryIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<ScoreRecord> wrapper = new LambdaQueryWrapper<ScoreRecord>()
                .in(ScoreRecord::getBeerEntryId, entryIds);
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            wrapper.eq(ScoreRecord::getFinalFlag, FLAG_TRUE);
        } else {
            wrapper.eq(ScoreRecord::getJudgeAccountId, judgeId).eq(ScoreRecord::getFinalFlag, FLAG_FALSE);
        }
        return Math.toIntExact(scoreRecordMapper.selectCount(wrapper));
    }

    private RoundTableMember requireRankingCaptainMember(Long roundTableId, Long judgeId) {
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE));
        if (member == null || !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            throw new ForbiddenException("无权操作该轮次桌");
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

    private boolean isRankingVisibleStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
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

    private void validateScoreRoundTableConfirmations(RoundTable table) {
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return;
        }
        int required = resolveConfirmationRequiredCount(table);
        if (required <= 0) {
            return;
        }
        int confirmed = resolveConfirmationConfirmedCount(table);
        if (confirmed < required) {
            throw new BaseException("同桌评审确认未完成，暂不能提交本桌结果");
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

    private int currentResultVersion(RoundTable table) {
        return table.getResultVersion() == null ? 0 : table.getResultVersion();
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

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isFeedbackOnlyCompetition(Long competitionId) {
        return resolveCompetitionType(competitionMapper.selectById(competitionId)) == CompetitionType.FEEDBACK_ONLY;
    }

    @Override
    public JudgeRoundTableVO getMyRoundTable(Long roundTableId) {
        return judgeRoundTableQueryService.getMyRoundTable(roundTableId);
    }
}
