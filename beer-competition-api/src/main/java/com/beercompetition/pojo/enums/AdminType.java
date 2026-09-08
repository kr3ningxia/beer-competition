package com.beercompetition.pojo.enums;

/**
 * 后台账号的组织范围身份。
 */
public enum AdminType {
    PLATFORM_SUPER_ADMIN,
    PLATFORM_EVENT_ADMIN,
    ORGANIZER_ADMIN,
    ORGANIZER_SUB_ADMIN;

    public boolean isOrganizerAdmin() {
        return this == ORGANIZER_ADMIN || this == ORGANIZER_SUB_ADMIN;
    }
}
