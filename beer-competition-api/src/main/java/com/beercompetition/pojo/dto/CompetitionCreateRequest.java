package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.CompetitionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CompetitionCreateRequest {

    @NotBlank(message = "比赛名称不能为空")
    private String name;

    @NotNull(message = "比赛日期不能为空")
    private LocalDate competitionDate;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "比赛状态不能为空")
    private CompetitionStatus status;

    @NotNull(message = "报名费不能为空")
    private BigDecimal entryFee;
}
