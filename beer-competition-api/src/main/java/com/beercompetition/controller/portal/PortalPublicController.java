package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 厂商端公开接口，提供无需登录即可查看的赛事和获奖信息。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/public")
public class PortalPublicController {

    private final CompetitionService competitionService;
    private final EntryService entryService;

    /**
     * 查询厂商端首页展示数据。
     */
    @GetMapping("/home")
    public Result<PortalHomeVO> home() {
        return Result.success(competitionService.getPortalHome());
    }

    /**
     * 查询厂商端可展示的赛事列表。
     */
    @GetMapping("/competitions")
    public Result<List<PortalCompetitionVO>> publicCompetitions() {
        return Result.success(competitionService.listPortalCompetitions());
    }

    /**
     * 查询厂商端赛事详情。
     */
    @GetMapping("/competitions/{id}")
    public Result<PortalCompetitionVO> publicCompetitionDetail(@PathVariable Long id) {
        return Result.success(competitionService.getPortalCompetitionDetail(id));
    }

    /**
     * 查询已发布成绩的赛事列表。
     */
    @GetMapping("/results")
    public Result<List<PortalCompetitionResultVO>> publicResults() {
        return Result.success(entryService.listPublishedCompetitionResults());
    }

    /**
     * 查询指定赛事的公开获奖结果。
     */
    @GetMapping("/results/{competitionId}")
    public Result<PortalCompetitionResultVO> publicResultDetail(@PathVariable Long competitionId) {
        return Result.success(entryService.getPublishedCompetitionResult(competitionId));
    }
}
