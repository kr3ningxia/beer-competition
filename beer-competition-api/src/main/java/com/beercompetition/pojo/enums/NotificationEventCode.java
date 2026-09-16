package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;

import java.util.Arrays;

public enum NotificationEventCode {
    SAMPLE_START("送样开始提醒", -10080),
    SAMPLE_DEADLINE_3D("收样截止前3天", -4320),
    SAMPLE_DEADLINE_1D("收样截止前1天", -1440),
    RESULT_PUBLISHED("结果发布通知", 0);

    private final String label;
    private final int defaultOffsetMinutes;

    NotificationEventCode(String label, int defaultOffsetMinutes) {
        this.label = label;
        this.defaultOffsetMinutes = defaultOffsetMinutes;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultOffsetMinutes() {
        return defaultOffsetMinutes;
    }

    public static NotificationEventCode parse(String value) {
        return Arrays.stream(values())
                .filter(item -> item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BaseException("通知类型无效"));
    }
}
