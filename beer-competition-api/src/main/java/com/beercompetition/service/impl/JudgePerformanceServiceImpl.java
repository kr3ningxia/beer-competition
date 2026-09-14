package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.judging.access.JudgeAccessService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.CompetitionJudgeEvaluationMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeScoreSessionMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.JudgePerformanceSaveRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionJudgeEvaluation;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeScoreSession;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionJudgePerformanceSummaryVO;
import com.beercompetition.pojo.vo.JudgeAccountPerformanceVO;
import com.beercompetition.pojo.vo.JudgePerformanceVO;
import com.beercompetition.service.JudgePerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgePerformanceServiceImpl implements JudgePerformanceService {

    private static final String TARGET_TYPE = "JUDGE_PERFORMANCE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private final CompetitionAccessService competitionAccessService;
    private final JudgeAccessService judgeAccessService;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AdminUserMapper adminUserMapper;
    private final CompetitionJudgeEvaluationMapper evaluationMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper roundMapper;
    private final CompetitionScoreConfigMapper scoreConfigMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper assignmentMapper;
    private final JudgeScoreSessionMapper sessionMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final ScoreRecordMapper scoreRecordMapper;

    @Override
    public CompetitionJudgePerformanceSummaryVO listCompetitionPerformances(Long competitionId) {
        Competition competition = requireCompetition(competitionId);
        List<JudgePerformanceVO> records = buildCompetitionRecords(competition);
        long evaluated = records.stream().filter(item -> !"UNRATED".equals(item.getEvaluationStatus())).count();
        long confirmed = records.stream().filter(item -> STATUS_CONFIRMED.equals(item.getEvaluationStatus())).count();
        long candidates = records.stream().filter(item -> Boolean.TRUE.equals(item.getExcellentCandidate())).count();
        boolean hasLockedScore = hasLockedScoreRound(competitionId);
        boolean confirmAllowed = canConfirm(competition, hasLockedScore);
        return CompetitionJudgePerformanceSummaryVO.builder()
                .competitionId(competitionId)
                .competitionName(competition.getName())
                .draftAllowed(hasLockedScore)
                .confirmAllowed(confirmAllowed)
                .confirmDisabledReason(confirmAllowed ? null : confirmReason(competition, hasLockedScore))
                .totalJudges(records.size())
                .evaluatedCount((int) evaluated)
                .confirmedCount((int) confirmed)
                .excellentCandidateCount((int) candidates)
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgePerformanceVO savePerformance(Long competitionId, String judgePublicId,
                                              JudgePerformanceSaveRequest request) {
        Competition competition = requireCompetition(competitionId);
        JudgeAccount judge = requireAssignedJudge(competitionId, judgePublicId);
        List<JudgePerformanceVO> records = buildCompetitionRecords(competition);
        JudgePerformanceVO current = records.stream()
                .filter(item -> judgePublicId.equals(item.getJudgePublicId()))
                .findFirst()
                .orElseThrow(() -> new BaseException("该评审在本场比赛中没有有效任务"));
        boolean hasLockedScoreRound = hasLockedScoreRound(competitionId);
        if (!hasLockedScoreRound) {
            throw new BaseException("评分轮尚未锁定，暂不能评价评审表现");
        }
        CompetitionJudgeEvaluation existing = evaluationMapper.selectOne(new LambdaQueryWrapper<CompetitionJudgeEvaluation>()
                .eq(CompetitionJudgeEvaluation::getCompetitionId, competitionId)
                .eq(CompetitionJudgeEvaluation::getJudgeAccountId, judge.getId())
                .last("LIMIT 1"));
        boolean wasConfirmed = existing != null && STATUS_CONFIRMED.equals(existing.getStatus());
        checkVersion(existing, request.getVersion());
        String status = request.getStatus();
        if (STATUS_CONFIRMED.equals(status)) {
            if (!canConfirm(competition, hasLockedScoreRound)) {
                throw new BaseException(confirmReason(competition, hasLockedScoreRound));
            }
            validateConfirmation(request, current);
        }
        BigDecimal manualScore = manualScore(request);
        BigDecimal commentScore = current.getCommentScore();
        BigDecimal totalScore = manualScore == null || commentScore == null ? null : manualScore.add(commentScore);
        CompetitionJudgeEvaluation target = existing == null ? newEvaluation(competitionId, judge.getId()) : existing;
        target.setJudgmentLevel(request.getJudgmentLevel());
        target.setFeedbackQualityLevel(request.getFeedbackQualityLevel());
        target.setRuleExecutionLevel(request.getRuleExecutionLevel());
        target.setProfessionalismLevel(request.getProfessionalismLevel());
        target.setManualScore(manualScore);
        target.setCommentTotalChars(current.getCommentTotalChars());
        target.setCommentAverageChars(current.getCommentAverageChars());
        target.setCommentRecordCount(current.getCommentRecordCount());
        target.setCommentRequirementRatio(current.getCommentRequirementRatio());
        target.setCommentPercentile(current.getCommentPercentile());
        target.setCommentScore(commentScore);
        target.setTaskCompletedCount(current.getTaskCompletedCount());
        target.setTaskTotalCount(current.getTaskTotalCount());
        target.setCompletionRate(current.getCompletionRate());
        target.setTotalScore(totalScore);
        target.setExcellentCandidate(STATUS_CONFIRMED.equals(status) && isExcellent(current, manualScore, totalScore) ? 1 : 0);
        target.setEvidence(StringUtils.hasText(request.getEvidence()) ? request.getEvidence().trim() : null);
        target.setStatus(status);
        target.setEvaluatedBy(BaseContext.getCurrentId());
        target.setEvaluatedTime(LocalDateTime.now());
        if (STATUS_CONFIRMED.equals(status)) {
            target.setConfirmedBy(BaseContext.getCurrentId());
            target.setConfirmedTime(LocalDateTime.now());
        }
        target.setVersion(existing == null ? 0 : existing.getVersion() + 1);
        if (existing == null) {
            evaluationMapper.insert(target);
        } else {
            boolean updated = evaluationMapper.update(target, new LambdaUpdateWrapper<CompetitionJudgeEvaluation>()
                    .eq(CompetitionJudgeEvaluation::getId, existing.getId())
                    .eq(CompetitionJudgeEvaluation::getVersion, existing.getVersion())) > 0;
            if (!updated) {
                throw new BaseException("评价已被其他管理员更新，请刷新后重试");
            }
        }
        writeLog(competitionId, judgePublicId, STATUS_CONFIRMED.equals(status)
                ? (wasConfirmed ? "修改已确认的评审表现" : "确认评审表现")
                : "保存评审表现草稿");
        return buildCompetitionRecords(competition).stream()
                .filter(item -> judgePublicId.equals(item.getJudgePublicId()))
                .findFirst()
                .orElseThrow(() -> new BaseException("评审表现保存后无法读取"));
    }

    @Override
    public List<JudgeAccountPerformanceVO> listAccountOverviews(List<String> judgePublicIds) {
        if (judgePublicIds == null || judgePublicIds.isEmpty()) {
            return List.of();
        }
        List<String> ids = judgePublicIds.stream().filter(StringUtils::hasText).distinct().limit(100).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, JudgeAccount> judgesByPublicId = judgeAccountMapper.selectList(new LambdaQueryWrapper<JudgeAccount>()
                        .in(JudgeAccount::getPublicId, ids))
                .stream().collect(Collectors.toMap(JudgeAccount::getPublicId, Function.identity()));
        JudgeAccessService.JudgeScope scope = judgeAccessService.currentScope();
        List<JudgeAccount> judges = ids.stream().map(publicId -> {
            JudgeAccount judge = judgesByPublicId.get(publicId);
            if (judge == null) {
                throw new BaseException("评审账号不存在");
            }
            if (!scope.all() && !scope.judgeIds().contains(judge.getId())) {
                throw new ForbiddenException("当前账号无权访问该评委");
            }
            return judge;
        }).toList();
        return buildAccountHistories(judges);
    }

    @Override
    public JudgeAccountPerformanceVO getAccountHistory(String judgePublicId) {
        JudgeAccount judge = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPublicId, judgePublicId).last("LIMIT 1"));
        if (judge == null) {
            throw new BaseException("评审账号不存在");
        }
        judgeAccessService.requireJudgeAccess(judge.getId());
        return buildAccountHistories(List.of(judge)).get(0);
    }

    private List<JudgeAccountPerformanceVO> buildAccountHistories(List<JudgeAccount> judges) {
        List<Long> competitionIds = accessibleCompetitionIds();
        if (competitionIds.isEmpty()) {
            return judges.stream().map(judge -> emptyHistory(judge.getPublicId())).toList();
        }
        List<Long> judgeIds = judges.stream().map(JudgeAccount::getId).toList();
        List<CompetitionJudgeEvaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<CompetitionJudgeEvaluation>()
                .in(CompetitionJudgeEvaluation::getJudgeAccountId, judgeIds)
                .eq(CompetitionJudgeEvaluation::getStatus, STATUS_CONFIRMED)
                .in(CompetitionJudgeEvaluation::getCompetitionId, competitionIds)
                .orderByDesc(CompetitionJudgeEvaluation::getConfirmedTime));
        List<Long> evaluatedCompetitionIds = evaluations.stream()
                .map(CompetitionJudgeEvaluation::getCompetitionId).distinct().toList();
        Map<Long, Competition> competitions = evaluatedCompetitionIds.isEmpty() ? Map.of()
                : competitionMapper.selectBatchIds(evaluatedCompetitionIds).stream()
                .collect(Collectors.toMap(Competition::getId, Function.identity()));
        Map<Long, List<CompetitionJudgeEvaluation>> evaluationsByJudge = evaluations.stream()
                .collect(Collectors.groupingBy(CompetitionJudgeEvaluation::getJudgeAccountId));
        return judges.stream().map(judge -> buildAccountHistory(judge,
                evaluationsByJudge.getOrDefault(judge.getId(), List.of()), competitions)).toList();
    }

    private JudgeAccountPerformanceVO buildAccountHistory(JudgeAccount judge,
                                                            List<CompetitionJudgeEvaluation> evaluations,
                                                            Map<Long, Competition> competitions) {
        List<JudgePerformanceVO> history = evaluations.stream()
                .map(item -> toHistoryVO(item, competitions.get(item.getCompetitionId()), judge))
                .toList();
        BigDecimal average = history.isEmpty() ? null : scale(history.stream().map(JudgePerformanceVO::getTotalScore)
                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0));
        JudgePerformanceVO latest = history.isEmpty() ? null : history.get(0);
        return JudgeAccountPerformanceVO.builder()
                .judgePublicId(judge.getPublicId())
                .averageScore(average)
                .evaluatedCompetitionCount(history.size())
                .excellentCount((int) history.stream().filter(item -> Boolean.TRUE.equals(item.getExcellentCandidate())).count())
                .latestScore(latest == null ? null : latest.getTotalScore())
                .latestCompetitionName(latest == null ? null : latest.getCompetitionName())
                .history(history)
                .build();
    }

    private List<JudgePerformanceVO> buildCompetitionRecords(Competition competition) {
        competitionAccessService.requireCompetitionAccess(competition.getId());
        List<JudgeAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competition.getId())
                .and(wrapper -> wrapper.isNull(JudgeAssignment::getStatus).or().eq(JudgeAssignment::getStatus, "ACTIVE")));
        if (assignments.isEmpty()) {
            return List.of();
        }
        Map<Long, JudgeAccount> judges = judgeAccountMapper.selectBatchIds(assignments.stream()
                        .map(JudgeAssignment::getJudgeAccountId).distinct().toList())
                .stream().collect(Collectors.toMap(JudgeAccount::getId, Function.identity()));
        Map<Long, JudgeTable> tables = judgeTableMapper.selectBatchIds(assignments.stream()
                        .map(JudgeAssignment::getTableId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(JudgeTable::getId, Function.identity()));
        MetricContext metrics = computeMetrics(competition, assignments);
        Map<Long, CompetitionJudgeEvaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<CompetitionJudgeEvaluation>()
                        .eq(CompetitionJudgeEvaluation::getCompetitionId, competition.getId()))
                .stream().collect(Collectors.toMap(CompetitionJudgeEvaluation::getJudgeAccountId, Function.identity(), (left, right) -> right));
        Map<Long, String> evaluatorNames = evaluatorNames(evaluations.values());
        List<JudgePerformanceVO> result = assignments.stream().map(assignment -> {
            JudgeAccount judge = judges.get(assignment.getJudgeAccountId());
            JudgeStats stats = metrics.byJudge().getOrDefault(assignment.getJudgeAccountId(), JudgeStats.empty());
            CompetitionJudgeEvaluation evaluation = evaluations.get(assignment.getJudgeAccountId());
            String evaluatorName = evaluation == null || evaluation.getEvaluatedBy() == null
                    ? null : evaluatorNames.get(evaluation.getEvaluatedBy());
            return toVO(competition, assignment, judge, tables.get(assignment.getTableId()), stats, evaluation,
                    evaluatorName);
        }).sorted(Comparator.comparing(JudgePerformanceVO::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(JudgePerformanceVO::getJudgeName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        assignOverallRanks(result);
        return result;
    }

    private MetricContext computeMetrics(Competition competition, List<JudgeAssignment> assignments) {
        Set<Long> assignedIds = assignments.stream().map(JudgeAssignment::getJudgeAccountId).collect(Collectors.toSet());
        List<CompetitionRound> rounds = roundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competition.getId())
                .eq(CompetitionRound::getRoundType, RoundType.SCORE.name())
                .eq(CompetitionRound::getStatus, RoundStatus.LOCKED.name()));
        Set<Long> roundIds = rounds.stream().map(CompetitionRound::getId).collect(Collectors.toSet());
        if (roundIds.isEmpty()) {
            return new MetricContext(Map.of(), 0);
        }
        Map<String, Integer> minimums = scoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competition.getId())).stream()
                .collect(Collectors.toMap(CompetitionScoreConfig::getJudgeRoleType,
                        item -> JudgePerformanceScorePolicy.minimumLength(item.getJudgeRoleType(), item.getMinCommentLength()),
                        (left, right) -> left));
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getCompetitionId, competition.getId()).in(RoundTable::getRoundId, roundIds));
        Set<Long> tableIds = tables.stream().map(RoundTable::getId).collect(Collectors.toSet());
        List<RoundTableEntry> entries = tableIds.isEmpty() ? List.of() : roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getCompetitionId, competition.getId()).in(RoundTableEntry::getRoundId, roundIds));
        List<RoundTableMember> members = tableIds.isEmpty() ? List.of() : roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tableIds).eq(RoundTableMember::getSystemTaskRequired, 1));
        Map<Long, Integer> expected = new HashMap<>();
        members.stream().filter(member -> member.getStatus() == null || !"REMOVED".equals(member.getStatus())).forEach(member -> {
            int dutyCount = "CAPTAIN".equals(member.getRole()) ? 2 : 1;
            int entryCount = (int) entries.stream().filter(entry -> Objects.equals(entry.getRoundTableId(), member.getRoundTableId())).count();
            expected.merge(member.getJudgeAccountId(), entryCount * dutyCount, Integer::sum);
        });
        List<ScoreRecord> scores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competition.getId()).in(ScoreRecord::getRoundId, roundIds));
        List<JudgeScoreSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<JudgeScoreSession>()
                .eq(JudgeScoreSession::getCompetitionId, competition.getId()).in(JudgeScoreSession::getRoundId, roundIds));
        Map<String, Integer> sessionChars = sessions.stream().filter(item -> item.getFirstSubmittedAt() != null)
                .collect(Collectors.toMap(item -> sampleKey(item.getRoundTableId(), item.getBeerEntryId(), item.getJudgeAccountId(), item.getJudgeRoleType()),
                        item -> item.getCommentCharCount() == null ? 0 : item.getCommentCharCount(), (left, right) -> right));
        Map<Long, JudgeStats> byJudge = new HashMap<>();
        for (ScoreRecord score : scores) {
            if (!assignedIds.contains(score.getJudgeAccountId())) {
                continue;
            }
            String role = score.getJudgeRoleType();
            Integer chars = score.getFinalFlag() != null && score.getFinalFlag() == 0
                    ? sessionChars.getOrDefault(sampleKey(score.getRoundTableId(), score.getBeerEntryId(), score.getJudgeAccountId(), role), score.getCommentCharCount())
                    : score.getCommentCharCount();
            int effectiveChars = chars == null ? 0 : Math.max(chars, 0);
            int minimum = JudgePerformanceScorePolicy.minimumLength(role, minimums.get(role));
            JudgeStats stats = byJudge.computeIfAbsent(score.getJudgeAccountId(), ignored -> new JudgeStats());
            stats.judgeId = score.getJudgeAccountId();
            stats.records++;
            stats.totalChars += effectiveChars;
            stats.requirementRatio += JudgePerformanceScorePolicy.normalizedCommentLength(effectiveChars, minimum);
        }
        byJudge.values().forEach(stats -> stats.averageChars = stats.records == 0 ? 0 : stats.totalChars / (double) stats.records);
        List<JudgeStats> eligible = byJudge.values().stream().filter(stats -> stats.records > 0).toList();
        int participantCount = eligible.size();
        eligible.forEach(stats -> {
            double normalizedRatio = stats.averageRequirementRatio();
            long greater = eligible.stream().filter(other -> other.averageRequirementRatio() > normalizedRatio).count();
            double percentile = JudgePerformanceScorePolicy.percentile(participantCount, greater);
            stats.percentile = percentile;
            stats.commentScore = JudgePerformanceScorePolicy.commentScore(percentile, stats.isComplete(expected));
            stats.commentRank = (int) greater + 1;
        });
        expected.forEach((judgeId, count) -> {
            JudgeStats stats = byJudge.computeIfAbsent(judgeId, ignored -> new JudgeStats());
            stats.judgeId = judgeId;
            stats.taskTotal = count;
        });
        byJudge.values().forEach(stats -> stats.taskTotal = expected.getOrDefault(stats.judgeId, 0));
        return new MetricContext(byJudge, participantCount);
    }

    private JudgePerformanceVO toVO(Competition competition, JudgeAssignment assignment, JudgeAccount judge,
                                    JudgeTable table, JudgeStats stats, CompetitionJudgeEvaluation evaluation,
                                    String evaluatorName) {
        boolean confirmed = evaluation != null && STATUS_CONFIRMED.equals(evaluation.getStatus());
        if (confirmed) {
            stats = stats.fromEvaluation(evaluation);
        }
        BigDecimal manual = evaluation == null ? null : evaluation.getManualScore();
        BigDecimal total = confirmed ? evaluation.getTotalScore() : null;
        boolean complete = stats.isComplete(stats.taskTotal);
        return JudgePerformanceVO.builder()
                .competitionId(competition.getId()).competitionName(competition.getName()).competitionDate(competition.getCompetitionDate())
                .competitionStatus(competition.getStatus()).judgePublicId(judge == null ? null : judge.getPublicId())
                .judgeName(judge == null ? "未知评审" : judge.getName()).role(assignment.getRole()).roleLabel(roleLabel(assignment.getRole()))
                .tableName(table == null ? null : table.getTableName()).taskCompletedCount(stats.records).taskTotalCount(stats.taskTotal)
                .completionRate(rate(stats.records, stats.taskTotal)).commentTotalChars(stats.totalChars)
                .commentAverageChars((int) Math.round(stats.averageChars)).commentRecordCount(stats.records)
                .commentRequirementRatio(scale(stats.averageRequirementRatio())).commentPercentile(scale(stats.percentile * 100))
                .commentTopPercent(JudgePerformanceScorePolicy.topPercent(stats.percentile)).commentScore(scale(stats.commentScore)).commentRank(stats.commentRank)
                .participantCount(stats.participantCount).judgmentLevel(evaluation == null ? null : evaluation.getJudgmentLevel())
                .feedbackQualityLevel(evaluation == null ? null : evaluation.getFeedbackQualityLevel())
                .ruleExecutionLevel(evaluation == null ? null : evaluation.getRuleExecutionLevel())
                .professionalismLevel(evaluation == null ? null : evaluation.getProfessionalismLevel()).manualScore(manual).totalScore(total)
                .excellentCandidate(confirmed && isExcellent(stats, manual, total)).sampleWarning(stats.participantCount < 5 || stats.records < 3)
                .evidence(evaluation == null ? null : evaluation.getEvidence()).evaluationStatus(evaluation == null ? "UNRATED" : evaluation.getStatus())
                .evaluatedByName(evaluatorName).evaluatedTime(evaluation == null ? null : evaluation.getEvaluatedTime())
                .confirmedTime(evaluation == null ? null : evaluation.getConfirmedTime()).version(evaluation == null ? 0 : evaluation.getVersion()).build();
    }

    private JudgePerformanceVO toHistoryVO(CompetitionJudgeEvaluation evaluation, Competition competition, JudgeAccount judge) {
        return JudgePerformanceVO.builder().competitionId(evaluation.getCompetitionId()).competitionName(competition == null ? "未知比赛" : competition.getName())
                .competitionDate(competition == null ? null : competition.getCompetitionDate()).competitionStatus(competition == null ? null : competition.getStatus())
                .judgePublicId(judge.getPublicId()).judgeName(judge.getName()).manualScore(evaluation.getManualScore())
                .commentTotalChars(evaluation.getCommentTotalChars()).commentAverageChars(evaluation.getCommentAverageChars())
                .commentRecordCount(evaluation.getCommentRecordCount()).commentRequirementRatio(evaluation.getCommentRequirementRatio())
                .commentPercentile(evaluation.getCommentPercentile()).commentScore(evaluation.getCommentScore())
                .taskCompletedCount(evaluation.getTaskCompletedCount()).taskTotalCount(evaluation.getTaskTotalCount())
                .completionRate(evaluation.getCompletionRate()).judgmentLevel(evaluation.getJudgmentLevel())
                .feedbackQualityLevel(evaluation.getFeedbackQualityLevel()).ruleExecutionLevel(evaluation.getRuleExecutionLevel())
                .professionalismLevel(evaluation.getProfessionalismLevel()).totalScore(evaluation.getTotalScore())
                .excellentCandidate(evaluation.getExcellentCandidate() != null && evaluation.getExcellentCandidate() == 1)
                .evaluationStatus(evaluation.getStatus()).evidence(evaluation.getEvidence()).confirmedTime(evaluation.getConfirmedTime())
                .version(evaluation.getVersion()).build();
    }

    private void assignOverallRanks(List<JudgePerformanceVO> records) {
        List<JudgePerformanceVO> rated = records.stream().filter(item -> STATUS_CONFIRMED.equals(item.getEvaluationStatus()) && item.getTotalScore() != null).toList();
        List<JudgePerformanceVO> ranked = rated.stream()
                .sorted(Comparator.comparing(JudgePerformanceVO::getTotalScore).reversed()
                        .thenComparing(JudgePerformanceVO::getJudgeName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).setOverallRank(index + 1);
        }
    }

    private JudgeAccount requireAssignedJudge(Long competitionId, String publicId) {
        JudgeAccount judge = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>().eq(JudgeAccount::getPublicId, publicId).last("LIMIT 1"));
        if (judge == null) throw new BaseException("评审账号不存在");
        judgeAccessService.requireJudgeAccess(judge.getId());
        long count = assignmentMapper.selectCount(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId).eq(JudgeAssignment::getJudgeAccountId, judge.getId())
                .and(wrapper -> wrapper.isNull(JudgeAssignment::getStatus).or().eq(JudgeAssignment::getStatus, "ACTIVE")));
        if (count == 0) throw new BaseException("该评审未分配到本场比赛");
        return judge;
    }

    private Competition requireCompetition(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) throw new BaseException("比赛不存在");
        competitionAccessService.requireCompetitionAccess(id);
        return competition;
    }

    private boolean hasLockedScoreRound(Long competitionId) {
        return roundMapper.selectCount(new LambdaQueryWrapper<CompetitionRound>().eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundType, RoundType.SCORE.name()).eq(CompetitionRound::getStatus, RoundStatus.LOCKED.name())) > 0;
    }

    private boolean canConfirm(Competition competition, boolean hasLockedScore) {
        return hasLockedScore && Set.of(CompetitionStatus.RESULT_CONFIRMING.name(), CompetitionStatus.PUBLISHED.name(), CompetitionStatus.ARCHIVED.name()).contains(competition.getStatus());
    }

    private String confirmReason(Competition competition, boolean hasLockedScore) {
        if (!hasLockedScore) return "评分轮尚未锁定，暂不能确认评审表现";
        return "请等比赛进入结果确认阶段后再确认评审表现";
    }

    private void validateConfirmation(JudgePerformanceSaveRequest request, JudgePerformanceVO current) {
        if (request.getJudgmentLevel() == null || request.getFeedbackQualityLevel() == null || request.getRuleExecutionLevel() == null || request.getProfessionalismLevel() == null) {
            throw new BaseException("确认评价前请完成四项人工评价");
        }
        if ((request.getJudgmentLevel() == 1 || request.getJudgmentLevel() == 5 || request.getFeedbackQualityLevel() == 1 || request.getFeedbackQualityLevel() == 5
                || request.getRuleExecutionLevel() == 1 || request.getRuleExecutionLevel() == 5 || request.getProfessionalismLevel() == 1 || request.getProfessionalismLevel() == 5)
                && !StringUtils.hasText(request.getEvidence())) {
            throw new BaseException("选择严重不足或优秀时请填写评价依据");
        }
    }

    private CompetitionJudgeEvaluation newEvaluation(Long competitionId, Long judgeId) {
        return CompetitionJudgeEvaluation.builder().competitionId(competitionId).judgeAccountId(judgeId).status(STATUS_DRAFT).version(0).build();
    }

    private void checkVersion(CompetitionJudgeEvaluation existing, Integer requestVersion) {
        int expectedVersion = existing == null ? 0 : existing.getVersion();
        if (!Objects.equals(expectedVersion, requestVersion)) {
            throw new BaseException("评价已被其他管理员更新，请刷新后重试");
        }
    }

    private BigDecimal manualScore(JudgePerformanceSaveRequest request) {
        if (request.getJudgmentLevel() == null || request.getFeedbackQualityLevel() == null || request.getRuleExecutionLevel() == null || request.getProfessionalismLevel() == null) return null;
        return scale(JudgePerformanceScorePolicy.weightedLevel(request.getJudgmentLevel(), 20)
                + JudgePerformanceScorePolicy.weightedLevel(request.getFeedbackQualityLevel(), 20)
                + JudgePerformanceScorePolicy.weightedLevel(request.getRuleExecutionLevel(), 15)
                + JudgePerformanceScorePolicy.weightedLevel(request.getProfessionalismLevel(), 15));
    }

    private boolean isExcellent(JudgePerformanceVO current, BigDecimal manual, BigDecimal total) {
        return total != null && manual != null && total.compareTo(BigDecimal.valueOf(85)) >= 0 && manual.compareTo(BigDecimal.valueOf(56)) >= 0
                && current.getCompletionRate() != null && current.getCompletionRate().compareTo(BigDecimal.valueOf(100)) >= 0;
    }

    private boolean isExcellent(JudgeStats stats, BigDecimal manual, BigDecimal total) {
        return total != null && manual != null && total.compareTo(BigDecimal.valueOf(85)) >= 0 && manual.compareTo(BigDecimal.valueOf(56)) >= 0 && stats.isComplete(stats.taskTotal);
    }

    private Map<Long, String> evaluatorNames(Iterable<CompetitionJudgeEvaluation> evaluations) {
        Set<Long> ids = new HashSet<>();
        for (CompetitionJudgeEvaluation evaluation : evaluations) if (evaluation.getEvaluatedBy() != null) ids.add(evaluation.getEvaluatedBy());
        if (ids.isEmpty()) return Map.of();
        return adminUserMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(AdminUser::getId, AdminUser::getName));
    }

    private List<Long> accessibleCompetitionIds() {
        if (competitionAccessService.canAccessAllOrganizers()) return competitionMapper.selectList(null).stream().map(Competition::getId).toList();
        Long organizerId = competitionAccessService.requireCurrentOrganizerId();
        return competitionMapper.selectList(new LambdaQueryWrapper<Competition>().eq(Competition::getOrganizerId, organizerId))
                .stream().map(Competition::getId).toList();
    }

    private JudgeAccountPerformanceVO emptyHistory(String publicId) {
        return JudgeAccountPerformanceVO.builder().judgePublicId(publicId).evaluatedCompetitionCount(0).excellentCount(0).history(List.of()).build();
    }

    private void writeLog(Long competitionId, String judgePublicId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) return;
        adminOperationLogMapper.insert(AdminOperationLog.builder().adminUserId(adminId).competitionId(competitionId)
                .action("JUDGE_PERFORMANCE_UPDATE").targetType(TARGET_TYPE).targetPublicId(judgePublicId).summary(summary).build());
    }

    private String roleLabel(String role) {
        return switch (role) { case "CAPTAIN" -> "桌长"; case "CROSS" -> "跨界评审"; case "PROFESSIONAL" -> "专业评审"; default -> "评审"; };
    }

    private BigDecimal rate(int complete, int total) {
        if (total <= 0) return complete > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return scale(Math.min(100, complete * 100d / total));
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private String sampleKey(Long tableId, Long entryId, Long judgeId, String role) {
        return tableId + ":" + entryId + ":" + judgeId + ":" + role;
    }

    private static class JudgeStats {
        private Long judgeId;
        private int records;
        private int totalChars;
        private int taskTotal;
        private int participantCount;
        private double averageChars;
        private double requirementRatio;
        private double percentile;
        private double commentScore;
        private int commentRank;

        static JudgeStats empty() { return new JudgeStats(); }

        boolean isComplete(Map<Long, Integer> expected) { return isComplete(expected.getOrDefault(judgeId, 0)); }
        boolean isComplete(int total) { return total <= 0 ? records > 0 : records >= total; }
        double averageRequirementRatio() { return JudgePerformanceScorePolicy.averageRequirementRatio(requirementRatio, records); }

        JudgeStats fromEvaluation(CompetitionJudgeEvaluation evaluation) {
            JudgeStats copy = new JudgeStats();
            copy.judgeId = judgeId;
            copy.records = evaluation.getCommentRecordCount() == null ? 0 : evaluation.getCommentRecordCount();
            copy.totalChars = evaluation.getCommentTotalChars() == null ? 0 : evaluation.getCommentTotalChars();
            copy.averageChars = evaluation.getCommentAverageChars() == null ? 0 : evaluation.getCommentAverageChars();
            copy.requirementRatio = evaluation.getCommentRequirementRatio() == null ? 0 : evaluation.getCommentRequirementRatio().doubleValue();
            copy.percentile = evaluation.getCommentPercentile() == null ? 0 : evaluation.getCommentPercentile().doubleValue() / 100;
            copy.commentScore = evaluation.getCommentScore() == null ? 0 : evaluation.getCommentScore().doubleValue();
            copy.taskTotal = evaluation.getTaskTotalCount() == null ? 0 : evaluation.getTaskTotalCount();
            copy.commentRank = 0;
            return copy;
        }
    }

    private record MetricContext(Map<Long, JudgeStats> byJudge, int participantCount) {
        private MetricContext {
            byJudge.forEach((id, stats) -> { stats.judgeId = id; stats.participantCount = participantCount; });
        }
    }
}
