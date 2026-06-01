package com.beercompetition.pojo.enums;

import com.beercompetition.common.exception.BaseException;
import lombok.Getter;

@Getter
public enum JudgeAccountStatus {
    DISABLED(0, "停用"),
    ACTIVE(1, "启用"),
    PENDING_REVIEW(2, "待审核"),
    PROFILE_INCOMPLETE(3, "资料未完善");

    private final int code;
    private final String label;

    JudgeAccountStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean canLogin() {
        return this == ACTIVE || this == PENDING_REVIEW || this == PROFILE_INCOMPLETE;
    }

    public static JudgeAccountStatus of(Integer code) {
        if (code == null) {
            return DISABLED;
        }
        for (JudgeAccountStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new BaseException("评审账号状态不正确");
    }
}
