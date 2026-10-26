package com.beercompetition.competition.configuration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 按赛事、组别和评审角色解析评分表；组别没有专属表时回退通用表。
 */
@Component
@RequiredArgsConstructor
public class ScoreConfigResolver {

    public static final long COMMON_CATEGORY_ID = 0L;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    public CompetitionScoreConfig resolve(Long competitionId, Long categoryId, JudgeRoleType role) {
        if (categoryId != null && categoryId > 0) {
            CompetitionScoreConfig scoped = selectOne(competitionId, categoryId, role);
            if (scoped != null) {
                return scoped;
            }
        }
        return selectOne(competitionId, COMMON_CATEGORY_ID, role);
    }

    private CompetitionScoreConfig selectOne(Long competitionId, Long categoryId, JudgeRoleType role) {
        return competitionScoreConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                .eq(CompetitionScoreConfig::getCategoryId, categoryId)
                .eq(CompetitionScoreConfig::getJudgeRoleType, role.name())
                .last("LIMIT 1"));
    }
}
