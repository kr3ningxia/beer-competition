package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 啤酒币购买支付模式。允许本地验收独立于报名支付使用 MOCK 模式。
 */
@Data
@ConfigurationProperties(prefix = "app.beer-coin")
public class BeerCoinPaymentProperties {

    private String paymentMode = "MOCK";

    public boolean isWechatMode() {
        return "WECHAT".equalsIgnoreCase(paymentMode);
    }

    public String normalizedMode() {
        return isWechatMode() ? "WECHAT" : "MOCK";
    }
}
