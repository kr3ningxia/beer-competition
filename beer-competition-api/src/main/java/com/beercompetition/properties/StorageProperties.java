package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider;
    private String localBaseDir;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
