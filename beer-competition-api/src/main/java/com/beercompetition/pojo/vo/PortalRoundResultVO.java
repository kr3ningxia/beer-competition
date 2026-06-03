package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalRoundResultVO {

    private String resultType;
    private Integer rankNo;
    private String slotLabel;
    private Boolean locked;
}
