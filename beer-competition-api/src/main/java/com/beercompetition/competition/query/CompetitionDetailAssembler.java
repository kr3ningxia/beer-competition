package com.beercompetition.competition.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.billing.beercoin.BeerCoinService;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionFeeTierMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionFeeTier;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionAlertVO;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionConfigNameVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.CompetitionPrimaryActionVO;
import com.beercompetition.pojo.vo.CompetitionStageCheckVO;
import com.beercompetition.pojo.vo.EntryFieldConfigVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeAssignmentVO;
import com.beercompetition.pojo.vo.JudgeTableVO;
import com.beercompetition.pojo.vo.ProgressSummaryVO;
import com.beercompetition.pojo.vo.ResultSetupVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.pojo.vo.CompetitionFeeTierVO;
import com.beercompetition.service.impl.competition.CompetitionProgressQueryService;
import com.beercompetition.service.impl.competition.CompetitionWorkspaceQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 集中组装赛事详情和阶段就绪信息，供查询及写操作完成后复用。
 */
@Service
@RequiredArgsConstructor
public class CompetitionDetailAssembler {

    private static final int FLAG_TRUE = 1;

    private static final int DEFAULT_MIN_COMMENT_LENGTH = 0;

    private static final String DEFAULT_DELIVERY_METHOD = "BOTH";

    private static final String DEFAULT_LOGISTICS_VISIBILITY = "PAYMENT_CONFIRMED";

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final JudgeTableMapper judgeTableMapper;

    private final JudgeAccountMapper judgeAccountMapper;

    private final JudgeAssignmentMapper judgeAssignmentMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final CompetitionFeeTierMapper competitionFeeTierMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionProgressQueryService competitionProgressQueryService;

    private final CompetitionWorkspaceQueryService competitionWorkspaceQueryService;

    private final ObjectMapper objectMapper;

    private final CompetitionReadinessEvaluator competitionReadinessEvaluator;

