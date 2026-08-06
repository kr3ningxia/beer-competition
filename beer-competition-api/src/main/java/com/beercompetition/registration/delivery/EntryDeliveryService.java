package com.beercompetition.registration.delivery;

import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;

/**
 * 维护厂商送样信息，送样状态仍受报名归属和赛事阶段约束。
 */
public interface EntryDeliveryService {

    EntryDetailVO submitPortalEntryDelivery(Long entryId, PortalEntryDeliverySubmitRequest request);
}
