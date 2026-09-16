package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail.notification")
public class EmailNotificationProperties {

    private boolean enabled = true;
    private int batchSize = 20;
    private int maxAttempts = 5;
    private int retryDelayMinutes = 5;
    private String portalBaseUrl = "http://localhost:5173";
}
