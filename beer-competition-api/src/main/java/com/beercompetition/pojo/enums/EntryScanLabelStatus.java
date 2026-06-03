package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

public enum EntryScanLabelStatus {
    ACTIVE,
    DISABLED,
    REGENERATED;

    public static EntryScanLabelStatus of(String value) {
        for (EntryScanLabelStatus item : values()) {
            if (item.name().equals(value)) {
                return item;
            }
        }
        throw new BaseException("扫码标签状态不合法：" + value);
    }
}
