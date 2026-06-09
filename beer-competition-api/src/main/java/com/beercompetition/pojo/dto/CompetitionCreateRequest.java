package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompetitionCreateRequest {

    @NotBlank(message = "比赛名称不能为空")
    private String name;

    @NotNull(message = "比赛日期不能为空")
    private LocalDate competitionDate;

    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime registrationStart;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "报名费不能为空")
    @DecimalMin(value = "0.00", message = "报名费不能小于 0")
    private BigDecimal entryFee;

    @DecimalMin(value = "0.00", message = "早鸟价不能小于 0")
    private BigDecimal earlyBirdFee;

    private LocalDateTime earlyBirdDeadline;

    @NotBlank(message = "赛事简介不能为空")
    @Size(max = 1000, message = "赛事简介不能超过 1000 个字符")
    private String description;

    @Size(max = 500, message = "参赛细则链接不能超过 500 个字符")
    private String rulesUrl;

    @NotBlank(message = "基础风格库不能为空")
    private String styleLibraryVersion;

    private String deliveryMethod;

    private LocalDateTime sampleArrivalStart;

    private LocalDateTime sampleArrivalDeadline;

    @Size(max = 255, message = "酒样数量要求不能超过 255 个字符")
    private String sampleQuantityNote;

    @Size(max = 64, message = "收件人不能超过 64 个字符")
    private String deliveryRecipient;

    @Size(max = 64, message = "收件联系电话不能超过 64 个字符")
    private String deliveryPhone;

    @Size(max = 500, message = "收件地址不能超过 500 个字符")
    private String deliveryAddress;

    @Size(max = 1000, message = "送样说明不能超过 1000 个字符")
    private String deliveryNote;

    private String logisticsVisibility;

    @Valid
    @NotEmpty(message = "投递组别不能为空")
    private List<ConfigNameItemRequest> categories;

    @Valid
    private List<EntryFieldItemRequest> entryFields;

    @Valid
    @NotEmpty(message = "评分表不能为空")
    private List<ScoreConfigItemRequest> scoreConfigs;
}
