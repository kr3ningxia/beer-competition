package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminFeedbackReviewPageVO {

    private long total;

    private List<AdminFeedbackReviewEntryVO> records;

    private List<String> tableNames;

    private List<String> categoryNames;
}
