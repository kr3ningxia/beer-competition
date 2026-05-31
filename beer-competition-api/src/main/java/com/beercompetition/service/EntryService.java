package com.beercompetition.service;

import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;

import java.util.List;

public interface EntryService {

    List<EntrySummaryVO> listPortalEntries();

    EntryDetailVO getPortalEntry(Long entryId);

    JudgeEntryVO getJudgeEntry(String uuid);
}
