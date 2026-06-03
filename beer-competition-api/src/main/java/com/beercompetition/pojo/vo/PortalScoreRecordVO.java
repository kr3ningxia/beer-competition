package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PortalScoreRecordVO {

    private String judgeLabel;
    private String judgeRoleType;
    private BigDecimal totalScore;
    private BigDecimal consensusScore;
    private String comments;
    private Boolean finalScore;
    private Boolean advanced;
    private List<PortalScoreDimensionVO> dimensions;
}
