package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 首页重点赛事下拉选项，包含比赛名称、状态和当前是否已被指定。
 */
@Data
@Builder
public class HomeFeaturedCompetitionOptionVO {

    private Long id;
    private String name;
    private String status;
    private Boolean homeFeatured;
}
