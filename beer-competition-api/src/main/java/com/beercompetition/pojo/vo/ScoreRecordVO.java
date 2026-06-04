package com.beercompetition.pojo.vo;

import com.beercompetition.pojo.dto.DimensionRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScoreRecordVO {

    private Long id;
    private String beerUuid;
    private String shortCode;
    private String categoryName;
    private String style;
    private String judgeName;
    private String judgeRoleType;
    private String roleLabel;
    private List<DimensionRequest> dimensions;
    private BigDecimal totalScore;
    private String comments;
    private Integer isFinal;
    private Integer isAdvanced;
    private BigDecimal consensusScore;
    private Boolean locked;
    private LocalDateTime submittedAt;
}
