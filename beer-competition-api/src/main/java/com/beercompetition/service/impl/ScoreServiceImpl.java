package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.JudgeScoreSessionMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreStartRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryScanLabelStatus;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.JudgeScoreSession;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.service.ReviewStatsService;
import com.beercompetition.service.ScoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private static final int FLAG_FALSE = 0;
    private static final int FLAG_TRUE = 1;
    private static final BigDecimal SCORE_TOTAL = BigDecimal.valueOf(50);

    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final EntryScanLabelMapper entryScanLabelMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeScoreSessionMapper judgeScoreSessionMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final RoundResultMapper roundResultMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ReviewStatsService reviewStatsService;
    private final ObjectMapper objectMapper;

    @Override
    public ScoreConfigVO getCurrentScoreConfig(JudgeRoleType role, Long competitionId) {
        // 1) 查询当前评审与目标比赛
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        Long targetCompetitionId = competitionId == null ? resolveCurrentCompetitionId(judgeId) : competitionId;
        assertCompetitionNotArchived(targetCompetitionId);
        requireAssignment(targetCompetitionId);

        // 2) 查询并返回当前比赛角色评分表
        CompetitionScoreConfig config = competitionScoreConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, targetCompetitionId)
                .eq(CompetitionScoreConfig::getJudgeRoleType, role.name())
                .last("LIMIT 1"));
        if (config == null) {
            throw new BaseException("当前角色评分表未配置");
        }
        return toScoreConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startScore(JudgeScoreStartRequest request) {
        // 1) 校验评分表和当前评审任务
        Long judgeId = BaseContext.getCurrentId();
        BeerEntry entry = requireEntry(request.getBeerUuid());
        JudgeAssignment assignment = requireAssignment(entry.getCompetitionId());
        if (!isScoreSubmissionAllowed(assignment.getRole(), request.getJudgeRoleType().name())) {
            throw new ForbiddenException("当前评审角色与提交评分类型不匹配");
        }
        RoundTableEntry roundEntry = requireScoreRoundTask(entry.getId(), request.getJudgeRoleType().name(), false);

        // 2) 幂等记录评分开始时间
        ensureScoreSession(roundEntry, judgeId, request.getJudgeRoleType().name(), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreRecordVO createScore(JudgeScoreSaveRequest request) {
        // 1) 校验评分表和当前评审任务
        Long judgeId = BaseContext.getCurrentId();
        BeerEntry entry = requireEntry(request.getBeerUuid());
        validateScoreRequest(entry.getCompetitionId(), request.getJudgeRoleType().name(),
                request.getDimensions(), request.getTotalScore(), request.getComments());
        JudgeAssignment assignment = requireAssignment(entry.getCompetitionId());
        RoundTableEntry roundEntry = requireScoreRoundTask(entry.getId(), request.getJudgeRoleType().name(), false);
        rejectNormalScoreAfterFinal(entry.getId());
        rejectDuplicatePersonalScore(entry.getId(), judgeId);
        if (!isScoreSubmissionAllowed(assignment.getRole(), request.getJudgeRoleType().name())) {
            throw new ForbiddenException("当前评审角色与提交评分类型不匹配");
        }

        // 2) 保存个人评分记录
        LocalDateTime submittedAt = LocalDateTime.now();
        int commentCharCount = countDimensionNotes(request.getDimensions());
        JudgeScoreSession session = completeScoreSession(roundEntry, judgeId, request.getJudgeRoleType().name(), submittedAt, commentCharCount, true);
        ScoreRecord scoreRecord = ScoreRecord.builder()
                .competitionId(entry.getCompetitionId())
                .roundId(roundEntry.getRoundId())
                .roundTableId(roundEntry.getRoundTableId())
                .beerEntryId(entry.getId())
                .judgeAccountId(BaseContext.getCurrentId())
                .assignmentId(assignment.getId())
                .judgeRoleType(request.getJudgeRoleType().name())
                .dimensionsJson(writeDimensions(request.getDimensions()))
                .totalScore(request.getTotalScore())
                .comments(request.getComments())
                .finalFlag(0)
                .advancedFlag(0)
                .durationSeconds(session.getDurationSeconds())
                .commentCharCount(commentCharCount)
                .build();
        try {
            scoreRecordMapper.insert(scoreRecord);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("这款酒已经提交过评分，请返回评分页修改");
        }
        reviewStatsService.evictReviewStats(roundEntry.getRoundId(), roundEntry.getRoundTableId(), judgeId, request.getJudgeRoleType().name());
        return toScoreRecordVO(scoreRecordMapper.selectById(scoreRecord.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreRecordVO updateScore(Long scoreId, JudgeScoreUpdateRequest request) {
        // 1) 查询并校验评分记录
        ScoreRecord scoreRecord = scoreRecordMapper.selectById(scoreId);
        if (scoreRecord == null) {
            throw new ResourceNotFoundException("评分记录不存在");
        }
        BeerEntry entry = requireEntryById(scoreRecord.getBeerEntryId());
        validateScoreRequest(entry.getCompetitionId(), scoreRecord.getJudgeRoleType(),
                request.getDimensions(), request.getTotalScore(), request.getComments());
        if (!scoreRecord.getJudgeAccountId().equals(BaseContext.getCurrentId())) {
            throw new ForbiddenException("无权修改该评分");
        }
        if (Integer.valueOf(1).equals(scoreRecord.getFinalFlag())) {
            throw new BaseException("桌长最终分不允许通过此接口修改");
        }
        RoundTableEntry roundEntry = requireScoreRoundTask(scoreRecord.getBeerEntryId(), scoreRecord.getJudgeRoleType(), false);
        rejectNormalScoreAfterFinal(scoreRecord.getBeerEntryId());

        // 2) 更新个人评分内容
        int commentCharCount = countDimensionNotes(request.getDimensions());
        completeScoreSession(roundEntry, scoreRecord.getJudgeAccountId(), scoreRecord.getJudgeRoleType(), LocalDateTime.now(), commentCharCount, false);
        scoreRecord.setRoundId(roundEntry.getRoundId());
        scoreRecord.setRoundTableId(roundEntry.getRoundTableId());
        scoreRecord.setDimensionsJson(writeDimensions(request.getDimensions()));
        scoreRecord.setTotalScore(request.getTotalScore());
        scoreRecord.setComments(request.getComments());
        scoreRecord.setCommentCharCount(commentCharCount);
        scoreRecordMapper.updateById(scoreRecord);
        reviewStatsService.evictReviewStats(roundEntry.getRoundId(), roundEntry.getRoundTableId(), scoreRecord.getJudgeAccountId(), scoreRecord.getJudgeRoleType());
        return toScoreRecordVO(scoreRecordMapper.selectById(scoreId));
    }

    @Override
    public List<ScoreRecordVO> listMyScores() {
        // 1) 查询当前评审已提交的个人评分
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getJudgeAccountId, judgeId)
                        .eq(ScoreRecord::getFinalFlag, 0)
                        .orderByDesc(ScoreRecord::getUpdateTime)
                        .orderByDesc(ScoreRecord::getId))
                .stream()
                .map(this::toScoreRecordVO)
                .toList();
    }

    @Override
    public ScoreRecordVO getMyScore(String uuid) {
        // 1) 查询当前评审在该酒款下的个人评分
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        BeerEntry entry = requireEntry(uuid);
        ScoreRecord scoreRecord = scoreRecordMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entry.getId())
                .eq(ScoreRecord::getJudgeAccountId, judgeId)
                .eq(ScoreRecord::getFinalFlag, 0)
                .orderByDesc(ScoreRecord::getUpdateTime)
                .orderByDesc(ScoreRecord::getId)
                .last("LIMIT 1"));
        return scoreRecord == null ? null : toScoreRecordVO(scoreRecord);
    }

    @Override
    public List<ScoreRecordVO> listTableScores(String uuid) {
        // 1) 校验桌长权限并查询同桌原始评分
        BeerEntry entry = requireEntry(uuid);
        RoundTableEntry roundEntry = requireScoreRoundTask(entry.getId(), JudgeRoleType.CAPTAIN.name(), true);
        Set<Long> tableJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, roundEntry.getRoundTableId())
                        .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        Long captainId = BaseContext.getCurrentId();
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getBeerEntryId, entry.getId())
                        .eq(ScoreRecord::getCompetitionId, entry.getCompetitionId())
                        .orderByAsc(ScoreRecord::getFinalFlag)
                        .orderByAsc(ScoreRecord::getId))
                .stream()
                .filter(score -> isCurrentTableScore(score, tableJudgeIds, captainId))
                .map(this::toScoreRecordVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreRecordVO finalizeTableScore(String uuid, TableScoreFinalizeRequest request) {
        // 1) 校验桌长汇总权限
        BeerEntry entry = requireEntry(uuid);
        validateConsensusRequest(request.getDimensions(), request.getConsensusScore());
        applyCaptainScoreConfig(entry.getCompetitionId(), request.getDimensions(), request.getConsensusScore(), request.getComments());
        JudgeAssignment captainAssignment = requireCaptainAssignment(entry.getCompetitionId());
        RoundTableEntry roundEntry = requireScoreRoundTask(entry.getId(), JudgeRoleType.CAPTAIN.name(), true);
        requireCaptainPersonalScoreSubmitted(roundEntry, BaseContext.getCurrentId());
        requireAllTableScoresSubmitted(roundEntry);
        int commentCharCount = countDimensionNotes(request.getDimensions());

        // 2) 新增或更新桌长共识结果
        ScoreRecord finalRecord = scoreRecordMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entry.getId())
                .eq(ScoreRecord::getJudgeAccountId, BaseContext.getCurrentId())
                .eq(ScoreRecord::getFinalFlag, 1));
        String nextDimensionsJson = writeDimensions(request.getDimensions());
        boolean changed = finalRecord == null || finalScoreChanged(finalRecord, nextDimensionsJson, request);
        if (finalRecord == null) {
            finalRecord = ScoreRecord.builder()
                    .competitionId(entry.getCompetitionId())
                    .beerEntryId(entry.getId())
                    .judgeAccountId(BaseContext.getCurrentId())
                    .assignmentId(captainAssignment.getId())
                    .judgeRoleType(JudgeRoleType.CAPTAIN.name())
                    .finalFlag(1)
                    .build();
        }
        finalRecord.setDimensionsJson(nextDimensionsJson);
        finalRecord.setTotalScore(request.getConsensusScore());
        finalRecord.setConsensusScore(request.getConsensusScore());
        finalRecord.setComments(request.getComments());
        finalRecord.setAdvancedFlag(Boolean.TRUE.equals(request.getAdvanced()) ? 1 : 0);
        finalRecord.setRoundId(roundEntry.getRoundId());
        finalRecord.setRoundTableId(roundEntry.getRoundTableId());
        finalRecord.setCommentCharCount(commentCharCount);

        if (finalRecord.getId() == null) {
            try {
                scoreRecordMapper.insert(finalRecord);
            } catch (DuplicateKeyException ex) {
                throw new BaseException("本桌最终意见已存在，请刷新后修改");
            }
        } else {
            scoreRecordMapper.updateById(finalRecord);
        }

        // 3) 同步第一轮晋级状态
        syncFirstRoundAdvance(roundEntry, finalRecord);
        if (changed) {
            bumpRoundTableResultVersion(roundEntry.getRoundTableId());
        }
        reviewStatsService.evictReviewStats(roundEntry.getRoundId(), roundEntry.getRoundTableId(), BaseContext.getCurrentId(), JudgeRoleType.CAPTAIN.name());
        return toScoreRecordVO(scoreRecordMapper.selectById(finalRecord.getId()));
    }

    private JudgeScoreSession ensureScoreSession(RoundTableEntry roundEntry, Long judgeId, String judgeRoleType, LocalDateTime startedAt) {
        JudgeScoreSession session = findScoreSession(roundEntry.getRoundTableId(), roundEntry.getBeerEntryId(), judgeId, judgeRoleType);
        if (session != null) {
            return session;
        }
        session = JudgeScoreSession.builder()
                .competitionId(roundEntry.getCompetitionId())
                .roundId(roundEntry.getRoundId())
                .roundTableId(roundEntry.getRoundTableId())
                .beerEntryId(roundEntry.getBeerEntryId())
                .judgeAccountId(judgeId)
                .judgeRoleType(judgeRoleType)
                .startedAt(startedAt)
                .commentCharCount(0)
                .build();
        try {
            judgeScoreSessionMapper.insert(session);
        } catch (DuplicateKeyException ex) {
            return findScoreSession(roundEntry.getRoundTableId(), roundEntry.getBeerEntryId(), judgeId, judgeRoleType);
        }
        return session;
    }

    private JudgeScoreSession completeScoreSession(RoundTableEntry roundEntry,
                                                   Long judgeId,
                                                   String judgeRoleType,
                                                   LocalDateTime submittedAt,
                                                   int commentCharCount,
                                                   boolean firstSubmit) {
        JudgeScoreSession session = ensureScoreSession(roundEntry, judgeId, judgeRoleType, submittedAt);
        if (firstSubmit && session.getFirstSubmittedAt() == null) {
            session.setFirstSubmittedAt(submittedAt);
            session.setDurationSeconds(Math.toIntExact(Math.max(0L, Duration.between(session.getStartedAt(), submittedAt).toSeconds())));
        }
        session.setLastSubmittedAt(submittedAt);
        session.setCommentCharCount(commentCharCount);
        judgeScoreSessionMapper.updateById(session);
        return session;
    }

    private JudgeScoreSession findScoreSession(Long roundTableId, Long beerEntryId, Long judgeId, String judgeRoleType) {
        return judgeScoreSessionMapper.selectOne(new LambdaQueryWrapper<JudgeScoreSession>()
                .eq(JudgeScoreSession::getRoundTableId, roundTableId)
                .eq(JudgeScoreSession::getBeerEntryId, beerEntryId)
                .eq(JudgeScoreSession::getJudgeAccountId, judgeId)
                .eq(JudgeScoreSession::getJudgeRoleType, judgeRoleType)
                .last("LIMIT 1"));
    }

    private boolean finalScoreChanged(ScoreRecord current, String nextDimensionsJson, TableScoreFinalizeRequest request) {
        return !Objects.equals(current.getDimensionsJson(), nextDimensionsJson)
                || compareDecimal(current.getConsensusScore(), request.getConsensusScore()) != 0
                || compareDecimal(current.getTotalScore(), request.getConsensusScore()) != 0
                || !Objects.equals(String.valueOf(current.getComments() == null ? "" : current.getComments()).trim(), request.getComments().trim())
                || !Objects.equals(Objects.equals(current.getAdvancedFlag(), FLAG_TRUE), Boolean.TRUE.equals(request.getAdvanced()));
    }

    private int compareDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null || right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private void bumpRoundTableResultVersion(Long roundTableId) {
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            return;
        }
        roundTableMapper.update(null, new LambdaUpdateWrapper<RoundTable>()
                .eq(RoundTable::getId, roundTableId)
                .set(RoundTable::getResultVersion, (table.getResultVersion() == null ? 0 : table.getResultVersion()) + 1)
                .set(RoundTable::getConfirmationOverrideFlag, FLAG_FALSE)
                .set(RoundTable::getConfirmationOverrideReason, null)
                .set(RoundTable::getConfirmationOverrideBy, null)
                .set(RoundTable::getConfirmationOverrideTime, null));
    }

    private void requireAllTableScoresSubmitted(RoundTableEntry roundEntry) {
        List<RoundTableMember> requiredMembers = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundEntry.getRoundTableId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()));
        if (requiredMembers.isEmpty()) {
            return;
        }

        Set<Long> requiredJudgeIds = requiredMembers.stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        long submittedCount = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getBeerEntryId, roundEntry.getBeerEntryId())
                        .eq(ScoreRecord::getFinalFlag, FLAG_FALSE)
                        .in(ScoreRecord::getJudgeAccountId, requiredJudgeIds))
                .stream()
                .map(ScoreRecord::getJudgeAccountId)
                .distinct()
                .count();
        if (submittedCount < requiredJudgeIds.size()) {
            throw new BaseException("同桌评分未完成，暂不能提交桌长意见");
        }
    }

    private void requireCaptainPersonalScoreSubmitted(RoundTableEntry roundEntry, Long captainId) {
        Long submitted = scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, roundEntry.getBeerEntryId())
                .eq(ScoreRecord::getJudgeAccountId, captainId)
                .eq(ScoreRecord::getFinalFlag, FLAG_FALSE));
        if (submitted <= 0) {
            throw new BaseException("请先完成你的评分，再提交桌长意见");
        }
    }

    private boolean isCurrentTableScore(ScoreRecord score, Set<Long> tableJudgeIds, Long captainId) {
        if (Integer.valueOf(FLAG_TRUE).equals(score.getFinalFlag())) {
            return score.getJudgeAccountId().equals(captainId);
        }
        return tableJudgeIds.contains(score.getJudgeAccountId());
    }

    private RoundTableEntry requireScoreRoundTask(Long beerEntryId, String role, boolean captainOnly) {
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, beerEntryId)
                .orderByDesc(RoundTableEntry::getId));
        Long judgeId = BaseContext.getCurrentId();
        boolean hasScoreRound = false;
        boolean hasEditableScoreRound = false;
        boolean hasVisibleTable = false;
        for (RoundTableEntry roundEntry : roundEntries) {
            CompetitionRound round = competitionRoundMapper.selectById(roundEntry.getRoundId());
            if (round == null || !RoundType.SCORE.name().equals(round.getRoundType())) {
                continue;
            }
            assertCompetitionNotArchived(round.getCompetitionId());
            hasScoreRound = true;
            if (!isScoreRoundEditable(round.getStatus(), captainOnly)) {
                continue;
            }
            hasEditableScoreRound = true;
            RoundTable table = roundTableMapper.selectById(roundEntry.getRoundTableId());
            if (table == null || RoundStatus.LOCKED.name().equals(table.getStatus())) {
                continue;
            }
            if (!isScoreTableEditable(table.getStatus(), captainOnly)) {
                continue;
            }
            hasVisibleTable = true;
            RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                    .eq(RoundTableMember::getRoundTableId, table.getId())
                    .eq(RoundTableMember::getJudgeAccountId, judgeId)
                    .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE));
            if (member == null) {
                continue;
            }
            if (captainOnly && !JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
                continue;
            }
            if (!captainOnly && !isScoreSubmissionAllowed(member.getRole(), role)) {
                continue;
            }
            return roundEntry;
        }
        if (!hasScoreRound || !hasEditableScoreRound) {
            throw new ForbiddenException("该酒款尚未进入当前评分轮次");
        }
        if (!hasVisibleTable) {
            throw new ForbiddenException("当前轮次尚未发布评分任务");
        }
        if (captainOnly) {
            throw new ForbiddenException("仅桌长可查看和提交小组最终分");
        }
        throw new ForbiddenException("当前评审角色与评分类型不匹配");
    }

    private boolean isScoreRoundEditable(String status, boolean captainOnly) {
        return RoundStatus.PUBLISHED.name().equals(status)
                || (captainOnly && RoundStatus.SUBMITTED.name().equals(status));
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new BaseException("比赛已归档");
        }
    }

    private boolean isScoreTableEditable(String status, boolean captainOnly) {
        return RoundStatus.PUBLISHED.name().equals(status)
                || (captainOnly && RoundStatus.SUBMITTED.name().equals(status));
    }

    private boolean isScoreSubmissionAllowed(String memberRole, String requestedRole) {
        if (JudgeRoleType.CAPTAIN.name().equals(memberRole)) {
            return JudgeRoleType.PROFESSIONAL.name().equals(requestedRole);
        }
        return memberRole.equals(requestedRole);
    }

    private void syncFirstRoundAdvance(RoundTableEntry roundEntry, ScoreRecord finalRecord) {
        boolean advanced = Integer.valueOf(1).equals(finalRecord.getAdvancedFlag());
        roundEntry.setStatus(advanced ? RoundEntryStatus.ADVANCED.name() : RoundEntryStatus.ELIMINATED.name());
        roundTableEntryMapper.updateById(roundEntry);
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, roundEntry.getRoundTableId())
                .eq(RoundResult::getBeerEntryId, roundEntry.getBeerEntryId())
                .eq(RoundResult::getResultType, RoundResultType.ADVANCE.name()));
        if (advanced) {
            roundResultMapper.insert(RoundResult.builder()
                    .competitionId(roundEntry.getCompetitionId())
                    .roundId(roundEntry.getRoundId())
                    .roundTableId(roundEntry.getRoundTableId())
                    .beerEntryId(roundEntry.getBeerEntryId())
                    .resultType(RoundResultType.ADVANCE.name())
                    .submittedBy(finalRecord.getJudgeAccountId())
                    .submittedTime(LocalDateTime.now())
                    .lockedFlag(0)
                    .build());
        }
    }

    private void rejectNormalScoreAfterFinal(Long beerEntryId) {
        Long finalCount = scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getFinalFlag, 1));
        if (finalCount > 0) {
            throw new BaseException("桌长已汇总，普通评审不能再修改评分");
        }
    }

    private void rejectDuplicatePersonalScore(Long beerEntryId, Long judgeId) {
        Long existingCount = scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getJudgeAccountId, judgeId)
                .eq(ScoreRecord::getFinalFlag, 0));
        if (existingCount > 0) {
            throw new BaseException("这款酒已经提交过评分，请返回评分页修改");
        }
    }

    private BeerEntry requireEntry(String uuid) {
        BeerEntry entry = beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, uuid));
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private JudgeAssignment requireAssignment(Long competitionId) {
        Long judgeId = BaseContext.getCurrentId();
        requireActiveJudge(judgeId);
        JudgeAssignment assignment = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId)
                .eq(JudgeAssignment::getJudgeAccountId, judgeId)
                .last("LIMIT 1"));
        if (assignment == null) {
            assignment = createAssignmentFromVisibleRoundMember(competitionId, judgeId);
        }
        if (assignment == null) {
            throw new ForbiddenException("当前评审未分配到该比赛");
        }
        return assignment;
    }

    private JudgeAssignment createAssignmentFromVisibleRoundMember(Long competitionId, Long judgeId) {
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE));
        if (members.isEmpty()) {
            return null;
        }
        Map<Long, RoundTableMember> memberByTableId = members.stream()
                .collect(Collectors.toMap(RoundTableMember::getRoundTableId, item -> item, (left, right) -> left));
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getCompetitionId, competitionId)
                .in(RoundTable::getId, memberByTableId.keySet())
                .orderByDesc(RoundTable::getRoundId)
                .orderByDesc(RoundTable::getId));
        for (RoundTable table : tables) {
            RoundTableMember member = memberByTableId.get(table.getId());
            CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
            if (member == null || round == null || !isVisibleJudgeRound(round, table, member)) {
                continue;
            }
            JudgeTable baseTable = ensureJudgeTable(competitionId, table.getTableName(), table.getSortOrder());
            JudgeAssignment created = JudgeAssignment.builder()
                    .competitionId(competitionId)
                    .tableId(baseTable.getId())
                    .judgeAccountId(judgeId)
                    .role(member.getRole())
                    .build();
            judgeAssignmentMapper.insert(created);
            return created;
        }
        return null;
    }

    private boolean isVisibleJudgeRound(CompetitionRound round, RoundTable table, RoundTableMember member) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return RoundStatus.PUBLISHED.name().equals(round.getStatus())
                    && (RoundStatus.PUBLISHED.name().equals(table.getStatus())
                    || JudgeRoleType.CAPTAIN.name().equals(member.getRole()));
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return RoundStatus.IN_PROGRESS.name().equals(round.getStatus())
                    || RoundStatus.SUBMITTED.name().equals(round.getStatus());
        }
        return false;
    }

    private JudgeTable ensureJudgeTable(Long competitionId, String tableName, Integer sortOrder) {
        String normalizedName = StringUtils.hasText(tableName) ? tableName.trim() : "评审桌";
        JudgeTable table = judgeTableMapper.selectOne(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .eq(JudgeTable::getTableName, normalizedName)
                .last("LIMIT 1"));
        if (table != null) {
            return table;
        }
        JudgeTable created = JudgeTable.builder()
                .competitionId(competitionId)
                .tableName(normalizedName)
                .sortOrder(sortOrder == null ? 0 : sortOrder)
                .build();
        judgeTableMapper.insert(created);
        return created;
    }

    private Long resolveCurrentCompetitionId(Long judgeId) {
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE));
        for (RoundTableMember member : members) {
            RoundTable table = roundTableMapper.selectById(member.getRoundTableId());
            if (table == null) {
                continue;
            }
            CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
            if (round == null) {
                continue;
            }
            if (RoundType.SCORE.name().equals(round.getRoundType()) && RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
                return round.getCompetitionId();
            }
            if (RoundType.RANKING.name().equals(round.getRoundType())
                    && (RoundStatus.IN_PROGRESS.name().equals(round.getStatus()) || RoundStatus.SUBMITTED.name().equals(round.getStatus()))) {
                return round.getCompetitionId();
            }
        }
        throw new ForbiddenException("当前评审没有可用的评分任务");
    }

    private JudgeAccount requireActiveJudge(Long judgeId) {
        JudgeAccount account = judgeAccountMapper.selectById(judgeId);
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用，不能提交评分");
        }
        return account;
    }

    private JudgeAssignment requireCaptainAssignment(Long competitionId) {
        JudgeAssignment assignment = requireAssignment(competitionId);
        if (!JudgeRoleType.CAPTAIN.name().equals(assignment.getRole())) {
            throw new ForbiddenException("仅桌长可查看和提交小组最终分");
        }
        return assignment;
    }

    private ScoreRecordVO toScoreRecordVO(ScoreRecord scoreRecord) {
        BeerEntry entry = beerEntryMapper.selectById(scoreRecord.getBeerEntryId());
        CompetitionCategory category = entry == null ? null : competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entry == null ? null : entryScanLabelMapper.selectOne(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getBeerEntryId, entry.getId())
                .eq(EntryScanLabel::getStatus, EntryScanLabelStatus.ACTIVE.name())
                .last("LIMIT 1"));
        JudgeAccount judge = judgeAccountMapper.selectById(scoreRecord.getJudgeAccountId());
        return ScoreRecordVO.builder()
                .id(scoreRecord.getId())
                .beerUuid(entry == null ? null : entry.getUuid())
                .shortCode(label == null ? null : label.getShortCode())
                .categoryName(category == null ? null : category.getName())
                .style(entry == null ? null : entry.getStyle())
                .judgeName(judge == null ? null : judge.getName())
                .judgeRoleType(scoreRecord.getJudgeRoleType())
                .roleLabel(roleLabel(scoreRecord.getJudgeRoleType()))
                .mine(scoreRecord.getJudgeAccountId().equals(BaseContext.getCurrentId()))
                .dimensions(readDimensions(scoreRecord.getDimensionsJson()))
                .totalScore(scoreRecord.getTotalScore())
                .comments(scoreRecord.getComments())
                .isFinal(scoreRecord.getFinalFlag())
                .isAdvanced(scoreRecord.getAdvancedFlag())
                .consensusScore(scoreRecord.getConsensusScore())
                .locked(entry != null && hasFinalScore(entry.getId()))
                .durationSeconds(scoreRecord.getDurationSeconds())
                .commentCharCount(scoreRecord.getCommentCharCount())
                .submittedAt(scoreRecord.getUpdateTime() == null ? scoreRecord.getCreateTime() : scoreRecord.getUpdateTime())
                .build();
    }

    private boolean hasFinalScore(Long beerEntryId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getFinalFlag, 1)) > 0;
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

    private ScoreConfigVO toScoreConfigVO(CompetitionScoreConfig config) {
        return ScoreConfigVO.builder()
                .competitionId(config.getCompetitionId())
                .judgeRoleType(config.getJudgeRoleType())
                .minCommentLength(config.getMinCommentLength())
                .dimensions(readDimensions(config.getDimensionsJson()))
                .build();
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析评分维度失败", ex);
        }
    }

    private String writeDimensions(List<DimensionRequest> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("保存评分维度失败", ex);
        }
    }

    private int countDimensionNotes(List<DimensionRequest> dimensions) {
        return dimensions.stream()
                .map(DimensionRequest::getNote)
                .mapToInt(this::countEffectiveChars)
                .sum();
    }

    private void validateDimensions(List<DimensionRequest> dimensions) {
        if (dimensions.stream().anyMatch(item -> item.getScore() == null)) {
            throw new BaseException("评分维度分值不能为空");
        }
    }

    private void validateScoreRequest(Long competitionId,
                                      String judgeRoleType,
                                      List<DimensionRequest> dimensions,
                                      BigDecimal totalScore,
                                      String comments) {
        validateDimensions(dimensions);
        CompetitionScoreConfig config = competitionScoreConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                .eq(CompetitionScoreConfig::getJudgeRoleType, judgeRoleType)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BaseException("当前角色评分表未配置");
        }
        List<DimensionRequest> configuredDimensions = readDimensions(config.getDimensionsJson());
        Map<String, DimensionRequest> configuredByKey = configuredDimensions.stream()
                .collect(Collectors.toMap(DimensionRequest::getKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        if (dimensions.size() != configuredByKey.size()) {
            throw new BaseException("评分维度与当前评分表不一致，请刷新后重试");
        }
        BigDecimal computedTotal = BigDecimal.ZERO;
        for (DimensionRequest item : dimensions) {
            DimensionRequest configured = configuredByKey.get(item.getKey());
            if (configured == null) {
                throw new BaseException("评分维度与当前评分表不一致，请刷新后重试");
            }
            BigDecimal score = item.getScore();
            BigDecimal maxScore = configured.getMaxScore();
            if (!isIntegerScore(score)) {
                throw new BaseException(configured.getLabel() + "必须填写整数分");
            }
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
                throw new BaseException(configured.getLabel() + "分值必须在 0-" + maxScore.stripTrailingZeros().toPlainString() + " 之间");
            }
            item.setLabel(configured.getLabel());
            item.setMaxScore(maxScore);
            item.setNotePrompt(configured.getNotePrompt());
            item.setNote(normalizeNullable(item.getNote()));
            computedTotal = computedTotal.add(score);
        }
        if (totalScore == null || computedTotal.compareTo(totalScore) != 0) {
            throw new BaseException("总分与各维度分值合计不一致");
        }
        if (computedTotal.compareTo(SCORE_TOTAL) > 0) {
            throw new BaseException("总分不能超过 50 分");
        }
        int minCommentLength = config.getMinCommentLength() == null ? 0 : config.getMinCommentLength();
        if (countEffectiveChars(comments) < minCommentLength) {
            throw new BaseException("文字反馈至少 " + minCommentLength + " 字");
        }
    }

    private void validateConsensusRequest(List<DimensionRequest> dimensions, BigDecimal consensusScore) {
        validateDimensions(dimensions);
        if (dimensions.size() != 1) {
            throw new BaseException("桌长评分表只能提交 1 个共识评分维度");
        }
        if (consensusScore == null || !isIntegerScore(consensusScore)) {
            throw new BaseException("共识分必须填写整数分");
        }
        if (consensusScore.compareTo(BigDecimal.ZERO) < 0 || consensusScore.compareTo(SCORE_TOTAL) > 0) {
            throw new BaseException("共识分必须在 0-50 分之间");
        }
        BigDecimal dimensionScore = dimensions.get(0).getScore();
        if (dimensionScore == null || dimensionScore.compareTo(consensusScore) != 0) {
            throw new BaseException("共识分与评分维度分值不一致");
        }
    }

    private void applyCaptainScoreConfig(Long competitionId,
                                         List<DimensionRequest> dimensions,
                                         BigDecimal consensusScore,
                                         String comments) {
        CompetitionScoreConfig config = competitionScoreConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                .eq(CompetitionScoreConfig::getJudgeRoleType, JudgeRoleType.CAPTAIN.name())
                .last("LIMIT 1"));
        if (config == null) {
            throw new BaseException("桌长评分表未配置");
        }
        List<DimensionRequest> configuredDimensions = readDimensions(config.getDimensionsJson());
        if (configuredDimensions.size() != 1) {
            throw new BaseException("桌长评分表只能配置 1 个共识评分维度");
        }
        DimensionRequest configured = configuredDimensions.get(0);
        if (configured.getMaxScore() == null || configured.getMaxScore().compareTo(SCORE_TOTAL) != 0) {
            throw new BaseException("桌长评分表总分必须等于 50 分");
        }
        int minCommentLength = config.getMinCommentLength() == null ? 0 : config.getMinCommentLength();
        if (countEffectiveChars(comments) < minCommentLength) {
            throw new BaseException("桌长综合评语至少 " + minCommentLength + " 字");
        }
        DimensionRequest dimension = dimensions.get(0);
        if (consensusScore.compareTo(configured.getMaxScore()) > 0) {
            throw new BaseException("共识分必须在 0-" + configured.getMaxScore().stripTrailingZeros().toPlainString() + " 分之间");
        }
        dimension.setKey(configured.getKey());
        dimension.setLabel(configured.getLabel());
        dimension.setMaxScore(configured.getMaxScore());
        dimension.setNotePrompt(configured.getNotePrompt());
        dimension.setNote(normalizeNullable(dimension.getNote()));
    }

    private boolean isIntegerScore(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }

    private int countEffectiveChars(String text) {
        return StringUtils.hasText(text) ? text.replaceAll("\\s+", "").length() : 0;
    }

    private String normalizeNullable(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private BeerEntry requireEntryById(Long beerEntryId) {
        BeerEntry entry = beerEntryMapper.selectById(beerEntryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }
}
