package com.beercompetition.competition.command;

import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionRefundPolicyUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;

/**
 * 负责赛事创建、删除和基础资料修改。
 */
public interface CompetitionCommandService {

    CompetitionVO createCompetition(CompetitionCreateRequest request);

    void deleteCompetition(Long id);

    CompetitionDetailVO updateBaseInfo(Long id, CompetitionBaseInfoUpdateRequest request);

    CompetitionDetailVO updateRefundPolicy(Long id, CompetitionRefundPolicyUpdateRequest request);

    /**
     * 设置厂商端首页重点赛事，competitionId 为空时取消手动指定。
     */
    void setHomeFeaturedCompetition(Long competitionId);
}
