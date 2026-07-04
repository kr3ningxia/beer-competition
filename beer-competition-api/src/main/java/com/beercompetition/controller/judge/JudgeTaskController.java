package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.RoundTableConfirmationRequest;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评委任务接口，承接评审桌任务、评分确认和排序确认流程。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/judge")
public class JudgeTaskController {

    private final RoundService roundService;

    /**
     * 查询当前评委的评审任务列表。
     */
    @GetMapping("/tasks")
    public Result<List<JudgeTaskVO>> tasks() {
        return Result.success(roundService.listMyTasks());
    }

    /**
     * 查询当前评委可访问的评审桌详情。
     */
    @GetMapping("/round-tables/{roundTableId}")
    public Result<JudgeRoundTableVO> roundTable(@PathVariable Long roundTableId) {
        return Result.success(roundService.getMyRoundTable(roundTableId));
    }

    /**
     * 桌长提交评分桌汇总。
     */
    @PostMapping("/round-tables/{roundTableId}/score-submit")
    public Result<String> submitScoreRoundTable(@PathVariable Long roundTableId) {
        roundService.submitScoreRoundTable(roundTableId);
        return Result.success("提交成功");
    }

    /**
     * 查询评分桌确认状态。
     */
    @GetMapping("/round-tables/{roundTableId}/score-confirmation")
    public Result<ScoreConfirmationVO> scoreConfirmation(@PathVariable Long roundTableId) {
        return Result.success(roundService.getScoreConfirmation(roundTableId));
    }

    /**
     * 当前评委确认评分桌结果。
     */
    @PostMapping("/round-tables/{roundTableId}/score-confirmation")
    public Result<ScoreConfirmationVO> confirmScoreRoundTable(@PathVariable Long roundTableId,
                                                              @RequestBody @Valid RoundTableConfirmationRequest request) {
        return Result.success(roundService.confirmScoreRoundTable(roundTableId, request));
    }

    /**
     * 桌长提交排序轮结果。
     */
    @PostMapping("/round-tables/{roundTableId}/ranking")
    public Result<String> submitRanking(@PathVariable Long roundTableId,
                                        @RequestBody @Valid RankingSubmitRequest request) {
        roundService.submitRanking(roundTableId, request);
        return Result.success("提交成功");
    }

    /**
     * 查询排序轮确认状态。
     */
    @GetMapping("/round-tables/{roundTableId}/ranking-confirmation")
    public Result<RankingConfirmationVO> rankingConfirmation(@PathVariable Long roundTableId) {
        return Result.success(roundService.getRankingConfirmation(roundTableId));
    }

    /**
     * 当前评委确认排序轮结果。
     */
    @PostMapping("/round-tables/{roundTableId}/ranking-confirmation")
    public Result<RankingConfirmationVO> confirmRankingRoundTable(@PathVariable Long roundTableId,
                                                                  @RequestBody @Valid RoundTableConfirmationRequest request) {
        return Result.success(roundService.confirmRankingRoundTable(roundTableId, request));
    }

    /**
     * 桌长最终提交排序轮确认结果。
     */
    @PostMapping("/round-tables/{roundTableId}/ranking-final-submit")
    public Result<String> finalizeRanking(@PathVariable Long roundTableId) {
        roundService.finalizeRanking(roundTableId);
        return Result.success("提交成功");
    }

    /**
     * 保存排序轮草稿，供桌长继续编辑。
     */
    @PostMapping("/round-tables/{roundTableId}/ranking-draft")
    public Result<String> saveRankingDraft(@PathVariable Long roundTableId,
                                           @RequestBody @Valid RankingDraftSaveRequest request) {
        roundService.saveRankingDraft(roundTableId, request);
        return Result.success("保存成功");
    }
}
