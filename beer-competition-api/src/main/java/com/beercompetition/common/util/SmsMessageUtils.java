package com.beercompetition.common.util;

public final class SmsMessageUtils {

    private SmsMessageUtils() {
    }

    public static String buildFrequencyMessage(long waitSeconds) {
        return "验证码发送过于频繁，请" + formatWaitTime(waitSeconds) + "后再试";
    }

    private static String formatWaitTime(long seconds) {
        long safeSeconds = Math.max(1, seconds);
        if (safeSeconds < 60) {
            return safeSeconds + "秒";
        }
        long minutes = safeSeconds / 60;
        long remainingSeconds = safeSeconds % 60;
        if (remainingSeconds == 0) {
            return minutes + "分钟";
        }
        return minutes + "分" + remainingSeconds + "秒";
    }
}
