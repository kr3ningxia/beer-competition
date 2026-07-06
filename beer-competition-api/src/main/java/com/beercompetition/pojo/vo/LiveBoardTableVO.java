package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveBoardTableVO {

    private Long id;
    private String displayName;
    private String personalProgress;
    private String captainProgress;
    private String confirmationProgress;
    private Integer reviewedCount;
    private Integer pendingCount;
    private Integer completionPercent;
    private Integer averageCommentChars;
    private String averageCommentText;
    private String statusText;
    private String tone;
}
