package com.beercompetition.controller.admin;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminFeedbackCommentUpdateRequest;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.FirstRoundCreateRequest;
import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.AdminConfirmationOverrideRequest;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.NextRoundCreateRequest;
import com.beercompetition.pojo.dto.RoundAllocationRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.AdminFeedbackReviewEntryVO;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.StyleLibraryVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.AdminExportService;
import com.beercompetition.service.AdminFeedbackService;
import com.beercompetition.service.AwardService;
import com.beercompetition.service.BankTransferPaymentService;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.JudgeService;
import com.beercompetition.service.RoundService;
import com.beercompetition.service.StyleLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final AdminExportService adminExportService;
    private final AdminFeedbackService adminFeedbackService;
    private final CompetitionService competitionService;
    private final BankTransferPaymentService bankTransferPaymentService;
    private final EntryService entryService;
    private final JudgeService judgeService;
    private final StyleLibraryService styleLibraryService;
    private final RoundService roundService;
    private final AwardService awardService;

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

    @DeleteMapping("/competitions/{id}")
    public Result<String> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return Result.success("删除成功");
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

    @PostMapping("/competitions/{id}/close-registration")
    public Result<CompetitionDetailVO> closeRegistration(@PathVariable Long id) {
        return Result.success(competitionService.closeRegistration(id));
    }

    @PostMapping("/competitions/{id}/prepare-judging")
    public Result<CompetitionDetailVO> prepareJudging(@PathVariable Long id) {
        return Result.success(competitionService.prepareJudging(id));
    }

    @PostMapping("/competitions/{id}/rounds/first")
    public Result<CompetitionDetailVO> createFirstRound(@PathVariable Long id,
                                                        @RequestBody @Valid FirstRoundCreateRequest request) {
        roundService.createFirstRound(id, request);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PutMapping("/competitions/{id}/rounds/{roundId}/allocation")
    public Result<CompetitionDetailVO> saveRoundAllocation(@PathVariable Long id,
                                                           @PathVariable Long roundId,
                                                           @RequestBody @Valid RoundAllocationRequest request) {
        roundService.saveRoundAllocation(id, roundId, request);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/rounds/{roundId}/publish")
    public Result<CompetitionDetailVO> publishRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundService.publishRound(id, roundId);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/rounds/{roundId}/complete-first-round")
    public Result<CompetitionDetailVO> completeFirstRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundService.completeFirstRound(id, roundId);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/rounds/next")
    public Result<CompetitionDetailVO> createNextRound(@PathVariable Long id,
                                                       @RequestBody @Valid NextRoundCreateRequest request) {
        roundService.createNextRound(id, request);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/rounds/{roundId}/sync-candidates")
    public Result<CompetitionDetailVO> syncRoundCandidates(@PathVariable Long id, @PathVariable Long roundId) {
        roundService.syncRoundCandidates(id, roundId);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @DeleteMapping("/competitions/{id}/rounds/{roundId}")
    public Result<CompetitionDetailVO> deleteDraftRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundService.deleteDraftRound(id, roundId);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/rounds/{roundId}/lock")
    public Result<CompetitionDetailVO> lockRound(@PathVariable Long id, @PathVariable Long roundId) {
        roundService.lockRound(id, roundId);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @PostMapping("/competitions/{id}/round-tables/{roundTableId}/confirmation-override")
    public Result<CompetitionDetailVO> overrideRoundTableConfirmation(@PathVariable Long id,
                                                                      @PathVariable Long roundTableId,
                                                                      @RequestBody @Valid AdminConfirmationOverrideRequest request) {
        roundService.overrideScoreConfirmation(id, roundTableId, request);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @GetMapping("/competitions/{id}/progress")
    public Result<CompetitionDetailVO> progress(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @GetMapping("/competitions/{id}/feedback-review")
    public Result<List<AdminFeedbackReviewEntryVO>> feedbackReview(@PathVariable Long id) {
        return Result.success(competitionService.getFeedbackReviewEntries(id));
    }

    @PatchMapping("/competitions/{id}/feedback-review/score-records/{scoreRecordId}/comments")
    public Result<String> updateFeedbackComment(@PathVariable Long id,
                                                @PathVariable Long scoreRecordId,
                                                @RequestBody @Valid AdminFeedbackCommentUpdateRequest request) {
        adminFeedbackService.updateScoreRecordComments(id, scoreRecordId, request);
        return Result.success("保存成功");
    }

    @GetMapping("/competitions/{id}/results/draft")
    public Result<List<ResultDraftVO>> resultDraft(@PathVariable Long id) {
        return Result.success(roundService.buildResultDrafts(id));
    }

    @GetMapping("/competitions/{id}/award-rules")
    public Result<List<AwardRuleVO>> awardRules(@PathVariable Long id) {
        return Result.success(awardService.listAwardRules(id));
    }

    @GetMapping("/competitions/{id}/awards")
    public Result<List<AwardResultVO>> awards(@PathVariable Long id) {
        return Result.success(awardService.listAwardResults(id));
    }

    @PostMapping("/competitions/{id}/awards/generate")
    public Result<List<AwardResultVO>> generateAwards(@PathVariable Long id) {
        return Result.success(awardService.generateAwardDrafts(id));
    }

    @PutMapping("/competitions/{id}/awards/confirm")
    public Result<List<AwardResultVO>> confirmAwards(@PathVariable Long id,
                                                     @RequestBody @Valid AwardConfirmRequest request) {
        return Result.success(awardService.confirmAwards(id, request));
    }

    @PostMapping(value = "/competitions/{id}/awards/{awardId}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AwardResultVO> uploadAwardCertificate(@PathVariable Long id,
                                                        @PathVariable Long awardId,
                                                        @RequestParam("file") MultipartFile file) {
        return Result.success(awardService.uploadCertificate(id, awardId, file));
    }

    @DeleteMapping("/competitions/{id}/awards/{awardId}/certificate")
    public Result<String> deleteAwardCertificate(@PathVariable Long id,
                                                 @PathVariable Long awardId) {
        awardService.deleteCertificate(id, awardId);
        return Result.success("删除成功");
    }

    @GetMapping("/competitions/{id}/awards/{awardId}/certificate")
    public ResponseEntity<byte[]> downloadAwardCertificate(@PathVariable Long id,
                                                           @PathVariable Long awardId) {
        return fileResponse(awardService.downloadCertificate(id, awardId));
    }

    @PostMapping("/competitions/{id}/results/publish")
    public Result<CompetitionDetailVO> publishResults(@PathVariable Long id) {
        roundService.publishResults(id);
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @GetMapping("/competitions/{id}/exports/scoring")
    public ResponseEntity<byte[]> exportScoringData(@PathVariable Long id) {
        byte[] content = competitionService.exportScoringData(id);
        adminExportService.logScoringExport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scoring-data.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/competitions/{id}/exports/entries")
    public ResponseEntity<byte[]> exportEntries(@PathVariable Long id,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String entryStatus,
                                                @RequestParam(required = false) String paymentStatus,
                                                @RequestParam(required = false) String deliveryStatus,
                                                @RequestParam(required = false) String keyword) {
        return fileResponse(adminExportService.exportEntries(id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword));
    }

    @GetMapping("/competitions/{id}/exports/delivery")
    public ResponseEntity<byte[]> exportDelivery(@PathVariable Long id,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String entryStatus,
                                                 @RequestParam(required = false) String paymentStatus,
                                                 @RequestParam(required = false) String deliveryStatus,
                                                 @RequestParam(required = false) String keyword) {
        return fileResponse(adminExportService.exportDelivery(id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword));
    }

    @GetMapping("/competitions/{id}/exports/labels")
    public ResponseEntity<byte[]> exportLabels(@PathVariable Long id,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) String entryStatus,
                                               @RequestParam(required = false) String paymentStatus,
                                               @RequestParam(required = false) String deliveryStatus,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer copies) {
        return fileResponse(adminExportService.exportLabels(id, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword, copies));
    }

    @GetMapping("/entries")
    public Result<PageResult<AdminEntryVO>> entries(@RequestParam(required = false) Long competitionId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String paymentStatus,
                                                    @RequestParam(required = false) String deliveryStatus,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) Boolean assigned,
                                                    @RequestParam(required = false) String refundStatus,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(entryService.listAdminEntries(competitionId, status, paymentStatus, deliveryStatus,
                categoryId, assigned, refundStatus, keyword, page, pageSize));
    }

    @GetMapping("/refunds")
    public Result<PageResult<AdminEntryVO>> refunds(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(entryService.listAdminRefunds(status, page, pageSize));
    }

    @GetMapping("/bank-transfers")
    public Result<PageResult<BankTransferVO>> bankTransfers(@RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long competitionId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer pageSize) {
        return Result.success(bankTransferPaymentService.listAdminTransfers(status, competitionId, keyword, page, pageSize));
    }

    @GetMapping("/bank-transfers/{id}")
    public Result<BankTransferVO> bankTransferDetail(@PathVariable Long id) {
        return Result.success(bankTransferPaymentService.getAdminTransfer(id));
    }

    @GetMapping("/bank-transfers/{id}/voucher")
    public ResponseEntity<byte[]> downloadBankTransferVoucher(@PathVariable Long id) {
        return fileResponse(bankTransferPaymentService.downloadVoucher(id));
    }

    @PostMapping("/bank-transfers/{id}/confirm")
    public Result<BankTransferVO> confirmBankTransfer(@PathVariable Long id,
                                                      @RequestBody(required = false) @Valid AdminBankTransferProcessRequest request) {
        return Result.success(bankTransferPaymentService.confirmTransfer(id, request));
    }

    @PostMapping("/bank-transfers/{id}/reject")
    public Result<BankTransferVO> rejectBankTransfer(@PathVariable Long id,
                                                     @RequestBody(required = false) @Valid AdminBankTransferProcessRequest request) {
        return Result.success(bankTransferPaymentService.rejectTransfer(id, request));
    }

    @GetMapping("/entries/{id}")
    public Result<AdminEntryDetailVO> entryDetail(@PathVariable Long id) {
        return Result.success(entryService.getAdminEntry(id));
    }

    @PutMapping("/entries/{id}")
    public Result<AdminEntryDetailVO> updateEntry(@PathVariable Long id,
                                                  @RequestBody @Valid AdminEntryUpdateRequest request) {
        return Result.success(entryService.updateAdminEntry(id, request));
    }

    @PostMapping("/entries/{id}/confirm-payment")
    public Result<String> confirmEntryPayment(@PathVariable Long id,
                                              @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.confirmPayment(id, request);
        return Result.success("确认成功");
    }

    @PostMapping("/entries/{id}/mark-stored")
    public Result<String> markEntryStored(@PathVariable Long id,
                                          @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.markStored(id, request);
        return Result.success("入库成功");
    }

    @PostMapping("/entries/{id}/unmark-stored")
    public Result<String> unmarkEntryStored(@PathVariable Long id,
                                            @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.unmarkStored(id, request);
        return Result.success("撤销成功");
    }

    @PostMapping("/entries/{id}/cancel")
    public Result<String> cancelEntry(@PathVariable Long id,
                                      @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.cancelEntry(id, request);
        return Result.success("取消成功");
    }

    @PostMapping("/refunds/{id}/approve")
    public Result<String> approveRefund(@PathVariable Long id,
                                        @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.approveRefund(id, request);
        return Result.success("退款成功");
    }

    @PostMapping("/refunds/{id}/reject")
    public Result<String> rejectRefund(@PathVariable Long id,
                                       @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.rejectRefund(id, request);
        return Result.success("已驳回退款");
    }

    @PostMapping("/refunds/{id}/retry")
    public Result<String> retryRefund(@PathVariable Long id,
                                      @RequestBody(required = false) @Valid AdminEntryStatusRequest request) {
        entryService.retryRefund(id, request);
        return Result.success("退款重试成功");
    }

    @GetMapping("/judges")
    public Result<List<JudgeAccountVO>> judges(@RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(judgeService.listJudges(status, keyword));
    }

    @GetMapping("/judges/page")
    public Result<PageResult<JudgeAccountVO>> judgesPage(@RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(judgeService.pageJudges(status, keyword, page, pageSize));
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

    private ResponseEntity<byte[]> fileResponse(FileDownloadVO file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }
}
