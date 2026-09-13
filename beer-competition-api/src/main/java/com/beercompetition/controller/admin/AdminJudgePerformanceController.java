package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.JudgePerformanceSaveRequest;
import com.beercompetition.pojo.vo.CompetitionJudgePerformanceSummaryVO;
import com.beercompetition.pojo.vo.JudgeAccountPerformanceVO;
import com.beercompetition.pojo.vo.JudgePerformanceVO;
import com.beercompetition.service.JudgePerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminJudgePerformanceController {

    private final JudgePerformanceService judgePerformanceService;

    @GetMapping("/competitions/{competitionId}/judge-performances")
    public Result<CompetitionJudgePerformanceSummaryVO> competitionPerformances(
            @PathVariable Long competitionId) {
        return Result.success(judgePerformanceService.listCompetitionPerformances(competitionId));
    }

    @PutMapping("/competitions/{competitionId}/judge-performances/{judgePublicId}")
    public Result<JudgePerformanceVO> savePerformance(
            @PathVariable Long competitionId,
            @PathVariable String judgePublicId,
            @RequestBody @Valid JudgePerformanceSaveRequest request) {
        return Result.success(judgePerformanceService.savePerformance(competitionId, judgePublicId, request));
    }

    @GetMapping("/judge-performances/accounts")
    public Result<List<JudgeAccountPerformanceVO>> accountOverviews(
            @RequestParam List<String> publicIds) {
        return Result.success(judgePerformanceService.listAccountOverviews(publicIds));
    }

    @GetMapping("/judges/{judgePublicId}/performances")
    public Result<JudgeAccountPerformanceVO> accountHistory(@PathVariable String judgePublicId) {
        return Result.success(judgePerformanceService.getAccountHistory(judgePublicId));
    }
}
