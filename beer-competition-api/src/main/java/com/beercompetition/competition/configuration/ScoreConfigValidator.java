package com.beercompetition.competition.configuration;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一维护评分维度类型、配置校验和提交校验，避免各入口口径分叉。
 */
@Component
public class ScoreConfigValidator {

    public static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);

    public static final int FLEXIBLE_MIN_DIMENSIONS = 2;

    public static final int FLEXIBLE_MAX_DIMENSIONS = 8;

    public static final String KIND_SCORE = "SCORE";

    public static final String KIND_TEXT = "TEXT";

    public static final String KIND_CHECK = "CHECK";

    public static final String SELECTION_SINGLE = "SINGLE";

    public static final String SELECTION_MULTI = "MULTI";

    private static final int MAX_OPTION_LENGTH = 50;

    private static final Set<String> DIMENSION_KINDS = Set.of(KIND_SCORE, KIND_TEXT, KIND_CHECK);

    private static final Set<String> SELECTION_MODES = Set.of(SELECTION_SINGLE, SELECTION_MULTI);

    public static boolean isScored(DimensionRequest dimension) {
        if (dimension == null) {
            return false;
        }
        if (dimension.getScored() != null) {
            return dimension.getScored();
        }
        String kind = legacyKind(dimension.getKind());
        return !KIND_TEXT.equals(kind) && !KIND_CHECK.equals(kind);
    }

    public static boolean isCheckbox(DimensionRequest dimension) {
        if (dimension == null) {
            return false;
        }
        if (dimension.getCheckbox() != null) {
            return dimension.getCheckbox();
        }
        return KIND_CHECK.equals(legacyKind(dimension.getKind()));
    }

    public static boolean isNoteEnabled(DimensionRequest dimension) {
        return dimension == null || dimension.getNoteEnabled() == null || dimension.getNoteEnabled();
    }

    public static String kindOf(DimensionRequest dimension) {
        if (dimension == null) {
            return KIND_SCORE;
        }
        if (dimension.getScored() == null && dimension.getCheckbox() == null) {
            return legacyKind(dimension.getKind());
        }
        if (isCheckbox(dimension)) {
            return KIND_CHECK;
        }
        return isScored(dimension) ? KIND_SCORE : KIND_TEXT;
    }

    public BigDecimal configScoreTotal(List<DimensionRequest> dimensions) {
        return (dimensions == null ? List.<DimensionRequest>of() : dimensions).stream()
                .filter(ScoreConfigValidator::isScored)
                .map(dimension -> dimension.getMaxScore() == null ? BigDecimal.ZERO : dimension.getMaxScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal submittedScoreTotal(List<DimensionRequest> dimensions) {
        return (dimensions == null ? List.<DimensionRequest>of() : dimensions).stream()
                .filter(ScoreConfigValidator::isScored)
                .map(dimension -> dimension.getScore() == null ? BigDecimal.ZERO : dimension.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void normalizeAndValidateForm(JudgeRoleType role, List<DimensionRequest> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            throw new BaseException("评分维度不能为空");
        }
        for (DimensionRequest dimension : dimensions) {
            normalizeDimension(role, dimension);
        }
        assertUnique(dimensions.stream().map(DimensionRequest::getKey).toList(), "评分维度 key");
        assertUnique(dimensions.stream().map(DimensionRequest::getLabel).toList(), "评分维度名称");
        if (role == JudgeRoleType.CAPTAIN) {
            if (dimensions.size() != 1) {
                throw new BaseException("桌长评分表只能配置 1 个共识评分维度");
            }
            DimensionRequest captainDimension = dimensions.get(0);
            if (!isScored(captainDimension) || isCheckbox(captainDimension)) {
                throw new BaseException("桌长评分表必须配置评分项");
            }
            if (captainDimension.getMaxScore() == null
                    || captainDimension.getMaxScore().compareTo(SCORE_FORM_TOTAL) != 0) {
                throw new BaseException("桌长评分表总分必须等于 50 分");
            }
            return;
        }
        if (dimensions.size() < FLEXIBLE_MIN_DIMENSIONS || dimensions.size() > FLEXIBLE_MAX_DIMENSIONS) {
            throw new BaseException(scoreRoleLabel(role) + "需配置 "
                    + FLEXIBLE_MIN_DIMENSIONS + "-" + FLEXIBLE_MAX_DIMENSIONS + " 个维度");
        }
        if (dimensions.stream().noneMatch(ScoreConfigValidator::isScored)) {
            throw new BaseException(scoreRoleLabel(role) + "至少需要 1 个评分项");
        }
        if (configScoreTotal(dimensions).compareTo(SCORE_FORM_TOTAL) != 0) {
            throw new BaseException(scoreRoleLabel(role) + "评分项合计必须等于 50 分");
        }
    }

    public boolean isValidForm(String roleName, List<DimensionRequest> dimensions) {
        try {
            normalizeAndValidateForm(JudgeRoleType.valueOf(roleName), dimensions);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public void validateNoteAvailability(int minCommentLength, List<DimensionRequest> dimensions) {
        if (minCommentLength > 0
                && (dimensions == null || dimensions.stream().noneMatch(ScoreConfigValidator::isNoteEnabled))) {
            throw new BaseException("评语字数下限大于 0 时，至少需要 1 个评语输入项");
        }
    }

    /**
     * 按配置校验并规范化评审提交的维度，返回评分项合计。
     */
    public BigDecimal normalizeAndValidateSubmission(List<DimensionRequest> configuredDimensions,
                                                     List<DimensionRequest> submittedDimensions) {
        Map<String, DimensionRequest> configuredByKey = (configuredDimensions == null ? List.<DimensionRequest>of() : configuredDimensions)
                .stream()
                .collect(java.util.stream.Collectors.toMap(DimensionRequest::getKey, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        if (submittedDimensions == null || submittedDimensions.size() != configuredByKey.size()) {
            throw new BaseException("评分维度与当前评分表不一致，请刷新后重试");
        }
        BigDecimal computedTotal = BigDecimal.ZERO;
        Set<String> submittedKeys = new LinkedHashSet<>();
        for (DimensionRequest item : submittedDimensions) {
            DimensionRequest configured = configuredByKey.get(item.getKey());
            if (configured == null || !submittedKeys.add(item.getKey())) {
                throw new BaseException("评分维度与当前评分表不一致，请刷新后重试");
            }
            boolean scored = isScored(configured);
            boolean checkbox = isCheckbox(configured);
            boolean noteEnabled = isNoteEnabled(configured);
            BigDecimal submittedScore = item.getScore();
            List<String> submittedSelectedOptions = item.getSelectedOptions();
            String note = normalizeNullable(item.getNote());
            item.setLabel(configured.getLabel());
            item.setScored(scored);
            item.setCheckbox(checkbox);
            item.setKind(kindOf(configured));
            item.setNoteEnabled(noteEnabled);
            item.setNotePrompt(configured.getNotePrompt());
            item.setNote(noteEnabled ? note : null);
            item.setScore(null);
            item.setMaxScore(null);
            item.setSelectionMode(null);
            item.setRequired(null);
            item.setOptions(null);
            item.setSelectedOptions(null);

            if (scored) {
                BigDecimal maxScore = configured.getMaxScore();
                if (submittedScore == null) {
                    throw new BaseException(configured.getLabel() + "分值不能为空");
                }
                if (!isIntegerScore(submittedScore)) {
                    throw new BaseException(configured.getLabel() + "必须填写整数分");
                }
                if (submittedScore.compareTo(BigDecimal.ZERO) <= 0 || submittedScore.compareTo(maxScore) > 0) {
                    throw new BaseException(configured.getLabel() + "分值必须在 1-"
                            + maxScore.stripTrailingZeros().toPlainString() + " 之间");
                }
                item.setScore(submittedScore);
                item.setMaxScore(maxScore);
                computedTotal = computedTotal.add(submittedScore);
            }

            if (checkbox) {
                String selectionMode = normalizeSelectionMode(configured.getSelectionMode());
                item.setSelectionMode(selectionMode);
                List<String> selected = normalizeSelectedOptions(submittedSelectedOptions, configured);
                if (Boolean.TRUE.equals(configured.getRequired()) && selected.isEmpty()) {
                    throw new BaseException("请选择" + configured.getLabel());
                }
                if (SELECTION_SINGLE.equals(selectionMode) && selected.size() > 1) {
                    throw new BaseException(configured.getLabel() + "只能选择 1 项");
                }
                item.setSelectedOptions(selected);
            } else if (!scored) {
                if (Boolean.TRUE.equals(configured.getRequired()) && !StringUtils.hasText(note)) {
                    throw new BaseException(configured.getLabel() + "不能为空");
                }
            }
        }
        if (!submittedKeys.containsAll(configuredByKey.keySet())) {
            throw new BaseException("评分维度与当前评分表不一致，请刷新后重试");
        }
        return computedTotal;
    }

    private void normalizeDimension(JudgeRoleType role, DimensionRequest dimension) {
        dimension.setKey(requireText(dimension.getKey(), "评分维度 key 不能为空"));
        dimension.setLabel(requireText(dimension.getLabel(), "评分维度名称不能为空"));
        dimension.setNotePrompt(normalizeNullable(dimension.getNotePrompt()));
        boolean scored = isScored(dimension);
        boolean checkbox = isCheckbox(dimension);
        dimension.setScored(scored);
        dimension.setCheckbox(checkbox);
        dimension.setKind(kindOf(dimension));
        dimension.setNoteEnabled(checkbox ? isNoteEnabled(dimension) : true);
        dimension.setScore(null);
        dimension.setSelectedOptions(null);

        if (scored) {
            if (dimension.getMaxScore() == null || dimension.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BaseException(dimension.getLabel() + "满分必须大于 0");
            }
        } else {
            dimension.setMaxScore(null);
        }

        if (checkbox) {
            dimension.setSelectionMode(normalizeSelectionMode(dimension.getSelectionMode()));
            dimension.setOptions(normalizeOptions(dimension.getOptions(), dimension.getLabel()));
            dimension.setRequired(Boolean.TRUE.equals(dimension.getRequired()));
        } else {
            dimension.setSelectionMode(null);
            dimension.setOptions(null);
            dimension.setRequired(scored ? null : Boolean.TRUE.equals(dimension.getRequired()));
        }

        if (role == JudgeRoleType.CAPTAIN && (!scored || checkbox)) {
            throw new BaseException("桌长评分表必须配置评分项");
        }
    }

    private String normalizeSelectionMode(String selectionMode) {
        if (!StringUtils.hasText(selectionMode)) {
            return SELECTION_MULTI;
        }
        String normalized = selectionMode.trim().toUpperCase(Locale.ROOT);
        if (!SELECTION_MODES.contains(normalized)) {
            throw new BaseException("勾选项选择方式不支持：" + selectionMode);
        }
        return normalized;
    }

    private List<String> normalizeOptions(List<String> options, String label) {
        if (options == null || options.isEmpty()) {
            throw new BaseException(label + "至少需要 1 个候选项");
        }
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String option : options) {
            if (!StringUtils.hasText(option)) {
                continue;
            }
            String value = option.trim();
            if (value.length() > MAX_OPTION_LENGTH) {
                throw new BaseException(label + "候选项不能超过 " + MAX_OPTION_LENGTH + " 字");
            }
            if (!seen.add(value)) {
                throw new BaseException(label + "候选项不能重复：" + value);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new BaseException(label + "至少需要 1 个候选项");
        }
        if (normalized.size() > 50) {
            throw new BaseException(label + "候选项不能超过 50 个");
        }
        return normalized;
    }

    private List<String> normalizeSelectedOptions(List<String> selectedOptions, DimensionRequest configured) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = new LinkedHashSet<>(configured.getOptions() == null ? List.of() : configured.getOptions());
        Set<String> selected = new LinkedHashSet<>();
        for (String option : selectedOptions) {
            if (!StringUtils.hasText(option)) {
                continue;
            }
            String value = option.trim();
            if (!allowed.contains(value)) {
                throw new BaseException(configured.getLabel() + "包含无效选项：" + value);
            }
            selected.add(value);
        }
        return (configured.getOptions() == null ? List.<String>of() : configured.getOptions()).stream()
                .filter(selected::contains)
                .toList();
    }

    private void assertUnique(List<String> values, String label) {
        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String value : values) {
            if (!uniqueValues.add(value)) {
                throw new BaseException(label + "不能重复：" + value);
            }
        }
    }

    private boolean isIntegerScore(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }

    private String scoreRoleLabel(JudgeRoleType role) {
        return role == JudgeRoleType.PROFESSIONAL ? "专业评审" : "跨界评审";
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String legacyKind(String kind) {
        if (!StringUtils.hasText(kind)) {
            return KIND_SCORE;
        }
        String normalized = kind.trim().toUpperCase(Locale.ROOT);
        return DIMENSION_KINDS.contains(normalized) ? normalized : KIND_SCORE;
    }
}
