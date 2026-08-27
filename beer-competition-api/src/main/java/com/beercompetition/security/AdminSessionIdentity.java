package com.beercompetition.security;

import com.beercompetition.pojo.enums.AdminType;

/**
 * 当前后台账号的可信会话属性。
 *
 * <p>组织范围来源于数据库成员关系。该对象可以写入 JWT 和线程上下文，
 * 但资源授权仍必须在业务服务中重新校验数据库状态。</p>
 */
public record AdminSessionIdentity(AdminType adminType,
                                   Long organizerId,
                                   boolean mustChangePassword,
                                   boolean mustChangeUsername) {

    /**
     * 兼容只关心密码状态的既有调用方。
     */
    public AdminSessionIdentity(AdminType adminType, Long organizerId, boolean mustChangePassword) {
        this(adminType, organizerId, mustChangePassword, false);
    }
}
