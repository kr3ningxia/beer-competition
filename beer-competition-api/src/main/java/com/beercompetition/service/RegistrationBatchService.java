package com.beercompetition.service;

import com.beercompetition.pojo.dto.PortalEntryBatchQuoteRequest;
import com.beercompetition.pojo.dto.PortalEntryBatchSubmitRequest;
import com.beercompetition.pojo.vo.RegistrationBatchQuoteVO;
import com.beercompetition.pojo.vo.RegistrationBatchVO;

public interface RegistrationBatchService {

    RegistrationBatchQuoteVO quote(Long competitionId, PortalEntryBatchQuoteRequest request);

    RegistrationBatchVO submit(Long competitionId, PortalEntryBatchSubmitRequest request);

    RegistrationBatchVO getPortalBatch(Long batchId);
}
