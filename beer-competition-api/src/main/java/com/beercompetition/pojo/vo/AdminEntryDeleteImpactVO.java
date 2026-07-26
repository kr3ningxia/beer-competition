package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminEntryDeleteImpactVO {

    private Long entryId;
    private String entryName;
    private String shortCode;
    private String status;
    private String paymentStatus;
    private String payMethod;
    private String deliveryStatus;
    private Integer roundAssignmentCount;
    private Integer scoreSessionCount;
    private Integer scoreRecordCount;
    private Integer roundResultCount;
    private Integer awardResultCount;
    private Boolean resultPublished;
    private Boolean highRisk;
    private Boolean refundConfirmationRequired;
}
