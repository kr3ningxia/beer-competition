package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardConfirmItemRequest {

    private Long id;
    private Long categoryId;

    @NotNull(message = "获奖酒款不能为空")
    private Long beerEntryId;

    private Long awardRuleId;

    @NotBlank(message = "奖项类型不能为空")
    private String awardType;

    @NotBlank(message = "奖项名称不能为空")
    private String awardName;

    private Integer rankNo;
    private Long sourceRoundId;
    private Long sourceRoundTableId;
    private Long sourceResultId;
}