    private final BeerCoinService beerCoinService;

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
    }

    public CompetitionDetailVO buildDetail(Competition competition) {
        return competitionWorkspaceQueryService.enrichFullDetail(buildOverview(competition, true));
    }

    public CompetitionDetailVO buildOverview(Competition competition) {
        return buildOverview(competition, false);
    }

    public CompetitionDetailVO buildOverview(Competition competition, boolean includeStyleSnapshot) {
        Long competitionId = competition.getId();
        List<CompetitionConfigNameVO> categories = listCategories(competitionId);
        List<CompetitionConfigNameVO> styles = includeStyleSnapshot ? listStyles(competitionId) : List.of();
        List<EntryFieldConfigVO> entryFields = listEntryFields(competitionId);
        List<JudgeTableVO> judgeTables = listJudgeTables(competitionId);
        List<ScoreConfigVO> scoreConfigs = listScoreConfigs(competitionId);
        EntrySummaryVO entriesSummary = competitionProgressQueryService.getEntrySummary(competitionId);
        ProgressSummaryVO progressSummary = competitionProgressQueryService.getProgressSummary(competitionId, entriesSummary);
        ResultSetupVO resultSetup = competitionReadinessEvaluator.buildResultSetup(competition);
        List<CompetitionCheckVO> checks = competitionReadinessEvaluator.buildChecks(competition, categories, styles, entryFields, judgeTables, scoreConfigs,
                entriesSummary, resultSetup);
        List<String> dataIntegrityIssues = competitionReadinessEvaluator.buildDataIntegrityIssues(competition, checks);
        List<CompetitionStageCheckVO> stageChecks = competitionReadinessEvaluator.buildStageChecks(competition, checks, dataIntegrityIssues);
        List<CompetitionAlertVO> alerts = competitionReadinessEvaluator.buildAlerts(checks, entriesSummary, dataIntegrityIssues);
        CompetitionPrimaryActionVO primaryAction = competitionReadinessEvaluator.buildPrimaryAction(competition, stageChecks, dataIntegrityIssues);

        return CompetitionDetailVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(competitionReadinessEvaluator.resolveCompetitionType(competition).name())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .earlyBirdFee(competition.getEarlyBirdFee())
                .earlyBirdDeadline(competition.getEarlyBirdDeadline())
                .tierPricingEnabled(Integer.valueOf(1).equals(competition.getTierPricingEnabled()))
                .feeTiers(listFeeTiers(competitionId))
                .refundApprovalMode(competitionReadinessEvaluator.resolveRefundApprovalMode(competition).name())
                .refundPolicyEditable(competitionReadinessEvaluator.isRefundPolicyEditable(competition, LocalDateTime.now()))
                .refundPolicyEditableUntil(competition.getRegistrationDeadline())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .styleLibraryVersion(competition.getStyleLibraryVersion())
                .logistics(toCompetitionLogisticsVO(competition))
                .currentStageLabel(competitionReadinessEvaluator.resolveStageLabel(competitionReadinessEvaluator.parseStatus(competition)))
                .primaryAction(primaryAction)
                .categories(categories)
                .styles(styles)
                .entryFields(entryFields)
                .judgeTables(judgeTables)
                .scoreConfigs(scoreConfigs)
                .checks(checks)
                .stageChecks(stageChecks)
                .editableScopes(competitionReadinessEvaluator.buildEditableScopes(competition))
                .entriesSummary(entriesSummary)
                .entries(List.of())
                .entryPool(List.of())
                .rounds(List.of())
                .currentRound(competitionProgressQueryService.getCurrentRoundSummary(competitionId))
                .resultDrafts(List.of())
                .awardRules(List.of())
                .awardResults(List.of())
                .progressSummary(progressSummary)
                .resultSetup(resultSetup)
                .alerts(alerts)
                .dataIntegrityIssues(dataIntegrityIssues)
                .beerCoinSettlement(beerCoinService.getSettlement(competitionId))
                .build();
    }

    private List<CompetitionFeeTierVO> listFeeTiers(Long competitionId) {
        return competitionFeeTierMapper.selectList(new LambdaQueryWrapper<CompetitionFeeTier>()
                        .eq(CompetitionFeeTier::getCompetitionId, competitionId)
                        .eq(CompetitionFeeTier::getEnabled, FLAG_TRUE)
                        .orderByAsc(CompetitionFeeTier::getStartQuantity))
                .stream()
                .map(item -> CompetitionFeeTierVO.builder().id(item.getId()).startQuantity(item.getStartQuantity())
                        .discountRate(item.getDiscountRate()).sortOrder(item.getSortOrder()).enabled(item.getEnabled()).build())
                .toList();
    }

    private CompetitionLogisticsVO toCompetitionLogisticsVO(Competition competition) {
        return CompetitionLogisticsVO.builder()
                .deliveryMethod(resolveDeliveryMethod(competition.getDeliveryMethod()))
                .sampleArrivalStart(competition.getSampleArrivalStart())
                .sampleArrivalDeadline(competition.getSampleArrivalDeadline())
                .sampleQuantityNote(competition.getSampleQuantityNote())
                .deliveryRecipient(competition.getDeliveryRecipient())
                .deliveryPhone(competition.getDeliveryPhone())
                .deliveryAddress(competition.getDeliveryAddress())
                .deliveryNote(competition.getDeliveryNote())
                .logisticsVisibility(resolveLogisticsVisibility(competition.getLogisticsVisibility()))
                .build();
    }

    private String resolveDeliveryMethod(String deliveryMethod) {
        return StringUtils.hasText(deliveryMethod) ? deliveryMethod : DEFAULT_DELIVERY_METHOD;
    }

    private String resolveLogisticsVisibility(String logisticsVisibility) {
        return StringUtils.hasText(logisticsVisibility) ? logisticsVisibility : DEFAULT_LOGISTICS_VISIBILITY;
    }

    private List<CompetitionConfigNameVO> listCategories(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionCategory::getSortOrder)
                        .orderByAsc(CompetitionCategory::getId))
                .stream()
                .map(item -> CompetitionConfigNameVO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<CompetitionConfigNameVO> listStyles(Long competitionId) {
        return competitionStyleConfigMapper.selectList(new LambdaQueryWrapper<CompetitionStyleConfig>()
                        .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                        .eq(CompetitionStyleConfig::getActiveFlag, FLAG_TRUE)
                        .orderByAsc(CompetitionStyleConfig::getSortOrder)
                        .orderByAsc(CompetitionStyleConfig::getId))
                .stream()
                .map(item -> CompetitionConfigNameVO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .categoryName(item.getCategoryName())
                        .styleCode(item.getStyleCode())
                        .description(item.getDescription())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<EntryFieldConfigVO> listEntryFields(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                        .eq(EntryFieldConfig::getCompetitionId, competitionId)
                        .eq(EntryFieldConfig::getActiveFlag, FLAG_TRUE)
                        .orderByAsc(EntryFieldConfig::getSortOrder)
                        .orderByAsc(EntryFieldConfig::getId))
                .stream()
                .map(item -> EntryFieldConfigVO.builder()
                        .id(item.getId())
                        .fieldKey(item.getFieldKey())
                        .fieldLabel(item.getFieldLabel())
                        .fieldType(item.getFieldType())
                        .helpText(item.getHelpText())
                        .options(readOptions(item.getOptionsJson()))
                        .required(Objects.equals(item.getRequiredFlag(), 1))
                        .visibleToJudges(Objects.equals(item.getVisibleToJudges(), 1))
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<JudgeTableVO> listJudgeTables(Long competitionId) {
        List<JudgeTable> tables = judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .orderByAsc(JudgeTable::getSortOrder)
                .orderByAsc(JudgeTable::getId));
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
        List<ScoreRecord> finalScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competitionId)
                .eq(ScoreRecord::getFinalFlag, 1));

        Map<Long, List<JudgeAssignment>> assignmentsByTable = assignments.stream()
                .collect(Collectors.groupingBy(JudgeAssignment::getTableId));
        Map<Long, Long> finalizedByTable = finalScores.stream()
                .filter(record -> record.getAssignmentId() != null)
                .collect(Collectors.groupingBy(ScoreRecord::getAssignmentId, Collectors.counting()));
        Map<Long, JudgeAssignment> assignmentById = assignments.stream()
                .collect(Collectors.toMap(JudgeAssignment::getId, Function.identity(), (left, right) -> left));
        Set<Long> judgeIds = assignments.stream()
                .map(JudgeAssignment::getJudgeAccountId)
                .collect(Collectors.toSet());
        Map<Long, JudgeAccount> judgeById = judgeIds.isEmpty()
                ? Map.of()
                : judgeAccountMapper.selectBatchIds(judgeIds)
                        .stream()
                        .collect(Collectors.toMap(JudgeAccount::getId, Function.identity()));

        return tables.stream()
                .map(table -> {
                    List<JudgeAssignment> tableAssignments = assignmentsByTable.getOrDefault(table.getId(), List.of());
                    int captain = countRole(tableAssignments, JudgeRoleType.CAPTAIN.name());
                    int professional = countRole(tableAssignments, JudgeRoleType.PROFESSIONAL.name());
                    int cross = countRole(tableAssignments, JudgeRoleType.CROSS.name());
                    int finalized = finalizedByTable.entrySet().stream()
                            .filter(entry -> {
                                JudgeAssignment assignment = assignmentById.get(entry.getKey());
                                return assignment != null && Objects.equals(assignment.getTableId(), table.getId());
                            })
                            .mapToInt(entry -> entry.getValue().intValue())
                            .sum();
                    return JudgeTableVO.builder()
                            .id(table.getId())
                            .tableName(table.getTableName())
                            .captainCount(captain)
                            .professionalCount(professional)
                            .crossCount(cross)
                            .finalized(finalized)
                            .total(0)
                            .assignments(tableAssignments.stream()
                                    .map(assignment -> JudgeAssignmentVO.builder()
                                            .id(assignment.getId())
                                            .tableId(assignment.getTableId())
                                            .judgePublicId(judgeById.get(assignment.getJudgeAccountId()) == null
                                                    ? null
                                                    : judgeById.get(assignment.getJudgeAccountId()).getPublicId())
                                            .judgeName(judgeById.get(assignment.getJudgeAccountId()) == null
                                                    ? null
                                                    : judgeById.get(assignment.getJudgeAccountId()).getName())
                                            .qualification(judgeById.get(assignment.getJudgeAccountId()) == null
                                                    ? null
                                                    : judgeById.get(assignment.getJudgeAccountId()).getQualification())
                                            .role(assignment.getRole())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    private List<ScoreConfigVO> listScoreConfigs(Long competitionId) {
        return competitionScoreConfigMapper.selectList(new LambdaQueryWrapper<CompetitionScoreConfig>()
                        .eq(CompetitionScoreConfig::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionScoreConfig::getId))
                .stream()
                .map(this::toScoreConfigVO)
                .toList();
    }

    private int countRole(List<JudgeAssignment> assignments, String role) {
        return (int) assignments.stream()
                .filter(assignment -> role.equals(assignment.getRole()))
                .count();
    }

    private Integer resolveMinCommentLength(Integer minCommentLength) {
        return minCommentLength == null ? DEFAULT_MIN_COMMENT_LENGTH : minCommentLength;
    }

    private ScoreConfigVO toScoreConfigVO(CompetitionScoreConfig config) {
        return ScoreConfigVO.builder()
                .competitionId(config.getCompetitionId())
                .judgeRoleType(config.getJudgeRoleType())
                .minCommentLength(resolveMinCommentLength(config.getMinCommentLength()))
                .dimensions(readDimensions(config.getDimensionsJson()))
                .build();
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分维度失败");
        }
    }

    private List<String> readOptions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析报名字段选项失败");
        }
    }
}
