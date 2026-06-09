package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CompetitionDetailVO {

    private Long id;
    private String code;
    private String name;
    private LocalDate competitionDate;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationDeadline;
    private String status;
    private BigDecimal entryFee;
    private BigDecimal earlyBirdFee;
    private LocalDateTime earlyBirdDeadline;
    private String description;
    private String rulesUrl;
    private String styleLibraryVersion;
    private CompetitionLogisticsVO logistics;
    private String currentStageLabel;
    private CompetitionPrimaryActionVO primaryAction;
    private List<CompetitionConfigNameVO> categories;
    private List<CompetitionConfigNameVO> styles;
    private List<EntryFieldConfigVO> entryFields;
    private List<JudgeTableVO> judgeTables;
    private List<ScoreConfigVO> scoreConfigs;
    private List<CompetitionCheckVO> checks;
    private List<CompetitionStageCheckVO> stageChecks;
    private Map<String, Boolean> editableScopes;
    private EntrySummaryVO entriesSummary;
    private List<CompetitionEntryVO> entries;
    private List<CompetitionEntryVO> entryPool;
    private List<CompetitionRoundVO> rounds;
    private CompetitionRoundVO currentRound;
    private List<ResultDraftVO> resultDrafts;
    private List<AwardRuleVO> awardRules;
    private List<AwardResultVO> awardResults;
    private ProgressSummaryVO progressSummary;
    private ResultSetupVO resultSetup;
    private List<CompetitionAlertVO> alerts;
    private List<String> dataIntegrityIssues;
}
