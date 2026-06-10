package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminFeedbackDimensionNoteRequest {

    @NotBlank(message = "评分维度不能为空")
    private String key;

    @Size(max = 500, message = "维度备注不能超过 500 字")
    private String note;
}
