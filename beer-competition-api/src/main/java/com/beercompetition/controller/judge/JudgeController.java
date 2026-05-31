package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.JudgeService;
import com.beercompetition.service.ScoreService;
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
@RequestMapping("/api/judge")
public class JudgeController {

    private final AuthService authService;
    private final JudgeService judgeService;
    private final EntryService entryService;
    private final ScoreService scoreService;

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.JUDGE));
    }

    @GetMapping("/competitions")
    public Result<List<CompetitionVO>> competitions() {
        return Result.success(judgeService.listMyCompetitions());
    }

    @GetMapping("/entries/{uuid}")
    public Result<JudgeEntryVO> entry(@PathVariable String uuid) {
        return Result.success(entryService.getJudgeEntry(uuid));
    }

    @PostMapping("/scores")
    public Result<ScoreRecordVO> createScore(@RequestBody @Valid JudgeScoreSaveRequest request) {
        return Result.success(scoreService.createScore(request));
    }

    @PutMapping("/scores/{id}")
    public Result<ScoreRecordVO> updateScore(@PathVariable Long id, @RequestBody @Valid JudgeScoreUpdateRequest request) {
        return Result.success(scoreService.updateScore(id, request));
    }

    @GetMapping("/table-scores/{uuid}")
    public Result<List<ScoreRecordVO>> tableScores(@PathVariable String uuid) {
        return Result.success(scoreService.listTableScores(uuid));
    }

    @PostMapping("/table-scores/{uuid}/finalize")
    public Result<ScoreRecordVO> finalizeScore(@PathVariable String uuid,
                                               @RequestBody @Valid TableScoreFinalizeRequest request) {
        return Result.success(scoreService.finalizeTableScore(uuid, request));
    }
}
