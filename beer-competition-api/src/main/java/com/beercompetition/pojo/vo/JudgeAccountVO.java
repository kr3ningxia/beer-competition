package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeAccountVO {

    private Long id;
    private String name;
    private String phone;
    private String wechat;
    private String qualification;
    private Integer status;
    private String statusLabel;
    private Boolean profileRequired;
    private String reviewRemark;
}
