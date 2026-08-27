package com.beercompetition.competition.access;

/**
 * 校验后台账号对平台、主办方和比赛资源的访问范围。
 *
 * <p>实现必须从已认证会话获取可信身份和主办方范围，不能信任请求参数中的
 * 主办方 ID。组织表、成员表和比赛主办方字段落地前不提供默认实现，避免遗漏
 * 校验时被静默放行。</p>
 */
public interface CompetitionAccessService {

    /**
     * 要求当前账号为平台超级管理员。
     */
    void requirePlatformSuperAdmin();

    /**
     * 要求当前账号可以访问指定主办方。
     *
     * @param organizerId 主办方 ID
     */
    void requireOrganizerAccess(Long organizerId);

    /**
     * 要求当前账号可以访问指定比赛及其关联业务数据。
     *
     * @param competitionId 比赛 ID
     */
    void requireCompetitionAccess(Long competitionId);

    /**
     * 返回当前后台账号默认创建比赛的组织。
     * 平台身份默认创建啤酒事务局平台赛事，主办方管理员使用所属组织。
     */
    Long requireCurrentOrganizerId();

    /**
     * 判断当前后台账号是否可以跨组织查看比赛。
     */
    boolean canAccessAllOrganizers();
}
