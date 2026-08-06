package com.beercompetition.competition.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.vo.CompetitionAlertVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsVO;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionConfigNameVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryStatsVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.CompetitionPrimaryActionVO;
import com.beercompetition.pojo.vo.CompetitionProgressVO;
import com.beercompetition.pojo.vo.CompetitionQuickSummaryVO;
import com.beercompetition.pojo.vo.CompetitionStageCheckVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.EntryFieldConfigVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalHomeVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.impl.competition.CompetitionProgressQueryService;
import com.beercompetition.service.impl.competition.CompetitionWorkspaceQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.service.impl.CompetitionAnalyticsService;

/**
 * 组装赛事管理端与厂商端只读视图。
 */
@Service
@RequiredArgsConstructor
public class CompetitionQueryServiceImpl implements CompetitionQueryService {

    private static final int FLAG_TRUE = 1;

    private static final int DEFAULT_MIN_COMMENT_LENGTH = 0;

    private static final String DEFAULT_DELIVERY_METHOD = "BOTH";

    private static final String DEFAULT_LOGISTICS_VISIBILITY = "PAYMENT_CONFIRMED";

    private static final String LEGACY_ENTRY_PUBLISHED_STATUS = "PUBLISHED";

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final JudgeTableMapper judgeTableMapper;

    private final JudgeAssignmentMapper judgeAssignmentMapper;

    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionProgressQueryService competitionProgressQueryService;

    private final CompetitionWorkspaceQueryService competitionWorkspaceQueryService;

    private final CompetitionAnalyticsService competitionAnalyticsService;

    private final ObjectMapper objectMapper;

    private final CompetitionDetailAssembler competitionDetailAssembler;

    private final CompetitionReadinessEvaluator competitionReadinessEvaluator;

    @Override
    public List<CompetitionVO> listCompetitions(boolean includeArchived) {
        // 1) 查询比赛主数据，常用列表默认排除归档赛事
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .orderByDesc(Competition::getCompetitionDate)
                .orderByDesc(Competition::getId);
        if (!includeArchived) {
            wrapper.ne(Competition::getStatus, CompetitionStatus.ARCHIVED.name());
        }
        List<Competition> competitions = competitionMapper.selectList(wrapper);
        if (competitions.isEmpty()) {
            return List.of();
        }

        // 2) 批量查询列表所需统计，避免逐场构建完整比赛详情
        List<Long> competitionIds = competitions.stream().map(Competition::getId).toList();
        Map<Long, CompetitionEntryStatsVO> entryStatsByCompetition = beerEntryMapper.selectCompetitionStats(
                        competitionIds,
                        EntryStatus.PENDING_PAYMENT.name(),
                        EntryStatus.CANCELED.name(),
                        EntryStatus.RESULT_PUBLISHED.name(),
                        LEGACY_ENTRY_PUBLISHED_STATUS,
                        FLAG_TRUE)
                .stream()
                .collect(Collectors.toMap(CompetitionEntryStatsVO::getCompetitionId, Function.identity()));
        Map<Long, Long> categoryCountByCompetition = competitionCategoryMapper.selectList(
                        new LambdaQueryWrapper<CompetitionCategory>().in(CompetitionCategory::getCompetitionId, competitionIds))
                .stream()
                .collect(Collectors.groupingBy(CompetitionCategory::getCompetitionId, Collectors.counting()));
        Map<Long, List<JudgeTable>> judgeTablesByCompetition = judgeTableMapper.selectList(
                        new LambdaQueryWrapper<JudgeTable>().in(JudgeTable::getCompetitionId, competitionIds))
                .stream()
                .collect(Collectors.groupingBy(JudgeTable::getCompetitionId));
        Map<Long, Long> judgeCountByCompetition = judgeAssignmentMapper.selectList(
                        new LambdaQueryWrapper<JudgeAssignment>().in(JudgeAssignment::getCompetitionId, competitionIds))
                .stream()
                .collect(Collectors.groupingBy(JudgeAssignment::getCompetitionId, Collectors.counting()));
        Map<Long, List<ScoreConfigVO>> scoreConfigsByCompetition = competitionScoreConfigMapper.selectList(
                        new LambdaQueryWrapper<CompetitionScoreConfig>().in(CompetitionScoreConfig::getCompetitionId, competitionIds))
                .stream()
                .map(this::toScoreConfigVO)
                .collect(Collectors.groupingBy(ScoreConfigVO::getCompetitionId));

        // 3) 按原有业务口径组装列表摘要，保持前端返回契约不变
        return competitions.stream()
                .map(competition -> buildCompetitionListVO(
                        competition,
                        entryStatsByCompetition.get(competition.getId()),
                        categoryCountByCompetition.getOrDefault(competition.getId(), 0L) > 0,
                        judgeTablesByCompetition.getOrDefault(competition.getId(), List.of()).size(),
                        Math.toIntExact(judgeCountByCompetition.getOrDefault(competition.getId(), 0L)),
                        scoreConfigsByCompetition.getOrDefault(competition.getId(), List.of())))
                .toList();
    }

