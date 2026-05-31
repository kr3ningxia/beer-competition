package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private long codeTtlMinutes;
    private boolean mockEnabled;
}
