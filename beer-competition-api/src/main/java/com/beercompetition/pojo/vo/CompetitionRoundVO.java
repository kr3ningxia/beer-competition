package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompetitionRoundVO {

    private Long id;
    private Integer roundNo;
    private String name;
    private String type;
    private String status;
    private Long sourceRoundId;
    private List<String> sourceEntryUuids;
    private Boolean sourceLocked;
    private Boolean candidatesSynced;
    private Boolean preparationDraft;
    private List<RoundTableVO> tables;
}
