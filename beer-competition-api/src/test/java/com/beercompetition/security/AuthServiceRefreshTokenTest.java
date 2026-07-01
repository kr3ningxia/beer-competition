package com.beercompetition.security;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.SmsCodeLogMapper;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.RefreshTokenRequest;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.service.SmsAuthProvider;
import com.beercompetition.service.impl.AuthServiceImpl;
import com.beercompetition.common.util.PiiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTest {

    private static final String REFRESH_KEY_PREFIX = "beer-competition:auth:refresh:";

    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private PortalAccountMapper portalAccountMapper;
    @Mock
    private BreweryMapper breweryMapper;
    @Mock
    private JudgeAccountMapper judgeAccountMapper;
    @Mock
    private JudgeAssignmentMapper judgeAssignmentMapper;
    @Mock
    private JudgeTableMapper judgeTableMapper;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private CompetitionRoundMapper competitionRoundMapper;
    @Mock
    private RoundTableMapper roundTableMapper;
    @Mock
    private RoundTableMemberMapper roundTableMemberMapper;
    @Mock
    private SmsCodeLogMapper smsCodeLogMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private SmsProperties smsProperties;
    @Mock
    private SmsAuthProvider smsAuthProvider;
    @Mock
    private PiiService piiService;

    private final Map<String, Object> redisStore = new ConcurrentHashMap<>();
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("refresh-token-test-secret");
        jwtProperties.setHeaderName("Authorization");
        jwtProperties.setAdminTtl(1_800_000);
        jwtProperties.setPortalTtl(3_600_000);
        jwtProperties.setJudgeTtl(3_600_000);
        jwtProperties.setAdminRefreshTtl(43_200_000);
        jwtProperties.setPortalRefreshTtl(2_592_000_000L);
        jwtProperties.setJudgeRefreshTtl(86_400_000);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), any(), any(Duration.class));
        when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = redisStore.get(key);
            Long nextValue = value instanceof Number number ? number.longValue() + 1 : 1L;
            redisStore.put(key, nextValue);
            return nextValue;
        });
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(valueOperations.get(anyString())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
        lenient().when(redisTemplate.delete(anyString())).thenAnswer(invocation -> redisStore.remove(invocation.getArgument(0)) != null);

        authService = new AuthServiceImpl(
                adminUserMapper,
                portalAccountMapper,
                breweryMapper,
                judgeAccountMapper,
                judgeAssignmentMapper,
                judgeTableMapper,
                competitionMapper,
                competitionRoundMapper,
                roundTableMapper,
                roundTableMemberMapper,
                smsCodeLogMapper,
                redisTemplate,
                jwtProperties,
                smsProperties,
                smsAuthProvider,
                piiService
        );
    }

    @Test
    void refreshTokenRotatesAndRevokesOldRedisSession() {
        AdminUser adminUser = AdminUser.builder()
                .id(1L)
                .username("admin")
                .password(Md5Util.encode("123456"))
                .name("测试管理员")
                .status(1)
                .build();
        when(adminUserMapper.selectOne(any())).thenReturn(adminUser);
        when(adminUserMapper.selectById(1L)).thenReturn(adminUser);

        LoginResponse loginResponse = authService.adminLogin(loginRequest());
        String oldRefreshToken = loginResponse.getRefreshToken();
        assertThat(redisStore).containsKey(refreshKey(oldRefreshToken));

        LoginResponse refreshResponse = authService.refreshToken(refreshRequest(oldRefreshToken));

        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isNotBlank();
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(loginResponse.getAccessToken());
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(oldRefreshToken);
        assertThat(redisStore).doesNotContainKey(refreshKey(oldRefreshToken));
        assertThat(redisStore).containsKey(refreshKey(refreshResponse.getRefreshToken()));
        assertThatThrownBy(() -> authService.refreshToken(refreshRequest(oldRefreshToken)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void adminLoginRejectsMoreThanFiveAttemptsWithinOneMinute() {
        AdminUser adminUser = AdminUser.builder()
                .id(1L)
                .username("admin")
                .password(Md5Util.encode("123456"))
                .name("测试管理员")
                .status(1)
                .build();
        when(adminUserMapper.selectOne(any())).thenReturn(adminUser);

        AdminLoginRequest request = loginRequest();
        request.setPassword("wrong-password");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.adminLogin(request))
                    .isInstanceOf(BaseException.class)
                    .hasMessage("用户名或密码错误");
        }

        assertThatThrownBy(() -> authService.adminLogin(request))
                .isInstanceOf(BaseException.class)
                .hasMessage("登录尝试过于频繁，请1分钟后再试");
    }

    private AdminLoginRequest loginRequest() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        return request;
    }

    private RefreshTokenRequest refreshRequest(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private String refreshKey(String refreshToken) {
        return REFRESH_KEY_PREFIX + refreshToken.substring(0, refreshToken.indexOf('.'));
    }
}
