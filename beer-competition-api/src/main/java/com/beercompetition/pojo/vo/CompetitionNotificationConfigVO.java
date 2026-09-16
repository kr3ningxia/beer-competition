package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CompetitionNotificationConfigVO {

    private Long competitionId;
    private String competitionName;
    private String competitionCode;
    private LocalDateTime sampleArrivalStart;
    private LocalDateTime sampleArrivalDeadline;
    private String status;
    private List<NotificationRuleVO> rules;
    private List<NotificationTemplateVO> templates;
    private NotificationStatsVO stats;
    private Map<String, String> previewVariables;
}
