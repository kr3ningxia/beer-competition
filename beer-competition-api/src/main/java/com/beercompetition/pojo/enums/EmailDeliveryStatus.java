package com.beercompetition.pojo.enums;

public enum EmailDeliveryStatus {
    PENDING("待发送"),
    SENDING("发送中"),
    SENT("已发送"),
    RETRY_WAITING("等待重试"),
    FAILED("发送失败"),
    SKIPPED("已跳过"),
    CANCELLED("已取消");

    private final String label;

    EmailDeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
