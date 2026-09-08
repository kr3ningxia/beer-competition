package com.beercompetition.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.OrganizerMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 后台账号身份和成员关系的统一读取入口。
 */
@Service
@RequiredArgsConstructor
public class AdminIdentityService {

    private static final int ACTIVE_STATUS = 1;

    private final AdminUserMapper adminUserMapper;
    private final OrganizerMemberMapper organizerMemberMapper;

    /**
     * 将账号记录解析为可信会话属性。
     */
    public AdminSessionIdentity resolve(AdminUser adminUser) {
        if (adminUser == null || adminUser.getId() == null) {
            throw new UnauthorizedException("管理员账号不存在");
        }
        AdminType adminType = parseAdminType(adminUser.getAdminType());
        Long organizerId = null;
        if (adminType.isOrganizerAdmin()) {
            List<OrganizerMember> members = organizerMemberMapper.selectList(new LambdaQueryWrapper<OrganizerMember>()
                    .eq(OrganizerMember::getAdminUserId, adminUser.getId())
                    .eq(OrganizerMember::getStatus, ACTIVE_STATUS)
                    .orderByAsc(OrganizerMember::getId));
            if (members.size() != 1 || members.get(0).getOrganizerId() == null) {
                throw new ForbiddenException("主办方管理员未关联唯一有效组织");
            }
            organizerId = members.get(0).getOrganizerId();
        }
        return new AdminSessionIdentity(adminType, organizerId,
                Integer.valueOf(ACTIVE_STATUS).equals(adminUser.getMustChangePassword()),
                Integer.valueOf(ACTIVE_STATUS).equals(adminUser.getMustChangeUsername()));
    }

    /**
     * 读取当前请求对应的后台账号。
     */
    public AdminUser requireCurrentAdmin() {
        if (!UserRole.ADMIN.name().equals(BaseContext.getCurrentRole()) || BaseContext.getCurrentId() == null) {
            throw new UnauthorizedException("未登录或当前身份不是后台账号");
        }
        AdminUser adminUser = adminUserMapper.selectById(BaseContext.getCurrentId());
        if (adminUser == null || !Integer.valueOf(ACTIVE_STATUS).equals(adminUser.getStatus())) {
            throw new UnauthorizedException("管理员账号已停用，请重新登录");
        }
        return adminUser;
    }

    /**
     * 读取当前后台账号及其可信身份。
     */
    public AdminSessionIdentity requireCurrentIdentity() {
        return resolve(requireCurrentAdmin());
    }

    /**
     * 读取账号管理页面展示用的组织归属。
     *
     * <p>该方法不参与授权：成员关系停用后仍允许平台管理员查看历史账号，
     * 因此只取最近一条关系，严格的有效成员关系校验继续由 {@link #resolve(AdminUser)} 负责。</p>
     */
    public Long findOrganizerIdForDisplay(AdminUser adminUser) {
        if (adminUser == null || adminUser.getId() == null
                || !(AdminType.ORGANIZER_ADMIN.name().equals(adminUser.getAdminType())
                || AdminType.ORGANIZER_SUB_ADMIN.name().equals(adminUser.getAdminType()))) {
            return null;
        }
        OrganizerMember member = organizerMemberMapper.selectOne(new LambdaQueryWrapper<OrganizerMember>()
                .eq(OrganizerMember::getAdminUserId, adminUser.getId())
                .orderByDesc(OrganizerMember::getId)
                .last("LIMIT 1"));
        return member == null ? null : member.getOrganizerId();
    }

    public void requirePlatformSuperAdmin() {
        if (requireCurrentIdentity().adminType() != AdminType.PLATFORM_SUPER_ADMIN) {
            throw new ForbiddenException("当前账号无平台超级管理员权限");
        }
    }

    public Long requireCurrentOrganizerId() {
        AdminSessionIdentity identity = requireCurrentIdentity();
        if (!identity.adminType().isOrganizerAdmin() || identity.organizerId() == null) {
            throw new ForbiddenException("当前账号未关联主办方组织");
        }
        return identity.organizerId();
    }

    public AdminType parseAdminType(String value) {
        try {
            return AdminType.valueOf(value);
        } catch (RuntimeException ex) {
            throw new ForbiddenException("后台账号身份配置不正确");
        }
    }
}
