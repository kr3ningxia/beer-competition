package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminPasswordUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserCreateRequest;
import com.beercompetition.pojo.dto.AdminUserPasswordResetRequest;
import com.beercompetition.pojo.dto.AdminUserStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminUserUpdateRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.AdminUserVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台账号接口，提供当前管理员登录态相关信息。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAccountController {

    private final AuthService authService;
    private final AdminUserService adminUserService;

    /**
     * 获取当前后台管理员身份信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.ADMIN));
    }

    /**
     * 查询管理员账号列表。
     */
    @GetMapping("/accounts")
    public Result<List<AdminUserVO>> list(@RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.listAdminUsers(status, keyword));
    }

    /**
     * 新增管理员账号。
     */
    @PostMapping("/accounts")
    public Result<AdminUserVO> create(@RequestBody @Valid AdminUserCreateRequest request) {
        return Result.success(adminUserService.createAdminUser(request));
    }

    /**
     * 更新管理员姓名。
     */
    @PutMapping("/accounts/{id}")
    public Result<AdminUserVO> update(@PathVariable Long id,
                                      @RequestBody @Valid AdminUserUpdateRequest request) {
        return Result.success(adminUserService.updateAdminUser(id, request));
    }

    /**
     * 更新管理员启停状态。
     */
    @PatchMapping("/accounts/{id}/status")
    public Result<AdminUserVO> updateStatus(@PathVariable Long id,
                                            @RequestBody @Valid AdminUserStatusUpdateRequest request) {
        return Result.success(adminUserService.updateAdminUserStatus(id, request));
    }

    /**
     * 重置管理员密码。
     */
    @PatchMapping("/accounts/{id}/password")
    public Result<String> resetPassword(@PathVariable Long id,
                                        @RequestBody @Valid AdminUserPasswordResetRequest request) {
        adminUserService.resetAdminUserPassword(id, request);
        return Result.success("密码已重置");
    }

    /**
     * 修改当前管理员密码。
     */
    @PatchMapping("/me/password")
    public Result<String> updateMyPassword(@RequestBody @Valid AdminPasswordUpdateRequest request) {
        adminUserService.updateMyPassword(request);
        return Result.success("密码已更新");
    }
}
