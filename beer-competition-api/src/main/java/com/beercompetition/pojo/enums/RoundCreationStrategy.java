package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum RoundCreationStrategy {
    MERGE_ONE,
    BY_CATEGORY,
    EVEN_SPLIT,
    MANUAL;

    public static RoundCreationStrategy of(String value) {
        for (RoundCreationStrategy item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("创建轮次策略不合法：" + value);
    }
}
