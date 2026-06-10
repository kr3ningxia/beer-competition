package com.beercompetition;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.beercompetition.mapper")
@ConfigurationPropertiesScan("com.beercompetition")
@EnableScheduling
public class BeerCompetitionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeerCompetitionApiApplication.class, args);
    }
}
