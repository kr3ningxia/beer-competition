package com.beercompetition.service;

import com.beercompetition.pojo.dto.AdminPasswordUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserCreateRequest;
import com.beercompetition.pojo.dto.AdminUserPasswordResetRequest;
import com.beercompetition.pojo.dto.AdminUserStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserUpdateRequest;
import com.beercompetition.pojo.vo.AdminUserVO;

import java.util.List;

public interface AdminUserService {

    List<AdminUserVO> listAdminUsers(Integer status, String keyword);

    AdminUserVO createAdminUser(AdminUserCreateRequest request);

    AdminUserVO updateAdminUser(Long id, AdminUserUpdateRequest request);

    AdminUserVO updateAdminUserStatus(Long id, AdminUserStatusUpdateRequest request);

    void resetAdminUserPassword(Long id, AdminUserPasswordResetRequest request);

    void updateMyPassword(AdminPasswordUpdateRequest request);
}
