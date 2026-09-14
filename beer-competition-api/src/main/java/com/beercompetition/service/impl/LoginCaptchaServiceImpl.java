package com.beercompetition.service.impl;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.vo.LoginCaptchaResponse;
import com.beercompetition.service.LoginCaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginCaptchaServiceImpl implements LoginCaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "beer-competition:auth:login-captcha:";
    private static final String FAILURE_KEY_PREFIX = "beer-competition:auth:sms-login-failure:";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final int IMAGE_WIDTH = 132;
    private static final int IMAGE_HEIGHT = 44;
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration FAILURE_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginCaptchaResponse createChallenge() {
        String captchaId = UUID.randomUUID().toString();
        String code = createCode();
        redisTemplate.opsForValue().set(buildCaptchaKey(captchaId), code, CAPTCHA_TTL);
        return new LoginCaptchaResponse(captchaId, renderImage(code));
    }

    @Override
    public void validateIfRequired(String subject, String captchaId, String captchaCode) {
        if (!isFailureRecorded(subject)) {
            return;
        }
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BaseException("请输入图形验证码");
        }

        Object expectedCode = redisTemplate.opsForValue().get(buildCaptchaKey(captchaId.trim()));
        if (expectedCode == null || !String.valueOf(expectedCode).equalsIgnoreCase(captchaCode.trim())) {
            throw new BaseException("图形验证码错误或已过期");
        }
        redisTemplate.delete(buildCaptchaKey(captchaId.trim()));
    }

    @Override
    public void recordFailure(String subject) {
        String key = buildFailureKey(subject);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, FAILURE_TTL);
        }
    }

    @Override
    public void clearFailure(String subject) {
        redisTemplate.delete(buildFailureKey(subject));
    }

    private boolean isFailureRecorded(String subject) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildFailureKey(subject)));
    }

    private String createCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(CODE_CHARS.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private String renderImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(255, 253, 247));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            for (int index = 0; index < 7; index++) {
                graphics.setColor(new Color(184, 117, 23, 55));
                graphics.drawLine(
                        ThreadLocalRandom.current().nextInt(IMAGE_WIDTH),
                        ThreadLocalRandom.current().nextInt(IMAGE_HEIGHT),
                        ThreadLocalRandom.current().nextInt(IMAGE_WIDTH),
                        ThreadLocalRandom.current().nextInt(IMAGE_HEIGHT));
            }
            for (int index = 0; index < 40; index++) {
                graphics.setColor(new Color(119, 93, 64, 90));
                graphics.fillRect(
                        ThreadLocalRandom.current().nextInt(IMAGE_WIDTH),
                        ThreadLocalRandom.current().nextInt(IMAGE_HEIGHT),
                        1,
                        1);
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
            FontMetrics metrics = graphics.getFontMetrics();
            int characterWidth = IMAGE_WIDTH / (CODE_LENGTH + 1);
            for (int index = 0; index < code.length(); index++) {
                String character = String.valueOf(code.charAt(index));
                int x = characterWidth / 2 + index * characterWidth;
                int y = (IMAGE_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
                double angle = ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
                AffineTransform original = graphics.getTransform();
                graphics.rotate(angle, x + 8, y - 9);
                graphics.setColor(new Color(74, 48, 25));
                graphics.drawString(character, x, y);
                graphics.setTransform(original);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("图形验证码生成失败", exception);
        }
    }

    private String buildCaptchaKey(String captchaId) {
        return CAPTCHA_KEY_PREFIX + captchaId;
    }

    private String buildFailureKey(String subject) {
        return FAILURE_KEY_PREFIX + subject;
    }
}
