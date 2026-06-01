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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicAuthController {

    private final AuthService authService;

    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
        return Result.success(authService.adminLogin(request));
    }

    @PostMapping("/sms/send")
    public Result<String> sendSmsCode(@RequestBody @Valid SmsSendRequest request) {
        authService.sendSmsCode(request);
        return Result.success("验证码已发送");
    }

    @PostMapping("/portal/login")
    public Result<LoginResponse> portalLogin(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.portalLogin(request));
    }

    @PostMapping("/judge/login")
    public Result<LoginResponse> judgeLogin(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.judgeLogin(request));
    }

    @PostMapping("/judge/register")
    public Result<LoginResponse> judgeRegister(@RequestBody @Valid SmsLoginRequest request) {
        return Result.success(authService.judgeRegister(request));
    }
}
