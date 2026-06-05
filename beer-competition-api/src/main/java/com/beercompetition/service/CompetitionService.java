package com.beercompetition.service;

import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;

import java.util.List;

public interface CompetitionService {

    List<CompetitionVO> listCompetitions();

    PortalHomeVO getPortalHome();

    List<PortalCompetitionVO> listPortalCompetitions();

    PortalCompetitionVO getPortalCompetitionDetail(Long id);

    CompetitionVO createCompetition(CompetitionCreateRequest request);

    CompetitionDetailVO getCompetitionDetail(Long id);

    void deleteCompetition(Long id);

    CompetitionDetailVO updateBaseInfo(Long id, CompetitionBaseInfoUpdateRequest request);

    CompetitionDetailVO updateCategories(Long id, ConfigNameBatchUpdateRequest request);

    CompetitionDetailVO updateStyles(Long id, CompetitionStyleLibraryUpdateRequest request);

    CompetitionDetailVO updateEntryFields(Long id, EntryFieldBatchUpdateRequest request);

    CompetitionDetailVO updateJudgeTables(Long id, JudgeTableBatchUpdateRequest request);

    CompetitionDetailVO openRegistration(Long id);

    CompetitionDetailVO closeRegistration(Long id);

    CompetitionDetailVO prepareJudging(Long id);

    List<ScoreConfigVO> getScoreConfigs(Long competitionId);

    List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request);

    byte[] exportScoringData(Long competitionId);
}
