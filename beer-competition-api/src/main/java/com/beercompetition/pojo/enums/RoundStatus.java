package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum RoundStatus {
    DRAFT,
    PUBLISHED,
    IN_PROGRESS,
    SUBMITTED,
    LOCKED;

    public static RoundStatus of(String value) {
        for (RoundStatus item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("轮次状态不合法：" + value);
    }
}
