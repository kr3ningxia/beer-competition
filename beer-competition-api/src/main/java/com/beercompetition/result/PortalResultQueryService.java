package com.beercompetition.result;

import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;

import java.util.List;

/**
 * 向厂商端提供已经发布的赛事结果、评分反馈和证书下载。
 */
public interface PortalResultQueryService {

    List<PortalCompetitionResultVO> listPublishedCompetitionResults();

    PortalCompetitionResultVO getPublishedCompetitionResult(Long competitionId);

    List<PortalResultSummaryVO> listPortalResults();

    PortalResultDetailVO getPortalResultDetail(Long entryId);

    FileDownloadVO downloadPortalResultCertificate(Long entryId);
}
