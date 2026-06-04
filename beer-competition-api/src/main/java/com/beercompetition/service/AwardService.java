package com.beercompetition.service;

import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;

import java.util.List;

public interface AwardService {

    List<AwardRuleVO> listAwardRules(Long competitionId);

    List<AwardResultVO> listAwardResults(Long competitionId);

    List<AwardResultVO> generateAwardDrafts(Long competitionId);

    List<AwardResultVO> confirmAwards(Long competitionId, AwardConfirmRequest request);

    void publishAwards(Long competitionId);
}
