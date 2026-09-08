package com.beercompetition.organization.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.EnterpriseAccountMapper;
import com.beercompetition.mapper.OrganizerApplicationMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.OrganizerApplicationStatus;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.EnterpriseAccount;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.po.OrganizerApplication;
import com.beercompetition.pojo.po.OrganizerMember;
import com.beercompetition.security.AdminIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 审核通过后的主办方组织和初始账号发放事务。
 */
@Service
@RequiredArgsConstructor
public class OrganizerProvisioningServiceImpl implements OrganizerProvisioningService {

    private static final int STATUS_ACTIVE = 1;
    private static final String ENTERPRISE_STATUS_ACTIVE = "ACTIVE";
    private static final String ORGANIZER_STATUS_ACTIVE = "ACTIVE";
    private static final String TARGET_APPLICATION = "ORGANIZER_APPLICATION";
    private static final String INITIAL_CREDENTIAL_DELIVERY_PREFIX =
            "beer-competition:organizer-application:credential:";
    private static final Duration INITIAL_CREDENTIAL_DELIVERY_TTL = Duration.ofDays(7);
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final EnterpriseAccountMapper enterpriseAccountMapper;
    private final OrganizerMapper organizerMapper;
    private final AdminUserMapper adminUserMapper;
    private final OrganizerMemberMapper organizerMemberMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AdminIdentityService adminIdentityService;
    private final PiiService piiService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProvisioningResult provisionApprovedApplication(Long applicationId) {
        adminIdentityService.requirePlatformSuperAdmin();
        OrganizerApplication application = applicationId == null
                ? null : organizerApplicationMapper.selectByIdForUpdate(applicationId);
        if (application == null) {
            throw new ResourceNotFoundException("入驻申请不存在");
        }
        OrganizerApplicationStatus status = parseStatus(application.getStatus());
        if (status == OrganizerApplicationStatus.ACCOUNT_ISSUED) {
            AdminUser existingAdmin = application.getInitialAdminUserId() == null
                    ? null : adminUserMapper.selectById(application.getInitialAdminUserId());
            return new ProvisioningResult(application.getOrganizerId(), application.getInitialAdminUserId(),
                    existingAdmin == null ? null : existingAdmin.getUsername(), null);
        }
        if (status != OrganizerApplicationStatus.APPROVED) {
            throw new BaseException("只有审核通过的申请可以发放账号");
        }

        LocalDateTime now = LocalDateTime.now();
        String accountCode = "EA-TENANT-" + application.getId();
        EnterpriseAccount enterpriseAccount = EnterpriseAccount.builder()
                .accountCode(accountCode)
                .name(application.getOrganizationName())
                .status(ENTERPRISE_STATUS_ACTIVE)
                .build();
        try {
            enterpriseAccountMapper.insert(enterpriseAccount);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("企业账户编号已存在，请检查申请开通数据");
        }

        Organizer organizer = Organizer.builder()
                .enterpriseAccountId(enterpriseAccount.getId())
                .name(application.getOrganizationName())
                .organizerType(OrganizerType.TENANT.name())
                .status(ORGANIZER_STATUS_ACTIVE)
                .contactName(application.getContactName())
                .contactPhone(encryptContactPhone(application))
                .build();
        organizerMapper.insert(organizer);

        String username = "org_admin_" + application.getId();
        String initialPassword = generateInitialPassword();
        AdminUser adminUser = AdminUser.builder()
                .username(username)
                .password(Md5Util.encode(initialPassword))
                .name(application.getContactName())
                .status(STATUS_ACTIVE)
                .adminType(AdminType.ORGANIZER_ADMIN.name())
                .mustChangePassword(STATUS_ACTIVE)
                .mustChangeUsername(STATUS_ACTIVE)
                .initialCredentialIssuedTime(now)
                .build();
        try {
            adminUserMapper.insert(adminUser);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("初始管理员账号已存在，请检查申请开通数据");
        }
        organizerMemberMapper.insert(OrganizerMember.builder()
                .organizerId(organizer.getId())
                .adminUserId(adminUser.getId())
                .status(STATUS_ACTIVE)
                .build());

        application.setOrganizerId(organizer.getId());
        application.setInitialAdminUserId(adminUser.getId());
        application.setAccountIssuedTime(now);
        application.setStatus(OrganizerApplicationStatus.ACCOUNT_ISSUED.name());
        organizerApplicationMapper.updateById(application);
        writeProvisioningLog(application, organizer.getId(), adminUser.getId());
        storeInitialCredential(application.getId(), initialPassword);

        return new ProvisioningResult(organizer.getId(), adminUser.getId(), username, initialPassword);
    }

    private void storeInitialCredential(Long applicationId, String initialPassword) {
        if (applicationId == null || !org.springframework.util.StringUtils.hasText(initialPassword)) {
            return;
        }
        // Redis 中只保留短期加密交付凭据；管理员账号表仍只保存密码哈希。
        redisTemplate.opsForValue().set(
                credentialDeliveryKey(applicationId),
                piiService.encrypt(initialPassword),
                INITIAL_CREDENTIAL_DELIVERY_TTL);
    }

    private String credentialDeliveryKey(Long applicationId) {
        return INITIAL_CREDENTIAL_DELIVERY_PREFIX + applicationId;
    }

    private String encryptContactPhone(OrganizerApplication application) {
        if (!StringUtils.hasText(application.getContactPhoneEnc())) {
            return null;
        }
        return application.getContactPhoneEnc();
    }

    private void writeProvisioningLog(OrganizerApplication application, Long organizerId, Long adminUserId) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .organizerId(organizerId)
                .action("ORGANIZER_APPLICATION_ACCOUNT_ISSUED")
                .targetType(TARGET_APPLICATION)
                .targetPublicId(String.valueOf(application.getId()))
                .summary("已创建主办方和初始管理员：" + adminUserId)
                .build());
    }

    private String generateInitialPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private OrganizerApplicationStatus parseStatus(String value) {
        try {
            return OrganizerApplicationStatus.valueOf(value);
        } catch (RuntimeException ex) {
            throw new BaseException("申请状态配置不正确");
        }
    }
}
