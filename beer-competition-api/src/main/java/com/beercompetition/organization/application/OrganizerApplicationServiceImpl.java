package com.beercompetition.organization.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.OrganizerApplicationMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.dto.OrganizerApplicationReviewRequest;
import com.beercompetition.pojo.dto.OrganizerApplicationSubmitRequest;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.OrganizerApplicationStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.OrganizerApplication;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.OrganizerApplicationAdminVO;
import com.beercompetition.pojo.vo.OrganizerApplicationStatusVO;
import com.beercompetition.pojo.vo.OrganizerApplicationSubmitVO;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * 入驻申请状态和平台审核用例。
 */
@Service
@RequiredArgsConstructor
public class OrganizerApplicationServiceImpl implements OrganizerApplicationService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_QUERY_LIMIT = 10;
    private static final String TARGET_APPLICATION = "ORGANIZER_APPLICATION";
    private static final String RATE_LIMIT_PREFIX = "beer-competition:organizer-application:query:";
    private static final String BUSINESS_TYPE_MATERIAL = "ORGANIZER_APPLICATION_MATERIAL";
    private static final String OWNER_TYPE_APPLICATION = "ORGANIZER_APPLICATION";
    private static final long MAX_MATERIAL_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> MATERIAL_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final FileAssetMapper fileAssetMapper;
    private final PiiService piiService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AdminIdentityService adminIdentityService;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationSubmitVO submit(OrganizerApplicationSubmitRequest request) {
        rejectClientAssetId(request);
        return createApplication(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationSubmitVO submit(OrganizerApplicationSubmitRequest request, MultipartFile material) {
        rejectClientAssetId(request);
        return createApplication(request, material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationSubmitVO submitForCurrentPortal(OrganizerApplicationSubmitRequest request) {
        rejectClientAssetId(request);
        PortalAccount account = requireCurrentPortalAccount();
        ensureCurrentPortalPhone(request, account);
        return createApplication(request, null, account.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationSubmitVO submitForCurrentPortal(OrganizerApplicationSubmitRequest request,
                                                               MultipartFile material) {
        rejectClientAssetId(request);
        PortalAccount account = requireCurrentPortalAccount();
        ensureCurrentPortalPhone(request, account);
        return createApplication(request, material, account.getId());
    }

    private OrganizerApplicationSubmitVO createApplication(OrganizerApplicationSubmitRequest request,
                                                            MultipartFile material) {
        return createApplication(request, material, null);
    }

    private OrganizerApplicationSubmitVO createApplication(OrganizerApplicationSubmitRequest request,
                                                            MultipartFile material,
                                                            Long portalAccountId) {
        String phone = normalizePhone(request.getContactPhone());
        OrganizerApplication application = OrganizerApplication.builder()
                .portalAccountId(portalAccountId)
                .applicationNo(generateApplicationNo())
                .organizationName(normalizeRequired(request.getOrganizationName(), "主体名称不能为空"))
                .contactName(normalizeRequired(request.getContactName(), "联系人姓名不能为空"))
                .contactPhoneEnc(piiService.encrypt(phone))
                .contactPhoneHash(piiService.hashPhone(phone))
                .contactPhoneLast4(piiService.phoneLast4(phone))
                .contactEmail(normalizeNullable(request.getContactEmail()))
                .wechatEnc(encryptNullable(request.getWechat()))
                .businessDescription(normalizeNullable(request.getBusinessDescription()))
                .expectedScale(normalizeNullable(request.getExpectedScale()))
                .supplementalNote(normalizeNullable(request.getSupplementalNote()))
                .status(OrganizerApplicationStatus.SUBMITTED.name())
                .submittedTime(LocalDateTime.now())
                .build();
        organizerApplicationMapper.insert(application);
        bindMaterial(application, material);
        return OrganizerApplicationSubmitVO.builder()
                .applicationNo(application.getApplicationNo())
                .status(application.getStatus())
                .statusLabel(statusLabel(application.getStatus()))
                .build();
    }

    @Override
    public OrganizerApplicationStatusVO queryStatus(String applicationNo, String contactPhone) {
        String phone = normalizePhone(contactPhone);
        checkQueryRateLimit(applicationNo, phone);
        OrganizerApplication application = findByVerification(applicationNo, phone);
        return toStatusVO(application, true, false);
    }

    @Override
    public List<OrganizerApplicationStatusVO> listForCurrentPortal() {
        PortalAccount account = requireCurrentPortalAccount();
        String phoneHash = piiService.hashPhone(account.getPhone());
        LambdaQueryWrapper<OrganizerApplication> wrapper = new LambdaQueryWrapper<OrganizerApplication>()
                .and(query -> query
                        .eq(OrganizerApplication::getPortalAccountId, account.getId())
                        .or(fallback -> fallback
                                .isNull(OrganizerApplication::getPortalAccountId)
                                .eq(OrganizerApplication::getContactPhoneHash, phoneHash)))
                .orderByDesc(OrganizerApplication::getSubmittedTime)
                .orderByDesc(OrganizerApplication::getId);
        return organizerApplicationMapper.selectList(wrapper).stream()
                .map(application -> toStatusVO(application, false, true))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationStatusVO resubmit(String applicationNo,
                                                  String contactPhone,
                                                  OrganizerApplicationSubmitRequest request) {
        rejectClientAssetId(request);
        return resubmitInternal(applicationNo, contactPhone, request, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationStatusVO resubmit(String applicationNo,
                                                  String contactPhone,
                                                  OrganizerApplicationSubmitRequest request,
                                                  MultipartFile material) {
        rejectClientAssetId(request);
        return resubmitInternal(applicationNo, contactPhone, request, material, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationStatusVO resubmitForCurrentPortal(String applicationNo,
                                                                  OrganizerApplicationSubmitRequest request) {
        rejectClientAssetId(request);
        PortalAccount account = requireCurrentPortalAccount();
        ensureCurrentPortalPhone(request, account);
        OrganizerApplication application = findByPortalAccount(applicationNo, account);
        application.setPortalAccountId(account.getId());
        return resubmitApplication(application, request, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationStatusVO resubmitForCurrentPortal(String applicationNo,
                                                                  OrganizerApplicationSubmitRequest request,
                                                                  MultipartFile material) {
        rejectClientAssetId(request);
        PortalAccount account = requireCurrentPortalAccount();
        ensureCurrentPortalPhone(request, account);
        OrganizerApplication application = findByPortalAccount(applicationNo, account);
        application.setPortalAccountId(account.getId());
        return resubmitApplication(application, request, material, true);
    }

    private OrganizerApplicationStatusVO resubmitInternal(String applicationNo,
                                                           String contactPhone,
                                                           OrganizerApplicationSubmitRequest request,
                                                           MultipartFile material,
                                                           boolean includeAccount) {
        String normalizedContactPhone = normalizePhone(contactPhone);
        checkQueryRateLimit(applicationNo, normalizedContactPhone);
        OrganizerApplication application = findByVerification(applicationNo, normalizedContactPhone);
        return resubmitApplication(application, request, material, includeAccount);
    }

    private OrganizerApplicationStatusVO resubmitApplication(OrganizerApplication application,
                                                              OrganizerApplicationSubmitRequest request,
                                                              MultipartFile material,
                                                              boolean includeAccount) {
        if (!OrganizerApplicationStatus.NEED_MORE_INFO.name().equals(application.getStatus())) {
            throw new BaseException("当前申请状态不允许补充资料");
        }
        String phone = normalizePhone(request.getContactPhone());
        String originalPhone = normalizePhone(piiService.decrypt(application.getContactPhoneEnc()));
        if (!phone.equals(originalPhone)) {
            throw new BaseException("补充资料时联系人手机号不能变更");
        }
        application.setOrganizationName(normalizeRequired(request.getOrganizationName(), "主体名称不能为空"));
        application.setContactName(normalizeRequired(request.getContactName(), "联系人姓名不能为空"));
        application.setContactEmail(normalizeNullable(request.getContactEmail()));
        application.setWechatEnc(encryptNullable(request.getWechat()));
        application.setBusinessDescription(normalizeNullable(request.getBusinessDescription()));
        application.setExpectedScale(normalizeNullable(request.getExpectedScale()));
        application.setSupplementalNote(normalizeNullable(request.getSupplementalNote()));
        application.setStatus(OrganizerApplicationStatus.SUBMITTED.name());
        application.setReviewRemark(null);
        organizerApplicationMapper.updateById(application);
        bindMaterial(application, material);
        return toStatusVO(application, false, includeAccount);
    }

    private void rejectClientAssetId(OrganizerApplicationSubmitRequest request) {
        if (request != null && request.getMaterialAssetId() != null) {
            throw new BaseException("材料文件必须通过上传接口提交");
        }
    }

    private void bindMaterial(OrganizerApplication application, MultipartFile material) {
        if (material == null || material.isEmpty()) {
            return;
        }
        validateMaterial(material);
        String filename = sanitizeUploadFilename(material.getOriginalFilename(), "organizer-material.pdf");
        byte[] bytes = readUploadBytes(material);
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_MATERIAL, filename, bytes);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_MATERIAL)
                .ownerType(OWNER_TYPE_APPLICATION)
                .ownerId(application.getId())
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(null)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);
        application.setMaterialAssetId(asset.getId());
        organizerApplicationMapper.updateById(application);
    }

    private void validateMaterial(MultipartFile material) {
        if (material.getSize() > MAX_MATERIAL_SIZE) {
            throw new BaseException("主体证明材料不能超过10MB");
        }
        String contentType = material.getContentType();
        if (contentType == null || !MATERIAL_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BaseException("主体证明材料仅支持 PDF、JPG、PNG 文件");
        }
    }

    private byte[] readUploadBytes(MultipartFile material) {
        try {
            return material.getBytes();
        } catch (IOException ex) {
            throw new BaseException("读取主体证明材料失败");
        }
    }

    private String sanitizeUploadFilename(String originalFilename, String fallback) {
        String raw = StringUtils.hasText(originalFilename) ? originalFilename.trim() : fallback;
        String filename = Path.of(raw.replace("\\", "/")).getFileName().toString();
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        return StringUtils.hasText(filename) ? filename : fallback;
    }

    @Override
    public List<OrganizerApplicationAdminVO> listForPlatform(String status) {
        adminIdentityService.requirePlatformSuperAdmin();
        LambdaQueryWrapper<OrganizerApplication> wrapper = new LambdaQueryWrapper<OrganizerApplication>()
                .eq(StringUtils.hasText(status), OrganizerApplication::getStatus, normalizeStatus(status))
                .orderByDesc(OrganizerApplication::getSubmittedTime)
                .orderByDesc(OrganizerApplication::getId);
        return organizerApplicationMapper.selectList(wrapper).stream().map(this::toAdminVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizerApplicationAdminVO review(Long applicationId, OrganizerApplicationReviewRequest request) {
        adminIdentityService.requirePlatformSuperAdmin();
        OrganizerApplication application = requireApplicationForUpdate(applicationId);
        OrganizerApplicationStatus current = parseStatus(application.getStatus());
        OrganizerApplicationStatus next = request.getStatus();
        if (!isReviewTransitionAllowed(current, next)) {
            throw new BaseException("当前申请状态不能变更为" + statusLabel(next.name()));
        }
        application.setStatus(next.name());
        application.setReviewedByAdminId(BaseContext.getCurrentId());
        application.setReviewedTime(LocalDateTime.now());
        application.setReviewRemark(normalizeNullable(request.getRemark()));
        organizerApplicationMapper.updateById(application);
        writeReviewLog(application, current, next);
        return toAdminVO(application);
    }

    private OrganizerApplication findByVerification(String applicationNo, String phone) {
        String normalizedNo = normalizeApplicationNo(applicationNo);
        OrganizerApplication application = organizerApplicationMapper.selectOne(new LambdaQueryWrapper<OrganizerApplication>()
                .eq(OrganizerApplication::getApplicationNo, normalizedNo)
                .eq(OrganizerApplication::getContactPhoneHash, piiService.hashPhone(phone))
                .last("LIMIT 1"));
        if (application == null) {
            throw new ResourceNotFoundException("申请不存在或验证信息不匹配");
        }
        return application;
    }

    private OrganizerApplication findByPortalAccount(String applicationNo, PortalAccount account) {
        OrganizerApplication application = organizerApplicationMapper.selectOne(new LambdaQueryWrapper<OrganizerApplication>()
                .eq(OrganizerApplication::getApplicationNo, normalizeApplicationNo(applicationNo))
                .and(query -> query
                        .eq(OrganizerApplication::getPortalAccountId, account.getId())
                        .or(fallback -> fallback
                                .isNull(OrganizerApplication::getPortalAccountId)
                                .eq(OrganizerApplication::getContactPhoneHash, piiService.hashPhone(account.getPhone()))))
                .last("LIMIT 1"));
        if (application == null) {
            throw new ResourceNotFoundException("申请不存在或当前账号无权访问");
        }
        return application;
    }

    private OrganizerApplication requireApplicationForUpdate(Long applicationId) {
        OrganizerApplication application = applicationId == null
                ? null : organizerApplicationMapper.selectByIdForUpdate(applicationId);
        if (application == null) {
            throw new ResourceNotFoundException("入驻申请不存在");
        }
        return application;
    }

    private boolean isReviewTransitionAllowed(OrganizerApplicationStatus current, OrganizerApplicationStatus next) {
        if (next == null || current == OrganizerApplicationStatus.ACCOUNT_ISSUED
                || current == OrganizerApplicationStatus.REJECTED) {
            return false;
        }
        if (next == OrganizerApplicationStatus.UNDER_REVIEW) {
            return current == OrganizerApplicationStatus.SUBMITTED
                    || current == OrganizerApplicationStatus.NEED_MORE_INFO;
        }
        return current == OrganizerApplicationStatus.UNDER_REVIEW
                && (next == OrganizerApplicationStatus.NEED_MORE_INFO
                || next == OrganizerApplicationStatus.APPROVED
                || next == OrganizerApplicationStatus.REJECTED);
    }

    private void checkQueryRateLimit(String applicationNo, String phone) {
        String key = RATE_LIMIT_PREFIX + normalizeApplicationNo(applicationNo) + ":" + piiService.hashPhone(phone);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(10));
        }
        if (count != null && count > STATUS_QUERY_LIMIT) {
            throw new BaseException("查询过于频繁，请稍后再试");
        }
    }

    private OrganizerApplicationStatusVO toStatusVO(OrganizerApplication application,
                                                    boolean external,
                                                    boolean includeAccount) {
        OrganizerApplicationStatus status = parseStatus(application.getStatus());
        return OrganizerApplicationStatusVO.builder()
                .applicationNo(application.getApplicationNo())
                .organizationName(application.getOrganizationName())
                .contactName(application.getContactName())
                .maskedContactPhone(piiService.maskPhone(piiService.decrypt(application.getContactPhoneEnc())))
                .status(status.name())
                .statusLabel(statusLabel(status.name()))
                .adminUsername(includeAccount && status == OrganizerApplicationStatus.ACCOUNT_ISSUED
                        ? findAdminUsername(application.getInitialAdminUserId()) : null)
                .reviewRemark(external && status != OrganizerApplicationStatus.NEED_MORE_INFO
                        && status != OrganizerApplicationStatus.REJECTED ? null : application.getReviewRemark())
                .submittedTime(application.getSubmittedTime())
                .accountIssuedTime(application.getAccountIssuedTime())
                .build();
    }

    private String findAdminUsername(Long adminUserId) {
        if (adminUserId == null) {
            return null;
        }
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        return adminUser == null ? null : adminUser.getUsername();
    }

    private PortalAccount requireCurrentPortalAccount() {
        if (!UserRole.PORTAL.name().equals(BaseContext.getCurrentRole()) || BaseContext.getCurrentId() == null) {
            throw new BaseException("请先登录厂商账号");
        }
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null || !Integer.valueOf(STATUS_ACTIVE).equals(account.getStatus())) {
            throw new ResourceNotFoundException("厂商账号不存在或已停用");
        }
        return account;
    }

    private void ensureCurrentPortalPhone(OrganizerApplicationSubmitRequest request, PortalAccount account) {
        String requestPhone = normalizePhone(request.getContactPhone());
        String accountPhone = normalizePhone(account.getPhone());
        if (!requestPhone.equals(accountPhone)) {
            throw new BaseException("联系人手机号须与当前登录账号一致");
        }
    }

    private OrganizerApplicationAdminVO toAdminVO(OrganizerApplication application) {
        OrganizerApplicationStatus status = parseStatus(application.getStatus());
        return OrganizerApplicationAdminVO.builder()
                .id(application.getId())
                .applicationNo(application.getApplicationNo())
                .organizationName(application.getOrganizationName())
                .contactName(application.getContactName())
                .maskedContactPhone(piiService.maskPhone(piiService.decrypt(application.getContactPhoneEnc())))
                .contactEmail(application.getContactEmail())
                .maskedWechat(piiService.maskWechat(piiService.decrypt(application.getWechatEnc())))
                .businessDescription(application.getBusinessDescription())
                .expectedScale(application.getExpectedScale())
                .supplementalNote(application.getSupplementalNote())
                .materialAssetId(application.getMaterialAssetId())
                .status(status.name())
                .statusLabel(statusLabel(status.name()))
                .reviewRemark(application.getReviewRemark())
                .organizerId(application.getOrganizerId())
                .initialAdminUserId(application.getInitialAdminUserId())
                .submittedTime(application.getSubmittedTime())
                .reviewedTime(application.getReviewedTime())
                .accountIssuedTime(application.getAccountIssuedTime())
                .build();
    }

    private void writeReviewLog(OrganizerApplication application,
                                OrganizerApplicationStatus current,
                                OrganizerApplicationStatus next) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .action("ORGANIZER_APPLICATION_" + next.name())
                .targetType(TARGET_APPLICATION)
                .targetPublicId(String.valueOf(application.getId()))
                .summary("申请状态：" + current.name() + " -> " + next.name())
                .build());
    }

    private OrganizerApplicationStatus parseStatus(String value) {
        try {
            return OrganizerApplicationStatus.valueOf(value);
        } catch (RuntimeException ex) {
            throw new BaseException("申请状态配置不正确");
        }
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OrganizerApplicationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BaseException("申请状态不正确");
        }
    }

    private String statusLabel(String value) {
        return switch (value) {
            case "SUBMITTED" -> "已提交";
            case "UNDER_REVIEW" -> "审核中";
            case "NEED_MORE_INFO" -> "待补充资料";
            case "APPROVED" -> "审核通过，待发放账号";
            case "ACCOUNT_ISSUED" -> "账号已发放";
            case "REJECTED" -> "已拒绝";
            default -> value;
        };
    }

    private String generateApplicationNo() {
        return "OA" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private String normalizeApplicationNo(String value) {
        return normalizeRequired(value, "申请编号不能为空").toUpperCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        return piiService.normalizePhone(normalizeRequired(value, "联系人手机号不能为空"));
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String encryptNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : piiService.encrypt(normalized);
    }
}
