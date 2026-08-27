package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.organization.application.OrganizerApplicationService;
import com.beercompetition.organization.application.OrganizerProvisioningService;
import com.beercompetition.pojo.dto.OrganizerApplicationReviewRequest;
import com.beercompetition.pojo.vo.OrganizerApplicationAdminVO;
import com.beercompetition.pojo.vo.OrganizerProvisioningVO;
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

/**
 * 平台超级管理员处理主办方入驻申请。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/organizer-applications")
public class AdminOrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;
    private final OrganizerProvisioningService organizerProvisioningService;

    @GetMapping
    public Result<List<OrganizerApplicationAdminVO>> list(@RequestParam(required = false) String status) {
        return Result.success(organizerApplicationService.listForPlatform(status));
    }

    @PutMapping("/{id}/review")
    public Result<OrganizerApplicationAdminVO> review(@PathVariable Long id,
                                                      @RequestBody @Valid OrganizerApplicationReviewRequest request) {
        return Result.success(organizerApplicationService.review(id, request));
    }

    @PostMapping("/{id}/provision")
    public Result<OrganizerProvisioningVO> provision(@PathVariable Long id) {
        OrganizerProvisioningService.ProvisioningResult result =
                organizerProvisioningService.provisionApprovedApplication(id);
        return Result.success(OrganizerProvisioningVO.builder()
                .organizerId(result.organizerId())
                .adminUserId(result.adminUserId())
                .username(result.username())
                .initialPassword(result.initialPassword())
                .newlyIssued(result.initialPassword() != null)
                .build());
    }
}
