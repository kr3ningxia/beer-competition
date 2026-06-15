package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminFeedbackCommentUpdateRequest;
import com.beercompetition.pojo.vo.AdminFeedbackReviewEntryVO;
import com.beercompetition.service.AdminFeedbackService;
import com.beercompetition.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台评语复核接口，支持发布前查看和修正第一轮评语。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions/{id}/feedback-review")
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;
    private final CompetitionService competitionService;

    /**
     * 查询指定比赛待复核的评语列表。
     */
    @GetMapping
    public Result<List<AdminFeedbackReviewEntryVO>> feedbackReview(@PathVariable Long id) {
        return Result.success(competitionService.getFeedbackReviewEntries(id));
    }

    /**
     * 更新指定评分记录的评委评语。
     */
    @PatchMapping("/score-records/{scoreRecordId}/comments")
    public Result<String> updateFeedbackComment(@PathVariable Long id,
                                                @PathVariable Long scoreRecordId,
                                                @RequestBody @Valid AdminFeedbackCommentUpdateRequest request) {
        adminFeedbackService.updateScoreRecordComments(id, scoreRecordId, request);
        return Result.success("保存成功");
    }
}
