package com.beercompetition.service;

import com.beercompetition.pojo.enums.SmsBizType;

public interface SmsAuthProvider {

    void sendCode(String phone, SmsBizType bizType);

    boolean checkCode(String phone, String code, SmsBizType bizType);
}
