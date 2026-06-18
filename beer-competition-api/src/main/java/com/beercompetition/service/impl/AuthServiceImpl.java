package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.exception.UnauthorizedException;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.SmsCodeLogMapper;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.RefreshTokenRequest;
import com.beercompetition.pojo.dto.SmsLoginRequest;
import com.beercompetition.pojo.dto.SmsSendRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.enums.SmsBizType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.SmsCodeLog;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.security.RefreshSession;
import com.beercompetition.security.JwtUtil;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.SmsAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PORTAL_PROFILE_PLACEHOLDER_PREFIX = "待完善";
    private static final String REFRESH_KEY_PREFIX = "beer-competition:auth:refresh:";
    private static final String REFRESH_TOKEN_SEPARATOR = ".";
    private static final int REFRESH_SECRET_BYTES = 32;
    private static final int ADMIN_STATUS_ACTIVE = 1;
    private static final int PORTAL_STATUS_ACTIVE = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminUserMapper adminUserMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final SmsCodeLogMapper smsCodeLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProperties jwtProperties;
    private final SmsProperties smsProperties;
    private final SmsAuthProvider smsAuthProvider;
    private final PiiService piiService;

    @Override
    public LoginResponse adminLogin(AdminLoginRequest request) {
        AdminUser adminUser = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername()));
        if (adminUser == null || adminUser.getStatus() == null || adminUser.getStatus() != 1) {
            throw new BaseException("管理员账号不存在或已禁用");
        }
        if (!Md5Util.encode(request.getPassword()).equals(adminUser.getPassword())) {
            throw new BaseException("用户名或密码错误");
        }
        return buildLoginResponse(adminUser.getId(), adminUser.getName(), UserRole.ADMIN,
                jwtProperties.getAdminTtl(), jwtProperties.getAdminRefreshTtl());
    }

    @Override
    public void sendSmsCode(SmsSendRequest request) {
        // 1) 参数规范化与发送频控
        String phone = piiService.normalizePhone(request.getPhone());
        String phoneHash = piiService.hashPhone(phone);
        ensureSmsSendAvailable(request.getBizType(), phoneHash);

        // 2) 按环境选择验证码提供方
        String code = "ALIYUN_MANAGED";
        if (smsProperties.isMockEnabled()) {
            code = "123456";
            redisTemplate.opsForValue().set(buildSmsKey(request.getBizType(), phoneHash), code,
                    Duration.ofMinutes(smsProperties.getCodeTtlMinutes()));
        } else {
            smsAuthProvider.sendCode(phone, request.getBizType());
        }
        redisTemplate.opsForValue().set(buildSmsIntervalKey(request.getBizType(), phoneHash), "1",
                Duration.ofSeconds(smsProperties.getSendIntervalSeconds()));

        // 3) 记录发送日志
        smsCodeLogMapper.insert(SmsCodeLog.builder()
                .phoneHash(phoneHash)
                .maskedPhone(piiService.maskPhone(phone))
                .bizType(request.getBizType().name())
                .status("SENT")
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse portalLogin(SmsLoginRequest request) {
        // 1) 验证短信验证码
        validateSmsCode(SmsBizType.PORTAL_LOGIN, request.getPhone(), request.getCode());
        String phone = piiService.normalizePhone(request.getPhone());

        // 2) 查询或自动创建厂牌账号
        boolean newAccount = false;
        PortalAccount account = portalAccountMapper.selectOne(new LambdaQueryWrapper<PortalAccount>()
                .eq(PortalAccount::getPhone, phone));
        if (account == null) {
            account = createPortalAccount(phone);
            newAccount = true;
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BaseException("厂牌账号已禁用");
        }

        // 3) 组装并返回登录态
        boolean profileComplete = isPortalProfileComplete(account);
        LoginResponse response = buildLoginResponse(account.getId(), account.getDisplayName(), UserRole.PORTAL,
                jwtProperties.getPortalTtl(), jwtProperties.getPortalRefreshTtl());
        response.setNewAccount(newAccount);
        response.setProfileComplete(profileComplete);
        response.setProfileRequired(!profileComplete);
        return response;
    }

    @Override
    public LoginResponse judgeLogin(SmsLoginRequest request) {
        // 1) 验证短信验证码
        validateSmsCode(SmsBizType.JUDGE_LOGIN, request.getPhone(), request.getCode());

        // 2) 查询评审账号状态
        String phone = piiService.normalizePhone(request.getPhone());
        JudgeAccount account = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPhoneHash, piiService.hashPhone(phone)));
        if (account == null) {
            throw new BaseException("评审账号不存在，请先注册");
        }
        JudgeAccountStatus status = JudgeAccountStatus.of(account.getStatus());
        if (!status.canLogin()) {
            throw new BaseException("评审账号已停用，请联系组委会");
        }

        // 3) 组装并返回登录态
        return buildJudgeLoginResponse(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse judgeRegister(SmsLoginRequest request) {
        // 1) 验证短信验证码
        validateSmsCode(SmsBizType.JUDGE_REGISTER, request.getPhone(), request.getCode());

        // 2) 查询或创建资料未完善账号
        String phone = piiService.normalizePhone(request.getPhone());
        JudgeAccount account = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPhoneHash, piiService.hashPhone(phone)));
        if (account == null) {
            account = JudgeAccount.builder()
                    .publicId(generateJudgePublicId())
                    .phoneEnc(piiService.encrypt(phone))
                    .phoneHash(piiService.hashPhone(phone))
                    .phoneLast4(piiService.phoneLast4(phone))
                    .name("")
                    .wechatEnc(null)
                    .qualification("")
                    .breweryConflictFlag(false)
                    .breweryConflictText(null)
                    .status(JudgeAccountStatus.PROFILE_INCOMPLETE.getCode())
                    .build();
            judgeAccountMapper.insert(account);
            return buildJudgeLoginResponse(judgeAccountMapper.selectById(account.getId()));
        }

        JudgeAccountStatus status = JudgeAccountStatus.of(account.getStatus());
        if (status == JudgeAccountStatus.PROFILE_INCOMPLETE) {
            return buildJudgeLoginResponse(account);
        }
        if (status == JudgeAccountStatus.DISABLED) {
            throw new BaseException("评审账号已停用，请联系组委会");
        }
        throw new BaseException("评审账号已存在，请直接登录");
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // 1) 解析刷新凭证并读取 Redis 会话
        String refreshToken = request.getRefreshToken();
        String sessionId = parseRefreshSessionId(refreshToken);
        String refreshKey = buildRefreshKey(sessionId);
        RefreshSession session = readRefreshSession(refreshKey);
        if (session == null || !tokenHashMatches(refreshToken, session.getTokenHash())) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        if (session.getExpiresAtMillis() != null && session.getExpiresAtMillis() <= System.currentTimeMillis()) {
            redisTemplate.delete(refreshKey);
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }

        // 2) 校验账号状态并立刻作废旧刷新凭证
        SessionSubject subject = validateRefreshSubject(session);
        Boolean deleted = redisTemplate.delete(refreshKey);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }

        // 3) 生成新的 access token 和新的 refresh token
        return buildLoginResponse(subject.userId(), subject.displayName(), subject.role(),
                accessTtlMillis(subject.role()), refreshTtlMillis(subject.role()));
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        // 1) 解析刷新凭证
        String refreshToken = request.getRefreshToken();
        String sessionId = parseRefreshSessionId(refreshToken);
        String refreshKey = buildRefreshKey(sessionId);
        RefreshSession session = readRefreshSession(refreshKey);
        if (session == null) {
            return;
        }

        // 2) 仅注销与当前明文凭证匹配的 Redis 会话
        if (tokenHashMatches(refreshToken, session.getTokenHash())) {
            redisTemplate.delete(refreshKey);
        }
    }

    @Override
    public CurrentUserResponse getCurrentUser(UserRole role) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new BaseException("当前用户不存在");
        }
        return switch (role) {
            case ADMIN -> {
                AdminUser adminUser = adminUserMapper.selectById(currentId);
                if (adminUser == null) {
                    throw new ResourceNotFoundException("管理员不存在");
                }
                if (adminUser.getStatus() == null || adminUser.getStatus() != ADMIN_STATUS_ACTIVE) {
                    throw new BaseException("管理员账号已停用，请重新登录");
                }
                yield CurrentUserResponse.builder()
                        .userId(adminUser.getId())
                        .role(role.name())
                        .displayName(adminUser.getName())
                        .build();
            }
            case PORTAL -> {
                PortalAccount account = portalAccountMapper.selectById(currentId);
                if (account == null) {
                    throw new ResourceNotFoundException("厂牌账号不存在");
                }
                boolean profileComplete = isPortalProfileComplete(account);
                yield CurrentUserResponse.builder()
                        .userId(account.getId())
                        .role(role.name())
                        .displayName(account.getDisplayName())
                        .phone(account.getPhone())
                        .profileComplete(profileComplete)
                        .profileRequired(!profileComplete)
                        .build();
            }
            case JUDGE -> {
                JudgeAccount account = judgeAccountMapper.selectById(currentId);
                if (account == null) {
                    throw new ResourceNotFoundException("评审账号不存在");
                }
                JudgeAccountStatus status = JudgeAccountStatus.of(account.getStatus());
                CurrentJudgeContext context = findCurrentJudgeContext(account.getId());
                yield CurrentUserResponse.builder()
                        .role(role.name())
                        .displayName(resolveJudgeDisplayName(account))
                        .phone(piiService.decrypt(account.getPhoneEnc()))
                        .wechat(piiService.decrypt(account.getWechatEnc()))
                        .qualification(account.getQualification())
                        .status(status.getCode())
                        .statusLabel(status.getLabel())
                        .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                        .canScore(status == JudgeAccountStatus.ACTIVE && context != null)
                        .currentCompetitionId(context == null || context.competition() == null ? null : context.competition().getId())
                        .currentCompetition(context == null || context.competition() == null ? null : context.competition().getName())
                        .judgeRoleType(context == null ? null : context.role())
                        .roleLabel(context == null ? null : roleLabel(context.role()))
                        .tableName(context == null ? null : context.tableName())
                        .build();
            }
        };
    }

    private void validateSmsCode(SmsBizType bizType, String phone, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BaseException("验证码不能为空");
        }
        String normalizedPhone = piiService.normalizePhone(phone);
        String phoneHash = piiService.hashPhone(normalizedPhone);
        boolean valid;
        if (smsProperties.isMockEnabled()) {
            Object cachedCode = redisTemplate.opsForValue().get(buildSmsKey(bizType, phoneHash));
            valid = "123456".equals(code) || (cachedCode != null && code.equals(String.valueOf(cachedCode)));
        } else {
            valid = smsAuthProvider.checkCode(normalizedPhone, code, bizType);
        }
        if (!valid) {
            throw new BaseException("验证码错误或已过期");
        }
    }

    private void ensureSmsSendAvailable(SmsBizType bizType, String phoneHash) {
        Object intervalFlag = redisTemplate.opsForValue().get(buildSmsIntervalKey(bizType, phoneHash));
        if (intervalFlag != null) {
            throw new BaseException("验证码发送过于频繁，请稍后再试");
        }
    }

    private String buildSmsKey(SmsBizType bizType, String phoneHash) {
        return "beer-competition:sms:" + bizType.name() + ":" + phoneHash;
    }

    private String buildSmsIntervalKey(SmsBizType bizType, String phoneHash) {
        return "beer-competition:sms-interval:" + bizType.name() + ":" + phoneHash;
    }

    private LoginResponse buildLoginResponse(Long userId, String displayName, UserRole role, long accessTtlMillis, long refreshTtlMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role.name());
        claims.put("scope", role.name().toLowerCase());
        claims.put("displayName", displayName);
        claims.put("jti", UUID.randomUUID().toString().replace("-", ""));
        String accessToken = JwtUtil.createToken(jwtProperties.getSecretKey(), accessTtlMillis, claims);
        String refreshToken = createRefreshSession(userId, displayName, role, refreshTtlMillis);
        return LoginResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTtlMillis / 1000)
                .scope(role.name().toLowerCase())
                .userId(userId)
                .role(role.name())
                .displayName(displayName)
                .build();
    }

    private PortalAccount createPortalAccount(String phone) {
        String suffix = phone.length() <= 4 ? phone : phone.substring(phone.length() - 4);
        String initialName = PORTAL_PROFILE_PLACEHOLDER_PREFIX + "厂牌" + suffix;
        Brewery brewery = Brewery.builder()
                .companyName(initialName)
                .contactName(PORTAL_PROFILE_PLACEHOLDER_PREFIX)
                .phone(phone)
                .wechat(null)
                .build();
        breweryMapper.insert(brewery);

        PortalAccount account = PortalAccount.builder()
                .phone(phone)
                .wechat(null)
                .displayName(initialName)
                .breweryId(brewery.getId())
                .status(1)
                .build();
        portalAccountMapper.insert(account);
        return portalAccountMapper.selectById(account.getId());
    }

    private boolean isPortalProfileComplete(PortalAccount account) {
        if (account == null || isPortalPlaceholderValue(account.getDisplayName())) {
            return false;
        }
        Brewery brewery = account.getBreweryId() == null ? null : breweryMapper.selectById(account.getBreweryId());
        return brewery != null
                && !isPortalPlaceholderValue(brewery.getCompanyName())
                && !isPortalPlaceholderValue(brewery.getContactName());
    }

    private boolean isPortalPlaceholderValue(String value) {
        return !StringUtils.hasText(value) || value.trim().startsWith(PORTAL_PROFILE_PLACEHOLDER_PREFIX);
    }

    private LoginResponse buildJudgeLoginResponse(JudgeAccount account) {
        JudgeAccountStatus status = JudgeAccountStatus.of(account.getStatus());
        LoginResponse response = buildLoginResponse(account.getId(), resolveJudgeDisplayName(account), UserRole.JUDGE,
                jwtProperties.getJudgeTtl(), jwtProperties.getJudgeRefreshTtl());
        response.setUserId(null);
        response.setStatus(status.getCode());
        response.setProfileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE);
        return response;
    }

    private String createRefreshSession(Long userId, String displayName, UserRole role, long refreshTtlMillis) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = sessionId + REFRESH_TOKEN_SEPARATOR + randomTokenSecret();
        long issuedAt = System.currentTimeMillis();
        RefreshSession session = RefreshSession.builder()
                .sessionId(sessionId)
                .tokenHash(hashRefreshToken(refreshToken))
                .userId(userId)
                .role(role.name())
                .scope(role.name().toLowerCase())
                .displayName(displayName)
                .issuedAtMillis(issuedAt)
                .expiresAtMillis(issuedAt + refreshTtlMillis)
                .build();
        redisTemplate.opsForValue().set(buildRefreshKey(sessionId), session, Duration.ofMillis(refreshTtlMillis));
        return refreshToken;
    }

    private RefreshSession readRefreshSession(String refreshKey) {
        Object value = redisTemplate.opsForValue().get(refreshKey);
        if (value instanceof RefreshSession session) {
            return session;
        }
        if (value instanceof Map<?, ?> map) {
            return RefreshSession.builder()
                    .sessionId(stringValue(map.get("sessionId")))
                    .tokenHash(stringValue(map.get("tokenHash")))
                    .userId(longValue(map.get("userId")))
                    .role(stringValue(map.get("role")))
                    .scope(stringValue(map.get("scope")))
                    .displayName(stringValue(map.get("displayName")))
                    .issuedAtMillis(longValue(map.get("issuedAtMillis")))
                    .expiresAtMillis(longValue(map.get("expiresAtMillis")))
                    .build();
        }
        return null;
    }

    private SessionSubject validateRefreshSubject(RefreshSession session) {
        UserRole role;
        try {
            role = UserRole.valueOf(session.getRole());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        return switch (role) {
            case ADMIN -> {
                AdminUser adminUser = adminUserMapper.selectById(session.getUserId());
                if (adminUser == null || adminUser.getStatus() == null || adminUser.getStatus() != ADMIN_STATUS_ACTIVE) {
                    throw new UnauthorizedException("管理员账号已停用，请重新登录");
                }
                yield new SessionSubject(adminUser.getId(), adminUser.getName(), role);
            }
            case PORTAL -> {
                PortalAccount account = portalAccountMapper.selectById(session.getUserId());
                if (account == null || account.getStatus() == null || account.getStatus() != PORTAL_STATUS_ACTIVE) {
                    throw new UnauthorizedException("厂牌账号已禁用，请重新登录");
                }
                yield new SessionSubject(account.getId(), account.getDisplayName(), role);
            }
            case JUDGE -> {
                JudgeAccount account = judgeAccountMapper.selectById(session.getUserId());
                if (account == null || !JudgeAccountStatus.of(account.getStatus()).canLogin()) {
                    throw new UnauthorizedException("评审账号已停用，请重新登录");
                }
                yield new SessionSubject(account.getId(), resolveJudgeDisplayName(account), role);
            }
        };
    }

    private String parseRefreshSessionId(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        int separatorIndex = refreshToken.indexOf(REFRESH_TOKEN_SEPARATOR);
        if (separatorIndex <= 0) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        return refreshToken.substring(0, separatorIndex);
    }

    private String buildRefreshKey(String sessionId) {
        return REFRESH_KEY_PREFIX + sessionId;
    }

    private String randomTokenSecret() {
        byte[] bytes = new byte[REFRESH_SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private boolean tokenHashMatches(String refreshToken, String expectedHash) {
        if (!StringUtils.hasText(expectedHash)) {
            return false;
        }
        byte[] actual = hashRefreshToken(refreshToken).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    private long accessTtlMillis(UserRole role) {
        return switch (role) {
            case ADMIN -> jwtProperties.getAdminTtl();
            case PORTAL -> jwtProperties.getPortalTtl();
            case JUDGE -> jwtProperties.getJudgeTtl();
        };
    }

    private long refreshTtlMillis(UserRole role) {
        return switch (role) {
            case ADMIN -> jwtProperties.getAdminRefreshTtl();
            case PORTAL -> jwtProperties.getPortalRefreshTtl();
            case JUDGE -> jwtProperties.getJudgeRefreshTtl();
        };
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private record SessionSubject(Long userId, String displayName, UserRole role) {
    }

    private String resolveJudgeDisplayName(JudgeAccount account) {
        return StringUtils.hasText(account.getName()) ? account.getName() : "现场评审";
    }

    private JudgeAssignment findFirstAssignment(Long judgeAccountId) {
        return judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                        .eq(JudgeAssignment::getJudgeAccountId, judgeAccountId)
                        .orderByAsc(JudgeAssignment::getCompetitionId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private CurrentJudgeContext findCurrentJudgeContext(Long judgeAccountId) {
        CurrentJudgeContext taskContext = findCurrentVisibleTaskContext(judgeAccountId);
        if (taskContext != null) {
            return taskContext;
        }
        JudgeAssignment assignment = findFirstAssignment(judgeAccountId);
        if (assignment == null) {
            return null;
        }
        JudgeTable table = judgeTableMapper.selectById(assignment.getTableId());
        Competition competition = competitionMapper.selectById(assignment.getCompetitionId());
        return new CurrentJudgeContext(competition, table == null ? null : table.getTableName(), assignment.getRole());
    }

    private CurrentJudgeContext findCurrentVisibleTaskContext(Long judgeAccountId) {
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeAccountId)
                .eq(RoundTableMember::getSystemTaskRequired, 1));
        CurrentJudgeContext bestContext = null;
        Long bestRoundId = null;
        Long bestTableId = null;
        for (RoundTableMember member : members) {
            RoundTable table = roundTableMapper.selectById(member.getRoundTableId());
            if (table == null) {
                continue;
            }
            CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
            if (round == null || !isVisibleJudgeTask(round, table, member)) {
                continue;
            }
            if (bestRoundId != null
                    && (round.getId() < bestRoundId
                    || (round.getId().equals(bestRoundId) && table.getId() <= bestTableId))) {
                continue;
            }
            Competition competition = competitionMapper.selectById(table.getCompetitionId());
            bestContext = new CurrentJudgeContext(competition, table.getTableName(), member.getRole());
            bestRoundId = round.getId();
            bestTableId = table.getId();
        }
        return bestContext;
    }

    private boolean isVisibleJudgeTask(CompetitionRound round, RoundTable table, RoundTableMember member) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return RoundStatus.PUBLISHED.name().equals(round.getStatus())
                    && (RoundStatus.PUBLISHED.name().equals(table.getStatus())
                    || JudgeRoleType.CAPTAIN.name().equals(member.getRole()));
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return RoundStatus.IN_PROGRESS.name().equals(round.getStatus())
                    || RoundStatus.SUBMITTED.name().equals(round.getStatus());
        }
        return false;
    }

    private record CurrentJudgeContext(Competition competition, String tableName, String role) {
    }

    private String roleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return role;
    }

    private String generateJudgePublicId() {
        String publicId;
        do {
            publicId = "J" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPublicId, publicId)) != null);
        return publicId;
    }
}
