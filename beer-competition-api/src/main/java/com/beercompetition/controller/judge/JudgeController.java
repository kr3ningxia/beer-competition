package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.dto.JudgeScoreSaveRequest;
import com.beercompetition.pojo.dto.JudgeScoreUpdateRequest;
import com.beercompetition.pojo.dto.RankingDraftSaveRequest;
import com.beercompetition.pojo.dto.RankingSubmitRequest;
import com.beercompetition.pojo.dto.TableScoreFinalizeRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.pojo.vo.ScoreRecordVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.JudgeService;
import com.beercompetition.service.RoundService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/judge")
public class JudgeController {

    private final AuthService authService;
    private final JudgeService judgeService;
    private final EntryService entryService;
    private final ScoreService scoreService;
    private final RoundService roundService;

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.JUDGE));
    }

    @GetMapping("/profile")
    public Result<JudgeAccountVO> profile() {
        return Result.success(judgeService.getMyProfile());
    }

    @PutMapping("/profile")
    public Result<JudgeAccountVO> updateProfile(@RequestBody @Valid JudgeProfileUpdateRequest request) {
        return Result.success(judgeService.updateMyProfile(request));
    }

    @GetMapping("/competitions")
    public Result<List<CompetitionVO>> competitions() {
        return Result.success(judgeService.listMyCompetitions());
    }

    @GetMapping("/tasks")
    public Result<List<JudgeTaskVO>> tasks() {
        return Result.success(roundService.listMyTasks());
    }

    @GetMapping("/round-tables/{roundTableId}")
    public Result<JudgeRoundTableVO> roundTable(@PathVariable Long roundTableId) {
        return Result.success(roundService.getMyRoundTable(roundTableId));
    }

    @PostMapping("/round-tables/{roundTableId}/score-submit")
    public Result<String> submitScoreRoundTable(@PathVariable Long roundTableId) {
        roundService.submitScoreRoundTable(roundTableId);
        return Result.success("提交成功");
    }

    @GetMapping("/round-tables/{roundTableId}/score-confirmation")
    public Result<ScoreConfirmationVO> scoreConfirmation(@PathVariable Long roundTableId) {
        return Result.success(roundService.getScoreConfirmation(roundTableId));
    }

    @PostMapping("/round-tables/{roundTableId}/score-confirmation")
    public Result<ScoreConfirmationVO> confirmScoreRoundTable(@PathVariable Long roundTableId) {
        return Result.success(roundService.confirmScoreRoundTable(roundTableId));
    }

    @PostMapping("/round-tables/{roundTableId}/ranking")
    public Result<String> submitRanking(@PathVariable Long roundTableId,
                                        @RequestBody @Valid RankingSubmitRequest request) {
        roundService.submitRanking(roundTableId, request);
        return Result.success("提交成功");
    }

    @PostMapping("/round-tables/{roundTableId}/ranking-draft")
    public Result<String> saveRankingDraft(@PathVariable Long roundTableId,
                                           @RequestBody @Valid RankingDraftSaveRequest request) {
        roundService.saveRankingDraft(roundTableId, request);
        return Result.success("保存成功");
    }

    @GetMapping("/entries/{uuid}")
    public Result<JudgeEntryVO> entry(@PathVariable String uuid) {
        return Result.success(entryService.getJudgeEntry(uuid));
    }

    @GetMapping("/scan/resolve")
    public Result<JudgeEntryVO> resolveScan(@RequestParam String code) {
        return Result.success(entryService.resolveJudgeScan(code));
    }

    @GetMapping("/score-configs/current")
    public Result<ScoreConfigVO> currentScoreConfig(@RequestParam JudgeRoleType role,
                                                    @RequestParam(required = false) Long competitionId) {
        return Result.success(scoreService.getCurrentScoreConfig(role, competitionId));
    }

    @PostMapping("/scores")
    public Result<ScoreRecordVO> createScore(@RequestBody @Valid JudgeScoreSaveRequest request) {
        return Result.success(scoreService.createScore(request));
    }

    @PutMapping("/scores/{id}")
    public Result<ScoreRecordVO> updateScore(@PathVariable Long id, @RequestBody @Valid JudgeScoreUpdateRequest request) {
        return Result.success(scoreService.updateScore(id, request));
    }

    @GetMapping("/my-scores")
    public Result<List<ScoreRecordVO>> myScores() {
        return Result.success(scoreService.listMyScores());
    }

    @GetMapping("/my-scores/{uuid}")
    public Result<ScoreRecordVO> myScore(@PathVariable String uuid) {
        return Result.success(scoreService.getMyScore(uuid));
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
