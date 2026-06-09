package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PortalEntrySubmitRequest {

    @NotBlank(message = "酒款名称不能为空")
    @Size(max = 128, message = "酒款名称不能超过128个字符")
    private String name;

    @NotNull(message = "投递组别不能为空")
    private Long categoryId;

    @NotBlank(message = "基础风格不能为空")
    @Size(max = 128, message = "基础风格不能超过128个字符")
    private String style;

    @NotNull(message = "ABV不能为空")
    @DecimalMin(value = "0.0", message = "ABV不能小于0")
    @DecimalMax(value = "99.9", message = "ABV不能大于99.9")
    private BigDecimal abv;

    private Map<String, Object> extraFields = new LinkedHashMap<>();
}
