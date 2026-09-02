package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    private String accessKeyId;
    private String accessKeySecret;
    private String endpoint = "dm.aliyuncs.com";
    private String fromAddress = "beernotice@notify.beermatters.cn";
    private String fromAlias = "啤酒大赛";
    private String replyAddress;
}
