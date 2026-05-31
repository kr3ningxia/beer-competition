package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final CompetitionMapper competitionMapper;

    @Override
    public List<JudgeAccountVO> listJudges() {
        return judgeAccountMapper.selectList(new LambdaQueryWrapper<JudgeAccount>()
                        .orderByAsc(JudgeAccount::getId))
                .stream()
                .map(judge -> JudgeAccountVO.builder()
                        .id(judge.getId())
                        .name(judge.getName())
                        .phone(judge.getPhone())
                        .wechat(judge.getWechat())
                        .qualification(judge.getQualification())
                        .status(judge.getStatus())
                        .build())
                .toList();
    }

    @Override
    public void createAssignment(JudgeAssignmentCreateRequest request) {
        if (judgeAccountMapper.selectById(request.getJudgeAccountId()) == null) {
            throw new ResourceNotFoundException("评审不存在");
        }
        if (competitionMapper.selectById(request.getCompetitionId()) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        JudgeTable judgeTable = judgeTableMapper.selectById(request.getTableId());
        if (judgeTable == null || !judgeTable.getCompetitionId().equals(request.getCompetitionId())) {
            throw new BaseException("桌次不存在或不属于当前比赛");
        }
        JudgeAssignment existing = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, request.getCompetitionId())
                .eq(JudgeAssignment::getJudgeAccountId, request.getJudgeAccountId()));
        if (existing != null) {
            existing.setTableId(request.getTableId());
            existing.setRole(request.getRole().name());
            judgeAssignmentMapper.updateById(existing);
            return;
        }
        judgeAssignmentMapper.insert(JudgeAssignment.builder()
                .competitionId(request.getCompetitionId())
                .judgeAccountId(request.getJudgeAccountId())
                .tableId(request.getTableId())
                .role(request.getRole().name())
                .build());
    }

    @Override
    public List<CompetitionVO> listMyCompetitions() {
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getJudgeAccountId, BaseContext.getCurrentId()));
        Set<Long> competitionIds = assignments.stream().map(JudgeAssignment::getCompetitionId).collect(java.util.stream.Collectors.toSet());
        if (competitionIds.isEmpty()) {
            return List.of();
        }
        return competitionMapper.selectBatchIds(competitionIds).stream()
                .map(this::toCompetitionVO)
                .toList();
    }

    private CompetitionVO toCompetitionVO(Competition competition) {
        return CompetitionVO.builder()
                .id(competition.getId())
                .name(competition.getName())
                .competitionDate(competition.getCompetitionDate())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .build();
    }
}
