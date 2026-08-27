package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizerProvisioningVO {

    private Long organizerId;
    private Long adminUserId;
    private String username;
    private String initialPassword;
    private Boolean newlyIssued;
}
