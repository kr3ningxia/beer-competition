package com.beercompetition.competition.query;

import com.beercompetition.pojo.vo.CompetitionAnalyticsVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionProgressVO;
import com.beercompetition.pojo.vo.CompetitionQuickSummaryVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.common.result.PageResult;

import java.util.List;

/**
 * 提供赛事工作台和厂商端使用的只读查询。
 *
 * <p>查询服务不得推进赛事状态或修改赛事配置。</p>
 */
public interface CompetitionQueryService {

    List<CompetitionVO> listCompetitions(boolean includeArchived);

    PortalHomeVO getPortalHome();

    List<PortalCompetitionVO> listPortalCompetitions();

    PortalCompetitionVO getPortalCompetitionDetail(Long id);

    CompetitionDetailVO getCompetitionDetail(Long id);

    CompetitionDetailVO getCompetitionOverview(Long id);

    CompetitionProgressVO getCompetitionProgress(Long id);

    List<CompetitionEntryVO> getCompetitionEntryPool(Long id);

    PageResult<CompetitionEntryVO> getCompetitionEntryPoolPage(Long id, Integer page, Integer pageSize);

    CompetitionQuickSummaryVO getCompetitionQuickSummary(Long id);

    CompetitionAnalyticsVO getCompetitionAnalytics(Long competitionId);
}
