package com.beercompetition.pojo.dto;

import lombok.Data;

/**
 * 设置厂商端首页重点赛事。competitionId 为空表示取消手动指定，回到自动选取规则。
 */
@Data
public class HomeFeaturedCompetitionUpdateRequest {

    private Long competitionId;
}
