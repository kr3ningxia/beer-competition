package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum CompetitionType {
    AWARD,
    FEEDBACK_ONLY;

    public static CompetitionType of(String value) {
        if (value == null || value.isBlank()) {
            return AWARD;
        }
        for (CompetitionType item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("比赛类型不合法：" + value);
    }
}
