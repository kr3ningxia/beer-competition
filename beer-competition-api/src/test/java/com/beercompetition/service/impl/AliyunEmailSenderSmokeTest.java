package com.beercompetition.service.impl;

import com.beercompetition.properties.EmailProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("external")
@EnabledIfEnvironmentVariable(named = "APP_MAIL_SMOKE_TEST", matches = "true")
class AliyunEmailSenderSmokeTest {

    @Test
    void sendsTestEmail() {
        EmailProperties properties = new EmailProperties();
        properties.setAccessKeyId(requiredEnvironment("APP_ALIYUN_ACCESS_KEY_ID"));
        properties.setAccessKeySecret(requiredEnvironment("APP_ALIYUN_ACCESS_KEY_SECRET"));
        properties.setEndpoint(environmentOrDefault("APP_ALIYUN_MAIL_ENDPOINT", "dm.aliyuncs.com"));
        properties.setFromAddress(environmentOrDefault(
                "APP_ALIYUN_MAIL_FROM", "beernotice@notify.beermatters.cn"));
        properties.setFromAlias(environmentOrDefault("APP_ALIYUN_MAIL_FROM_ALIAS", "啤酒大赛"));

        String requestId = new AliyunEmailSender(properties).send(
                requiredEnvironment("APP_MAIL_SMOKE_TO"),
                "啤酒大赛邮件接入测试",
                "<h2>邮件接入测试成功</h2><p>这是一封用于验证阿里云邮件推送的测试邮件。</p>");

        assertThat(requestId).isNotBlank();
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量: " + name);
        }
        return value;
    }

    private String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
