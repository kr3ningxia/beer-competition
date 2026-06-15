package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 后台比赛管理接口，负责比赛主流程和配置项的请求入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions")
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    /**
     * 查询后台比赛列表。
     */
    @GetMapping
    public Result<List<CompetitionVO>> competitions(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return Result.success(competitionService.listCompetitions(includeArchived));
    }

    /**
     * 创建新的比赛草稿。
     */
    @PostMapping
    public Result<CompetitionVO> createCompetition(@RequestBody @Valid CompetitionCreateRequest request) {
        return Result.success(competitionService.createCompetition(request));
    }

    /**
     * 查询比赛详情及其配置完整性。
     */
    @GetMapping("/{id}")
    public Result<CompetitionDetailVO> getCompetitionDetail(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    /**
     * 删除草稿比赛或归档已进入流程的比赛。
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return Result.success("删除成功");
    }

    /**
     * 更新比赛基础信息。
     */
    @PutMapping("/{id}/base-info")
    public Result<CompetitionDetailVO> updateBaseInfo(@PathVariable Long id,
                                                      @RequestBody @Valid CompetitionBaseInfoUpdateRequest request) {
        return Result.success(competitionService.updateBaseInfo(id, request));
    }

    /**
     * 更新比赛投递组别配置。
     */
    @PutMapping("/{id}/categories")
    public Result<CompetitionDetailVO> updateCategories(@PathVariable Long id,
                                                        @RequestBody @Valid ConfigNameBatchUpdateRequest request) {
        return Result.success(competitionService.updateCategories(id, request));
    }

    /**
     * 更新比赛风格库快照。
     */
    @PutMapping("/{id}/styles")
    public Result<CompetitionDetailVO> updateStyles(@PathVariable Long id,
                                                    @RequestBody @Valid CompetitionStyleLibraryUpdateRequest request) {
        return Result.success(competitionService.updateStyles(id, request));
    }

    /**
     * 更新报名补充字段配置。
     */
    @PutMapping("/{id}/entry-fields")
    public Result<CompetitionDetailVO> updateEntryFields(@PathVariable Long id,
                                                         @RequestBody @Valid EntryFieldBatchUpdateRequest request) {
        return Result.success(competitionService.updateEntryFields(id, request));
    }

    /**
     * 更新比赛基础评审桌配置。
     */
    @PutMapping("/{id}/judge-tables")
    public Result<CompetitionDetailVO> updateJudgeTables(@PathVariable Long id,
                                                         @RequestBody @Valid JudgeTableBatchUpdateRequest request) {
        return Result.success(competitionService.updateJudgeTables(id, request));
    }

    /**
     * 将配置完整的比赛开放报名。
     */
    @PostMapping("/{id}/open-registration")
    public Result<CompetitionDetailVO> openRegistration(@PathVariable Long id) {
        return Result.success(competitionService.openRegistration(id));
    }

    /**
     * 手动关闭指定比赛报名。
     */
    @PostMapping("/{id}/close-registration")
    public Result<CompetitionDetailVO> closeRegistration(@PathVariable Long id) {
        return Result.success(competitionService.closeRegistration(id));
    }

    /**
     * 推进比赛到评审准备阶段。
     */
    @PostMapping("/{id}/prepare-judging")
    public Result<CompetitionDetailVO> prepareJudging(@PathVariable Long id) {
        return Result.success(competitionService.prepareJudging(id));
    }

    /**
     * 查询比赛当前进度，供后台流程页刷新使用。
     */
    @GetMapping("/{id}/progress")
    public Result<CompetitionDetailVO> progress(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }
}
