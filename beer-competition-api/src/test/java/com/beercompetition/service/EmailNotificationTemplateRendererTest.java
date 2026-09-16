package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.enums.NotificationEventCode;
import com.beercompetition.pojo.po.NotificationTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailNotificationTemplateRendererTest {

    private final EmailNotificationTemplateRenderer renderer = new EmailNotificationTemplateRenderer();

    @Test
    void rendersEscapedTextAndKeepsResultTableHtml() {
        NotificationTemplate template = NotificationTemplate.builder()
                .eventCode(NotificationEventCode.RESULT_PUBLISHED.name())
                .subject("{{competition.name}} 结果")
                .htmlBody("<p>{{delivery.recipient}}</p>{{resultTable}}<script>alert(1)</script>")
                .build();

        renderer.validate(NotificationEventCode.RESULT_PUBLISHED, template.getSubject(), template.getHtmlBody());
        String html = renderer.renderBody(template, Map.of(
                "delivery.recipient", "<厂商>",
                "resultTable", "<table><tr><td>金奖</td></tr></table>"));

        assertThat(html).contains("&lt;厂商&gt;");
        assertThat(html).contains("<table>").contains("金奖");
        assertThat(html).doesNotContain("<script>");
    }

    @Test
    void rejectsUnknownVariables() {
        assertThatThrownBy(() -> renderer.validate(NotificationEventCode.SAMPLE_START,
                "{{competition.unknown}}", "<p>内容</p>"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("模板变量不存在");
    }

    @Test
    void removesEventHandlersAndJavascriptLinks() {
        String sanitized = renderer.sanitizeHtml("<a onclick=\"alert(1)\" href=\"javascript:alert(1)\">打开</a>");

        assertThat(sanitized).doesNotContain("onclick");
        assertThat(sanitized).doesNotContain("javascript:");
    }

    @Test
    void keepsOnlyEmailSafeMarkup() {
        String sanitized = renderer.sanitizeHtml("<img src=x onerror=alert(1)><iframe src=javascript:alert(1)></iframe><p>正文</p>");

        assertThat(sanitized).contains("<p>正文</p>");
        assertThat(sanitized).doesNotContain("<img").doesNotContain("<iframe").doesNotContain("onerror");
    }

    @Test
    void preservesValidatedVariableLinks() {
        String sanitized = renderer.sanitizeHtml("<a href=\"{{portalUrl}}\">进入平台</a>");

        assertThat(sanitized).contains("href=\"{{portalUrl}}\"");
    }
}
