package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台账号接口，提供当前管理员登录态相关信息。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAccountController {

    private final AuthService authService;

    /**
     * 获取当前后台管理员身份信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.ADMIN));
    }
}
