package com.beercompetition.competition.configuration;

import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;

import java.util.List;

/**
 * 维护赛事配置快照，并在写入前检查当前赛事阶段是否允许修改。
 */
public interface CompetitionConfigurationService {

    CompetitionDetailVO updateCategories(Long id, ConfigNameBatchUpdateRequest request);

    CompetitionDetailVO updateStyles(Long id, CompetitionStyleLibraryUpdateRequest request);

    CompetitionDetailVO updateEntryFields(Long id, EntryFieldBatchUpdateRequest request);

    CompetitionDetailVO updateJudgeTables(Long id, JudgeTableBatchUpdateRequest request);

    List<ScoreConfigVO> getScoreConfigs(Long competitionId);

    List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request);
}
