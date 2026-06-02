package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.service.ScoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final BeerEntryMapper beerEntryMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ScoreRecordVO createScore(JudgeScoreSaveRequest request) {
        validateDimensions(request.getDimensions());
        BeerEntry entry = requireEntry(request.getBeerUuid());
        JudgeAssignment assignment = requireAssignment(entry.getCompetitionId());
        if (!assignment.getRole().equals(request.getJudgeRoleType().name())) {
            throw new ForbiddenException("当前评审角色与提交评分类型不匹配");
        }

        ScoreRecord scoreRecord = ScoreRecord.builder()
                .competitionId(entry.getCompetitionId())
                .beerEntryId(entry.getId())
                .judgeAccountId(BaseContext.getCurrentId())
                .assignmentId(assignment.getId())
                .judgeRoleType(request.getJudgeRoleType().name())
                .dimensionsJson(writeDimensions(request.getDimensions()))
                .totalScore(request.getTotalScore())
                .comments(request.getComments())
                .finalFlag(0)
                .advancedFlag(0)
                .build();
        scoreRecordMapper.insert(scoreRecord);
        return toScoreRecordVO(scoreRecordMapper.selectById(scoreRecord.getId()));
    }

    @Override
    public ScoreRecordVO updateScore(Long scoreId, JudgeScoreUpdateRequest request) {
        validateDimensions(request.getDimensions());
        ScoreRecord scoreRecord = scoreRecordMapper.selectById(scoreId);
        if (scoreRecord == null) {
            throw new ResourceNotFoundException("评分记录不存在");
        }
        if (!scoreRecord.getJudgeAccountId().equals(BaseContext.getCurrentId())) {
            throw new ForbiddenException("无权修改该评分");
        }
        if (Integer.valueOf(1).equals(scoreRecord.getFinalFlag())) {
            throw new BaseException("桌长最终分不允许通过此接口修改");
        }
        scoreRecord.setDimensionsJson(writeDimensions(request.getDimensions()));
        scoreRecord.setTotalScore(request.getTotalScore());
        scoreRecord.setComments(request.getComments());
        scoreRecordMapper.updateById(scoreRecord);
        return toScoreRecordVO(scoreRecordMapper.selectById(scoreId));
    }

    @Override
    public List<ScoreRecordVO> listTableScores(String uuid) {
        BeerEntry entry = requireEntry(uuid);
        requireCaptainAssignment(entry.getCompetitionId());
        return scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getBeerEntryId, entry.getId())
                        .eq(ScoreRecord::getFinalFlag, 0))
                .stream()
                .map(this::toScoreRecordVO)
                .toList();
    }

    @Override
    public ScoreRecordVO finalizeTableScore(String uuid, TableScoreFinalizeRequest request) {
        validateDimensions(request.getDimensions());
        BeerEntry entry = requireEntry(uuid);
        JudgeAssignment captainAssignment = requireCaptainAssignment(entry.getCompetitionId());

        ScoreRecord finalRecord = scoreRecordMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entry.getId())
                .eq(ScoreRecord::getJudgeAccountId, BaseContext.getCurrentId())
                .eq(ScoreRecord::getFinalFlag, 1));
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
        finalRecord.setDimensionsJson(writeDimensions(request.getDimensions()));
        finalRecord.setTotalScore(request.getConsensusScore());
        finalRecord.setConsensusScore(request.getConsensusScore());
        finalRecord.setComments(request.getComments());
        finalRecord.setAdvancedFlag(Boolean.TRUE.equals(request.getAdvanced()) ? 1 : 0);

        if (finalRecord.getId() == null) {
            scoreRecordMapper.insert(finalRecord);
        } else {
            scoreRecordMapper.updateById(finalRecord);
        }
        return toScoreRecordVO(scoreRecordMapper.selectById(finalRecord.getId()));
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
        JudgeAccount account = judgeAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用，不能提交评分");
        }
        JudgeAssignment assignment = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId)
                .eq(JudgeAssignment::getJudgeAccountId, BaseContext.getCurrentId()));
        if (assignment == null) {
            throw new ForbiddenException("当前评审未分配到该比赛");
        }
        return assignment;
    }

    private JudgeAssignment requireCaptainAssignment(Long competitionId) {
        JudgeAssignment assignment = requireAssignment(competitionId);
        if (!JudgeRoleType.CAPTAIN.name().equals(assignment.getRole())) {
            throw new ForbiddenException("仅桌长可查看和提交小组最终分");
        }
        return assignment;
    }

    private ScoreRecordVO toScoreRecordVO(ScoreRecord scoreRecord) {
        return ScoreRecordVO.builder()
                .id(scoreRecord.getId())
                .judgeRoleType(scoreRecord.getJudgeRoleType())
                .dimensions(readDimensions(scoreRecord.getDimensionsJson()))
                .totalScore(scoreRecord.getTotalScore())
                .comments(scoreRecord.getComments())
                .isFinal(scoreRecord.getFinalFlag())
                .isAdvanced(scoreRecord.getAdvancedFlag())
                .consensusScore(scoreRecord.getConsensusScore())
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

    private void validateDimensions(List<DimensionRequest> dimensions) {
        if (dimensions.stream().anyMatch(item -> item.getScore() == null)) {
            throw new BaseException("评分维度分值不能为空");
        }
    }
}
