package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CompetitionVO {

    private Long id;
    private String name;
    private LocalDate competitionDate;
    private LocalDateTime registrationDeadline;
    private String status;
    private BigDecimal entryFee;
}
