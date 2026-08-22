package com.beercompetition.judging.round;

import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.common.result.PageResult;

import java.util.List;

/**
 * 提供赛事轮次、候选池和结果草稿的只读查询。
 */
public interface RoundQueryService {

    List<CompetitionRoundVO> listCompetitionRounds(Long competitionId);

    List<CompetitionEntryVO> listEntryPool(Long competitionId);

    PageResult<CompetitionEntryVO> listEntryPoolPage(Long competitionId, Integer page, Integer pageSize);

    List<ResultDraftVO> buildResultDrafts(Long competitionId);
}
