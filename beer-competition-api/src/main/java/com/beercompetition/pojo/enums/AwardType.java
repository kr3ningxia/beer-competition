package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum AwardType {
    MEDAL,
    CHAMPION;

    public static AwardType of(String value) {
        for (AwardType item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("奖项类型不合法：" + value);
    }
}
