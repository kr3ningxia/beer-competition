package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.mapper.OrganizerApplicationMapper;
import com.beercompetition.mapper.OrganizerMemberMapper;
import com.beercompetition.pojo.dto.AdminPasswordUpdateRequest;
import com.beercompetition.pojo.dto.AdminCredentialsUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserCreateRequest;
import com.beercompetition.pojo.dto.AdminUserPasswordResetRequest;
import com.beercompetition.pojo.dto.AdminUserStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserUpdateRequest;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.po.OrganizerApplication;
import com.beercompetition.pojo.po.OrganizerMember;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.vo.AdminUserVO;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.security.AdminSessionIdentity;
import com.beercompetition.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final String TARGET_ADMIN_USER = "ADMIN_USER";
    private static final String INITIAL_CREDENTIAL_DELIVERY_PREFIX =
            "beer-competition:organizer-application:credential:";

    private final AdminUserMapper adminUserMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final OrganizerMemberMapper organizerMemberMapper;
    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final AdminIdentityService adminIdentityService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<AdminUserVO> listAdminUsers(Integer status, String adminType, String keyword) {
        AdminSessionIdentity currentIdentity = adminIdentityService.requireCurrentIdentity();
        List<Long> manageableIds = manageableAdminIds(currentIdentity);
        // 1) 构造查询条件
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (manageableIds != null) {
            wrapper.in(!manageableIds.isEmpty(), AdminUser::getId, manageableIds)
                    .eq(manageableIds.isEmpty(), AdminUser::getId, -1L);
        }
        if (status != null) {
            validateStatus(status);
            wrapper.eq(AdminUser::getStatus, status);
        }
        if (StringUtils.hasText(adminType)) {
            wrapper.eq(AdminUser::getAdminType, parseRequestedAdminType(adminType).name());
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(query -> query
                    .like(AdminUser::getUsername, normalizedKeyword)
                    .or()
                    .like(AdminUser::getName, normalizedKeyword));
        }

        // 2) 查询并组装账号列表
        return adminUserMapper.selectList(wrapper.orderByAsc(AdminUser::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createAdminUser(AdminUserCreateRequest request) {
        AdminSessionIdentity currentIdentity = adminIdentityService.requireCurrentIdentity();
        AdminType newAdminType = resolveCreatedAdminType(currentIdentity, request.getAdminType());
        // 1) 参数规范化与账号唯一性前置校验
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username);

        // 2) 创建启用管理员账号
        // 内部新建账号由创建者设定登录名，只强制首次改密；改登录名的要求仅适用于平台发放的初始账号。
        AdminUser adminUser = AdminUser.builder()
                .username(username)
                .name(request.getName().trim())
                .password(Md5Util.encode(request.getPassword()))
                .status(STATUS_ACTIVE)
                .adminType(newAdminType.name())
                .mustChangePassword(1)
                .mustChangeUsername(0)
                .build();
        try {
            adminUserMapper.insert(adminUser);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("登录账号已存在");
        }
        if (newAdminType.isOrganizerAdmin()) {
            organizerMemberMapper.insert(OrganizerMember.builder()
                    .organizerId(currentIdentity.organizerId())
                    .adminUserId(adminUser.getId())
                    .status(STATUS_ACTIVE)
                    .build());
        }
        writeAdminLog("ADMIN_USER_CREATE", adminUser.getId(), "新增管理员账号：" + adminUser.getUsername());

        // 3) 组装并返回结果
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdminUser(Long id, AdminUserUpdateRequest request) {
        // 1) 查询目标账号并解析类型变更
        AdminUser adminUser = requireManageableAdmin(id);
        AdminType nextType = resolveUpdatedAdminType(adminUser, request.getAdminType());

        // 2) 更新显示姓名与管理员类型
        adminUser.setName(request.getName().trim());
        if (nextType != null) {
            adminUser.setAdminType(nextType.name());
        }
        adminUserMapper.updateById(adminUser);
        writeAdminLog("ADMIN_USER_UPDATE", adminUser.getId(), "更新管理员资料：" + adminUser.getUsername());

        // 3) 组装并返回结果
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    /**
     * 解析编辑时的类型变更。返回 null 表示不修改类型。
     * 只允许在同一侧内调整，且不允许修改自己的类型，避免把自己锁出后台。
     */
    private AdminType resolveUpdatedAdminType(AdminUser adminUser, String requestedType) {
        AdminType requested = parseRequestedAdminType(requestedType);
        if (requested == null || requested.name().equals(adminUser.getAdminType())) {
            return null;
        }
        if (adminUser.getId().equals(BaseContext.getCurrentId())) {
            throw new BaseException("不能修改自己的管理员类型");
        }
        AdminType current = parseRequestedAdminType(adminUser.getAdminType());
        if (isPlatformType(current) != isPlatformType(requested)) {
            throw new BaseException("只能在同一侧内调整管理员类型");
        }
        if (current == AdminType.ORGANIZER_ADMIN && requested == AdminType.ORGANIZER_SUB_ADMIN) {
            ensureOrganizerKeepsMainAdmin(adminUser);
        }
        return requested;
    }

    private void ensureOrganizerKeepsMainAdmin(AdminUser adminUser) {
        OrganizerMember member = organizerMemberMapper.selectOne(new LambdaQueryWrapper<OrganizerMember>()
                .eq(OrganizerMember::getAdminUserId, adminUser.getId())
                .eq(OrganizerMember::getStatus, STATUS_ACTIVE)
                .orderByAsc(OrganizerMember::getId)
                .last("LIMIT 1"));
        if (member == null || member.getOrganizerId() == null) {
            return;
        }
        List<Long> memberIds = organizerMemberMapper.selectList(new LambdaQueryWrapper<OrganizerMember>()
                        .eq(OrganizerMember::getOrganizerId, member.getOrganizerId())
                        .eq(OrganizerMember::getStatus, STATUS_ACTIVE))
                .stream()
                .map(OrganizerMember::getAdminUserId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (memberIds.isEmpty()) {
            return;
        }
        Long mainAdminCount = adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .in(AdminUser::getId, memberIds)
                .eq(AdminUser::getStatus, STATUS_ACTIVE)
                .eq(AdminUser::getAdminType, AdminType.ORGANIZER_ADMIN.name()));
        if (mainAdminCount <= 1) {
            throw new BaseException("至少保留一个主办方主管理员");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdminUserStatus(Long id, AdminUserStatusUpdateRequest request) {
        // 1) 查询目标账号并校验状态
        validateStatus(request.getStatus());
        AdminUser adminUser = requireManageableAdmin(id);
        if (adminUser.getStatus() != null && adminUser.getStatus().equals(request.getStatus())) {
            return toVO(adminUser);
        }
        if (request.getStatus() == STATUS_DISABLED) {
            ensureCanDisable(adminUser);
        }

        // 2) 更新账号启停状态
        adminUser.setStatus(request.getStatus());
        adminUserMapper.updateById(adminUser);
        writeAdminLog(resolveStatusAction(request.getStatus()), adminUser.getId(),
                statusLabel(request.getStatus()) + "管理员账号：" + adminUser.getUsername());

        // 3) 组装并返回结果
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAdminUserPassword(Long id, AdminUserPasswordResetRequest request) {
        // 1) 查询目标账号并拒绝重置本人密码，避免把自己变成待首次设置状态
        AdminUser adminUser = requireManageableAdmin(id);
        if (adminUser.getId().equals(BaseContext.getCurrentId())) {
            throw new BaseException("请到账号设置修改自己的密码");
        }

        // 2) 重置登录密码；管理员直接下发可用密码，目标账号不再被强制改密或改登录名。
        adminUser.setPassword(Md5Util.encode(request.getPassword()));
        adminUser.setMustChangePassword(0);
        adminUser.setMustChangeUsername(0);
        adminUserMapper.updateById(adminUser);
        clearInitialCredentialDelivery(adminUser.getId());
        writeAdminLog("ADMIN_USER_PASSWORD_RESET", adminUser.getId(), "重置管理员密码：" + adminUser.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyPassword(AdminPasswordUpdateRequest request) {
        if (isUsernameSetupRequired(requireAdminUser(BaseContext.getCurrentId()))) {
            throw new BaseException("请先完成账号设置");
        }
        updateMyCredentials(AdminCredentialsUpdateRequest.builder()
                .oldPassword(request.getOldPassword())
                .newPassword(request.getNewPassword())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateMyCredentials(AdminCredentialsUpdateRequest request) {
        // 1) 读取当前账号并判断是首次初始化还是普通设置
        AdminUser adminUser = requireAdminUser(BaseContext.getCurrentId());
        boolean firstSetup = isCredentialSetupRequired(adminUser);
        String username = normalizeUsername(request.getUsername());
        String newPassword = request.getNewPassword();

        validateCredentialRequest(adminUser, firstSetup, username, newPassword, request.getOldPassword());

        // 2) 只更新提交的凭据字段；首次设置成功后清除两个强制标记
        boolean usernameChanged = StringUtils.hasText(username) && !username.equals(adminUser.getUsername());
        if (StringUtils.hasText(username)) {
            adminUser.setUsername(username);
        }
        if (StringUtils.hasText(newPassword)) {
            adminUser.setPassword(Md5Util.encode(newPassword));
        }
        if (firstSetup) {
            adminUser.setMustChangePassword(0);
            adminUser.setMustChangeUsername(0);
        } else if (StringUtils.hasText(newPassword)) {
            adminUser.setMustChangePassword(0);
        }
        try {
            adminUserMapper.updateById(adminUser);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("该登录账号已被使用");
        }
        if (firstSetup) {
            clearInitialCredentialDelivery(adminUser.getId());
        }

        String summary = usernameChanged ? "修改管理员登录账号" : "修改当前管理员密码";
        writeAdminLog("ADMIN_USER_CREDENTIALS_CHANGE", adminUser.getId(), summary);
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    private void validateCredentialRequest(AdminUser adminUser,
                                           boolean firstSetup,
                                           String username,
                                           String newPassword,
                                           String oldPassword) {
        if (firstSetup) {
            if (Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangeUsername())) {
                if (!StringUtils.hasText(username)) {
                    throw new BaseException("请输入新的登录账号");
                }
                if (username.equals(adminUser.getUsername())) {
                    throw new BaseException("登录账号需要设置为新的账号");
                }
            }
            validateNewPassword(newPassword);
            return;
        }
        if (!StringUtils.hasText(oldPassword) || !Md5Util.encode(oldPassword).equals(adminUser.getPassword())) {
            throw new BaseException("当前密码不正确");
        }
        if (!StringUtils.hasText(username) && !StringUtils.hasText(newPassword)) {
            throw new BaseException("请至少修改登录账号或密码");
        }
        if (StringUtils.hasText(newPassword)) {
            validateNewPassword(newPassword);
            if (oldPassword.equals(newPassword)) {
                throw new BaseException("新密码不能与当前密码相同");
            }
        }
    }

    private void validateNewPassword(String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BaseException("密码长度需为6到32位");
        }
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        String normalized = username.trim();
        if (!normalized.matches("[A-Za-z0-9._-]{4,32}")) {
            throw new BaseException("登录账号需为4到32位英文、数字或._-");
        }
        return normalized;
    }

    private boolean isCredentialSetupRequired(AdminUser adminUser) {
        return Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangePassword())
                || Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangeUsername());
    }

    private boolean isUsernameSetupRequired(AdminUser adminUser) {
        return Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangeUsername());
    }

    private AdminUser requireAdminUser(Long id) {
        if (id == null) {
            throw new BaseException("管理员账号不存在");
        }
        AdminUser adminUser = adminUserMapper.selectById(id);
        if (adminUser == null) {
            throw new ResourceNotFoundException("管理员账号不存在");
        }
        return adminUser;
    }

    private AdminUser requireManageableAdmin(Long id) {
        AdminUser adminUser = requireAdminUser(id);
        AdminSessionIdentity currentIdentity = adminIdentityService.requireCurrentIdentity();
        if (currentIdentity.adminType() == AdminType.PLATFORM_SUPER_ADMIN) {
            return adminUser;
        }
        if (currentIdentity.adminType() != AdminType.ORGANIZER_ADMIN
                || !(AdminType.ORGANIZER_ADMIN.name().equals(adminUser.getAdminType())
                || AdminType.ORGANIZER_SUB_ADMIN.name().equals(adminUser.getAdminType()))
                || !hasActiveMembership(currentIdentity.organizerId(), adminUser.getId())) {
            throw new ForbiddenException("当前账号无权管理该管理员");
        }
        return adminUser;
    }

    private boolean hasActiveMembership(Long organizerId, Long adminUserId) {
        return organizerId != null && adminUserId != null
                && organizerMemberMapper.selectOne(new LambdaQueryWrapper<OrganizerMember>()
                .eq(OrganizerMember::getOrganizerId, organizerId)
                .eq(OrganizerMember::getAdminUserId, adminUserId)
                .eq(OrganizerMember::getStatus, STATUS_ACTIVE)
                .last("LIMIT 1")) != null;
    }

    private List<Long> manageableAdminIds(AdminSessionIdentity identity) {
        if (identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN) {
            return null;
        }
        if (identity.adminType() != AdminType.ORGANIZER_ADMIN || identity.organizerId() == null) {
            throw new ForbiddenException("当前账号无权管理管理员账号");
        }
        return organizerMemberMapper.selectList(new LambdaQueryWrapper<OrganizerMember>()
                        .eq(OrganizerMember::getOrganizerId, identity.organizerId())
                        .eq(OrganizerMember::getStatus, STATUS_ACTIVE)
                        .orderByAsc(OrganizerMember::getId))
                .stream()
                .map(OrganizerMember::getAdminUserId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private AdminType resolveCreatedAdminType(AdminSessionIdentity identity, String requestedType) {
        AdminType requested = parseRequestedAdminType(requestedType);
        if (identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN) {
            if (requested != null && !isPlatformType(requested)) {
                throw new BaseException("平台账号只能创建平台赛事管理员或平台超级管理员");
            }
            return requested == null ? AdminType.PLATFORM_EVENT_ADMIN : requested;
        }
        if (identity.adminType() == AdminType.ORGANIZER_ADMIN && identity.organizerId() != null) {
            if (requested != null && isPlatformType(requested)) {
                throw new BaseException("主办方账号不能创建平台管理员");
            }
            return requested == null ? AdminType.ORGANIZER_ADMIN : requested;
        }
        throw new ForbiddenException("当前账号无权新增管理员账号");
    }

    private AdminType parseRequestedAdminType(String requestedType) {
        if (!StringUtils.hasText(requestedType)) {
            return null;
        }
        try {
            return AdminType.valueOf(requestedType);
        } catch (IllegalArgumentException ex) {
            throw new BaseException("管理员类型不正确");
        }
    }

    private boolean isPlatformType(AdminType adminType) {
        return adminType == AdminType.PLATFORM_SUPER_ADMIN || adminType == AdminType.PLATFORM_EVENT_ADMIN;
    }

    private void ensureUsernameAvailable(String username) {
        AdminUser existing = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        if (existing != null) {
            throw new BaseException("登录账号已存在");
        }
    }

    private void ensureCanDisable(AdminUser adminUser) {
        Long currentId = BaseContext.getCurrentId();
        if (adminUser.getId().equals(currentId)) {
            throw new BaseException("不能停用当前登录账号");
        }
        AdminSessionIdentity identity = adminIdentityService.requireCurrentIdentity();
        LambdaQueryWrapper<AdminUser> countWrapper = new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getStatus, STATUS_ACTIVE);
        List<Long> manageableIds = manageableAdminIds(identity);
        if (manageableIds != null) {
            countWrapper.in(!manageableIds.isEmpty(), AdminUser::getId, manageableIds)
                    .eq(manageableIds.isEmpty(), AdminUser::getId, -1L);
        }
        Long activeCount = adminUserMapper.selectCount(countWrapper);
        if (activeCount <= 1) {
            throw new BaseException("至少保留一个启用的管理员账号");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != STATUS_ACTIVE && status != STATUS_DISABLED)) {
            throw new BaseException("账号状态不正确");
        }
    }

    private String resolveStatusAction(Integer status) {
        return status == STATUS_ACTIVE ? "ADMIN_USER_ENABLE" : "ADMIN_USER_DISABLE";
    }

    private AdminUserVO toVO(AdminUser adminUser) {
        return AdminUserVO.builder()
                .id(adminUser.getId())
                .username(adminUser.getUsername())
                .name(adminUser.getName())
                .adminType(adminUser.getAdminType())
                .organizerId(adminIdentityService.findOrganizerIdForDisplay(adminUser))
                .mustChangePassword(Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangePassword()))
                .mustChangeUsername(Integer.valueOf(STATUS_ACTIVE).equals(adminUser.getMustChangeUsername()))
                .status(adminUser.getStatus())
                .statusLabel(statusLabel(adminUser.getStatus()))
                .currentUser(adminUser.getId().equals(BaseContext.getCurrentId()))
                .createTime(adminUser.getCreateTime())
                .updateTime(adminUser.getUpdateTime())
                .build();
    }

    private String statusLabel(Integer status) {
        if (status != null && status == STATUS_ACTIVE) {
            return "启用";
        }
        return "停用";
    }

    private void writeAdminLog(String action, Long targetId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetType(TARGET_ADMIN_USER)
                .targetPublicId(String.valueOf(targetId))
                .summary(summary)
                .build());
    }

    private void clearInitialCredentialDelivery(Long adminUserId) {
        if (adminUserId == null) {
            return;
        }
        OrganizerApplication application = organizerApplicationMapper.selectOne(new LambdaQueryWrapper<OrganizerApplication>()
                .eq(OrganizerApplication::getInitialAdminUserId, adminUserId)
                .last("LIMIT 1"));
        if (application != null && application.getId() != null) {
            redisTemplate.delete(INITIAL_CREDENTIAL_DELIVERY_PREFIX + application.getId());
        }
    }
}
