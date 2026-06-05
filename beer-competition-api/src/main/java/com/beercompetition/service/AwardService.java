package com.beercompetition.service;

import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AwardService {

    List<AwardRuleVO> listAwardRules(Long competitionId);

    List<AwardResultVO> listAwardResults(Long competitionId);

    List<AwardResultVO> generateAwardDrafts(Long competitionId);

    List<AwardResultVO> generateAwardDraftsForRound(Long competitionId, Long roundId);

    List<AwardResultVO> confirmAwards(Long competitionId, AwardConfirmRequest request);

    void publishAwards(Long competitionId);

    AwardResultVO uploadCertificate(Long competitionId, Long awardId, MultipartFile file);

    void deleteCertificate(Long competitionId, Long awardId);

    FileDownloadVO downloadCertificate(Long competitionId, Long awardId);
}
