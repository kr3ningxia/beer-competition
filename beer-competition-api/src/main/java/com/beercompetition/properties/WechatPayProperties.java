package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.wechat-pay")
public class WechatPayProperties {

    private String mode = "MOCK";
    private String appId;
    private String mchId;
    private String merchantSerialNo;
    private String apiV3Key;
    private String privateKeyPath;
    private String notifyBaseUrl;

    public boolean isWechatMode() {
        return "WECHAT".equalsIgnoreCase(mode);
    }

    public String normalizedMode() {
        return isWechatMode() ? "WECHAT" : "MOCK";
    }
}
