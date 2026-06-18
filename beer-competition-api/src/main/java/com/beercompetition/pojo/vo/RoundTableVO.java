package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoundTableVO {

    private Long id;
    private String name;
    private String captainPublicId;
    private Integer professionalCount;
    private Integer crossCount;
    private Long categoryId;
    private String categoryMode;
    private String categoryName;
    private Integer targetCount;
    private String targetMode;
    private String status;
    private List<String> entryUuids;
    private Integer advancedCount;
    private Integer judgeProgress;
    private Integer captainProgress;
    private Integer averageDurationSeconds;
    private Integer averageCommentChars;
    private Integer resultVersion;
    private Integer confirmationConfirmedCount;
    private Integer confirmationRequiredCount;
    private Boolean confirmationReady;
    private Boolean confirmationOverrideFlag;
    private String confirmationOverrideReason;
    private LocalDateTime confirmationOverrideTime;
    private List<RoundTableMemberVO> members;
    private List<RoundTableJudgeProgressVO> judgeDetails;
    private List<RoundRankingSlotVO> rankings;
}
