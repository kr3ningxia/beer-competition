package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum BrewType {
    COMMERCIAL,
    HOMEBREW,
    MIXED;

    public static BrewType of(String value) {
        if (value == null || value.isBlank()) {
            return COMMERCIAL;
        }
        for (BrewType item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("酿造类型不合法：" + value);
    }
}
