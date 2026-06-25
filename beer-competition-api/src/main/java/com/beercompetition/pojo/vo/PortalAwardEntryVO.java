package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalAwardEntryVO {

    private Long id;
    private Long awardResultId;
    private Long roundResultId;
    private String resultType;
    private String slotLabel;
    private String awardType;
    private String awardName;
    private Integer rankNo;
    private Boolean champion;
    private Long beerEntryId;
    private String beerName;
    private String breweryName;
    private Long groupId;
    private String groupName;
    private String style;
}
