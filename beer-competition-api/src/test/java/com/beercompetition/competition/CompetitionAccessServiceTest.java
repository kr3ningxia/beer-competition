package com.beercompetition.competition;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.competition.access.CompetitionAccessServiceImpl;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.po.OrganizerMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompetitionAccessServiceTest {

    private final AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
    private final CompetitionMapper competitionMapper = mock(CompetitionMapper.class);
    private final OrganizerMapper organizerMapper = mock(OrganizerMapper.class);
    private final OrganizerMemberMapper organizerMemberMapper = mock(OrganizerMemberMapper.class);

    private final CompetitionAccessServiceImpl accessService = new CompetitionAccessServiceImpl(
            adminUserMapper, competitionMapper, organizerMapper, organizerMemberMapper);

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @Test
    void organizerAdminCannotAccessAnotherOrganizerCompetition() {
        asAdmin(10L, AdminType.ORGANIZER_ADMIN);
        when(adminUserMapper.selectById(10L)).thenReturn(activeAdmin(10L, AdminType.ORGANIZER_ADMIN));
        when(competitionMapper.selectById(99L)).thenReturn(Competition.builder().id(99L).organizerId(2L).build());
        when(organizerMapper.selectById(2L)).thenReturn(activeOrganizer(2L));
        when(organizerMemberMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> accessService.requireCompetitionAccess(99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void platformEventAdminCanOnlyAccessPlatformOrganizer() {
        asAdmin(11L, AdminType.PLATFORM_EVENT_ADMIN);
        when(adminUserMapper.selectById(11L)).thenReturn(activeAdmin(11L, AdminType.PLATFORM_EVENT_ADMIN));
        when(organizerMapper.selectById(2L)).thenReturn(activeOrganizer(2L));

        assertThatThrownBy(() -> accessService.requireOrganizerAccess(2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void organizerAdminUsesMembershipAsCreationScope() {
        asAdmin(12L, AdminType.ORGANIZER_ADMIN);
        when(adminUserMapper.selectById(12L)).thenReturn(activeAdmin(12L, AdminType.ORGANIZER_ADMIN));
        when(organizerMemberMapper.selectOne(any())).thenReturn(
                OrganizerMember.builder().organizerId(3L).adminUserId(12L).status(1).build());
        when(organizerMapper.selectById(3L)).thenReturn(activeOrganizer(3L));

        assertThat(accessService.requireCurrentOrganizerId()).isEqualTo(3L);
    }

    @Test
    void missingOrNonAdminSessionIsRejected() {
        assertThatThrownBy(() -> accessService.requireCurrentOrganizerId())
                .isInstanceOf(UnauthorizedException.class);

        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(100L)
                .role(UserRole.PORTAL.name())
                .build());
        assertThatThrownBy(() -> accessService.requireCurrentOrganizerId())
                .isInstanceOf(UnauthorizedException.class);
    }

    private void asAdmin(Long userId, AdminType adminType) {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(userId)
                .role(UserRole.ADMIN.name())
                .build());
    }

    private AdminUser activeAdmin(Long id, AdminType adminType) {
        return AdminUser.builder().id(id).status(1).adminType(adminType.name()).build();
    }

    private Organizer activeOrganizer(Long id) {
        return Organizer.builder().id(id).organizerType("TENANT").status("ACTIVE").build();
    }
}
