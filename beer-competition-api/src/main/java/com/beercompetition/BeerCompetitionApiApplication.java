package com.beercompetition;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.beercompetition.mapper")
@ConfigurationPropertiesScan("com.beercompetition")
@EnableScheduling
public class BeerCompetitionApiApplication {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    public static void main(String[] args) {
        configureBusinessTimeZone();
        SpringApplication.run(BeerCompetitionApiApplication.class, args);
    }

    @PostConstruct
    void initializeBusinessTimeZone() {
        configureBusinessTimeZone();
    }

    private static void configureBusinessTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_ZONE));
    }
}
