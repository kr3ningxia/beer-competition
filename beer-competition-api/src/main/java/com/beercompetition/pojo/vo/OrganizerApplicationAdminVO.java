package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizerApplicationAdminVO {

    private Long id;
    private String applicationNo;
    private String organizationName;
    private String contactName;
    private String contactPhone;
    private String maskedContactPhone;
    private String contactEmail;
    private String maskedWechat;
    private String wechat;
    private String businessDescription;
    private String expectedScale;
    private String supplementalNote;
    private Long materialAssetId;
    private String materialFileName;
    private String status;
    private String statusLabel;
    private String reviewRemark;
    private Long organizerId;
    private Long initialAdminUserId;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime accountIssuedTime;
}
