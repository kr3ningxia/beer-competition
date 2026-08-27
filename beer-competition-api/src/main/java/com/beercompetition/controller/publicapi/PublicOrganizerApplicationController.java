package com.beercompetition.controller.publicapi;

import com.beercompetition.common.result.Result;
import com.beercompetition.organization.application.OrganizerApplicationService;
import com.beercompetition.pojo.dto.OrganizerApplicationSubmitRequest;
import com.beercompetition.pojo.vo.OrganizerApplicationStatusVO;
import com.beercompetition.pojo.vo.OrganizerApplicationSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公开入驻申请接口。申请人不因此获得后台身份。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/organizer-applications")
public class PublicOrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrganizerApplicationSubmitVO> submit(@RequestBody @Valid OrganizerApplicationSubmitRequest request) {
        return Result.success(organizerApplicationService.submit(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OrganizerApplicationSubmitVO> submitWithMaterial(
            @ModelAttribute @Valid OrganizerApplicationSubmitRequest request,
            @RequestPart(value = "material", required = false) MultipartFile material) {
        return Result.success(organizerApplicationService.submit(request, material));
    }

    @GetMapping("/{applicationNo}")
    public Result<OrganizerApplicationStatusVO> status(@PathVariable String applicationNo,
                                                       @RequestParam String contactPhone) {
        return Result.success(organizerApplicationService.queryStatus(applicationNo, contactPhone));
    }

    @PutMapping(value = "/{applicationNo}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<OrganizerApplicationStatusVO> resubmit(@PathVariable String applicationNo,
                                                         @RequestParam String contactPhone,
                                                         @RequestBody @Valid OrganizerApplicationSubmitRequest request) {
        return Result.success(organizerApplicationService.resubmit(applicationNo, contactPhone, request));
    }

    @PutMapping(value = "/{applicationNo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OrganizerApplicationStatusVO> resubmitWithMaterial(
            @PathVariable String applicationNo,
            @RequestParam String contactPhone,
            @ModelAttribute @Valid OrganizerApplicationSubmitRequest request,
            @RequestPart(value = "material", required = false) MultipartFile material) {
        return Result.success(organizerApplicationService.resubmit(applicationNo, contactPhone, request, material));
    }
}
