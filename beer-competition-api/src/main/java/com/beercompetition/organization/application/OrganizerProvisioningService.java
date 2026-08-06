package com.beercompetition.organization.application;

/**
 * 为审核通过的入驻申请发放主办方组织和初始管理员账号。
 *
 * <p>实现需要锁定申请记录并保证幂等，在同一事务内创建企业账户、主办方、
 * 管理员和成员关系。初始密码只允许生成并返回一次，不得写入申请表、操作日志
 * 或普通应用日志。</p>
 */
public interface OrganizerProvisioningService {

    /**
     * 为已审核通过且尚未发放账号的申请执行开通。
     *
     * @param applicationId 入驻申请 ID
     * @return 本次新发放的组织、管理员和一次性初始凭据
     */
    ProvisioningResult provisionApprovedApplication(Long applicationId);

    /**
     * 账号发放结果。初始密码是仅供本次响应展示的敏感数据，调用方使用后应立即丢弃。
     *
     * @param organizerId 新建主办方 ID
     * @param adminUserId 初始管理员 ID
     * @param username 初始登录账号
     * @param initialPassword 一次性初始密码，不得持久化或记录日志
     */
    record ProvisioningResult(Long organizerId,
                              Long adminUserId,
                              String username,
                              String initialPassword) {
    }
}
