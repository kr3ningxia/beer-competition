package com.beercompetition.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminOperationLogVO {

    private Long id;
    private LocalDateTime createTime;
    private Long adminUserId;
    private String adminUsername;
    private String adminName;
    private String action;
    private String actionLabel;
    private String actionGroup;
    private String riskLevel;
    private String targetType;
    private String targetLabel;
    private String targetPublicId;
    private String targetName;
    private String targetRoute;
    private String summary;
    private String summaryText;
    private String detailText;
}
