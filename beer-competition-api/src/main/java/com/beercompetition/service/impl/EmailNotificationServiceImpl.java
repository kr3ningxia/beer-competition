package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionNotificationRuleMapper;
import com.beercompetition.mapper.EmailDeliveryMapper;
import com.beercompetition.mapper.NotificationTemplateMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.dto.NotificationRuleUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTemplateUpdateRequest;
import com.beercompetition.pojo.dto.NotificationTestSendRequest;
import com.beercompetition.pojo.enums.EmailDeliveryStatus;
import com.beercompetition.pojo.enums.NotificationEventCode;
import com.beercompetition.pojo.enums.NotificationScheduleMode;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionNotificationRule;
import com.beercompetition.pojo.po.EmailDelivery;
import com.beercompetition.pojo.po.NotificationTemplate;
import com.beercompetition.pojo.vo.CompetitionNotificationConfigVO;
import com.beercompetition.pojo.vo.EmailDeliveryVO;
import com.beercompetition.pojo.vo.NotificationRecipientRow;
import com.beercompetition.pojo.vo.NotificationRuleVO;
import com.beercompetition.pojo.vo.NotificationStatsVO;
import com.beercompetition.pojo.vo.NotificationTemplateVO;
import com.beercompetition.properties.EmailNotificationProperties;
import com.beercompetition.service.EmailNotificationService;
import com.beercompetition.service.EmailNotificationTemplateRenderer;
import com.beercompetition.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_SEND_TIME = "10:00";
    private static final int RESULT_VERSION = 1;

    private final CompetitionAccessService competitionAccessService;
    private final CompetitionMapper competitionMapper;
    private final CompetitionNotificationRuleMapper ruleMapper;
    private final NotificationTemplateMapper templateMapper;
    private final EmailDeliveryMapper deliveryMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final AwardResultMapper awardResultMapper;
    private final EmailSender emailSender;
    private final EmailNotificationTemplateRenderer templateRenderer;
    private final EmailNotificationProperties properties;
    private final PiiService piiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionNotificationConfigVO getConfiguration(Long competitionId) {
        Competition competition = requireCompetition(competitionId, true);
        ensureDefaultConfiguration(competition);
        return toConfiguration(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionNotificationConfigVO updateRule(Long competitionId, NotificationRuleUpdateRequest request) {
        Competition competition = requireCompetition(competitionId, true);
        NotificationEventCode eventCode = NotificationEventCode.parse(request.getEventCode());
        ensureDefaultConfiguration(competition);
        CompetitionNotificationRule rule = findRule(competitionId, eventCode);
        applyRuleUpdate(rule, request, eventCode);
        ruleMapper.updateById(rule);
        return toConfiguration(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionNotificationConfigVO updateTemplate(Long competitionId, String eventCodeValue,
                                                          NotificationTemplateUpdateRequest request) {
        Competition competition = requireCompetition(competitionId, true);
        NotificationEventCode eventCode = NotificationEventCode.parse(eventCodeValue);
        templateRenderer.validate(eventCode, request.getSubject(), request.getHtmlBody());
        ensureDefaultConfiguration(competition);
        NotificationTemplate previous = latestTemplate(eventCode);
        if (previous == null) {
            throw new BaseException("通知模板不存在");
        }
        previous.setStatus("HISTORY");
        templateMapper.updateById(previous);
        NotificationTemplate next = NotificationTemplate.builder()
                .eventCode(eventCode.name())
                .name(eventCode.getLabel())
                .version(previous.getVersion() + 1)
                .subject(request.getSubject().trim())
                .htmlBody(templateRenderer.sanitizeHtml(request.getHtmlBody().trim()))
                .status("ACTIVE")
                .createdBy(com.beercompetition.common.context.BaseContext.getCurrentId())
                .publishedTime(LocalDateTime.now())
                .build();
        templateMapper.insert(next);
        CompetitionNotificationRule rule = findRule(competitionId, eventCode);
        rule.setTemplateId(next.getId());
        ruleMapper.updateById(rule);
        return toConfiguration(competition);
    }

    @Override
    public String sendTest(Long competitionId, NotificationTestSendRequest request) {
        Competition competition = requireCompetition(competitionId, true);
        NotificationEventCode eventCode = NotificationEventCode.parse(request.getEventCode());
        ensureDefaultConfiguration(competition);
        NotificationTemplate template = templateFor(competitionId, eventCode);
        Map<String, String> variables = buildVariables(competition, null, eventCode, true);
        String subject = templateRenderer.renderSubject(template, variables);
        String body = templateRenderer.renderBody(template, variables);
        return emailSender.send(request.getEmail().trim().toLowerCase(), "【测试】" + subject, body);
    }

    @Override
    public List<EmailDeliveryVO> listDeliveries(Long competitionId, String status) {
        requireCompetition(competitionId, true);
        LambdaQueryWrapper<EmailDelivery> wrapper = new LambdaQueryWrapper<EmailDelivery>()
                .eq(EmailDelivery::getCompetitionId, competitionId)
                .orderByDesc(EmailDelivery::getId)
                .last("LIMIT 200");
        if (StringUtils.hasText(status)) {
            wrapper.eq(EmailDelivery::getStatus, status);
        }
        return deliveryMapper.selectList(wrapper).stream().map(this::toDeliveryVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryDelivery(Long competitionId, Long deliveryId) {
        EmailDelivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) {
            throw new ResourceNotFoundException("邮件记录不存在");
        }
        if (!Objects.equals(competitionId, delivery.getCompetitionId())) {
            throw new ResourceNotFoundException("邮件记录不存在");
        }
        competitionAccessService.requireCompetitionAccess(competitionId);
        if (!EmailDeliveryStatus.FAILED.name().equals(delivery.getStatus())
                && !EmailDeliveryStatus.SKIPPED.name().equals(delivery.getStatus())) {
            throw new BaseException("当前邮件状态不能重发");
        }
        delivery.setStatus(EmailDeliveryStatus.PENDING.name());
        delivery.setNextRetryTime(LocalDateTime.now());
        delivery.setLastError(null);
        deliveryMapper.updateById(delivery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateScheduledNotifications(LocalDateTime now) {
        if (!properties.isEnabled()) {
            return 0;
        }
        int generated = 0;
        List<Competition> competitions = competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                .ne(Competition::getStatus, "ARCHIVED"));
        for (Competition competition : competitions) {
            ensureDefaultConfiguration(competition);
            for (CompetitionNotificationRule rule : ruleMapper.selectList(new LambdaQueryWrapper<CompetitionNotificationRule>()
                    .eq(CompetitionNotificationRule::getCompetitionId, competition.getId())
                    .eq(CompetitionNotificationRule::getEnabled, 1))) {
                LocalDateTime dueTime = resolveDueTime(competition, rule);
                if (dueTime == null || dueTime.isAfter(now)) {
                    continue;
                }
                generated += enqueueForRule(competition, rule, dueTime);
            }
        }
        return generated;
    }

    @Override
    public int sendPendingEmails(LocalDateTime now) {
        if (!properties.isEnabled()) {
            return 0;
        }
        LocalDateTime staleBefore = now.minusMinutes(10);
        recoverStaleDeliveries(now, staleBefore);
        List<EmailDelivery> candidates = deliveryMapper.selectList(new LambdaQueryWrapper<EmailDelivery>()
                .in(EmailDelivery::getStatus, EmailDeliveryStatus.PENDING.name(), EmailDeliveryStatus.RETRY_WAITING.name())
                .le(EmailDelivery::getScheduledTime, now)
                .and(next -> next.isNull(EmailDelivery::getNextRetryTime)
                        .or().le(EmailDelivery::getNextRetryTime, now))
                .orderByAsc(EmailDelivery::getScheduledTime)
                .last("LIMIT " + Math.max(1, properties.getBatchSize())));
        int sent = 0;
        for (EmailDelivery candidate : candidates) {
            if (!claim(candidate, now)) {
                continue;
            }
            EmailDelivery delivery = deliveryMapper.selectById(candidate.getId());
            try {
                String email = piiService.decrypt(delivery.getRecipientEmailEnc());
                String requestId = emailSender.send(email, delivery.getSubjectSnapshot(), delivery.getHtmlBodySnapshot());
                delivery.setStatus(EmailDeliveryStatus.SENT.name());
                delivery.setProviderRequestId(requestId);
                delivery.setSentTime(now);
                delivery.setNextRetryTime(null);
                delivery.setLastError(null);
                deliveryMapper.updateById(delivery);
                sent++;
            } catch (RuntimeException ex) {
                markSendFailure(delivery, ex, now);
            }
        }
        return sent;
    }

    private void ensureDefaultConfiguration(Competition competition) {
        for (NotificationEventCode eventCode : NotificationEventCode.values()) {
            NotificationTemplate template = latestTemplate(eventCode);
            if (template == null) {
                template = templateRenderer.defaultTemplate(eventCode);
                templateMapper.insert(template);
            }
            CompetitionNotificationRule rule = findRule(competition.getId(), eventCode);
            if (rule == null) {
                ruleMapper.insert(CompetitionNotificationRule.builder()
                        .competitionId(competition.getId())
                        .eventCode(eventCode.name())
                        .enabled(1)
                        .scheduleMode(eventCode == NotificationEventCode.RESULT_PUBLISHED
                                ? NotificationScheduleMode.AFTER_EVENT.name() : NotificationScheduleMode.RELATIVE.name())
                        .scheduledAt(null)
                        .offsetMinutes(eventCode.getDefaultOffsetMinutes())
                        .sendTime(DEFAULT_SEND_TIME)
                        .timezone("Asia/Shanghai")
                        .templateId(template.getId())
                        .build());
            } else if (rule.getTemplateId() == null) {
                rule.setTemplateId(template.getId());
                ruleMapper.updateById(rule);
            }
        }
    }

    private CompetitionNotificationConfigVO toConfiguration(Competition competition) {
        Map<String, NotificationTemplate> latestTemplates = latestTemplates();
        List<NotificationRuleVO> rules = ruleMapper.selectList(new LambdaQueryWrapper<CompetitionNotificationRule>()
                        .eq(CompetitionNotificationRule::getCompetitionId, competition.getId())
                        .orderByAsc(CompetitionNotificationRule::getId))
                .stream()
                .map(rule -> toRuleVO(rule, competition, effectiveTemplate(rule, latestTemplates)))
                .toList();
        List<NotificationTemplateVO> templateVOs = rules.stream()
                .map(NotificationRuleVO::getTemplateId)
                .map(templateMapper::selectById)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(NotificationTemplate::getEventCode))
                .map(this::toTemplateVO)
                .toList();
        return CompetitionNotificationConfigVO.builder()
                .competitionId(competition.getId())
                .competitionName(competition.getName())
                .competitionCode(competition.getCode())
                .sampleArrivalStart(competition.getSampleArrivalStart())
                .sampleArrivalDeadline(competition.getSampleArrivalDeadline())
                .status(competition.getStatus())
                .rules(rules)
                .templates(templateVOs)
                .stats(buildStats(competition.getId()))
                .previewVariables(buildVariables(competition, null, NotificationEventCode.SAMPLE_START, true))
                .build();
    }

    private NotificationRuleVO toRuleVO(CompetitionNotificationRule rule, Competition competition,
                                        NotificationTemplate template) {
        NotificationEventCode eventCode = NotificationEventCode.parse(rule.getEventCode());
        return NotificationRuleVO.builder()
                .id(rule.getId())
                .eventCode(rule.getEventCode())
                .eventLabel(eventCode.getLabel())
                .enabled(Integer.valueOf(1).equals(rule.getEnabled()))
                .scheduleMode(rule.getScheduleMode())
                .scheduledAt(rule.getScheduledAt())
                .offsetMinutes(rule.getOffsetMinutes())
                .sendTime(rule.getSendTime())
                .timezone(rule.getTimezone())
                .templateId(rule.getTemplateId())
                .templateVersion(template == null ? null : template.getVersion())
                .nextTriggerDescription(describeTrigger(resolveDueTime(competition, rule)))
                .build();
    }

    private NotificationTemplateVO toTemplateVO(NotificationTemplate template) {
        NotificationEventCode eventCode = NotificationEventCode.parse(template.getEventCode());
        return NotificationTemplateVO.builder()
                .id(template.getId())
                .eventCode(template.getEventCode())
                .eventLabel(eventCode.getLabel())
                .name(template.getName())
                .version(template.getVersion())
                .subject(template.getSubject())
                .htmlBody(template.getHtmlBody())
                .status(template.getStatus())
                .availableVariables(templateRenderer.availableVariables(eventCode))
                .build();
    }

    private NotificationStatsVO buildStats(Long competitionId) {
        List<NotificationRecipientRow> recipients = portalAccountMapper.selectNotificationRecipients(competitionId);
        Map<String, Integer> counts = new HashMap<>();
        deliveryMapper.selectList(new LambdaQueryWrapper<EmailDelivery>().eq(EmailDelivery::getCompetitionId, competitionId))
                .forEach(item -> counts.merge(item.getStatus(), 1, Integer::sum));
        int withEmail = (int) recipients.stream().filter(item -> StringUtils.hasText(item.getEmailEnc())).count();
        return NotificationStatsVO.builder()
                .recipientCount(recipients.size())
                .emailRecipientCount(withEmail)
                .missingEmailCount(recipients.size() - withEmail)
                .pendingCount(counts.getOrDefault(EmailDeliveryStatus.PENDING.name(), 0))
                .sentCount(counts.getOrDefault(EmailDeliveryStatus.SENT.name(), 0))
                .retryingCount(counts.getOrDefault(EmailDeliveryStatus.RETRY_WAITING.name(), 0))
                .failedCount(counts.getOrDefault(EmailDeliveryStatus.FAILED.name(), 0))
                .skippedCount(counts.getOrDefault(EmailDeliveryStatus.SKIPPED.name(), 0))
                .build();
    }

    private int enqueueForRule(Competition competition, CompetitionNotificationRule rule, LocalDateTime dueTime) {
        NotificationEventCode eventCode = NotificationEventCode.parse(rule.getEventCode());
        NotificationTemplate template = templateFor(competition.getId(), eventCode);
        int count = 0;
        for (NotificationRecipientRow recipient : portalAccountMapper.selectNotificationRecipients(competition.getId())) {
            if (!StringUtils.hasText(recipient.getEmailEnc())) {
                continue;
            }
            if (eventCode != NotificationEventCode.RESULT_PUBLISHED
                    && (recipient.getPendingDeliveryCount() == null || recipient.getPendingDeliveryCount() <= 0)) {
                continue;
            }
            Map<String, String> variables = buildVariables(competition, recipient, eventCode, false);
            EmailDelivery delivery = EmailDelivery.builder()
                    .competitionId(competition.getId())
                    .eventCode(eventCode.name())
                    .recipientAccountId(recipient.getAccountId())
                    .recipientEmailEnc(recipient.getEmailEnc())
                    .recipientEmailHash(recipient.getEmailHash())
                    .resultVersion(RESULT_VERSION)
                    .subjectSnapshot(templateRenderer.renderSubject(template, variables))
                    .htmlBodySnapshot(templateRenderer.renderBody(template, variables))
                    .scheduledTime(dueTime)
                    .status(EmailDeliveryStatus.PENDING.name())
                    .attemptCount(0)
                    .build();
            try {
                deliveryMapper.insert(delivery);
                count++;
            } catch (DuplicateKeyException ignored) {
                // 调度器可能在多实例或重试场景下重复扫描，唯一键保证只生成一封。
            }
        }
        return count;
    }

    private boolean claim(EmailDelivery candidate, LocalDateTime now) {
        UpdateWrapper<EmailDelivery> update = new UpdateWrapper<EmailDelivery>()
                .eq("id", candidate.getId())
                .in("status", EmailDeliveryStatus.PENDING.name(), EmailDeliveryStatus.RETRY_WAITING.name())
                .set("status", EmailDeliveryStatus.SENDING.name())
                .set("next_retry_time", null)
                .setSql("attempt_count = COALESCE(attempt_count, 0) + 1")
                .set("update_time", now);
        return deliveryMapper.update(null, update) == 1;
    }

    private void recoverStaleDeliveries(LocalDateTime now, LocalDateTime staleBefore) {
        int recovered = deliveryMapper.update(null, new UpdateWrapper<EmailDelivery>()
                .eq("status", EmailDeliveryStatus.SENDING.name())
                .le("update_time", staleBefore)
                .set("status", EmailDeliveryStatus.RETRY_WAITING.name())
                .set("next_retry_time", now)
                .set("last_error", "发送任务超时，已安排重试")
                .set("update_time", now));
        if (recovered > 0) {
            log.warn("Recovered stale email deliveries, count={}", recovered);
        }
    }

    private void markSendFailure(EmailDelivery delivery, RuntimeException exception, LocalDateTime now) {
        int attempts = delivery.getAttemptCount() == null ? 1 : delivery.getAttemptCount();
        delivery.setLastError(truncate(exception.getMessage(), 1000));
        if (attempts >= Math.max(1, properties.getMaxAttempts())) {
            delivery.setStatus(EmailDeliveryStatus.FAILED.name());
            delivery.setNextRetryTime(null);
        } else {
            delivery.setStatus(EmailDeliveryStatus.RETRY_WAITING.name());
            delivery.setNextRetryTime(now.plusMinutes(retryDelayMinutes(attempts)));
        }
        deliveryMapper.updateById(delivery);
        log.warn("邮件发送失败 deliveryId={}, attempt={}, status={}", delivery.getId(), attempts, delivery.getStatus());
    }

    private int retryDelayMinutes(int attempt) {
        int base = Math.max(1, properties.getRetryDelayMinutes());
        return Math.min(60, base * (1 << Math.min(4, Math.max(0, attempt - 1))));
    }

    private Map<String, String> buildVariables(Competition competition, NotificationRecipientRow recipient,
                                                NotificationEventCode eventCode, boolean test) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("competition.name", value(competition.getName(), "赛事名称"));
        variables.put("competition.code", value(competition.getCode(), "赛事编号"));
        variables.put("sample.arrivalStart", formatDateTime(competition.getSampleArrivalStart(), "待定"));
        variables.put("sample.arrivalDeadline", formatDateTime(competition.getSampleArrivalDeadline(), "待定"));
        variables.put("delivery.recipient", recipient == null ? "参赛厂商" : value(recipient.getContactName(), recipient.getCompanyName()));
        variables.put("delivery.phone", value(competition.getDeliveryPhone(), "请联系组委会"));
        variables.put("delivery.address", value(competition.getDeliveryAddress(), "请登录平台查看"));
        variables.put("delivery.note", value(competition.getDeliveryNote(), "请按赛事页面说明完成送样"));
        variables.put("entryCount", recipient == null ? "2" : String.valueOf(recipient.getEntryCount()));
        variables.put("pendingDeliveryCount", recipient == null ? "2" : String.valueOf(recipient.getPendingDeliveryCount()));
        String portalUrl = trimTrailingSlash(properties.getPortalBaseUrl()) + "/portal";
        variables.put("portalUrl", portalUrl);
        variables.put("resultUrl", portalUrl + "/results");
        if (eventCode == NotificationEventCode.RESULT_PUBLISHED) {
            variables.put("resultTable", test ? sampleResultTable() : buildResultTable(competition, recipient));
        }
        return variables;
    }

    private String buildResultTable(Competition competition, NotificationRecipientRow recipient) {
        if (recipient == null) {
            return sampleResultTable();
        }
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competition.getId())
                .eq(BeerEntry::getBreweryId, recipient.getBreweryId() == null ? -1L : recipient.getBreweryId())
                .eq(BeerEntry::getDeletedFlag, 0)
                .ne(BeerEntry::getStatus, "CANCELED")
                .orderByAsc(BeerEntry::getId));
        if (entries.isEmpty()) {
            return "<p style=\"padding:14px;background:#f4f1e9\">本场暂无可展示的结果明细。</p>";
        }
        List<Long> entryIds = entries.stream().map(BeerEntry::getId).toList();
        Map<Long, List<AwardResult>> awards = awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                        .in(AwardResult::getBeerEntryId, entryIds)
                        .eq(AwardResult::getStatus, "PUBLISHED")
                        .orderByDesc(AwardResult::getChampionFlag)
                        .orderByAsc(AwardResult::getRankNo))
                .stream().collect(Collectors.groupingBy(AwardResult::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        StringBuilder html = new StringBuilder("<table style=\"width:100%;border-collapse:collapse;margin:20px 0\"><tr style=\"background:#eef5ef\"><th style=\"padding:10px;text-align:left\">酒款</th><th style=\"padding:10px;text-align:left\">结果</th></tr>");
        for (BeerEntry entry : entries) {
            List<AwardResult> entryAwards = awards.getOrDefault(entry.getId(), List.of());
            String result = entryAwards.isEmpty() ? "结果已发布" : entryAwards.stream().map(AwardResult::getAwardName).filter(Objects::nonNull).collect(Collectors.joining("、"));
            html.append("<tr><td style=\"padding:10px;border-bottom:1px solid #e8e2d6\">")
                    .append(HtmlUtils.htmlEscape(value(entry.getName(), "未命名酒款")))
                    .append("</td><td style=\"padding:10px;border-bottom:1px solid #e8e2d6;color:#2f7651;font-weight:700\">")
                    .append(HtmlUtils.htmlEscape(result)).append("</td></tr>");
        }
        return html.append("</table>").toString();
    }

    private String sampleResultTable() {
        return "<table style=\"width:100%;border-collapse:collapse;margin:20px 0\"><tr style=\"background:#eef5ef\"><th style=\"padding:10px;text-align:left\">酒款</th><th style=\"padding:10px;text-align:left\">结果</th></tr><tr><td style=\"padding:10px;border-bottom:1px solid #e8e2d6\">示例酒款</td><td style=\"padding:10px;border-bottom:1px solid #e8e2d6;color:#2f7651;font-weight:700\">金奖</td></tr></table>";
    }

    private LocalDateTime resolveDueTime(Competition competition, CompetitionNotificationRule rule) {
        NotificationEventCode eventCode = NotificationEventCode.parse(rule.getEventCode());
        NotificationScheduleMode mode = parseScheduleMode(rule.getScheduleMode());
        if (eventCode == NotificationEventCode.RESULT_PUBLISHED) {
            if (!"PUBLISHED".equals(competition.getStatus())) {
                return null;
            }
            LocalDateTime publishedAt = competition.getUpdateTime();
            if (mode == NotificationScheduleMode.FIXED && rule.getScheduledAt() != null) {
                return publishedAt == null || publishedAt.isAfter(rule.getScheduledAt()) ? publishedAt : rule.getScheduledAt();
            }
            return publishedAt;
        }
        LocalDateTime base = eventCode == NotificationEventCode.SAMPLE_START
                ? competition.getSampleArrivalStart() : competition.getSampleArrivalDeadline();
        if (base == null) {
            return null;
        }
        if (mode == NotificationScheduleMode.FIXED) {
            return rule.getScheduledAt();
        }
        int offset = rule.getOffsetMinutes() == null ? eventCode.getDefaultOffsetMinutes() : rule.getOffsetMinutes();
        LocalDateTime target = base.plusMinutes(offset);
        LocalTime sendTime = parseTime(rule.getSendTime());
        return sendTime == null ? target : target.toLocalDate().atTime(sendTime);
    }

    private void applyRuleUpdate(CompetitionNotificationRule rule, NotificationRuleUpdateRequest request,
                                 NotificationEventCode eventCode) {
        NotificationScheduleMode mode = parseScheduleMode(request.getScheduleMode());
        if (mode == NotificationScheduleMode.FIXED && request.getScheduledAt() == null) {
            throw new BaseException("固定时间模式必须填写发送时间");
        }
        if (mode == NotificationScheduleMode.RELATIVE && eventCode == NotificationEventCode.RESULT_PUBLISHED) {
            throw new BaseException("结果通知请选择发布后发送或固定时间");
        }
        if (mode != NotificationScheduleMode.RELATIVE && eventCode != NotificationEventCode.RESULT_PUBLISHED) {
            throw new BaseException("收样提醒请选择节点前发送或固定时间");
        }
        if (StringUtils.hasText(request.getSendTime()) && parseTime(request.getSendTime()) == null) {
            throw new BaseException("发送时间格式应为 HH:mm");
        }
        rule.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        rule.setScheduleMode(mode.name());
        rule.setScheduledAt(request.getScheduledAt());
        rule.setOffsetMinutes(request.getOffsetMinutes() == null ? eventCode.getDefaultOffsetMinutes() : request.getOffsetMinutes());
        rule.setSendTime(StringUtils.hasText(request.getSendTime()) ? request.getSendTime() : DEFAULT_SEND_TIME);
        if (request.getTemplateId() != null) {
            NotificationTemplate template = templateMapper.selectById(request.getTemplateId());
            if (template == null || !eventCode.name().equals(template.getEventCode())) {
                throw new BaseException("选择的邮件模板与通知类型不匹配");
            }
            rule.setTemplateId(request.getTemplateId());
        }
    }

    private Competition requireCompetition(Long competitionId, boolean checkAccess) {
        if (checkAccess) {
            competitionAccessService.requireCompetitionAccess(competitionId);
        }
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private CompetitionNotificationRule findRule(Long competitionId, NotificationEventCode eventCode) {
        return ruleMapper.selectOne(new LambdaQueryWrapper<CompetitionNotificationRule>()
                .eq(CompetitionNotificationRule::getCompetitionId, competitionId)
                .eq(CompetitionNotificationRule::getEventCode, eventCode.name())
                .last("LIMIT 1"));
    }

    private NotificationTemplate templateFor(Long competitionId, NotificationEventCode eventCode) {
        CompetitionNotificationRule rule = findRule(competitionId, eventCode);
        NotificationTemplate template = rule == null || rule.getTemplateId() == null ? null : templateMapper.selectById(rule.getTemplateId());
        return template == null ? latestTemplate(eventCode) : template;
    }

    private NotificationTemplate effectiveTemplate(CompetitionNotificationRule rule,
                                                   Map<String, NotificationTemplate> latestTemplates) {
        if (rule.getTemplateId() != null) {
            NotificationTemplate template = templateMapper.selectById(rule.getTemplateId());
            if (template != null) {
                return template;
            }
        }
        return latestTemplates.get(rule.getEventCode());
    }

    private NotificationTemplate latestTemplate(NotificationEventCode eventCode) {
        return templateMapper.selectOne(new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getEventCode, eventCode.name())
                .orderByDesc(NotificationTemplate::getVersion)
                .last("LIMIT 1"));
    }

    private Map<String, NotificationTemplate> latestTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<NotificationTemplate>()
                        .orderByDesc(NotificationTemplate::getVersion))
                .stream().collect(Collectors.toMap(NotificationTemplate::getEventCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String describeTrigger(LocalDateTime time) {
        return time == null ? "等待赛事时间配置" : "预计 " + time.format(DATETIME_FORMATTER);
    }

    private NotificationScheduleMode parseScheduleMode(String value) {
        try {
            return NotificationScheduleMode.valueOf(value);
        } catch (RuntimeException ex) {
            throw new BaseException("发送方式无效");
        }
    }

    private LocalTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private EmailDeliveryVO toDeliveryVO(EmailDelivery delivery) {
        String recipientName = delivery.getRecipientAccountId() == null ? "-" : recipientName(delivery.getRecipientAccountId());
        EmailDeliveryStatus status;
        try {
            status = EmailDeliveryStatus.valueOf(delivery.getStatus());
        } catch (RuntimeException ex) {
            status = EmailDeliveryStatus.FAILED;
        }
        String email;
        try {
            email = piiService.maskEmail(piiService.decrypt(delivery.getRecipientEmailEnc()));
        } catch (RuntimeException ex) {
            email = "邮箱不可读";
        }
        return EmailDeliveryVO.builder()
                .id(delivery.getId())
                .eventCode(delivery.getEventCode())
                .eventLabel(NotificationEventCode.parse(delivery.getEventCode()).getLabel())
                .recipientName(recipientName)
                .recipientEmail(email)
                .subject(delivery.getSubjectSnapshot())
                .status(delivery.getStatus())
                .statusLabel(status.getLabel())
                .attemptCount(delivery.getAttemptCount())
                .scheduledTime(delivery.getScheduledTime())
                .sentTime(delivery.getSentTime())
                .lastError(delivery.getLastError())
                .build();
    }

    private String recipientName(Long accountId) {
        var account = portalAccountMapper.selectById(accountId);
        if (account == null) {
            return "厂商账号";
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        return brewery == null ? value(account.getDisplayName(), "厂商账号") : value(brewery.getCompanyName(), account.getDisplayName());
    }

    private String formatDateTime(LocalDateTime value, String fallback) {
        return value == null ? fallback : value.format(DATETIME_FORMATTER);
    }

    private String value(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "http://localhost:5173";
        }
        return value.replaceAll("/+$", "");
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "邮件供应商未返回错误信息";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
