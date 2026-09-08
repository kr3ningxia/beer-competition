package com.beercompetition.competition.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.po.OrganizerMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 后台比赛资源的组织范围校验。
 *
 * <p>该服务只依赖可信会话和数据库关系，不接受请求参数中的组织身份作为授权依据。</p>
 */
@Service
@RequiredArgsConstructor
public class CompetitionAccessServiceImpl implements CompetitionAccessService {

    private static final int ACTIVE_STATUS = 1;
    private static final String ORGANIZER_ACTIVE = "ACTIVE";

    private final AdminUserMapper adminUserMapper;
    private final CompetitionMapper competitionMapper;
    private final OrganizerMapper organizerMapper;
    private final OrganizerMemberMapper organizerMemberMapper;

    @Override
    public void requirePlatformSuperAdmin() {
        if (resolveAdminType(requireCurrentAdmin()) != AdminType.PLATFORM_SUPER_ADMIN) {
            throw new ForbiddenException("当前账号无平台超级管理员权限");
        }
    }

    @Override
    public void requireStyleLibraryWriteAccess() {
        AdminType type = resolveAdminType(requireCurrentAdmin());
        if (type != AdminType.PLATFORM_SUPER_ADMIN && !type.isOrganizerAdmin()) {
            throw new ForbiddenException("当前账号无风格库维护权限");
        }
    }

    @Override
    public boolean isPlatformSuperAdmin() {
        return resolveAdminType(requireCurrentAdmin()) == AdminType.PLATFORM_SUPER_ADMIN;
    }

    @Override
    public boolean isOrganizerAdmin() {
        return resolveAdminType(requireCurrentAdmin()).isOrganizerAdmin();
    }

    @Override
    public void requireOrganizerAccess(Long organizerId) {
        if (organizerId == null) {
            throw new ForbiddenException("资源未关联主办方");
        }
        Organizer organizer = organizerMapper.selectById(organizerId);
        if (organizer == null || !ORGANIZER_ACTIVE.equals(organizer.getStatus())) {
            throw new ResourceNotFoundException("主办方不存在或已停用");
        }

        AdminUser adminUser = requireCurrentAdmin();
        AdminType adminType = resolveAdminType(adminUser);
        if (adminType == AdminType.PLATFORM_SUPER_ADMIN) {
            return;
        }
        if (adminType == AdminType.PLATFORM_EVENT_ADMIN) {
            if (!OrganizerType.PLATFORM.name().equals(organizer.getOrganizerType())) {
                throw new ForbiddenException("当前账号只能管理平台赛事");
            }
            return;
        }

        OrganizerMember member = organizerMemberMapper.selectOne(new LambdaQueryWrapper<OrganizerMember>()
                .eq(OrganizerMember::getAdminUserId, adminUser.getId())
                .eq(OrganizerMember::getOrganizerId, organizerId)
                .eq(OrganizerMember::getStatus, ACTIVE_STATUS)
                .last("LIMIT 1"));
        if (member == null) {
            throw new ForbiddenException("当前账号无权访问该主办方");
        }
    }

    @Override
    public void requireCompetitionAccess(Long competitionId) {
        if (competitionId == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        requireOrganizerAccess(competition.getOrganizerId());
    }

    @Override
    public Long requireCurrentOrganizerId() {
        AdminUser adminUser = requireCurrentAdmin();
        AdminType adminType = resolveAdminType(adminUser);
        if (adminType.isOrganizerAdmin()) {
            OrganizerMember member = organizerMemberMapper.selectOne(new LambdaQueryWrapper<OrganizerMember>()
                    .eq(OrganizerMember::getAdminUserId, adminUser.getId())
                    .eq(OrganizerMember::getStatus, ACTIVE_STATUS)
                    .last("LIMIT 1"));
            if (member == null) {
                throw new ForbiddenException("当前账号未关联有效主办方");
            }
            requireOrganizerAccess(member.getOrganizerId());
            return member.getOrganizerId();
        }
        return requirePlatformOrganizer().getId();
    }

    @Override
    public boolean canAccessAllOrganizers() {
        return resolveAdminType(requireCurrentAdmin()) == AdminType.PLATFORM_SUPER_ADMIN;
    }

    private AdminUser requireCurrentAdmin() {
        SessionUser sessionUser = BaseContext.getCurrentUser();
        if (sessionUser == null || sessionUser.getUserId() == null
                || !UserRole.ADMIN.name().equals(sessionUser.getRole())) {
            throw new UnauthorizedException("未登录或当前身份不是后台账号");
        }
        AdminUser adminUser = adminUserMapper.selectById(sessionUser.getUserId());
        if (adminUser == null || adminUser.getStatus() == null || adminUser.getStatus() != ACTIVE_STATUS) {
            throw new UnauthorizedException("管理员账号已停用，请重新登录");
        }
        return adminUser;
    }

    private AdminType resolveAdminType(AdminUser adminUser) {
        try {
            return AdminType.valueOf(adminUser.getAdminType());
        } catch (RuntimeException ex) {
            throw new ForbiddenException("后台账号身份配置不正确");
        }
    }

    private Organizer requirePlatformOrganizer() {
        Organizer organizer = organizerMapper.selectOne(new LambdaQueryWrapper<Organizer>()
                .eq(Organizer::getOrganizerType, OrganizerType.PLATFORM.name())
                .eq(Organizer::getStatus, ORGANIZER_ACTIVE)
                .last("LIMIT 1"));
        if (organizer == null) {
            throw new IllegalStateException("平台组织尚未初始化");
        }
        return organizer;
    }
}
