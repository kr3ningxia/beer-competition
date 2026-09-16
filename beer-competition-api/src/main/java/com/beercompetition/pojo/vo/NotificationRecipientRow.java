package com.beercompetition.pojo.vo;

import lombok.Data;

@Data
public class NotificationRecipientRow {

    private Long accountId;
    private Long breweryId;
    private String emailEnc;
    private String emailHash;
    private String displayName;
    private String companyName;
    private String contactName;
    private Integer entryCount;
    private Integer pendingDeliveryCount;
}
