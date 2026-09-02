package com.beercompetition.service.impl;

import com.aliyun.dm20151123.Client;
import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.aliyun.dm20151123.models.SingleSendMailResponse;
import com.aliyun.dm20151123.models.SingleSendMailResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.properties.EmailProperties;
import com.beercompetition.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliyunEmailSender implements EmailSender {

    private static final int VERIFIED_SENDER_ADDRESS_TYPE = 1;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EmailProperties emailProperties;

    @Override
    public String send(String toAddress, String subject, String htmlBody) {
        ensureConfigured();
        validateMessage(toAddress, subject, htmlBody);

        try {
            SingleSendMailRequest request = new SingleSendMailRequest()
                    .setAccountName(emailProperties.getFromAddress())
                    .setAddressType(VERIFIED_SENDER_ADDRESS_TYPE)
                    .setFromAlias(emailProperties.getFromAlias())
                    .setToAddress(toAddress)
                    .setSubject(subject)
                    .setHtmlBody(htmlBody);
            if (StringUtils.hasText(emailProperties.getReplyAddress())) {
                request.setReplyToAddress(true)
                        .setReplyAddress(emailProperties.getReplyAddress());
            } else {
                request.setReplyToAddress(false);
            }

            SingleSendMailResponse response = createClient().singleSendMail(request);
            SingleSendMailResponseBody body = response == null ? null : response.getBody();
            if (body == null || !StringUtils.hasText(body.getRequestId())) {
                log.warn("阿里云邮件发送未返回请求编号 to={}", maskEmail(toAddress));
                throw new BaseException("邮件发送失败，请稍后重试");
            }
            log.info("阿里云邮件发送成功 to={}, requestId={}", maskEmail(toAddress), body.getRequestId());
            return body.getRequestId();
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("阿里云邮件发送异常 to={}", maskEmail(toAddress), ex);
            throw new BaseException("邮件发送失败，请稍后重试");
        }
    }

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(emailProperties.getAccessKeyId())
                .setAccessKeySecret(emailProperties.getAccessKeySecret())
                .setEndpoint(emailProperties.getEndpoint());
        return new Client(config);
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(emailProperties.getAccessKeyId())
                || !StringUtils.hasText(emailProperties.getAccessKeySecret())
                || !StringUtils.hasText(emailProperties.getEndpoint())
                || !StringUtils.hasText(emailProperties.getFromAddress())) {
            throw new BaseException("阿里云邮件配置不完整");
        }
    }

    private void validateMessage(String toAddress, String subject, String htmlBody) {
        if (!StringUtils.hasText(toAddress) || !EMAIL_PATTERN.matcher(toAddress).matches()) {
            throw new BaseException("收件人邮箱格式不正确");
        }
        if (!StringUtils.hasText(subject) || containsLineBreak(subject)) {
            throw new BaseException("邮件主题不能为空且不能包含换行");
        }
        if (!StringUtils.hasText(htmlBody)) {
            throw new BaseException("邮件正文不能为空");
        }
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private String maskEmail(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        if (atIndex <= 0) {
            return "已填写";
        }
        String localPart = email.substring(0, atIndex);
        String visiblePart = localPart.length() <= 2 ? localPart.substring(0, 1) : localPart.substring(0, 2);
        return visiblePart + "***" + email.substring(atIndex);
    }
}
