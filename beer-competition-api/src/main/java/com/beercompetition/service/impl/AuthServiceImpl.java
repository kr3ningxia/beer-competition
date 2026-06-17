package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.SmsCodeLogMapper;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.SmsLoginRequest;
import com.beercompetition.pojo.dto.SmsSendRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.SmsBizType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.SmsCodeLog;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.security.JwtUtil;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.SmsAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PORTAL_PROFILE_PLACEHOLDER_PREFIX = "待完善";

    private final AdminUserMapper adminUserMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final CompetitionMapper competitionMapper;
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
        return buildLoginResponse(adminUser.getId(), adminUser.getName(), UserRole.ADMIN, jwtProperties.getAdminTtl());
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

        // 2) 查询或自动创建厂商账号
        boolean newAccount = false;
        PortalAccount account = portalAccountMapper.selectOne(new LambdaQueryWrapper<PortalAccount>()
                .eq(PortalAccount::getPhone, phone));
        if (account == null) {
            account = createPortalAccount(phone);
            newAccount = true;
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BaseException("厂商账号已禁用");
        }

        // 3) 组装并返回登录态
        boolean profileComplete = isPortalProfileComplete(account);
        LoginResponse response = buildLoginResponse(account.getId(), account.getDisplayName(), UserRole.PORTAL, jwtProperties.getPortalTtl());
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
            throw new BaseException("评审账号已停用，请联系主办方");
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
            throw new BaseException("评审账号已停用，请联系主办方");
        }
        throw new BaseException("评审账号已存在，请直接登录");
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
                if (adminUser.getStatus() == null || adminUser.getStatus() != 1) {
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
                    throw new ResourceNotFoundException("厂商账号不存在");
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
                JudgeAssignment assignment = findFirstAssignment(account.getId());
                JudgeTable table = assignment == null ? null : judgeTableMapper.selectById(assignment.getTableId());
                Competition competition = assignment == null ? null : competitionMapper.selectById(assignment.getCompetitionId());
                yield CurrentUserResponse.builder()
                        .role(role.name())
                        .displayName(resolveJudgeDisplayName(account))
                        .phone(piiService.decrypt(account.getPhoneEnc()))
                        .wechat(piiService.decrypt(account.getWechatEnc()))
                        .qualification(account.getQualification())
                        .status(status.getCode())
                        .statusLabel(status.getLabel())
                        .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                        .canScore(status == JudgeAccountStatus.ACTIVE && assignment != null)
                        .currentCompetitionId(competition == null ? null : competition.getId())
                        .currentCompetition(competition == null ? null : competition.getName())
                        .judgeRoleType(assignment == null ? null : assignment.getRole())
                        .roleLabel(assignment == null ? null : roleLabel(assignment.getRole()))
                        .tableName(table == null ? null : table.getTableName())
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

    private LoginResponse buildLoginResponse(Long userId, String displayName, UserRole role, long ttlMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role.name());
        claims.put("scope", role.name().toLowerCase());
        claims.put("displayName", displayName);
        String token = JwtUtil.createToken(jwtProperties.getSecretKey(), ttlMillis, claims);
        return LoginResponse.builder()
                .token(token)
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
        LoginResponse response = buildLoginResponse(account.getId(), resolveJudgeDisplayName(account), UserRole.JUDGE, jwtProperties.getJudgeTtl());
        response.setUserId(null);
        response.setStatus(status.getCode());
        response.setProfileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE);
        return response;
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
