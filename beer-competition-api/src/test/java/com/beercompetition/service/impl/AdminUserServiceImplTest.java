package com.beercompetition.service.impl;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.security.AdminSessionIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;
    @Mock
    private OrganizerMemberMapper organizerMemberMapper;
    @Mock
    private AdminIdentityService adminIdentityService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @Test
    void platformCanListAccountWithStoppedMembership() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(1L)
                .role(UserRole.ADMIN.name())
                .build());
        when(adminIdentityService.requireCurrentIdentity()).thenReturn(
                new AdminSessionIdentity(AdminType.PLATFORM_SUPER_ADMIN, null, false));
        AdminUser user = AdminUser.builder()
                .id(9L)
                .username("tenant-admin")
                .name("租户管理员")
                .adminType(AdminType.ORGANIZER_ADMIN.name())
                .status(0)
                .build();
        when(adminUserMapper.selectList(any())).thenReturn(List.of(user));
        when(adminIdentityService.findOrganizerIdForDisplay(user)).thenReturn(18L);

        var result = adminUserService.listAdminUsers(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganizerId()).isEqualTo(18L);
        verify(adminIdentityService).findOrganizerIdForDisplay(user);
        verify(adminIdentityService, never()).resolve(user);
    }
}
