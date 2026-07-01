package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AdminEntryUpdateRequest {

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
    @DecimalMax(value = "99.99", message = "ABV不能大于99.99")
    @Digits(integer = 2, fraction = 2, message = "ABV最多支持两位整数和两位小数")
    private BigDecimal abv;

    private Map<String, Object> extraFields;

    @NotBlank(message = "请填写修改原因")
    @Size(max = 300, message = "修改原因不能超过300个字符")
    private String reason;
}
