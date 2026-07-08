package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WechatPayClientConfigVO {

    private String mode;
    private String appId;
    private boolean jsapiConfigured;
}
