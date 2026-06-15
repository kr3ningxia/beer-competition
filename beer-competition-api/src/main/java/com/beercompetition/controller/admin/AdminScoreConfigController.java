package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台评分表配置接口，维护比赛各评审角色的评分维度。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/score-configs")
public class AdminScoreConfigController {

    private final CompetitionService competitionService;

    /**
     * 查询指定比赛的评分表配置。
     */
    @GetMapping("/{competitionId}")
    public Result<List<ScoreConfigVO>> getScoreConfigs(@PathVariable Long competitionId) {
        return Result.success(competitionService.getScoreConfigs(competitionId));
    }

    /**
     * 批量更新指定比赛的评分表配置。
     */
    @PutMapping("/{competitionId}")
    public Result<List<ScoreConfigVO>> updateScoreConfigs(@PathVariable Long competitionId,
                                                          @RequestBody @Valid ScoreConfigBatchUpdateRequest request) {
        return Result.success(competitionService.updateScoreConfigs(competitionId, request));
    }
}
