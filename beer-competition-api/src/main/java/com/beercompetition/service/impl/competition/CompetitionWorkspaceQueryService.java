package com.beercompetition.service.impl.competition;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.service.AwardService;
import com.beercompetition.judging.round.RoundQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按比赛详情标签加载酒款、轮次和结果工作区数据。
 */
@Service
@RequiredArgsConstructor
public class CompetitionWorkspaceQueryService {

    private final RoundQueryService roundQueryService;
    private final AwardService awardService;

    public List<CompetitionEntryVO> listEntryPool(Long competitionId) {
        return roundQueryService.listEntryPool(competitionId);
    }

    public PageResult<CompetitionEntryVO> listEntryPoolPage(Long competitionId, Integer page, Integer pageSize) {
        return roundQueryService.listEntryPoolPage(competitionId, page, pageSize);
    }

    public List<CompetitionRoundVO> listRounds(Long competitionId) {
        return roundQueryService.listCompetitionRounds(competitionId);
    }

    public List<ResultDraftVO> listResultDrafts(Long competitionId) {
        return roundQueryService.buildResultDrafts(competitionId);
    }

    public List<AwardRuleVO> listAwardRules(Long competitionId) {
        return awardService.listAwardRules(competitionId);
    }

    public List<AwardResultVO> listAwardResults(Long competitionId) {
        return awardService.listAwardResults(competitionId);
    }

    public CompetitionDetailVO enrichFullDetail(CompetitionDetailVO detail) {
        Long competitionId = detail.getId();
        List<CompetitionRoundVO> rounds = listRounds(competitionId);
        List<CompetitionEntryVO> entryPool = listEntryPool(competitionId);
        detail.setEntries(entryPool);
        detail.setEntryPool(entryPool);
        detail.setRounds(rounds);
        detail.setCurrentRound(rounds.isEmpty() ? null : rounds.get(rounds.size() - 1));
        detail.setResultDrafts(listResultDrafts(competitionId));
        detail.setAwardRules(listAwardRules(competitionId));
        detail.setAwardResults(listAwardResults(competitionId));
        return detail;
    }
}
