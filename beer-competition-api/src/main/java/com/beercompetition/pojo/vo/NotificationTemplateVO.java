package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationTemplateVO {

    private Long id;
    private String eventCode;
    private String eventLabel;
    private String name;
    private Integer version;
    private String subject;
    private String htmlBody;
    private String status;
    private List<String> availableVariables;
}