    @Override
    public PortalHomeVO getPortalHome() {
        // 1) 查询厂商端可展示赛事
        List<PortalCompetitionVO> competitions = listPortalCompetitions();
        List<PortalCompetitionVO> openCompetitions = competitions.stream()
                .filter(item -> CompetitionStatus.REGISTRATION_OPEN.name().equals(item.getStatus()))
                .toList();

        // 2) 选择首页主赛事
        PortalCompetitionVO activeCompetition = openCompetitions.stream()
                .findFirst()
                .orElse(competitions.stream().findFirst().orElse(null));

        // 3) 组装首页数据
        return PortalHomeVO.builder()
                .activeCompetition(activeCompetition)
                .openCompetitions(openCompetitions)
                .competitions(competitions)
                .build();
    }

    @Override
    public List<PortalCompetitionVO> listPortalCompetitions() {
        // 1) 查询非草稿、非归档赛事
        return competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                        .ne(Competition::getStatus, CompetitionStatus.DRAFT.name())
                        .ne(Competition::getStatus, CompetitionStatus.ARCHIVED.name()))
                .stream()
                .sorted(Comparator
                        .comparing((Competition item) -> !CompetitionStatus.REGISTRATION_OPEN.name().equals(item.getStatus()))
                        .thenComparing(Competition::getRegistrationDeadline,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Competition::getCompetitionDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Competition::getId, Comparator.reverseOrder()))
                .map(this::toPortalCompetitionVO)
                .toList();
    }

    @Override
    public PortalCompetitionVO getPortalCompetitionDetail(Long id) {
        // 1) 查询赛事并校验公开范围
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = competitionReadinessEvaluator.parseStatus(competition);
        if (status == CompetitionStatus.DRAFT || status == CompetitionStatus.ARCHIVED) {
            throw new ResourceNotFoundException("赛事不存在");
        }

        // 3) 返回厂商端赛事配置
        return toPortalCompetitionVO(competition);
    }

    @Override
    public CompetitionDetailVO getCompetitionDetail(Long id) {
        Competition competition = getCompetitionOrThrow(id);
        return competitionDetailAssembler.buildDetail(competition);
    }

    @Override
    public CompetitionDetailVO getCompetitionOverview(Long id) {
        return competitionDetailAssembler.buildOverview(getCompetitionOrThrow(id));
    }

    @Override
    public CompetitionProgressVO getCompetitionProgress(Long id) {
        Competition competition = getCompetitionOrThrow(id);
        EntrySummaryVO entrySummary = competitionProgressQueryService.getEntrySummary(competition.getId());
        List<CompetitionRoundVO> rounds = competitionWorkspaceQueryService.listRounds(competition.getId());
        return CompetitionProgressVO.builder()
                .progressSummary(competitionProgressQueryService.getProgressSummary(competition.getId(), entrySummary))
                .rounds(rounds)
                .currentRound(rounds.isEmpty() ? null : rounds.get(rounds.size() - 1))
                .build();
    }

    @Override
    public List<CompetitionEntryVO> getCompetitionEntryPool(Long id) {
        Competition competition = getCompetitionOrThrow(id);
        return competitionWorkspaceQueryService.listEntryPool(competition.getId());
    }

    @Override
    public CompetitionQuickSummaryVO getCompetitionQuickSummary(Long id) {
        // 1) 复用轻量概览计算，避免列表抽屉加载酒款、轮次和结果工作区
        CompetitionDetailVO detail = getCompetitionOverview(id);

        // 2) 仅返回列表抽屉需要的进度和提醒
        return CompetitionQuickSummaryVO.builder()
                .progressSummary(detail.getProgressSummary())
                .alerts(detail.getAlerts())
                .dataIntegrityIssues(detail.getDataIntegrityIssues())
                .build();
    }

    @Override
    public CompetitionAnalyticsVO getCompetitionAnalytics(Long competitionId) {
        getCompetitionOrThrow(competitionId);
        return competitionAnalyticsService.buildAnalytics(competitionId);
    }

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
    }

    private CompetitionVO buildCompetitionListVO(Competition competition,
                                                   CompetitionEntryStatsVO entryStats,
                                                   boolean hasCategories,
                                                   int judgeTableCount,
                                                   int judgeCount,
                                                   List<ScoreConfigVO> scoreConfigs) {
        EntrySummaryVO entriesSummary = buildListEntriesSummary(entryStats);
        List<CompetitionCheckVO> checks = competitionReadinessEvaluator.buildListChecks(
                competition, hasCategories, judgeTableCount > 0, scoreConfigs, entriesSummary);
        List<String> dataIntegrityIssues = competitionReadinessEvaluator.buildDataIntegrityIssues(competition, checks);
        List<CompetitionStageCheckVO> stageChecks = competitionReadinessEvaluator.buildStageChecks(competition, checks, dataIntegrityIssues);
        List<CompetitionAlertVO> alerts = competitionReadinessEvaluator.buildAlerts(checks, entriesSummary, dataIntegrityIssues);
        CompetitionPrimaryActionVO primaryAction = competitionReadinessEvaluator.buildPrimaryAction(competition, stageChecks, dataIntegrityIssues);

        return CompetitionVO.builder()
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
                .refundApprovalMode(competitionReadinessEvaluator.resolveRefundApprovalMode(competition).name())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .styleLibraryVersion(competition.getStyleLibraryVersion())
                .logistics(toCompetitionLogisticsVO(competition))
                .currentStageLabel(competitionReadinessEvaluator.resolveStageLabel(competitionReadinessEvaluator.parseStatus(competition)))
                .primaryAction(primaryAction)
                .readyCount(competitionReadinessEvaluator.countDoneChecks(checks))
                .checkTotal(checks.size())
                .alertCount(alerts.size())
                .nextAction(competitionReadinessEvaluator.resolveNextAction(competitionReadinessEvaluator.parseStatus(competition)))
                .dataIntegrityIssues(dataIntegrityIssues)
                .entriesSummary(entriesSummary)
                .progressSummary(null)
                .judgeTableCount(judgeTableCount)
                .judgeCount(judgeCount)
                .build();
    }

    private EntrySummaryVO buildListEntriesSummary(CompetitionEntryStatsVO stats) {
        if (stats == null) {
            return EntrySummaryVO.builder()
                    .total(0)
                    .pendingPayment(0)
                    .registered(0)
                    .stored(0)
                    .canceled(0)
                    .resultPublished(0)
                    .build();
        }
        int activeEntries = toInt(stats.getRegisteredCount());
        return EntrySummaryVO.builder()
                .total(activeEntries)
                .pendingPayment(toInt(stats.getPendingPaymentCount()))
                .registered(activeEntries)
                .stored(toInt(stats.getStoredCount()))
                .canceled(0)
                .resultPublished(toInt(stats.getResultPublishedCount()))
                .build();
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private PortalCompetitionVO toPortalCompetitionVO(Competition competition) {
        return PortalCompetitionVO.builder()
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
                .refundApprovalMode(competitionReadinessEvaluator.resolveRefundApprovalMode(competition).name())
                .description(competition.getDescription())
                .rulesUrl(competition.getRulesUrl())
                .logistics(toPublicCompetitionLogisticsVO(competition))
                .currentStageLabel(competitionReadinessEvaluator.resolveStageLabel(competitionReadinessEvaluator.parseStatus(competition)))
                .categories(listCategories(competition.getId()))
                .styles(listStyles(competition.getId()))
                .entryFields(listEntryFields(competition.getId()))
                .build();
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

    private CompetitionLogisticsVO toPublicCompetitionLogisticsVO(Competition competition) {
        CompetitionLogisticsVO logistics = toCompetitionLogisticsVO(competition);
        if (LogisticsVisibility.PUBLIC.name().equals(logistics.getLogisticsVisibility())) {
            return logistics;
        }
        logistics.setDeliveryRecipient(null);
        logistics.setDeliveryPhone(null);
        logistics.setDeliveryAddress(null);
        return logistics;
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

    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
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
