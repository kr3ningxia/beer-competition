package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StyleLibraryVisibilityRequest {

    @NotBlank(message = "可见范围不能为空")
    private String visibility;
}
