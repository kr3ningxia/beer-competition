package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CompetitionBaseInfoUpdateRequest {

    @NotBlank(message = "比赛名称不能为空")
    private String name;

    @NotBlank(message = "比赛编号不能为空")
    private String code;

    @NotBlank(message = "届次不能为空")
    private String edition;

    @NotNull(message = "比赛日期不能为空")
    private LocalDate competitionDate;

    private LocalDateTime registrationStart;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "报名费不能为空")
    private BigDecimal entryFee;
}
