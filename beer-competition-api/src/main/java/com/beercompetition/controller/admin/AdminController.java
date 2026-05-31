package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final CompetitionService competitionService;
    private final JudgeService judgeService;

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.ADMIN));
    }

    @GetMapping("/competitions")
    public Result<List<CompetitionVO>> competitions() {
        return Result.success(competitionService.listCompetitions());
    }

    @PostMapping("/competitions")
    public Result<CompetitionVO> createCompetition(@RequestBody @Valid CompetitionCreateRequest request) {
        return Result.success(competitionService.createCompetition(request));
    }

    @GetMapping("/judges")
    public Result<List<JudgeAccountVO>> judges() {
        return Result.success(judgeService.listJudges());
    }

    @PostMapping("/judge-assignments")
    public Result<String> createAssignment(@RequestBody @Valid JudgeAssignmentCreateRequest request) {
        judgeService.createAssignment(request);
        return Result.success("分配成功");
    }

    @GetMapping("/score-configs/{competitionId}")
    public Result<List<ScoreConfigVO>> getScoreConfigs(@PathVariable Long competitionId) {
        return Result.success(competitionService.getScoreConfigs(competitionId));
    }

    @PutMapping("/score-configs/{competitionId}")
    public Result<List<ScoreConfigVO>> updateScoreConfigs(@PathVariable Long competitionId,
                                                          @RequestBody @Valid ScoreConfigBatchUpdateRequest request) {
        return Result.success(competitionService.updateScoreConfigs(competitionId, request));
    }
}
