package com.beercompetition.controller.publicapi;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.SmsLoginRequest;
import com.beercompetition.pojo.dto.SmsSendRequest;
import com.beercompetition.pojo.vo.LoginResponse;
import com.beercompetition.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开认证接口，处理后台登录、短信验证码和厂商/评委登录注册。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicAuthController {

    private final AuthService authService;

    /**
     * 后台管理员账号密码登录。
     */
    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
        return Result.success(authService.adminLogin(request));
    }

    /**
     * 发送短信验证码。
     */
    @PostMapping("/sms/send")
    public Result<String> sendSmsCode(@RequestBody @Valid SmsSendRequest request) {
        authService.sendSmsCode(request);
        return Result.success("验证码已发送");
    }

    /**
     * 厂商使用短信验证码登录。
     */
    @PostMapping("/portal/login")
    public Result<LoginResponse> portalLogin(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.portalLogin(request));
    }

    /**
     * 评委使用短信验证码登录。
     */
    @PostMapping("/judge/login")
    public Result<LoginResponse> judgeLogin(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.judgeLogin(request));
    }

    /**
     * 评委使用短信验证码注册或补全初始账号。
     */
    @PostMapping("/judge/register")
    public Result<LoginResponse> judgeRegister(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.judgeRegister(request));
    }
}
