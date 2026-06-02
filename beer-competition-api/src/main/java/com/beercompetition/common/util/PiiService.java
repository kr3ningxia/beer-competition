package com.beercompetition.common.util;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.properties.PiiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PiiService {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PiiProperties piiProperties;

    public String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\s+", "");
    }

    public String hashPhone(String phone) {
        return hmacSha256(normalizePhone(phone));
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new BaseException("敏感信息加密失败");
        }
    }

    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return "";
        }
        try {
            String[] parts = encryptedText.split(":");
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException("invalid encrypted text");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BaseException("敏感信息解密失败");
        }
    }

    public String maskPhone(String phone) {
        String value = normalizePhone(phone);
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        if (value.length() < 7) {
            return "已填写";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public String maskWechat(String wechat) {
        String value = wechat == null ? "" : wechat.trim();
        if (!StringUtils.hasText(value)) {
            return "未填写微信号";
        }
        if (value.length() <= 2) {
            return "已填写";
        }
        return value.substring(0, 1) + "***" + value.substring(value.length() - 1);
    }

    public String phoneLast4(String phone) {
        String value = normalizePhone(phone);
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private SecretKeySpec aesKey() throws Exception {
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                .digest(requireKey(piiProperties.getAesKey(), "APP_PII_AES_KEY").getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(requireKey(piiProperties.getHashKey(), "APP_PII_HASH_KEY").getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BaseException("敏感信息摘要生成失败");
        }
    }

    private String requireKey(String key, String name) {
        if (!StringUtils.hasText(key) || key.length() < 16) {
            throw new BaseException(name + " 配置不正确");
        }
        return key;
    }
}
