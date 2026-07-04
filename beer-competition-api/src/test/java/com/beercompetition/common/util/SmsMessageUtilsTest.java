package com.beercompetition.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsMessageUtilsTest {

    @Test
    void buildFrequencyMessageFormatsSecondsMinutesAndMixedTime() {
        assertThat(SmsMessageUtils.buildFrequencyMessage(42))
                .isEqualTo("验证码发送过于频繁，请42秒后再试");
        assertThat(SmsMessageUtils.buildFrequencyMessage(60))
                .isEqualTo("验证码发送过于频繁，请1分钟后再试");
        assertThat(SmsMessageUtils.buildFrequencyMessage(75))
                .isEqualTo("验证码发送过于频繁，请1分15秒后再试");
    }
}
