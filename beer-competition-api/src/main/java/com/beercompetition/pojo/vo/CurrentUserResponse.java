package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {

    private Long userId;
    private String role;
    private String displayName;
    private String phone;
    private String wechat;
    private String qualification;
    private Integer status;
    private String statusLabel;
    private Boolean profileRequired;
    private Boolean canScore;
    private Long currentCompetitionId;
    private String currentCompetition;
    private String judgeRoleType;
    private String roleLabel;
    private String tableName;
}
