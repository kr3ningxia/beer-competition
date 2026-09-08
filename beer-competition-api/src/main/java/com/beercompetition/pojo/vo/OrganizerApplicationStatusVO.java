package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizerApplicationStatusVO {

    private String applicationNo;
    private String organizationName;
    private String contactName;
    private String maskedContactPhone;
    private String status;
    private String statusLabel;
    private String adminUsername;
    private String initialPassword;
    private String reviewRemark;
    private LocalDateTime submittedTime;
    private LocalDateTime accountIssuedTime;
}
