package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeAssignmentVO {

    private Long id;
    private Long judgeAccountId;
    private Long tableId;
    private String role;
}
