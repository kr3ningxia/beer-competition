package com.beercompetition.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.util.SmsMessageUtils;
import com.beercompetition.pojo.enums.SmsBizType;
import com.beercompetition.properties.SmsProperties;
import com.beercompetition.service.SmsAuthProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliyunSmsAuthProvider implements SmsAuthProvider {

    private static final String SUCCESS_CODE = "OK";
    private static final String VERIFY_PASS = "PASS";
    private static final String FREQUENCY_KEYWORD = "frequency";

    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void sendCode(String phone, SmsBizType bizType) {
        ensureConfigured();

        try {
            SendSmsVerifyCodeResponse response = createClient().sendSmsVerifyCode(new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setCountryCode(smsProperties.getCountryCode())
                    .setSchemeName(smsProperties.getSchemeName())
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(smsProperties.getTemplateCode())
                    .setTemplateParam(buildTemplateParam())
                    .setValidTime(smsProperties.getCodeTtlMinutes() * 60)
                    .setInterval(smsProperties.getSendIntervalSeconds())
                    .setCodeLength(smsProperties.getCodeLength())
                    .setCodeType(smsProperties.getCodeType())
                    .setDuplicatePolicy(smsProperties.getDuplicatePolicy())
                    .setReturnVerifyCode(false)
                    .setOutId(buildOutId(bizType)));
            SendSmsVerifyCodeResponseBody body = response.getBody();
            if (!isSuccess(body == null ? null : body.getCode(), body == null ? null : body.getSuccess())) {
                String message = body == null ? "短信验证码发送失败" : body.getMessage();
                log.warn("阿里云短信发送失败 phone={}, code={}, message={}", maskPhone(phone),
                        body == null ? null : body.getCode(), message);
                throw new BaseException(resolveSendFailureMessage(body == null ? null : body.getCode(), message));
            }
            log.info("阿里云短信发送成功 phone={}, requestId={}", maskPhone(phone), body.getRequestId());
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("阿里云短信发送异常 phone={}", maskPhone(phone), ex);
            throw new BaseException("短信验证码发送失败，请稍后重试");
        }
    }

    @Override
    public boolean checkCode(String phone, String code, SmsBizType bizType) {
        ensureConfigured();

        try {
            CheckSmsVerifyCodeResponse response = createClient().checkSmsVerifyCode(new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setCountryCode(smsProperties.getCountryCode())
                    .setSchemeName(smsProperties.getSchemeName())
                    .setVerifyCode(code)
                    .setOutId(buildOutId(bizType)));
            CheckSmsVerifyCodeResponseBody body = response.getBody();
            if (!isSuccess(body == null ? null : body.getCode(), body == null ? null : body.getSuccess())) {
                log.warn("阿里云短信校验失败 phone={}, code={}, message={}", maskPhone(phone),
                        body == null ? null : body.getCode(), body == null ? null : body.getMessage());
                return false;
            }
            String verifyResult = body.getModel() == null ? null : body.getModel().getVerifyResult();
            return VERIFY_PASS.equals(verifyResult);
        } catch (Exception ex) {
            log.error("阿里云短信校验异常 phone={}", maskPhone(phone), ex);
            throw new BaseException("验证码校验失败，请稍后重试");
        }
    }

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(smsProperties.getAccessKeyId())
                .setAccessKeySecret(smsProperties.getAccessKeySecret())
                .setEndpoint(smsProperties.getEndpoint());
        return new Client(config);
    }

    private String buildTemplateParam() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", "##code##");
        params.put("min", String.valueOf(smsProperties.getCodeTtlMinutes()));
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException ex) {
            throw new BaseException("短信模板参数生成失败");
        }
    }

    private String buildOutId(SmsBizType bizType) {
        return "beer-" + bizType.name().toLowerCase() + "-" + UUID.randomUUID();
    }

    private boolean isSuccess(String code, Boolean success) {
        return SUCCESS_CODE.equals(code) && Boolean.TRUE.equals(success);
    }

    private String resolveSendFailureMessage(String code, String message) {
        if (isFrequencyFailure(code, message)) {
            return SmsMessageUtils.buildFrequencyMessage(smsProperties.getSendIntervalSeconds());
        }
        return StringUtils.hasText(message) ? message : "短信验证码发送失败";
    }

    private boolean isFrequencyFailure(String code, String message) {
        return containsFrequencyKeyword(code) || containsFrequencyKeyword(message);
    }

    private boolean containsFrequencyKeyword(String value) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(FREQUENCY_KEYWORD);
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(smsProperties.getAccessKeyId())
                || !StringUtils.hasText(smsProperties.getAccessKeySecret())
                || !StringUtils.hasText(smsProperties.getSignName())
                || !StringUtils.hasText(smsProperties.getTemplateCode())) {
            throw new BaseException("阿里云短信认证配置不完整");
        }
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "已填写";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
