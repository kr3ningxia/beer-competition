package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeAccountVO {

    private String publicId;
    private String name;
    private String phone;
    private String wechat;
    private String maskedPhone;
    private String maskedWechat;
    private String qualification;
    private Boolean breweryConflictFlag;
    private String breweryConflictText;
    private Boolean phoneBreweryConflictFlag;
    private Long phoneConflictBreweryId;
    private String phoneConflictBreweryName;
    private String phoneConflictText;
    private Integer status;
    private String statusLabel;
    private Boolean profileRequired;
    private String reviewRemark;
}
