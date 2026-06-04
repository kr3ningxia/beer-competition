package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 128, message = "场地名称不能超过 128 个字符")
    private String venueName;

    @Size(max = 500, message = "场地地址不能超过 500 个字符")
    private String venueAddress;

    @Size(max = 255, message = "现场时间说明不能超过 255 个字符")
    private String venueTimeNote;

    @Size(max = 128, message = "现场联系人不能超过 128 个字符")
    private String venueContact;

    @Size(max = 500, message = "地图链接不能超过 500 个字符")
    private String venueMapUrl;

    private String logisticsVisibility;
}
