package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.StyleLibraryVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.JudgeService;
import com.beercompetition.service.StyleLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final StyleLibraryService styleLibraryService;

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.ADMIN));
    }

    @GetMapping("/style-libraries")
    public Result<List<StyleLibraryVO>> styleLibraries() {
        return Result.success(styleLibraryService.listLibraries());
    }

    @GetMapping("/style-libraries/{code}")
    public Result<StyleLibraryVO> styleLibraryDetail(@PathVariable String code) {
        return Result.success(styleLibraryService.getLibrary(code));
    }

    @PostMapping("/style-libraries")
    public Result<StyleLibraryVO> createStyleLibrary(@RequestBody @Valid StyleLibraryUpsertRequest request) {
        return Result.success(styleLibraryService.saveLibrary(request));
    }

    @PutMapping("/style-libraries/{code}")
    public Result<StyleLibraryVO> updateStyleLibrary(@PathVariable String code,
                                                     @RequestBody @Valid StyleLibraryUpsertRequest request) {
        request.setCode(code);
        return Result.success(styleLibraryService.saveLibrary(request));
    }

    @GetMapping("/competitions")
    public Result<List<CompetitionVO>> competitions() {
        return Result.success(competitionService.listCompetitions());
    }

    @PostMapping("/competitions")
    public Result<CompetitionVO> createCompetition(@RequestBody @Valid CompetitionCreateRequest request) {
        return Result.success(competitionService.createCompetition(request));
    }

    @GetMapping("/competitions/{id}")
    public Result<CompetitionDetailVO> getCompetitionDetail(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PutMapping("/competitions/{id}/base-info")
    public Result<CompetitionDetailVO> updateBaseInfo(@PathVariable Long id,
                                                      @RequestBody @Valid CompetitionBaseInfoUpdateRequest request) {
        return Result.success(competitionService.updateBaseInfo(id, request));
    }

    @PutMapping("/competitions/{id}/categories")
    public Result<CompetitionDetailVO> updateCategories(@PathVariable Long id,
                                                       @RequestBody @Valid ConfigNameBatchUpdateRequest request) {
        return Result.success(competitionService.updateCategories(id, request));
    }

    @PutMapping("/competitions/{id}/styles")
    public Result<CompetitionDetailVO> updateStyles(@PathVariable Long id,
                                                   @RequestBody @Valid CompetitionStyleLibraryUpdateRequest request) {
        return Result.success(competitionService.updateStyles(id, request));
    }

    @PutMapping("/competitions/{id}/entry-fields")
    public Result<CompetitionDetailVO> updateEntryFields(@PathVariable Long id,
                                                        @RequestBody @Valid EntryFieldBatchUpdateRequest request) {
        return Result.success(competitionService.updateEntryFields(id, request));
    }

    @PutMapping("/competitions/{id}/judge-tables")
    public Result<CompetitionDetailVO> updateJudgeTables(@PathVariable Long id,
                                                        @RequestBody @Valid JudgeTableBatchUpdateRequest request) {
        return Result.success(competitionService.updateJudgeTables(id, request));
    }

    @PostMapping("/competitions/{id}/open-registration")
    public Result<CompetitionDetailVO> openRegistration(@PathVariable Long id) {
        return Result.success(competitionService.openRegistration(id));
    }

    @GetMapping("/judges")
    public Result<List<JudgeAccountVO>> judges(@RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(judgeService.listJudges(status, keyword));
    }

    @GetMapping("/judges/{publicId}")
    public Result<JudgeAccountVO> judgeDetail(@PathVariable String publicId) {
        return Result.success(judgeService.getJudgeDetail(publicId));
    }

    @PutMapping("/judges/{publicId}")
    public Result<JudgeAccountVO> updateJudge(@PathVariable String publicId,
                                              @RequestBody @Valid AdminJudgeUpdateRequest request) {
        return Result.success(judgeService.updateJudge(publicId, request));
    }

    @PatchMapping("/judges/{publicId}/phone")
    public Result<JudgeAccountVO> updateJudgePhone(@PathVariable String publicId,
                                                   @RequestBody @Valid AdminJudgePhoneUpdateRequest request) {
        return Result.success(judgeService.updateJudgePhone(publicId, request));
    }

    @PatchMapping("/judges/{publicId}/status")
    public Result<JudgeAccountVO> updateJudgeStatus(@PathVariable String publicId,
                                                    @RequestBody @Valid AdminJudgeStatusUpdateRequest request) {
        return Result.success(judgeService.updateJudgeStatus(publicId, request));
    }

    @PostMapping("/judge-assignments")
    public Result<String> createAssignment(@RequestBody @Valid JudgeAssignmentCreateRequest request) {
        judgeService.createAssignment(request);
        return Result.success("分配成功");
    }

    @PutMapping("/competitions/{id}/judge-assignments")
    public Result<String> updateCompetitionAssignments(@PathVariable Long id,
                                                       @RequestBody @Valid JudgeAssignmentBatchUpdateRequest request) {
        judgeService.updateCompetitionAssignments(id, request);
        return Result.success("保存成功");
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
