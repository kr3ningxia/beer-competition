package com.beercompetition.competition.query;

/**
 * 生成赛事评分归档文件，导出内容保持评审记录的历史快照语义。
 */
public interface CompetitionScoringExportService {

    byte[] exportScoringData(Long competitionId);
}
