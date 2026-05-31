package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.SmsCodeLogMapper;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.SmsLoginRequest;
import com.beercompetition.pojo.dto.SmsSendRequest;
import com.beercompetition.pojo.enums.SmsBizType;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.SmsCodeLog;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.properties.JwtProperties;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.security.JwtUtil;
import com.beercompetition.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminUserMapper adminUserMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final SmsCodeLogMapper smsCodeLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProperties jwtProperties;
    private final SmsProperties smsProperties;

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
        String code = smsProperties.isMockEnabled()
                ? "123456"
                : String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set(buildSmsKey(request.getBizType(), request.getPhone()), code,
                Duration.ofMinutes(smsProperties.getCodeTtlMinutes()));
        smsCodeLogMapper.insert(SmsCodeLog.builder()
                .phone(request.getPhone())
                .bizType(request.getBizType().name())
                .code(code)
                .status("SENT")
                .build());
    }

    @Override
    public LoginResponse portalLogin(SmsLoginRequest request) {
        validateSmsCode(SmsBizType.PORTAL_LOGIN, request.getPhone(), request.getCode());
        PortalAccount account = portalAccountMapper.selectOne(new LambdaQueryWrapper<PortalAccount>()
                .eq(PortalAccount::getPhone, request.getPhone()));
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BaseException("厂商账号不存在或已禁用");
        }
        return buildLoginResponse(account.getId(), account.getDisplayName(), UserRole.PORTAL, jwtProperties.getPortalTtl());
    }

    @Override
    public LoginResponse judgeLogin(SmsLoginRequest request) {
        validateSmsCode(SmsBizType.JUDGE_LOGIN, request.getPhone(), request.getCode());
        JudgeAccount account = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPhone, request.getPhone()));
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BaseException("评审账号不存在或已禁用");
        }
        return buildLoginResponse(account.getId(), account.getName(), UserRole.JUDGE, jwtProperties.getJudgeTtl());
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
                yield CurrentUserResponse.builder()
                        .userId(account.getId())
                        .role(role.name())
                        .displayName(account.getDisplayName())
                        .phone(account.getPhone())
                        .build();
            }
            case JUDGE -> {
                JudgeAccount account = judgeAccountMapper.selectById(currentId);
                if (account == null) {
                    throw new ResourceNotFoundException("评审账号不存在");
                }
                yield CurrentUserResponse.builder()
                        .userId(account.getId())
                        .role(role.name())
                        .displayName(account.getName())
                        .phone(account.getPhone())
                        .build();
            }
        };
    }

    private void validateSmsCode(SmsBizType bizType, String phone, String code) {
        if (smsProperties.isMockEnabled() && "123456".equals(code)) {
            return;
        }
        Object cachedCode = redisTemplate.opsForValue().get(buildSmsKey(bizType, phone));
        if (!StringUtils.hasText(code) || cachedCode == null || !code.equals(String.valueOf(cachedCode))) {
            throw new BaseException("验证码错误或已过期");
        }
    }

    private String buildSmsKey(SmsBizType bizType, String phone) {
        return "beer-competition:sms:" + bizType.name() + ":" + phone;
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
}
