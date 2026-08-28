package com.beercompetition.service.impl;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.SmsCodeLogMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.security.AdminSessionIdentity;
import com.beercompetition.service.SmsAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCurrentUserTest {

    @Mock private AdminUserMapper adminUserMapper;
    @Mock private PortalAccountMapper portalAccountMapper;
    @Mock private BreweryMapper breweryMapper;
    @Mock private JudgeAccountMapper judgeAccountMapper;
    @Mock private JudgeAssignmentMapper judgeAssignmentMapper;
    @Mock private JudgeTableMapper judgeTableMapper;
    @Mock private CompetitionMapper competitionMapper;
    @Mock private CompetitionRoundMapper competitionRoundMapper;
    @Mock private RoundTableMapper roundTableMapper;
    @Mock private RoundTableMemberMapper roundTableMemberMapper;
    @Mock private OrganizerMapper organizerMapper;
    @Mock private SmsCodeLogMapper smsCodeLogMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private JwtProperties jwtProperties;
    @Mock private SmsProperties smsProperties;
    @Mock private SmsAuthProvider smsAuthProvider;
    @Mock private PiiService piiService;
    @Mock private AdminIdentityService adminIdentityService;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @Test
    void organizerAdminCurrentUserIncludesTrustedOrganizerName() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(27L)
                .role(UserRole.ADMIN.name())
                .build());
        AdminUser adminUser = AdminUser.builder()
                .id(27L)
                .username("tenant-admin")
                .name("赛事管理员")
                .status(1)
                .adminType(AdminType.ORGANIZER_ADMIN.name())
                .build();
        when(adminUserMapper.selectById(27L)).thenReturn(adminUser);
        when(adminIdentityService.resolve(adminUser)).thenReturn(
                new AdminSessionIdentity(AdminType.ORGANIZER_ADMIN, 64L, false, false));
        when(organizerMapper.selectById(64L)).thenReturn(Organizer.builder()
                .id(64L)
                .name("测试主办方")
                .build());

        var currentUser = authService.getCurrentUser(UserRole.ADMIN);

        assertThat(currentUser.getOrganizerId()).isEqualTo(64L);
        assertThat(currentUser.getOrganizerName()).isEqualTo("测试主办方");
        assertThat(currentUser.getAdminType()).isEqualTo(AdminType.ORGANIZER_ADMIN.name());
    }
}
