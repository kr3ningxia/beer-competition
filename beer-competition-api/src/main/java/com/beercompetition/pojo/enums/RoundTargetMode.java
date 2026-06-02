package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum RoundTargetMode {
    ADVANCE_COUNT,
    TOP_N,
    MEDALS,
    CHAMPION;

    public static RoundTargetMode of(String value) {
        for (RoundTargetMode item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("轮次目标模式不合法：" + value);
    }
}
