package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompetitionCreateRequest {

    @NotBlank(message = "比赛名称不能为空")
    private String name;

    @NotBlank(message = "届次不能为空")
    private String edition;

    @NotNull(message = "比赛日期不能为空")
    private LocalDate competitionDate;

    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime registrationStart;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "报名费不能为空")
    @DecimalMin(value = "0.00", message = "报名费不能小于 0")
    private BigDecimal entryFee;

    @NotBlank(message = "基础风格库不能为空")
    private String styleLibraryVersion;

    @Valid
    @NotEmpty(message = "投递组别不能为空")
    private List<ConfigNameItemRequest> categories;

    @Valid
    private List<EntryFieldItemRequest> entryFields;

    @Valid
    @NotEmpty(message = "评分表不能为空")
    private List<ScoreConfigItemRequest> scoreConfigs;
}
