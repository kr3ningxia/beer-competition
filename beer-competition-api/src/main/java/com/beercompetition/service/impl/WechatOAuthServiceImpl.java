package com.beercompetition.service.impl;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.service.WechatOAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class WechatOAuthServiceImpl implements WechatOAuthService {

    private static final String WECHAT_OAUTH_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private final WechatPayProperties wechatPayProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String resolveOpenid(String code) {
        // 1) 测试模式直接返回稳定的模拟用户标识
        if (!wechatPayProperties.isWechatMode()) {
            return "mock-openid";
        }

        // 2) 校验公众号配置并请求微信 OAuth 接口
        String appId = requireConfig(wechatPayProperties.getAppId(), "微信支付 AppID 未配置");
        String appSecret = requireConfig(wechatPayProperties.getAppSecret(), "公众号 AppSecret 未配置");
        String normalizedCode = requireConfig(code, "请先完成微信授权");
        String query = "appid=" + encode(appId)
                + "&secret=" + encode(appSecret)
                + "&code=" + encode(normalizedCode)
                + "&grant_type=authorization_code";
        HttpRequest request = HttpRequest.newBuilder(URI.create(WECHAT_OAUTH_TOKEN_URL + "?" + query))
                .GET()
                .build();

        // 3) 解析 openid 并转换微信错误为用户可理解的提示
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            String openid = root.path("openid").asText(null);
            if (StringUtils.hasText(openid)) {
                return openid;
            }
            throw new BaseException("微信授权失败：" + root.path("errmsg").asText("请重新打开页面后再试"));
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException("微信授权失败，请重新打开页面后再试");
        }
    }

    private String requireConfig(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
