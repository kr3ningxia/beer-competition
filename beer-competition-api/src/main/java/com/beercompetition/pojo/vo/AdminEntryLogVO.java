package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminEntryLogVO {

    private Long id;
    private Long adminUserId;
    private String action;
    private String summary;
    private LocalDateTime createTime;
}
