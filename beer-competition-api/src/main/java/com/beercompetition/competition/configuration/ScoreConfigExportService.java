package com.beercompetition.competition.configuration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.SimpleXlsxBuilder;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 导出赛事当前的全部评分表配置，供主办方快速核对。
 */
@Service
@RequiredArgsConstructor
public class ScoreConfigExportService {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final List<JudgeRoleType> ROLE_ORDER = List.of(
            JudgeRoleType.CROSS, JudgeRoleType.PROFESSIONAL, JudgeRoleType.CAPTAIN);

    private final CompetitionAccessService competitionAccessService;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final ScoreConfigValidator scoreConfigValidator;

    private final ObjectMapper objectMapper;

    public FileDownloadVO export(Long competitionId) {
        competitionAccessService.requireCompetitionAccess(competitionId);
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }

        List<CompetitionCategory> categories = competitionCategoryMapper.selectList(
                new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionCategory::getSortOrder)
                        .orderByAsc(CompetitionCategory::getId));
        Map<Long, CompetitionCategory> categoryById = categories.stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        List<CompetitionScoreConfig> configs = competitionScoreConfigMapper.selectList(
                new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionScoreConfig::getCategoryId)
                        .orderByAsc(CompetitionScoreConfig::getId));
        Map<Long, List<CompetitionScoreConfig>> configsByScope = configs.stream()
                .collect(Collectors.groupingBy(
                        config -> config.getCategoryId() == null ? 0L : config.getCategoryId(),
                        LinkedHashMap::new, Collectors.toList()));

        LinkedHashSet<Long> scopeIds = new LinkedHashSet<>();
        scopeIds.add(0L);
        categories.stream()
                .map(CompetitionCategory::getId)
                .filter(configsByScope::containsKey)
                .forEach(scopeIds::add);
        configsByScope.keySet().stream()
                .filter(categoryId -> categoryId > 0 && !scopeIds.contains(categoryId))
                .sorted()
                .forEach(scopeIds::add);
        Set<Long> specialScopeIds = scopeIds.stream()
                .filter(categoryId -> categoryId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<List<String>> summaryRows = new ArrayList<>();
        summaryRows.add(List.of("评分表", "范围", "适用组别", "评审角色",
                "评分项", "勾选项", "文本项", "评分项合计", "评语字数下限", "状态"));
        List<List<String>> detailRows = new ArrayList<>();
        detailRows.add(List.of("评分表", "范围", "评审角色", "序号", "维度名称",
                "属性", "满分", "选择方式", "必选", "补充评语", "候选项数", "评语提示"));
        List<List<String>> optionRows = new ArrayList<>();
        optionRows.add(List.of("评分表", "范围", "评审角色", "维度序号", "维度名称",
                "属性", "选项序号", "候选项"));

        for (Long scopeId : scopeIds) {
            String scopeName = scopeId == 0 ? "通用评分表" : categoryName(categoryById, scopeId);
            String range = scopeId == 0 ? "通用" : "特殊";
            String applicable = scopeId == 0
                    ? commonApplicableCategories(categories, specialScopeIds)
                    : scopeName;
            List<CompetitionScoreConfig> scopeConfigs = configsByScope.getOrDefault(scopeId, List.of());
            for (JudgeRoleType role : ROLE_ORDER) {
                CompetitionScoreConfig config = scopeConfigs.stream()
                        .filter(item -> role.name().equals(item.getJudgeRoleType()))
                        .findFirst()
                        .orElse(null);
                String roleLabel = roleLabel(role);
                if (config == null) {
                    summaryRows.add(List.of(scopeName, range, applicable, roleLabel,
                            "", "", "", "", "", "缺少评审角色"));
                    continue;
                }
                List<DimensionRequest> dimensions = readDimensions(config.getDimensionsJson());
                long scoredCount = dimensions.stream().filter(ScoreConfigValidator::isScored).count();
                long checkboxCount = dimensions.stream().filter(ScoreConfigValidator::isCheckbox).count();
                long textCount = dimensions.stream()
                        .filter(dimension -> !ScoreConfigValidator.isScored(dimension)
                                && !ScoreConfigValidator.isCheckbox(dimension))
                        .count();
                BigDecimal total = dimensions.stream()
                        .filter(ScoreConfigValidator::isScored)
                        .map(DimensionRequest::getMaxScore)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean valid = scoreConfigValidator.isValidForm(role.name(), dimensions);
                summaryRows.add(List.of(
                        scopeName,
                        range,
                        applicable,
                        roleLabel,
                        String.valueOf(scoredCount),
                        String.valueOf(checkboxCount),
                        String.valueOf(textCount),
                        toPlain(total),
                        String.valueOf(resolveMinCommentLength(config)),
                        valid ? "完成" : "待调整"
                ));

                for (int index = 0; index < dimensions.size(); index++) {
                    DimensionRequest dimension = dimensions.get(index);
                    boolean scored = ScoreConfigValidator.isScored(dimension);
                    boolean checkbox = ScoreConfigValidator.isCheckbox(dimension);
                    boolean noteEnabled = ScoreConfigValidator.isNoteEnabled(dimension);
                    List<String> options = checkbox ? dimension.getOptions() : List.of();
                    if (options == null) {
                        options = List.of();
                    }
                    detailRows.add(List.of(
                            scopeName,
                            range,
                            roleLabel,
                            String.valueOf(index + 1),
                            text(dimension.getLabel()),
                            dimensionTypeLabel(scored, checkbox),
                            scored ? toPlain(dimension.getMaxScore()) : "",
                            checkbox ? selectionModeLabel(dimension.getSelectionMode()) : "",
                            checkbox ? (Boolean.TRUE.equals(dimension.getRequired()) ? "是" : "否") : "",
                            checkbox ? (noteEnabled ? "是" : "否") : "",
                            checkbox ? String.valueOf(options.size()) : "",
                            noteEnabled ? text(dimension.getNotePrompt()) : ""
                    ));
                    if (!checkbox) {
                        continue;
                    }
                    if (options.isEmpty()) {
                        optionRows.add(List.of(scopeName, range, roleLabel,
                                String.valueOf(index + 1), text(dimension.getLabel()),
                                dimensionTypeLabel(scored, true), "", "未配置候选项"));
                        continue;
                    }
                    for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                        optionRows.add(List.of(
                                scopeName,
                                range,
                                roleLabel,
                                String.valueOf(index + 1),
                                text(dimension.getLabel()),
                                dimensionTypeLabel(scored, true),
                                String.valueOf(optionIndex + 1),
                                text(options.get(optionIndex))
                        ));
                    }
                }
            }
        }

        byte[] content = SimpleXlsxBuilder.build(List.of(
                new SimpleXlsxBuilder.Sheet("评分表汇总", summaryRows,
                        List.of(22, 8, 28, 12, 9, 9, 9, 12, 12, 12)),
                new SimpleXlsxBuilder.Sheet("评分表明细", detailRows,
                        List.of(22, 8, 12, 6, 24, 14, 8, 10, 8, 10, 10, 45)),
                new SimpleXlsxBuilder.Sheet("勾选候选项", optionRows,
                        List.of(22, 8, 12, 10, 24, 14, 10, 50))
        ));
        return FileDownloadVO.builder()
                .fileName(safeFilename(competition.getName()) + "-评分表配置.xlsx")
                .contentType(XLSX_CONTENT_TYPE)
                .content(content)
                .build();
    }

    private String commonApplicableCategories(List<CompetitionCategory> categories, Set<Long> specialScopeIds) {
        if (specialScopeIds.isEmpty()) {
            return "全部组别";
        }
        List<String> names = categories.stream()
                .filter(category -> !specialScopeIds.contains(category.getId()))
                .map(category -> text(category.getName()))
                .filter(StringUtils::hasText)
                .toList();
        return names.isEmpty() ? "无（所有组别均已配置特殊评分表）" : String.join("、", names);
    }

    private String categoryName(Map<Long, CompetitionCategory> categoryById, Long categoryId) {
        CompetitionCategory category = categoryById.get(categoryId);
        return category == null ? "组别 " + categoryId : text(category.getName());
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分维度失败");
        }
    }

    private String dimensionTypeLabel(boolean scored, boolean checkbox) {
        if (scored && checkbox) {
            return "评分 + 勾选";
        }
        if (checkbox) {
            return "勾选项";
        }
        return scored ? "评分项" : "文本项";
    }

    private String selectionModeLabel(String selectionMode) {
        return ScoreConfigValidator.SELECTION_SINGLE.equalsIgnoreCase(selectionMode) ? "单选" : "多选";
    }

    private String roleLabel(JudgeRoleType role) {
        return switch (role) {
            case CROSS -> "跨界评审";
            case PROFESSIONAL -> "专业评审";
            case CAPTAIN -> "桌长";
        };
    }

    private int resolveMinCommentLength(CompetitionScoreConfig config) {
        return config.getMinCommentLength() == null ? 0 : config.getMinCommentLength();
    }

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeFilename(String competitionName) {
        String name = StringUtils.hasText(competitionName) ? competitionName.trim() : "比赛";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
