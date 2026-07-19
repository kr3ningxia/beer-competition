package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalEntryBatchQuoteRequest;
import com.beercompetition.pojo.dto.PortalEntryBatchSubmitRequest;
import com.beercompetition.pojo.vo.RegistrationBatchQuoteVO;
import com.beercompetition.pojo.vo.RegistrationBatchVO;
import com.beercompetition.service.RegistrationBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalRegistrationBatchController {

    private final RegistrationBatchService registrationBatchService;

    @PostMapping("/competitions/{competitionId}/entry-batches/quote")
    public Result<RegistrationBatchQuoteVO> quote(@PathVariable Long competitionId,
                                                   @RequestBody @Valid PortalEntryBatchQuoteRequest request) {
        return Result.success(registrationBatchService.quote(competitionId, request));
    }

    @PostMapping("/competitions/{competitionId}/entry-batches")
    public Result<RegistrationBatchVO> submit(@PathVariable Long competitionId,
                                               @RequestBody @Valid PortalEntryBatchSubmitRequest request) {
        return Result.success(registrationBatchService.submit(competitionId, request));
    }

    @GetMapping("/entry-batches/{batchId}")
    public Result<RegistrationBatchVO> detail(@PathVariable Long batchId) {
        return Result.success(registrationBatchService.getPortalBatch(batchId));
    }
}
