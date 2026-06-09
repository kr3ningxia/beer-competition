package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CompetitionVO {

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
    private Integer readyCount;
    private Integer checkTotal;
    private Integer alertCount;
    private String nextAction;
    private List<String> dataIntegrityIssues;
    private EntrySummaryVO entriesSummary;
    private ProgressSummaryVO progressSummary;
    private Integer judgeTableCount;
    private Integer judgeCount;
    private String judgeRoleType;
    private String roleLabel;
    private String tableName;
}
