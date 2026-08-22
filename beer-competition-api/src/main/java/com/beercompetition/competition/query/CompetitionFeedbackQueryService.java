package com.beercompetition.competition.query;

import com.beercompetition.pojo.vo.AdminFeedbackReviewEntryVO;

import java.util.List;

/**
 * 提供赛事反馈审核数据，集中处理匿名信息和评语完整性判断。
 */
public interface CompetitionFeedbackQueryService {

    List<AdminFeedbackReviewEntryVO> getFeedbackReviewEntries(Long competitionId);
}
