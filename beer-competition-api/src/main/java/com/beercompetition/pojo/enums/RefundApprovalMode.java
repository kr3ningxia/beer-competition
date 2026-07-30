package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;
import org.springframework.util.StringUtils;

public enum RefundApprovalMode {
    AUTO_APPROVE,
    MANUAL_REVIEW;

    public static RefundApprovalMode of(String value) {
        if (!StringUtils.hasText(value)) {
            return AUTO_APPROVE;
        }
        try {
            return RefundApprovalMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("退款审批方式不正确");
        }
    }
}
