package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompetitionStyleLibraryUpdateRequest {

    @NotBlank(message = "基础风格库不能为空")
    private String styleLibraryVersion;
}
