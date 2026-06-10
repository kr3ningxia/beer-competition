package com.beercompetition.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.export")
public class ExportProperties {

    private String judgeH5BaseUrl = "http://localhost:5174";
}
