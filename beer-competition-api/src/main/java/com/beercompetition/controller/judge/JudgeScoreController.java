package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.service.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评委评分接口，处理个人评分和桌长共识评分。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/judge")
public class JudgeScoreController {

    private final ScoreService scoreService;

    /**
     * 查询当前比赛指定评审角色的评分表。
     */
    @GetMapping("/score-configs/current")
    public Result<ScoreConfigVO> currentScoreConfig(@RequestParam JudgeRoleType role,
                                                    @RequestParam(required = false) Long competitionId) {
        return Result.success(scoreService.getCurrentScoreConfig(role, competitionId));
    }

    /**
     * 新建当前评委的个人评分记录。
     */
    @PostMapping("/scores")
    public Result<ScoreRecordVO> createScore(@RequestBody @Valid JudgeScoreSaveRequest request) {
        return Result.success(scoreService.createScore(request));
    }

    /**
     * 更新当前评委的个人评分记录。
     */
    @PutMapping("/scores/{id}")
    public Result<ScoreRecordVO> updateScore(@PathVariable Long id, @RequestBody @Valid JudgeScoreUpdateRequest request) {
        return Result.success(scoreService.updateScore(id, request));
    }

    /**
     * 查询当前评委已提交的评分记录。
     */
    @GetMapping("/my-scores")
    public Result<List<ScoreRecordVO>> myScores() {
        return Result.success(scoreService.listMyScores());
    }

    /**
     * 查询当前评委对指定酒款的评分详情。
     */
    @GetMapping("/my-scores/{uuid}")
    public Result<ScoreRecordVO> myScore(@PathVariable String uuid) {
        return Result.success(scoreService.getMyScore(uuid));
    }

    /**
     * 查询指定酒款的同桌评分记录。
     */
    @GetMapping("/table-scores/{uuid}")
    public Result<List<ScoreRecordVO>> tableScores(@PathVariable String uuid) {
        return Result.success(scoreService.listTableScores(uuid));
    }

    /**
     * 由桌长提交指定酒款的共识评分。
     */
    @PostMapping("/table-scores/{uuid}/finalize")
    public Result<ScoreRecordVO> finalizeScore(@PathVariable String uuid,
                                               @RequestBody @Valid TableScoreFinalizeRequest request) {
        return Result.success(scoreService.finalizeTableScore(uuid, request));
    }
}
