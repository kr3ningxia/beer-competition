package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeTableVO {

    private Long id;
    private String tableName;
    private Integer captainCount;
    private Integer professionalCount;
    private Integer crossCount;
    private Integer finalized;
    private Integer total;
}
