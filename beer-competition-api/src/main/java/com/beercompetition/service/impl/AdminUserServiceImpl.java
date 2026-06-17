package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.Md5Util;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.AdminUserMapper;
import com.beercompetition.pojo.dto.AdminPasswordUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserCreateRequest;
import com.beercompetition.pojo.dto.AdminUserPasswordResetRequest;
import com.beercompetition.pojo.dto.AdminUserStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserUpdateRequest;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.AdminUser;
import com.beercompetition.pojo.vo.AdminUserVO;
import com.beercompetition.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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

    private final AdminUserMapper adminUserMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    @Override
    public List<AdminUserVO> listAdminUsers(Integer status, String keyword) {
        // 1) 构造查询条件
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            validateStatus(status);
            wrapper.eq(AdminUser::getStatus, status);
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
        // 1) 参数规范化与账号唯一性前置校验
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username);

        // 2) 创建启用管理员账号
        AdminUser adminUser = AdminUser.builder()
                .username(username)
                .name(request.getName().trim())
                .password(Md5Util.encode(request.getPassword()))
                .status(STATUS_ACTIVE)
                .build();
        try {
            adminUserMapper.insert(adminUser);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("登录账号已存在");
        }
        writeAdminLog("ADMIN_USER_CREATE", adminUser.getId(), "新增管理员账号：" + adminUser.getUsername());

        // 3) 组装并返回结果
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdminUser(Long id, AdminUserUpdateRequest request) {
        // 1) 查询目标账号
        AdminUser adminUser = requireAdminUser(id);

        // 2) 更新显示姓名
        adminUser.setName(request.getName().trim());
        adminUserMapper.updateById(adminUser);
        writeAdminLog("ADMIN_USER_UPDATE", adminUser.getId(), "更新管理员姓名：" + adminUser.getUsername());

        // 3) 组装并返回结果
        return toVO(adminUserMapper.selectById(adminUser.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateAdminUserStatus(Long id, AdminUserStatusUpdateRequest request) {
        // 1) 查询目标账号并校验状态
        validateStatus(request.getStatus());
        AdminUser adminUser = requireAdminUser(id);
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
        // 1) 查询目标账号
        AdminUser adminUser = requireAdminUser(id);

        // 2) 重置登录密码
        adminUser.setPassword(Md5Util.encode(request.getPassword()));
        adminUserMapper.updateById(adminUser);
        writeAdminLog("ADMIN_USER_PASSWORD_RESET", adminUser.getId(), "重置管理员密码：" + adminUser.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyPassword(AdminPasswordUpdateRequest request) {
        // 1) 查询当前账号并校验旧密码
        AdminUser adminUser = requireAdminUser(BaseContext.getCurrentId());
        if (!Md5Util.encode(request.getOldPassword()).equals(adminUser.getPassword())) {
            throw new BaseException("当前密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BaseException("新密码不能与当前密码相同");
        }

        // 2) 更新当前账号密码
        adminUser.setPassword(Md5Util.encode(request.getNewPassword()));
        adminUserMapper.updateById(adminUser);
        writeAdminLog("ADMIN_USER_PASSWORD_CHANGE", adminUser.getId(), "修改当前管理员密码");
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
        Long activeCount = adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getStatus, STATUS_ACTIVE));
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
}
