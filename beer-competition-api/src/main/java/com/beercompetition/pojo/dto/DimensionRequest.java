package com.beercompetition.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DimensionRequest {

    @NotBlank(message = "维度 key 不能为空")
    private String key;

    @NotBlank(message = "维度名称不能为空")
    private String label;

    /**
     * 兼容历史数据的展示类型；新数据以 scored / checkbox 为准。
     */
    private String kind;

    /**
     * 是否为评分项；为空时按历史 kind 推导。
     */
    private Boolean scored;

    /**
     * 是否为勾选项；为空时按历史 kind 推导。
     */
    private Boolean checkbox;

    /**
     * 是否需要补充评语输入框；为空默认需要，文本项和纯评分项始终需要。
     */
    private Boolean noteEnabled;

    private BigDecimal score;

    private BigDecimal maxScore;

    /**
     * CHECK 勾选项的选择方式：SINGLE / MULTI。
     */
    private String selectionMode;

    /**
     * TEXT / CHECK 是否必填。
     */
    private Boolean required;

    /**
     * CHECK 勾选项的候选项配置。
     */
    private List<String> options;

    /**
     * CHECK 勾选项的已选值。
     */
    private List<String> selectedOptions;

    private String notePrompt;

    @Size(max = 500, message = "维度评语不能超过 500 字")
    private String note;
}
