package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {

    private Long userId;
    private String role;
    private String username;
    private String displayName;
    private String adminType;
    private Long organizerId;
    private String organizerName;
    private String organizerType;
    private Boolean mustChangePassword;
    private Boolean mustChangeUsername;
    private String phone;
    private String wechat;
    private String qualification;
    private Integer status;
    private String statusLabel;
    private Boolean profileRequired;
    private Boolean profileComplete;
    private Boolean canScore;
    private Long currentCompetitionId;
    private String currentCompetition;
    private String judgeRoleType;
    private String roleLabel;
    private String tableName;
}
