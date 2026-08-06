package com.beercompetition.registration.entry;

import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;

import java.util.List;

/**
 * 处理厂商报名的创建、维护、取消和参赛概览。
 */
public interface PortalEntryService {

    List<EntrySummaryVO> listPortalEntries();

    EntryDetailVO getPortalEntry(Long entryId);

    EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request);

    EntryDetailVO updatePortalEntry(Long entryId, PortalEntryUpdateRequest request);

    EntryDetailVO cancelPortalEntry(Long entryId);

    PortalMyParticipationVO getPortalMyParticipation();
}
