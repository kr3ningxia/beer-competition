package com.beercompetition.service;

import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;

import java.util.List;

public interface EntryService {

    List<EntrySummaryVO> listPortalEntries();

    EntryDetailVO getPortalEntry(Long entryId);

    EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request);

    EntryDetailVO submitPortalEntryDelivery(Long entryId, PortalEntryDeliverySubmitRequest request);

    PortalEntryLabelVO getPortalEntryLabel(Long entryId);

    PortalProfileVO getPortalProfile();

    PortalProfileVO updatePortalProfile(PortalProfileUpdateRequest request);

    PortalMyParticipationVO getPortalMyParticipation();

    List<PortalResultSummaryVO> listPortalResults();

    PortalResultDetailVO getPortalResultDetail(Long entryId);

    void confirmPayment(Long entryId);

    void markStored(Long entryId);

    void cancelEntry(Long entryId);

    JudgeEntryVO getJudgeEntry(String uuid);

    JudgeEntryVO resolveJudgeScan(String code);
}
