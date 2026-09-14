package com.beercompetition.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一次性登录图形验证码挑战。
 */
@Data
@AllArgsConstructor
public class LoginCaptchaResponse {

    private String captchaId;
    private String image;
}
