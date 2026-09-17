package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortalCompetitionSummaryVO {

    private Long id;
    private String code;
    private String name;
    private String competitionType;
    private String organizerType;
    private String organizerName;
    private LocalDate competitionDate;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationDeadline;
    private String status;
    private BigDecimal entryFee;
    private BigDecimal earlyBirdFee;
    private LocalDateTime earlyBirdDeadline;
    private String description;
    private String currentStageLabel;
    private LocalDateTime sampleArrivalDeadline;
    private List<PortalCategorySummaryVO> categories;
}
