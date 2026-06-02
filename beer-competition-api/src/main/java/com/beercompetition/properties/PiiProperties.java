package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.pii")
public class PiiProperties {

    private String aesKey;
    private String hashKey;
}
