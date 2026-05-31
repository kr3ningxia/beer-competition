package com.beercompetition.pojo.vo;

import com.beercompetition.pojo.dto.DimensionRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ScoreRecordVO {

    private Long id;
    private Long judgeAccountId;
    private String judgeRoleType;
    private List<DimensionRequest> dimensions;
    private BigDecimal totalScore;
    private String comments;
    private Integer isFinal;
    private Integer isAdvanced;
    private BigDecimal consensusScore;
}
