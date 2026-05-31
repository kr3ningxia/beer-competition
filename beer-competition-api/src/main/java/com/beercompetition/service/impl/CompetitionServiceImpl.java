package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.CompetitionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionMapper competitionMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<CompetitionVO> listCompetitions() {
        return competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                        .orderByDesc(Competition::getCompetitionDate))
                .stream()
                .map(this::toCompetitionVO)
                .toList();
    }

    @Override
    public CompetitionVO createCompetition(CompetitionCreateRequest request) {
        Competition competition = Competition.builder()
                .name(request.getName())
                .competitionDate(request.getCompetitionDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .status(request.getStatus().name())
                .entryFee(request.getEntryFee())
                .build();
        competitionMapper.insert(competition);
        return toCompetitionVO(competitionMapper.selectById(competition.getId()));
    }

    @Override
    public List<ScoreConfigVO> getScoreConfigs(Long competitionId) {
        return competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId))
                .stream()
                .map(this::toScoreConfigVO)
                .toList();
    }

    @Override
    public List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request) {
        competitionScoreConfigMapper.delete(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId));
        for (ScoreConfigItemRequest item : request.getConfigs()) {
            CompetitionScoreConfig config = CompetitionScoreConfig.builder()
                    .competitionId(competitionId)
                    .judgeRoleType(item.getJudgeRoleType().name())
                    .dimensionsJson(writeDimensions(item.getDimensions()))
                    .build();
            competitionScoreConfigMapper.insert(config);
        }
        return getScoreConfigs(competitionId);
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

    private ScoreConfigVO toScoreConfigVO(CompetitionScoreConfig config) {
        return ScoreConfigVO.builder()
                .competitionId(config.getCompetitionId())
                .judgeRoleType(config.getJudgeRoleType())
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
}
