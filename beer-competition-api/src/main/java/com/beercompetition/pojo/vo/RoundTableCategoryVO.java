package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 评审端显示的一张桌内组别工作区。 */
@Data
@Builder
public class RoundTableCategoryVO {

    private Long categoryId;
    private String categoryName;
    private Integer resultVersion;
    private String status;
    private Integer entryCount;
    private Integer resultCount;
    private Integer confirmedCount;
    private Integer requiredCount;
    private Boolean readyForConfirmation;
    private Boolean readyForFinalSubmit;
    private List<RoundRankingSlotVO> rankings;
    private List<RoundRankingSlotVO> myRankingDraft;
}
