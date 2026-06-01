package com.beercompetition.pojo.vo;

import com.beercompetition.pojo.dto.DimensionRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScoreConfigVO {

    private Long competitionId;
    private String judgeRoleType;
    private Integer minCommentLength;
    private List<DimensionRequest> dimensions;
}
