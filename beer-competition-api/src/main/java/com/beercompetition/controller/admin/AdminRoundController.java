package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.judging.assignment.RoundAllocationService;
import com.beercompetition.judging.round.RoundLifecycleService;
import com.beercompetition.judging.round.RoundQueryService;
import com.beercompetition.judging.scoring.ScoreConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台轮次编排接口，处理评审轮次创建、发布、锁定和结果发布。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions/{id}")
public class AdminRoundController {

    private final CompetitionQueryService competitionQueryService;
    private final RoundAllocationService roundAllocationService;
    private final RoundLifecycleService roundLifecycleService;
    private final RoundQueryService roundQueryService;
    private final ScoreConfirmationService scoreConfirmationService;

    /**
     * 创建指定比赛的第一轮评审。
     */
    @PostMapping("/rounds/first")
    public Result<CompetitionDetailVO> createFirstRound(@PathVariable Long id,
                                                        @RequestBody @Valid FirstRoundCreateRequest request) {
        roundAllocationService.createFirstRound(id, request);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 保存指定轮次的桌次和酒款分配。
     */
    @PutMapping("/rounds/{roundId}/allocation")
    public Result<CompetitionDetailVO> saveRoundAllocation(@PathVariable Long id,
                                                           @PathVariable Long roundId,
                                                           @RequestBody @Valid RoundAllocationRequest request) {
        roundAllocationService.saveRoundAllocation(id, roundId, request);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 发布轮次任务给评委端。
     */
    @PostMapping("/rounds/{roundId}/publish")
    public Result<CompetitionDetailVO> publishRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundLifecycleService.publishRound(id, roundId);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 完成第一轮评分并生成晋级结果。
     */
    @PostMapping("/rounds/{roundId}/complete-first-round")
    public Result<CompetitionDetailVO> completeFirstRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundLifecycleService.completeFirstRound(id, roundId);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 基于上一轮结果创建后续排序轮。
     */
    @PostMapping("/rounds/next")
    public Result<CompetitionDetailVO> createNextRound(@PathVariable Long id,
                                                       @RequestBody @Valid NextRoundCreateRequest request) {
        roundAllocationService.createNextRound(id, request);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 同步排序轮候选酒款，补齐未分配候选。
     */
    @PostMapping("/rounds/{roundId}/sync-candidates")
    public Result<CompetitionDetailVO> syncRoundCandidates(@PathVariable Long id, @PathVariable Long roundId) {
        roundAllocationService.syncRoundCandidates(id, roundId);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 删除尚未发布的草稿轮次。
     */
    @DeleteMapping("/rounds/{roundId}")
    public Result<CompetitionDetailVO> deleteDraftRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundAllocationService.deleteDraftRound(id, roundId);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 锁定排序轮结果。
     */
    @PostMapping("/rounds/{roundId}/lock")
    public Result<CompetitionDetailVO> lockRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundLifecycleService.lockRound(id, roundId);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 后台代处理桌次确认异常。
     */
    @PostMapping("/round-tables/{roundTableId}/confirmation-override")
    public Result<CompetitionDetailVO> overrideRoundTableConfirmation(@PathVariable Long id,
                                                                      @PathVariable Long roundTableId,
                                                                      @RequestBody @Valid AdminConfirmationOverrideRequest request) {
        scoreConfirmationService.overrideScoreConfirmation(id, roundTableId, request);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 构建发布前的成绩草稿。
     */
    @GetMapping("/results/draft")
    public Result<List<ResultDraftVO>> resultDraft(@PathVariable Long id) {
        return Result.success(roundQueryService.buildResultDrafts(id));
    }

    /**
     * 发布比赛最终成绩。
     */
    @PostMapping("/results/publish")
    public Result<CompetitionDetailVO> publishResults(@PathVariable Long id) {
        roundLifecycleService.publishResults(id);
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }
}
