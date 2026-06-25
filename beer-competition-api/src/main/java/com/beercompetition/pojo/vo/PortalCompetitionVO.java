package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortalCompetitionVO {

    private Long id;
    private String code;
    private String name;
    private String competitionType;
    private LocalDate competitionDate;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationDeadline;
    private String status;
    private BigDecimal entryFee;
    private BigDecimal earlyBirdFee;
    private LocalDateTime earlyBirdDeadline;
    private String description;
    private String rulesUrl;
    private CompetitionLogisticsVO logistics;
    private String currentStageLabel;
    private List<CompetitionConfigNameVO> categories;
    private List<CompetitionConfigNameVO> styles;
    private List<EntryFieldConfigVO> entryFields;
}
