package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum AwardResultStatus {
    DRAFT,
    CONFIRMED,
    PUBLISHED;

    public static AwardResultStatus of(String value) {
        for (AwardResultStatus item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("奖项结果状态不合法：" + value);
    }
}
