package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeAssignmentVO {

    private Long id;
    private String judgePublicId;
    private String judgeName;
    private String qualification;
    private Long tableId;
    private String role;
    private String status;
    private String statusLabel;
    private String withdrawnTime;
}
