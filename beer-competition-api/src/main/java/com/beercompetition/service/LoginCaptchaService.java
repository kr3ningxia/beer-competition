package com.beercompetition.service;

import com.beercompetition.pojo.vo.LoginCaptchaResponse;

public interface LoginCaptchaService {

    LoginCaptchaResponse createChallenge();

    void validateIfRequired(String subject, String captchaId, String captchaCode);

    void recordFailure(String subject);

    void clearFailure(String subject);
}
