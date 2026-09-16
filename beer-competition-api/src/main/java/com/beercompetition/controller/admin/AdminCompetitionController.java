package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionReopenRegistrationRequest;
import com.beercompetition.pojo.dto.CompetitionRefundPolicyUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionReturnToSampleCheckRequest;
import com.beercompetition.pojo.dto.CompetitionSponsorBatchUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCollectionConfigUpdateRequest;
import com.beercompetition.pojo.dto.NotificationRuleUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTemplateUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTestSendRequest;
import com.beercompetition.pojo.vo.CompetitionAnalyticsVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionProgressVO;
import com.beercompetition.pojo.vo.CompetitionLiveBoardVO;
import com.beercompetition.pojo.vo.CompetitionQuickSummaryVO;
import com.beercompetition.pojo.vo.CompetitionSponsorLogoVO;
import com.beercompetition.pojo.vo.CompetitionSponsorVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CompetitionCollectionConfigVO;
import com.beercompetition.pojo.vo.CompetitionCollectionQrVO;
import com.beercompetition.pojo.vo.CompetitionNotificationConfigVO;
import com.beercompetition.pojo.vo.EmailDeliveryVO;
import com.beercompetition.service.CompetitionSponsorService;
import com.beercompetition.competition.command.CompetitionCommandService;
import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.collection.CompetitionCollectionService;
import com.beercompetition.competition.lifecycle.CompetitionLifecycleService;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.service.LiveBoardService;
import com.beercompetition.service.EmailNotificationService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台比赛管理接口，负责比赛主流程和配置项的请求入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions")
public class AdminCompetitionController {

    private final CompetitionQueryService competitionQueryService;
    private final CompetitionCommandService competitionCommandService;
    private final CompetitionConfigurationService competitionConfigurationService;
    private final CompetitionCollectionService competitionCollectionService;
    private final CompetitionLifecycleService competitionLifecycleService;
    private final CompetitionSponsorService competitionSponsorService;
    private final LiveBoardService liveBoardService;
    private final EmailNotificationService emailNotificationService;

    /**
     * 查询后台比赛列表。
     */
    @GetMapping
    public Result<List<CompetitionVO>> competitions(@RequestParam(defaultValue = "false") boolean includeArchived,
                                                    @RequestParam(required = false) String organizerType) {
        return Result.success(competitionQueryService.listCompetitions(includeArchived, organizerType));
    }

    /**
     * 创建新的比赛草稿。
     */
    @PostMapping
    public Result<CompetitionVO> createCompetition(@RequestBody @Valid CompetitionCreateRequest request) {
        return Result.success(competitionCommandService.createCompetition(request));
    }

