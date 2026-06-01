package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private long codeTtlMinutes;
    private boolean mockEnabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String endpoint = "dypnsapi.aliyuncs.com";
    private String schemeName = "默认方案";
    private String signName;
    private String templateCode;
    private String countryCode = "86";
    private long sendIntervalSeconds = 60;
    private long codeLength = 6;
    private long codeType = 1;
    private long duplicatePolicy = 1;
}
