package com.beercompetition.service;

import com.beercompetition.pojo.dto.AdminLoginRequest;
import com.beercompetition.pojo.dto.SmsLoginRequest;
import com.beercompetition.pojo.dto.SmsSendRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.LoginResponse;

public interface AuthService {

    LoginResponse adminLogin(AdminLoginRequest request);

    void sendSmsCode(SmsSendRequest request);

    LoginResponse portalLogin(SmsLoginRequest request);

    LoginResponse judgeLogin(SmsLoginRequest request);

    CurrentUserResponse getCurrentUser(UserRole role);
}