    /**
     * 查询比赛详情及其配置完整性。
     */
    @GetMapping("/{id}")
    public Result<CompetitionDetailVO> getCompetitionDetail(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionDetail(id));
    }

    /**
     * 查询比赛详情首屏和配置标签需要的轻量数据。
     */
    @GetMapping("/{id}/overview")
    public Result<CompetitionDetailVO> getCompetitionOverview(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionOverview(id));
    }

    /**
     * 按需查询比赛酒款池。
     */
    @GetMapping("/{id}/entry-pool")
    public Result<List<CompetitionEntryVO>> getCompetitionEntryPool(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionEntryPool(id));
    }

    @GetMapping("/{id}/entry-pool/page")
    public Result<PageResult<CompetitionEntryVO>> getCompetitionEntryPoolPage(@PathVariable Long id,
                                                                                @RequestParam(defaultValue = "1") Integer page,
                                                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(competitionQueryService.getCompetitionEntryPoolPage(id, page, pageSize));
    }

    @GetMapping("/{id}/box-numbers")
    public Result<List<String>> getCompetitionBoxNumbers(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionBoxNumbers(id));
    }

    /**
     * 查询比赛列表快速概览所需的进度和提醒。
     */
    @GetMapping("/{id}/quick-summary")
    public Result<CompetitionQuickSummaryVO> getCompetitionQuickSummary(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionQuickSummary(id));
    }

    /**
     * 删除草稿比赛或归档已进入流程的比赛。
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteCompetition(@PathVariable Long id) {
        competitionCommandService.deleteCompetition(id);
        return Result.success("删除成功");
    }

    /**
     * 更新比赛基础信息。
     */
    @PutMapping("/{id}/base-info")
    public Result<CompetitionDetailVO> updateBaseInfo(@PathVariable Long id,
                                                      @RequestBody @Valid CompetitionBaseInfoUpdateRequest request) {
        return Result.success(competitionCommandService.updateBaseInfo(id, request));
    }

    /**
     * 在退款申请截止前更新比赛退款审批方式。
     */
    @PutMapping("/{id}/refund-policy")
    public Result<CompetitionDetailVO> updateRefundPolicy(
            @PathVariable Long id,
            @RequestBody @Valid CompetitionRefundPolicyUpdateRequest request) {
        return Result.success(competitionCommandService.updateRefundPolicy(id, request));
    }

    /**
     * 更新比赛投递组别配置。
     */
    @PutMapping("/{id}/categories")
    public Result<CompetitionDetailVO> updateCategories(@PathVariable Long id,
                                                        @RequestBody @Valid ConfigNameBatchUpdateRequest request) {
        return Result.success(competitionConfigurationService.updateCategories(id, request));
    }

    /**
     * 更新比赛风格库快照。
     */
    @PutMapping("/{id}/styles")
    public Result<CompetitionDetailVO> updateStyles(@PathVariable Long id,
                                                    @RequestBody @Valid CompetitionStyleLibraryUpdateRequest request) {
        return Result.success(competitionConfigurationService.updateStyles(id, request));
    }

    /**
     * 更新报名补充字段配置。
     */
    @PutMapping("/{id}/entry-fields")
    public Result<CompetitionDetailVO> updateEntryFields(@PathVariable Long id,
                                                         @RequestBody @Valid EntryFieldBatchUpdateRequest request) {
        return Result.success(competitionConfigurationService.updateEntryFields(id, request));
    }

    /**
     * 更新比赛基础评审桌配置。
     */
    @PutMapping("/{id}/judge-tables")
    public Result<CompetitionDetailVO> updateJudgeTables(@PathVariable Long id,
                                                         @RequestBody @Valid JudgeTableBatchUpdateRequest request) {
        return Result.success(competitionConfigurationService.updateJudgeTables(id, request));
    }

    @GetMapping("/{id}/collection")
    public Result<CompetitionCollectionConfigVO> collectionConfig(@PathVariable Long id) {
        return Result.success(competitionCollectionService.getAdminConfig(id));
    }

    @PutMapping("/{id}/collection")
    public Result<CompetitionCollectionConfigVO> updateCollectionConfig(
            @PathVariable Long id,
            @RequestBody @Valid CompetitionCollectionConfigUpdateRequest request) {
        return Result.success(competitionCollectionService.updateAdminConfig(id, request));
    }

    @PostMapping("/{id}/collection/wechat-qr")
    public Result<CompetitionCollectionQrVO> uploadCollectionQr(@PathVariable Long id,
                                                                 @RequestParam("file") MultipartFile file) {
        return Result.success(competitionCollectionService.uploadWechatQr(id, file));
    }

    /**
     * 将配置完整的比赛开放报名。
     */
    @PostMapping("/{id}/open-registration")
    public Result<CompetitionDetailVO> openRegistration(@PathVariable Long id) {
        return Result.success(competitionLifecycleService.openRegistration(id));
    }

    /**
     * 手动关闭指定比赛报名。
     */
    @PostMapping("/{id}/close-registration")
    public Result<CompetitionDetailVO> closeRegistration(@PathVariable Long id) {
        return Result.success(competitionLifecycleService.closeRegistration(id));
    }

    /**
     * 推进比赛到评审准备阶段。
     */
    @PostMapping("/{id}/prepare-judging")
    public Result<CompetitionDetailVO> prepareJudging(@PathVariable Long id) {
        return Result.success(competitionLifecycleService.prepareJudging(id));
    }

    /**
     * 将误截止或临时延长的比赛重新开放报名。
     */
    @PostMapping("/{id}/reopen-registration")
    public Result<CompetitionDetailVO> reopenRegistration(@PathVariable Long id,
                                                          @RequestBody @Valid CompetitionReopenRegistrationRequest request) {
        return Result.success(competitionLifecycleService.reopenRegistration(id, request));
    }

    /**
     * 将尚未发布第一轮的比赛退回收样核对阶段。
     */
    @PostMapping("/{id}/return-to-sample-check")
    public Result<CompetitionDetailVO> returnToSampleCheck(@PathVariable Long id,
                                                           @RequestBody @Valid CompetitionReturnToSampleCheckRequest request) {
        return Result.success(competitionLifecycleService.returnToSampleCheck(id, request));
    }

    /**
     * 查询比赛当前进度，供后台流程页刷新使用。
     */
    @GetMapping("/{id}/progress")
    public Result<CompetitionProgressVO> progress(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionProgress(id));
    }

    /**
     * 查询公开投屏看板数据。
     */
    @GetMapping("/{id}/live-board")
    public Result<CompetitionLiveBoardVO> liveBoard(@PathVariable Long id) {
        return Result.success(liveBoardService.getCompetitionLiveBoard(id));
    }

    /**
     * 查询本场比赛赞助商配置。
     */
    @GetMapping("/{id}/sponsors")
    public Result<List<CompetitionSponsorVO>> sponsors(@PathVariable Long id) {
        return Result.success(competitionSponsorService.listSponsors(id));
    }

    /**
     * 整体保存本场比赛赞助商配置。
     */
    @PutMapping("/{id}/sponsors")
    public Result<List<CompetitionSponsorVO>> updateSponsors(@PathVariable Long id,
                                                             @RequestBody @Valid CompetitionSponsorBatchUpdateRequest request) {
        return Result.success(competitionSponsorService.updateSponsors(id, request));
    }

    /**
     * 上传本场比赛赞助商 Logo。
     */
    @PostMapping("/{id}/sponsors/logo")
    public Result<CompetitionSponsorLogoVO> uploadSponsorLogo(@PathVariable Long id,
                                                              @RequestParam("file") MultipartFile file) {
        return Result.success(competitionSponsorService.uploadSponsorLogo(id, file));
    }

    /**
     * 查询比赛数据分析结果。
     */
    @GetMapping("/{id}/analytics")
    public Result<CompetitionAnalyticsVO> analytics(@PathVariable Long id) {
        return Result.success(competitionQueryService.getCompetitionAnalytics(id));
    }

    @GetMapping("/{id}/notifications")
    public Result<CompetitionNotificationConfigVO> notifications(@PathVariable Long id) {
        return Result.success(emailNotificationService.getConfiguration(id));
    }

    @PutMapping("/{id}/notifications/rule")
    public Result<CompetitionNotificationConfigVO> updateNotificationRule(
            @PathVariable Long id, @RequestBody @Valid NotificationRuleUpdateRequest request) {
        return Result.success(emailNotificationService.updateRule(id, request));
    }

    @PutMapping("/{id}/notifications/templates/{eventCode}")
    public Result<CompetitionNotificationConfigVO> updateNotificationTemplate(
            @PathVariable Long id, @PathVariable String eventCode,
            @RequestBody @Valid NotificationTemplateUpdateRequest request) {
        return Result.success(emailNotificationService.updateTemplate(id, eventCode, request));
    }

    @PostMapping("/{id}/notifications/test")
    public Result<String> sendNotificationTest(
            @PathVariable Long id, @RequestBody @Valid NotificationTestSendRequest request) {
        return Result.success(emailNotificationService.sendTest(id, request));
    }

    @GetMapping("/{id}/notifications/deliveries")
    public Result<List<EmailDeliveryVO>> notificationDeliveries(@PathVariable Long id,
                                                                 @RequestParam(required = false) String status) {
        return Result.success(emailNotificationService.listDeliveries(id, status));
    }

    @PostMapping("/{id}/notifications/deliveries/{deliveryId}/retry")
    public Result<String> retryNotification(@PathVariable Long id, @PathVariable Long deliveryId) {
        emailNotificationService.retryDelivery(id, deliveryId);
        return Result.success("邮件已加入重试队列");
    }
}
