package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalController {

    private final AuthService authService;
    private final CompetitionService competitionService;
    private final EntryService entryService;

    @GetMapping("/public/home")
    public Result<PortalHomeVO> home() {
        return Result.success(competitionService.getPortalHome());
    }

    @GetMapping("/public/competitions")
    public Result<List<PortalCompetitionVO>> publicCompetitions() {
        return Result.success(competitionService.listPortalCompetitions());
    }

    @GetMapping("/public/competitions/{id}")
    public Result<PortalCompetitionVO> publicCompetitionDetail(@PathVariable Long id) {
        return Result.success(competitionService.getPortalCompetitionDetail(id));
    }

    @GetMapping("/public/results")
    public Result<List<PortalCompetitionResultVO>> publicResults() {
        return Result.success(entryService.listPublishedCompetitionResults());
    }

    @GetMapping("/public/results/{competitionId}")
    public Result<PortalCompetitionResultVO> publicResultDetail(@PathVariable Long competitionId) {
        return Result.success(entryService.getPublishedCompetitionResult(competitionId));
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.PORTAL));
    }

    @GetMapping("/entries")
    public Result<List<EntrySummaryVO>> entries() {
        return Result.success(entryService.listPortalEntries());
    }

    @GetMapping("/entries/{id}")
    public Result<EntryDetailVO> entryDetail(@PathVariable Long id) {
        return Result.success(entryService.getPortalEntry(id));
    }

    @PostMapping("/entries/{id}/delivery")
    public Result<EntryDetailVO> submitEntryDelivery(@PathVariable Long id,
                                                     @RequestBody @Valid PortalEntryDeliverySubmitRequest request) {
        return Result.success(entryService.submitPortalEntryDelivery(id, request));
    }

    @PostMapping("/entries/{id}/payment/simulate")
    public Result<EntryDetailVO> simulatePayment(@PathVariable Long id) {
        return Result.success(entryService.simulatePayment(id));
    }

    @PostMapping("/competitions/{competitionId}/entries")
    public Result<EntryDetailVO> submitEntry(@PathVariable Long competitionId,
                                             @RequestBody @Valid PortalEntrySubmitRequest request) {
        return Result.success(entryService.submitPortalEntry(competitionId, request));
    }

    @GetMapping("/entries/{id}/label")
    public Result<PortalEntryLabelVO> entryLabel(@PathVariable Long id) {
        return Result.success(entryService.getPortalEntryLabel(id));
    }

    @GetMapping("/profile")
    public Result<PortalProfileVO> profile() {
        return Result.success(entryService.getPortalProfile());
    }

    @PutMapping("/profile")
    public Result<PortalProfileVO> updateProfile(@RequestBody @Valid PortalProfileUpdateRequest request) {
        return Result.success(entryService.updatePortalProfile(request));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PortalProfileVO> uploadProfileAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(entryService.uploadPortalAvatar(file));
    }

    @GetMapping("/my")
    public Result<PortalMyParticipationVO> myParticipation() {
        return Result.success(entryService.getPortalMyParticipation());
    }

    @GetMapping("/results")
    public Result<List<PortalResultSummaryVO>> results() {
        return Result.success(entryService.listPortalResults());
    }

    @GetMapping("/results/{entryId}")
    public Result<PortalResultDetailVO> resultDetail(@PathVariable Long entryId) {
        return Result.success(entryService.getPortalResultDetail(entryId));
    }

    @GetMapping("/results/{entryId}/certificate")
    public ResponseEntity<byte[]> resultCertificate(@PathVariable Long entryId) {
        return fileResponse(entryService.downloadPortalResultCertificate(entryId));
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
