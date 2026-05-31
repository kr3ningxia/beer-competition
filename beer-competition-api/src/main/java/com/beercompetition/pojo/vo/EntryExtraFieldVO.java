package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntryExtraFieldVO {

    private String key;
    private String label;
    private String value;
}
