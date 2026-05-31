package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secretKey;
    private String headerName;
    private long adminTtl;
    private long portalTtl;
    private long judgeTtl;
}
