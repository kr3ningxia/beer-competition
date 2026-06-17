package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.AdminFeedbackCommentUpdateRequest;
import com.beercompetition.pojo.dto.AdminFeedbackDimensionNoteRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.service.AdminFeedbackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private static final int FLAG_TRUE = 1;
    private static final int FLAG_FALSE = 0;
    private static final int COMMENT_MAX_LENGTH = 1000;
    private static final int LOG_SUMMARY_MAX_LENGTH = 255;
    private static final String TARGET_SCORE_RECORD = "SCORE_RECORD";
    private static final String ACTION_SCORE_COMMENT_UPDATE = "SCORE_COMMENT_UPDATE";

    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScoreRecordComments(Long competitionId, Long scoreRecordId, AdminFeedbackCommentUpdateRequest request) {
        // 1) 参数规范化与前置校验
        String comments = normalizeRequired(request.getComments(), "评价内容不能为空");
        String reason = normalizeRequired(request.getReason(), "请填写修改原因");
        Competition competition = requireCompetition(competitionId);
        assertCompetitionAllowsFeedbackEdit(competition);
        ScoreRecord scoreRecord = requireScoreRecord(scoreRecordId);
        assertScoreRecordBelongsToCompetition(scoreRecord, competition.getId());
        assertUpdateTimeFresh(scoreRecord, request.getExpectedUpdatedAt());

        // 2) 查询首轮评分上下文并校验记录归属
        CompetitionRound firstScoreRound = requireFirstScoreRound(competition.getId());
        RoundTableEntry roundEntry = requireFirstRoundEntry(firstScoreRound.getId(), scoreRecord.getBeerEntryId());
        RoundTable table = requireRoundTable(roundEntry.getRoundTableId());
        assertScoreRecordBelongsToRoundTable(scoreRecord, table);

        // 3) 更新评价内容
        boolean finalScore = Objects.equals(scoreRecord.getFinalFlag(), FLAG_TRUE);
        String oldComments = scoreRecord.getComments();
        String oldDimensionsJson = scoreRecord.getDimensionsJson();
        if (finalScore) {
            updateCaptainComments(scoreRecord, table, comments);
        } else {
            updatePersonalComments(scoreRecord, comments, request.getDimensionNotes());
        }
        int commentCharCount = countScoreRecordCommentChars(scoreRecord);
        validateMinCommentLength(competition.getId(), finalScore, scoreRecord.getJudgeRoleType(), commentCharCount);
        scoreRecord.setCommentCharCount(commentCharCount);
        scoreRecordMapper.updateById(scoreRecord);
        if (finalScore && scoreChanged(oldComments, oldDimensionsJson, scoreRecord) && isConfirmationOpen(firstScoreRound, table)) {
            bumpRoundTableResultVersion(table);
        }

        // 4) 写入后台操作摘要
        writeAdminLog(scoreRecord, finalScore, reason);
    }

    private void updateCaptainComments(ScoreRecord scoreRecord, RoundTable table, String comments) {
        if (!Objects.equals(table.getCaptainJudgeId(), scoreRecord.getJudgeAccountId())) {
            throw new ForbiddenException("只能修改本桌桌长提交的最终意见");
        }
        List<DimensionRequest> dimensions = readDimensions(scoreRecord.getDimensionsJson());
        if (!dimensions.isEmpty()) {
            dimensions.get(0).setNote(comments);
            scoreRecord.setDimensionsJson(writeDimensions(dimensions));
        }
        scoreRecord.setComments(comments);
    }

    private void updatePersonalComments(ScoreRecord scoreRecord,
                                        String fallbackComments,
                                        List<AdminFeedbackDimensionNoteRequest> dimensionNotes) {
        if (!Objects.equals(scoreRecord.getFinalFlag(), FLAG_FALSE)) {
            throw new ForbiddenException("只能修改评审原始评分或桌长最终意见");
        }
        List<DimensionRequest> dimensions = readDimensions(scoreRecord.getDimensionsJson());
        if (!dimensions.isEmpty()) {
            if (dimensionNotes == null || dimensionNotes.isEmpty()) {
                throw new BaseException("请按评分维度提交评语");
            }
            mergeDimensionNotes(dimensions, dimensionNotes);
            String nextComments = buildComments(dimensions);
            validateCommentLength(nextComments);
            scoreRecord.setDimensionsJson(writeDimensions(dimensions));
            scoreRecord.setComments(nextComments);
            return;
        }
        scoreRecord.setComments(fallbackComments);
    }

    private void mergeDimensionNotes(List<DimensionRequest> dimensions, List<AdminFeedbackDimensionNoteRequest> dimensionNotes) {
        Map<String, DimensionRequest> dimensionByKey = dimensions.stream()
                .collect(Collectors.toMap(DimensionRequest::getKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<String> allowedKeys = dimensionByKey.keySet();
        for (AdminFeedbackDimensionNoteRequest noteRequest : dimensionNotes) {
            String key = normalizeRequired(noteRequest.getKey(), "评分维度不能为空");
            if (!allowedKeys.contains(key)) {
                throw new BaseException("评分维度已变化，请刷新后再修改");
            }
            DimensionRequest dimension = dimensionByKey.get(key);
            dimension.setNote(normalizeNullable(noteRequest.getNote()));
        }
    }

    private String buildComments(List<DimensionRequest> dimensions) {
        String comments = dimensions.stream()
                .map(dimension -> {
                    String note = normalizeNullable(dimension.getNote());
                    if (!StringUtils.hasText(note)) {
                        return "";
                    }
                    return firstText(dimension.getLabel(), firstText(dimension.getKey(), "评分维度")) + "：" + note;
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return normalizeRequired(comments, "评价内容不能为空");
    }

    private void assertScoreRecordBelongsToCompetition(ScoreRecord scoreRecord, Long competitionId) {
        if (!Objects.equals(scoreRecord.getCompetitionId(), competitionId)) {
            throw new ForbiddenException("评分记录不属于当前比赛");
        }
    }

    private void assertScoreRecordBelongsToRoundTable(ScoreRecord scoreRecord, RoundTable table) {
        if (Objects.equals(scoreRecord.getJudgeAccountId(), table.getCaptainJudgeId())) {
            return;
        }
        Long count = roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getJudgeAccountId, scoreRecord.getJudgeAccountId()));
        if (count == null || count == 0) {
            throw new ForbiddenException("评分记录不属于当前评审桌");
        }
    }

    private void assertUpdateTimeFresh(ScoreRecord scoreRecord, LocalDateTime expectedUpdatedAt) {
        if (expectedUpdatedAt == null) {
            return;
        }
        LocalDateTime currentUpdatedAt = firstTime(scoreRecord.getUpdateTime(), scoreRecord.getCreateTime());
        if (currentUpdatedAt != null && !currentUpdatedAt.equals(expectedUpdatedAt)) {
            throw new BaseException("评价已被更新，请刷新后再修改");
        }
    }

    private Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private void assertCompetitionAllowsFeedbackEdit(Competition competition) {
        if (CompetitionStatus.PUBLISHED.name().equals(competition.getStatus())
                || CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new ForbiddenException("结果已发布，不能修改评审评价");
        }
    }

    private ScoreRecord requireScoreRecord(Long scoreRecordId) {
        ScoreRecord scoreRecord = scoreRecordMapper.selectById(scoreRecordId);
        if (scoreRecord == null) {
            throw new ResourceNotFoundException("评分记录不存在");
        }
        return scoreRecord;
    }

    private CompetitionRound requireFirstScoreRound(Long competitionId) {
        CompetitionRound firstScoreRound = competitionRoundMapper.selectOne(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundNo, 1)
                .eq(CompetitionRound::getRoundType, RoundType.SCORE.name())
                .orderByAsc(CompetitionRound::getSortOrder)
                .orderByAsc(CompetitionRound::getId)
                .last("LIMIT 1"));
        if (firstScoreRound == null) {
            throw new ResourceNotFoundException("首轮评分轮次不存在");
        }
        return firstScoreRound;
    }

    private RoundTableEntry requireFirstRoundEntry(Long roundId, Long beerEntryId) {
        RoundTableEntry roundEntry = roundTableEntryMapper.selectOne(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, roundId)
                .eq(RoundTableEntry::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
        if (roundEntry == null) {
            throw new ForbiddenException("评分记录不属于首轮评分酒款");
        }
        return roundEntry;
    }

    private RoundTable requireRoundTable(Long roundTableId) {
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            throw new ResourceNotFoundException("评审桌不存在");
        }
        return table;
    }

    private List<DimensionRequest> readDimensions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分明细失败");
        }
    }

    private String writeDimensions(List<DimensionRequest> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存评分明细失败");
        }
    }

    private boolean scoreChanged(String oldComments, String oldDimensionsJson, ScoreRecord scoreRecord) {
        return !Objects.equals(oldComments, scoreRecord.getComments())
                || !Objects.equals(oldDimensionsJson, scoreRecord.getDimensionsJson());
    }

    private boolean isConfirmationOpen(CompetitionRound round, RoundTable table) {
        return Set.of(RoundStatus.PUBLISHED.name(), RoundStatus.SUBMITTED.name()).contains(round.getStatus())
                && Set.of(RoundStatus.PUBLISHED.name(), RoundStatus.SUBMITTED.name()).contains(table.getStatus());
    }

    private void bumpRoundTableResultVersion(RoundTable table) {
        roundTableMapper.update(null, new LambdaUpdateWrapper<RoundTable>()
                .eq(RoundTable::getId, table.getId())
                .set(RoundTable::getResultVersion, (table.getResultVersion() == null ? 0 : table.getResultVersion()) + 1)
                .set(RoundTable::getConfirmationOverrideFlag, FLAG_FALSE)
                .set(RoundTable::getConfirmationOverrideReason, null)
                .set(RoundTable::getConfirmationOverrideBy, null)
                .set(RoundTable::getConfirmationOverrideTime, null));
    }

    private void writeAdminLog(ScoreRecord scoreRecord, boolean finalScore, String reason) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        String recordType = finalScore ? "桌长综合评语" : "评委原始评语";
        String summary = truncate("更新" + recordType + "，酒款ID " + scoreRecord.getBeerEntryId() + "，原因：" + reason);
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(ACTION_SCORE_COMMENT_UPDATE)
                .targetType(TARGET_SCORE_RECORD)
                .targetPublicId(String.valueOf(scoreRecord.getId()))
                .summary(summary)
                .build());
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BaseException(message);
        }
        validateCommentLength(normalized);
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateCommentLength(String value) {
        if (value != null && value.length() > COMMENT_MAX_LENGTH) {
            throw new BaseException("评价内容不能超过 1000 字");
        }
    }

    private void validateMinCommentLength(Long competitionId, boolean finalScore, String judgeRoleType, int commentCharCount) {
        String role = finalScore ? "CAPTAIN" : judgeRoleType;
        int minCommentLength = competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .eq(CompetitionScoreConfig::getJudgeRoleType, role))
                .stream()
                .findFirst()
                .map(CompetitionScoreConfig::getMinCommentLength)
                .filter(Objects::nonNull)
                .orElse(0);
        if (minCommentLength > 0 && commentCharCount < minCommentLength) {
            throw new BaseException((finalScore ? "桌长综合评语" : "评价内容") + "至少 " + minCommentLength + " 字");
        }
    }

    private int countScoreRecordCommentChars(ScoreRecord scoreRecord) {
        List<DimensionRequest> dimensions = readDimensions(scoreRecord.getDimensionsJson());
        String dimensionText = dimensions.stream()
                .map(DimensionRequest::getNote)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        String text = StringUtils.hasText(dimensionText) ? dimensionText : scoreRecord.getComments();
        return countEffectiveChars(text);
    }

    private int countEffectiveChars(String text) {
        return StringUtils.hasText(text) ? text.replaceAll("\\s+", "").length() : 0;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private LocalDateTime firstTime(LocalDateTime primary, LocalDateTime fallback) {
        return primary == null ? fallback : primary;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= LOG_SUMMARY_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LOG_SUMMARY_MAX_LENGTH);
    }
}
