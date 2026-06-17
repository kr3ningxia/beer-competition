package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.vo.AdminOperationLogVO;
import com.beercompetition.service.AdminOperationLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String TARGET_ENTRY = "BEER_ENTRY";
    private static final String TARGET_JUDGE = "JUDGE";
    private static final String TARGET_COMPETITION = "COMPETITION";
    private static final String TARGET_ADMIN_USER = "ADMIN_USER";
    private static final String TARGET_SCORE_RECORD = "SCORE_RECORD";
    private static final String RISK_NORMAL = "normal";
    private static final String RISK_WARNING = "warning";
    private static final String RISK_CRITICAL = "critical";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );
    private static final Map<String, ActionMeta> ACTION_META = buildActionMeta();
    private static final Map<String, List<String>> ACTION_GROUPS = buildActionGroups();

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final EntryScanLabelMapper entryScanLabelMapper;
    private final CompetitionMapper competitionMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<AdminOperationLogVO> listAdminOperationLogs(String startTime,
                                                                  String endTime,
                                                                  Long adminUserId,
                                                                  String targetType,
                                                                  String actionGroup,
                                                                  String keyword,
                                                                  Integer page,
                                                                  Integer pageSize) {
        // 1) 参数规范化与前置校验
        int currentPage = Math.max(page == null ? DEFAULT_PAGE : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? DEFAULT_PAGE_SIZE : pageSize, 1), MAX_PAGE_SIZE);
        LocalDateTime normalizedStartTime = parseTime(startTime);
        LocalDateTime normalizedEndTime = parseTime(endTime);
        String normalizedTargetType = normalizeText(targetType);
        String normalizedKeyword = normalizeText(keyword);
        Collection<String> actions = ACTION_GROUPS.get(normalizeText(actionGroup));

        // 2) 查询日志分页数据
        Page<AdminOperationLogVO> result = adminOperationLogMapper.selectAdminOperationLogPage(
                new Page<>(currentPage, currentPageSize),
                normalizedStartTime,
                normalizedEndTime,
                adminUserId,
                normalizedTargetType,
                actions,
                normalizedKeyword);

        // 3) 组装页面展示字段
        TargetLookupContext targetContext = new TargetLookupContext();
        List<AdminOperationLogVO> records = result.getRecords().stream()
                .map(log -> enrichLog(log, targetContext))
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    private AdminOperationLogVO enrichLog(AdminOperationLogVO log, TargetLookupContext targetContext) {
        ActionMeta meta = ACTION_META.getOrDefault(log.getAction(),
                new ActionMeta(log.getAction(), resolveGroupFromAction(log.getAction()), RISK_NORMAL));
        log.setActionLabel(meta.label());
        log.setActionGroup(meta.group());
        log.setRiskLevel(meta.riskLevel());
        log.setTargetLabel(resolveTargetLabel(log.getTargetType()));
        log.setAdminName(normalizeAdminName(log.getAdminName()));
        applySummary(log);
        applyTargetInfo(log, targetContext);
        return log;
    }

    private void applySummary(AdminOperationLogVO log) {
        String summary = normalizeText(log.getSummary());
        if (!StringUtils.hasText(summary)) {
            log.setSummaryText(log.getActionLabel());
            log.setDetailText("");
            return;
        }
        if (!summary.startsWith("{")) {
            log.setSummaryText(summary);
            log.setDetailText("");
            return;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(summary, MAP_TYPE);
            Object action = payload.get("action");
            Object reason = payload.get("reason");
            Object count = payload.get("count");
            Object filters = payload.get("filters");
            Object fromStatus = payload.get("fromStatus");
            Object toStatus = payload.get("toStatus");

            String summaryText = firstText(action, log.getActionLabel());
            if (count != null) {
                summaryText = summaryText + "，数量 " + count;
            }
            List<String> details = new ArrayList<>();
            if (StringUtils.hasText(String.valueOf(reason)) && !"null".equals(String.valueOf(reason))) {
                details.add("原因：" + reason);
            }
            if (fromStatus != null || toStatus != null) {
                details.add("阶段：" + textOrDash(fromStatus) + " -> " + textOrDash(toStatus));
            }
            if (filters != null && StringUtils.hasText(String.valueOf(filters))) {
                details.add("筛选：" + filters);
            }
            log.setSummaryText(summaryText);
            log.setDetailText(String.join("；", details));
        } catch (Exception ex) {
            log.setSummaryText(summary);
            log.setDetailText("");
        }
    }

    private void applyTargetInfo(AdminOperationLogVO log, TargetLookupContext targetContext) {
        String targetType = log.getTargetType();
        String targetId = log.getTargetPublicId();
        if (!StringUtils.hasText(targetType) || !StringUtils.hasText(targetId)) {
            return;
        }
        if (TARGET_ENTRY.equals(targetType)) {
            applyEntryTargetInfo(log, targetId, targetContext);
            return;
        }
        if (TARGET_COMPETITION.equals(targetType)) {
            applyCompetitionTargetInfo(log, targetId, targetContext);
            return;
        }
        if (TARGET_JUDGE.equals(targetType)) {
            applyJudgeTargetInfo(log, targetId, targetContext);
            return;
        }
        if (TARGET_ADMIN_USER.equals(targetType)) {
            log.setTargetName("管理员账号 " + targetId);
            log.setTargetRoute("/admin/admin-users?keyword=" + targetId);
            return;
        }
        if (TARGET_SCORE_RECORD.equals(targetType)) {
            log.setTargetName("评分记录 " + targetId);
        }
    }

    private void applyEntryTargetInfo(AdminOperationLogVO log, String targetId, TargetLookupContext targetContext) {
        BeerEntry entry = targetContext.entryByUuid.computeIfAbsent(targetId, key -> beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, key)
                .last("LIMIT 1")));
        if (entry == null) {
            log.setTargetName(targetId);
            log.setTargetRoute("/admin/entries?keyword=" + targetId);
            return;
        }
        EntryScanLabel label = targetContext.labelByEntryId.computeIfAbsent(entry.getId(), key -> entryScanLabelMapper.selectOne(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getBeerEntryId, key)
                .last("LIMIT 1")));
        String name = firstText(label == null ? null : label.getShortCode(), entry.getName(), entry.getUuid());
        log.setTargetName(name);
        log.setTargetRoute("/admin/entries?entryId=" + entry.getId() + "&entryTab=logs");
    }

    private void applyCompetitionTargetInfo(AdminOperationLogVO log, String targetId, TargetLookupContext targetContext) {
        Long competitionId = parseLong(targetId);
        if (competitionId == null) {
            log.setTargetName(targetId);
            return;
        }
        Competition competition = targetContext.competitionById.computeIfAbsent(competitionId, competitionMapper::selectById);
        if (competition == null) {
            log.setTargetName("比赛 " + targetId);
            log.setTargetRoute("/admin/competitions");
            return;
        }
        log.setTargetName(competition.getName());
        log.setTargetRoute("/admin/competitions/" + competition.getId());
    }

    private void applyJudgeTargetInfo(AdminOperationLogVO log, String targetId, TargetLookupContext targetContext) {
        if (targetId.startsWith("COMP-")) {
            String competitionId = targetId.substring("COMP-".length());
            log.setTargetName("比赛评审编排 " + competitionId);
            log.setTargetRoute("/admin/competitions/" + competitionId);
            return;
        }
        JudgeAccount judge = targetContext.judgeByPublicId.computeIfAbsent(targetId, key -> judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPublicId, key)
                .last("LIMIT 1")));
        if (judge == null) {
            log.setTargetName(targetId);
            log.setTargetRoute("/admin/judges?keyword=" + targetId);
            return;
        }
        log.setTargetName(firstText(judge.getName(), judge.getPublicId()));
        log.setTargetRoute("/admin/judges?keyword=" + judge.getPublicId());
    }

    private LocalDateTime parseTime(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个常见格式。
            }
        }
        return null;
    }

    private String normalizeAdminName(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized) || normalized.matches("\\?+")) {
            return null;
        }
        return normalized;
    }

    private String resolveTargetLabel(String targetType) {
        return switch (String.valueOf(targetType)) {
            case TARGET_ENTRY -> "酒款";
            case TARGET_JUDGE -> "评审";
            case TARGET_COMPETITION -> "比赛";
            case TARGET_ADMIN_USER -> "管理员";
            case TARGET_SCORE_RECORD -> "评分记录";
            default -> StringUtils.hasText(targetType) ? targetType : "-";
        };
    }

    private String resolveGroupFromAction(String action) {
        String normalized = String.valueOf(action);
        if (normalized.startsWith("ENTRY_REFUND")) {
            return "refund";
        }
        if (normalized.contains("BANK_TRANSFER") || normalized.contains("PAYMENT")) {
            return "payment";
        }
        if (normalized.startsWith("ENTRY_")) {
            return "entry";
        }
        if (normalized.startsWith("JUDGE_")) {
            return "judge";
        }
        if (normalized.startsWith("COMPETITION_")) {
            return "competition";
        }
        if (normalized.startsWith("EXPORT_")) {
            return "export";
        }
        if (normalized.startsWith("ADMIN_USER_")) {
            return "account";
        }
        if (normalized.startsWith("SCORE_")) {
            return "score";
        }
        return "other";
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equals(text)) {
                return text;
            }
        }
        return "";
    }

    private String textOrDash(Object value) {
        String text = firstText(value);
        return StringUtils.hasText(text) ? text : "-";
    }

    private static Map<String, ActionMeta> buildActionMeta() {
        Map<String, ActionMeta> items = new LinkedHashMap<>();
        put(items, "ENTRY_UPDATE", "编辑报名信息", "entry", RISK_NORMAL);
        put(items, "ENTRY_CONFIRM_PAYMENT", "确认付款", "payment", RISK_WARNING);
        put(items, "ENTRY_MARK_STORED", "确认入库", "entry", RISK_NORMAL);
        put(items, "ENTRY_UNMARK_STORED", "撤销入库", "entry", RISK_WARNING);
        put(items, "ENTRY_CANCEL", "取消报名", "entry", RISK_WARNING);
        put(items, "ENTRY_BANK_TRANSFER_CONFIRM", "确认转账到账", "payment", RISK_WARNING);
        put(items, "ENTRY_BANK_TRANSFER_REJECT", "驳回转账", "payment", RISK_WARNING);
        put(items, "ENTRY_REFUND_APPROVE", "受理退款", "refund", RISK_CRITICAL);
        put(items, "ENTRY_REFUND_SUCCESS", "退款完成", "refund", RISK_CRITICAL);
        put(items, "ENTRY_REFUND_REJECT", "驳回退款", "refund", RISK_WARNING);
        put(items, "JUDGE_CONTACT_VIEW", "查看评审联系方式", "judge", RISK_WARNING);
        put(items, "JUDGE_PROFILE_UPDATE", "更新评审资料", "judge", RISK_NORMAL);
        put(items, "JUDGE_PHONE_UPDATE", "更正评审手机号", "judge", RISK_WARNING);
        put(items, "JUDGE_APPROVE_OR_ENABLE", "启用评审", "judge", RISK_NORMAL);
        put(items, "JUDGE_DISABLE", "停用评审", "judge", RISK_WARNING);
        put(items, "JUDGE_ASSIGNMENT_UPDATE", "保存评审编排", "judge", RISK_NORMAL);
        put(items, "COMPETITION_OPEN_REGISTRATION", "开放报名", "competition", RISK_WARNING);
        put(items, "COMPETITION_CLOSE_REGISTRATION", "截止报名", "competition", RISK_WARNING);
        put(items, "COMPETITION_PREPARE_JUDGING", "进入评审准备", "competition", RISK_WARNING);
        put(items, "COMPETITION_REOPEN_REGISTRATION", "重新开放报名", "competition", RISK_WARNING);
        put(items, "COMPETITION_RETURN_TO_SAMPLE_CHECK", "退回收样核对", "competition", RISK_WARNING);
        put(items, "EXPORT_ENTRIES", "导出报名数据", "export", RISK_CRITICAL);
        put(items, "EXPORT_DELIVERY", "导出送样数据", "export", RISK_WARNING);
        put(items, "EXPORT_LABELS", "导出标签", "export", RISK_WARNING);
        put(items, "EXPORT_SCORING", "导出评分数据", "export", RISK_CRITICAL);
        put(items, "ADMIN_USER_CREATE", "新增管理员", "account", RISK_WARNING);
        put(items, "ADMIN_USER_UPDATE", "更新管理员", "account", RISK_NORMAL);
        put(items, "ADMIN_USER_ENABLE", "启用管理员", "account", RISK_WARNING);
        put(items, "ADMIN_USER_DISABLE", "停用管理员", "account", RISK_WARNING);
        put(items, "ADMIN_USER_PASSWORD_RESET", "重置管理员密码", "account", RISK_CRITICAL);
        put(items, "ADMIN_USER_PASSWORD_CHANGE", "修改本人密码", "account", RISK_WARNING);
        put(items, "SCORE_COMMENT_UPDATE", "更新评分反馈", "score", RISK_WARNING);
        return items;
    }

    private static void put(Map<String, ActionMeta> items, String action, String label, String group, String riskLevel) {
        items.put(action, new ActionMeta(label, group, riskLevel));
    }

    private static Map<String, List<String>> buildActionGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        ACTION_META.forEach((action, meta) -> groups.computeIfAbsent(meta.group(), key -> new ArrayList<>()).add(action));
        return groups;
    }

    private record ActionMeta(String label, String group, String riskLevel) {
    }

    private static class TargetLookupContext {
        private final Map<String, BeerEntry> entryByUuid = new LinkedHashMap<>();
        private final Map<Long, EntryScanLabel> labelByEntryId = new LinkedHashMap<>();
        private final Map<Long, Competition> competitionById = new LinkedHashMap<>();
        private final Map<String, JudgeAccount> judgeByPublicId = new LinkedHashMap<>();
    }
}
