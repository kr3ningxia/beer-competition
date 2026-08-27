package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.organization.application.OrganizerApplicationService;
import com.beercompetition.pojo.dto.OrganizerApplicationSubmitRequest;
import com.beercompetition.pojo.vo.OrganizerApplicationStatusVO;
import com.beercompetition.pojo.vo.OrganizerApplicationSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 当前厂商账号的主办方入驻申请接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/organizer-applications")
public class PortalOrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    @GetMapping
    public Result<List<OrganizerApplicationStatusVO>> listMine() {
        return Result.success(organizerApplicationService.listForCurrentPortal());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrganizerApplicationSubmitVO> submit(@RequestBody @Valid OrganizerApplicationSubmitRequest request) {
        return Result.success(organizerApplicationService.submitForCurrentPortal(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OrganizerApplicationSubmitVO> submitWithMaterial(
            @ModelAttribute @Valid OrganizerApplicationSubmitRequest request,
            @RequestPart(value = "material", required = false) MultipartFile material) {
        return Result.success(organizerApplicationService.submitForCurrentPortal(request, material));
    }

    @PutMapping(value = "/{applicationNo}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrganizerApplicationStatusVO> resubmit(
            @PathVariable String applicationNo,
            @RequestBody @Valid OrganizerApplicationSubmitRequest request) {
        return Result.success(organizerApplicationService.resubmitForCurrentPortal(applicationNo, request));
    }

    @PutMapping(value = "/{applicationNo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OrganizerApplicationStatusVO> resubmitWithMaterial(
            @PathVariable String applicationNo,
            @ModelAttribute @Valid OrganizerApplicationSubmitRequest request,
            @RequestPart(value = "material", required = false) MultipartFile material) {
        return Result.success(organizerApplicationService.resubmitForCurrentPortal(applicationNo, request, material));
    }
}
