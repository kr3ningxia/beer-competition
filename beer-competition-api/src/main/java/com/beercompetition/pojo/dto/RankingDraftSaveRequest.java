package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RankingDraftSaveRequest {

    /** 奖牌轮桌内组别；普通排序轮可为空。 */
    private Long categoryId;

    @Valid
    @NotNull(message = "参考排序不能为空")
    private List<@NotNull(message = "参考排序项不能为空") RankingResultItemRequest> results;
}
