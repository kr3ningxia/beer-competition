package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum RoundType {
    SCORE,
    RANKING;

    public static RoundType of(String value) {
        for (RoundType item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("轮次类型不合法：" + value);
    }
}
