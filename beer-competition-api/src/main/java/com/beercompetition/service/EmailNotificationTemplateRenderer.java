package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.enums.NotificationEventCode;
import com.beercompetition.pojo.po.NotificationTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 负责通知模板的变量校验、默认模板和渲染。动态成绩表由服务端生成 HTML 区块，模板中不开放循环语法。
 */
@Component
public class EmailNotificationTemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_.]*)\\s*}} ".trim());
    private static final Pattern VARIABLE_HREF_PATTERN = Pattern.compile(
            "(?i)(href\\s*=\\s*[\\\"'])\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_.]*)\\s*}}\\s*([\\\"'])");
    private static final Pattern SAFE_VARIABLE_HREF_PATTERN = Pattern.compile(
            "https://template\\.invalid/__email_variable__/([a-zA-Z][a-zA-Z0-9_.]*)");
    private static final Safelist TEMPLATE_SAFELIST = Safelist.none()
            .addTags("div", "span", "h1", "h2", "h3", "p", "strong", "em", "br",
                    "table", "thead", "tbody", "tr", "th", "td", "a", "ul", "ol", "li")
            .addAttributes(":all", "style")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https", "mailto");
    private static final Set<String> COMMON_VARIABLES = Set.of(
            "competition.name", "competition.code", "sample.arrivalStart", "sample.arrivalDeadline",
            "delivery.recipient", "delivery.phone", "delivery.address", "delivery.note",
            "entryCount", "pendingDeliveryCount", "resultUrl", "portalUrl");

    public NotificationTemplate defaultTemplate(NotificationEventCode eventCode) {
        boolean result = eventCode == NotificationEventCode.RESULT_PUBLISHED;
        String subject = result
                ? "{{competition.name}} 结果已公布"
                : "{{competition.name}} 送样提醒｜请在 {{sample.arrivalDeadline}} 前寄送";
        String body = result ? resultBody() : sampleBody(eventCode);
        return NotificationTemplate.builder()
                .eventCode(eventCode.name())
                .name(eventCode.getLabel())
                .version(1)
                .subject(subject)
                .htmlBody(body)
                .status("ACTIVE")
                .build();
    }

    public List<String> availableVariables(NotificationEventCode eventCode) {
        LinkedHashSet<String> variables = new LinkedHashSet<>(COMMON_VARIABLES);
        if (eventCode == NotificationEventCode.RESULT_PUBLISHED) {
            variables.add("resultTable");
        }
        return new ArrayList<>(variables);
    }

    public void validate(NotificationEventCode eventCode, String subject, String htmlBody) {
        if (!StringUtils.hasText(subject) || subject.contains("\r") || subject.contains("\n")) {
            throw new BaseException("邮件主题不能为空且不能包含换行");
        }
        if (!StringUtils.hasText(htmlBody)) {
            throw new BaseException("邮件正文不能为空");
        }
        validateVariables(eventCode, subject);
        validateVariables(eventCode, htmlBody);
    }

    public String renderSubject(NotificationTemplate template, Map<String, String> variables) {
        validateVariables(NotificationEventCode.parse(template.getEventCode()), template.getSubject());
        return replace(template.getSubject(), variables, false);
    }

    public String renderBody(NotificationTemplate template, Map<String, String> variables) {
        NotificationEventCode eventCode = NotificationEventCode.parse(template.getEventCode());
        validateVariables(eventCode, template.getHtmlBody());
        return sanitizeHtml(replace(template.getHtmlBody(), variables, true));
    }

    public String sanitizeHtml(String htmlBody) {
        if (!StringUtils.hasText(htmlBody)) {
            return "";
        }
        String htmlWithSafeVariableLinks = VARIABLE_HREF_PATTERN.matcher(htmlBody)
                .replaceAll("$1https://template.invalid/__email_variable__/$2$3");
        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(htmlWithSafeVariableLinks, "", TEMPLATE_SAFELIST, outputSettings);
        cleaned = SAFE_VARIABLE_HREF_PATTERN.matcher(cleaned).replaceAll("{{$1}}");
        return cleaned
                .replaceAll("(?i)url\\s*\\(\\s*['\"]?\\s*(?:javascript|data):[^)]*\\)", "")
                .replaceAll("(?i)(?:expression|behavior|-moz-binding)\\s*:", "");
    }

    private void validateVariables(NotificationEventCode eventCode, String content) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        Set<String> available = Set.copyOf(availableVariables(eventCode));
        while (matcher.find()) {
            if (!available.contains(matcher.group(1))) {
                throw new BaseException("模板变量不存在：" + matcher.group(1));
            }
        }
    }

    private String replace(String content, Map<String, String> variables, boolean html) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            String replacement = html && "resultTable".equals(key) ? value : (html ? HtmlUtils.htmlEscape(value) : value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String sampleBody(NotificationEventCode eventCode) {
        String timing = switch (eventCode) {
            case SAMPLE_START -> "收样窗口即将开始";
            case SAMPLE_DEADLINE_3D -> "距离收样截止还有3天";
            case SAMPLE_DEADLINE_1D -> "距离收样截止还有1天";
            default -> "请按赛事安排完成送样";
        };
        return "<div style=\"max-width:640px;margin:0 auto;padding:28px;font-family:Arial,'Microsoft YaHei',sans-serif;color:#25343a;line-height:1.8;background:#fffdf7\">"
                + "<div style=\"border-bottom:3px solid #c28a2c;padding-bottom:16px\"><span style=\"font-size:12px;letter-spacing:2px;color:#2f7651\">BEER COMPETITION</span><h1 style=\"margin:8px 0 0;color:#263b31;font-size:25px\">" + timing + "</h1></div>"
                + "<p>您好，{{delivery.recipient}}：</p><p>您报名参加的 <strong>{{competition.name}}</strong> 已进入送样阶段，请按以下信息安排寄送。</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin:20px 0\"><tr><td style=\"padding:10px;border-bottom:1px solid #e8e2d6;color:#68736d\">收样时间</td><td style=\"padding:10px;border-bottom:1px solid #e8e2d6\">{{sample.arrivalStart}} 至 {{sample.arrivalDeadline}}</td></tr>"
                + "<tr><td style=\"padding:10px;border-bottom:1px solid #e8e2d6;color:#68736d\">收样人</td><td style=\"padding:10px;border-bottom:1px solid #e8e2d6\">{{delivery.recipient}} · {{delivery.phone}}</td></tr>"
                + "<tr><td style=\"padding:10px;border-bottom:1px solid #e8e2d6;color:#68736d\">收样地址</td><td style=\"padding:10px;border-bottom:1px solid #e8e2d6\">{{delivery.address}}</td></tr></table>"
                + "<p style=\"padding:14px 16px;background:#eef5ef;color:#2f5e42\">当前待寄送 {{pendingDeliveryCount}} 款，请核对外箱标识和物流信息。</p>"
                + "<p>{{delivery.note}}</p><p style=\"color:#7a827d;font-size:13px\">如已完成寄送，可忽略本提醒。登录赛事平台可查看报名与送样状态。</p>"
                + "<p><a href=\"{{portalUrl}}\" style=\"display:inline-block;padding:11px 18px;color:#fff;background:#2f7651;text-decoration:none\">进入赛事平台</a></p></div>";
    }

    private String resultBody() {
        return "<div style=\"max-width:700px;margin:0 auto;padding:28px;font-family:Arial,'Microsoft YaHei',sans-serif;color:#25343a;line-height:1.8;background:#fffdf7\">"
                + "<div style=\"border-bottom:3px solid #c28a2c;padding-bottom:16px\"><span style=\"font-size:12px;letter-spacing:2px;color:#2f7651\">BEER COMPETITION</span><h1 style=\"margin:8px 0 0;color:#263b31;font-size:25px\">比赛结果已公布</h1></div>"
                + "<p>您好，{{delivery.recipient}}：</p><p><strong>{{competition.name}}</strong> 的结果现已发布，以下为您本场赛事的结果：</p>"
                + "{{resultTable}}<p style=\"color:#7a827d;font-size:13px\">登录赛事平台可查看完整结果、评分反馈和可下载的证书。</p>"
                + "<p><a href=\"{{resultUrl}}\" style=\"display:inline-block;padding:11px 18px;color:#fff;background:#2f7651;text-decoration:none\">查看我的结果</a></p></div>";
    }
}
