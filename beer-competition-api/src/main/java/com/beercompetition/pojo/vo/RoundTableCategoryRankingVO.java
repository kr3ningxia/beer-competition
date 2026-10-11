package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 后台排序详情里按组别展示的名次结果（多组别奖牌桌专用）。 */
@Data
@Builder
public class RoundTableCategoryRankingVO {

    private Long categoryId;
    private String categoryName;
    private Integer filledCount;
    private List<RoundRankingSlotVO> rankings;
}
