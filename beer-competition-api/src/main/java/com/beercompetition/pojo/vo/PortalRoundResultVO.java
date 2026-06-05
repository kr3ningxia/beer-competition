package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalRoundResultVO {

    private String resultType;
    private Integer rankNo;
    private String slotLabel;
    private String awardType;
    private String awardName;
    private Boolean champion;
    private Boolean certificateAvailable;
    private String certificateFilename;
    private Boolean locked;
}
