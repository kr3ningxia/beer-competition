package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AwardResultVO {

    private Long id;
    private Long competitionId;
    private Long categoryId;
    private String categoryName;
    private Long beerEntryId;
    private String uuid;
    private String beerName;
    private String style;
    private Long awardRuleId;
    private String awardType;
    private String awardName;
    private Integer rankNo;
    private Long sourceRoundId;
    private String sourceRoundName;
    private Long sourceRoundTableId;
    private String sourceTableName;
    private Long sourceResultId;
    private Boolean champion;
    private String status;
    private Boolean certificateUploaded;
    private String certificateFilename;
    private LocalDateTime certificateUploadedAt;
    private String certificateDownloadUrl;
}
