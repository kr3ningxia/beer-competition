package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalProfileVO {

    private Long accountId;
    private Long breweryId;
    private String displayName;
    private String companyName;
    private String contactName;
    private String phone;
    private String wechat;
    private String avatarUrl;
}
