package com.beercompetition.service.impl;

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
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.CompetitionBaseInfoUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionCreateRequest;
import com.beercompetition.pojo.dto.ConfigNameBatchUpdateRequest;
import com.beercompetition.pojo.dto.ConfigNameItemRequest;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldItemRequest;
import com.beercompetition.pojo.dto.JudgeTableBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeTableItemRequest;
import com.beercompetition.pojo.dto.ScoreConfigBatchUpdateRequest;
import com.beercompetition.pojo.dto.ScoreConfigItemRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionAlertVO;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionConfigNameVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionPrimaryActionVO;
import com.beercompetition.pojo.vo.CompetitionStageCheckVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.EntryFieldConfigVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeTableVO;
import com.beercompetition.pojo.vo.ProgressSummaryVO;
import com.beercompetition.pojo.vo.ResultSetupVO;
import com.beercompetition.pojo.vo.ScoreConfigVO;
import com.beercompetition.service.CompetitionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private static final BigDecimal SCORE_FORM_TOTAL = BigDecimal.valueOf(50);
    private static final String CHECK_DONE = "done";
    private static final String CHECK_PENDING = "pending";
    private static final String CHECK_LOCKED = "locked";
    private static final String CHECK_INVALID = "invalid";
    private static final String GROUP_CURRENT = "current";
    private static final String GROUP_REQUIRED = "required";
    private static final String GROUP_FUTURE = "future";
    private static final String GROUP_LOCKED = "locked";
    private static final String GROUP_DATA_ISSUE = "data_issue";

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final EntryFieldConfigMapper entryFieldConfigMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<CompetitionVO> listCompetitions() {
        // 1) 查询比赛主数据
        List<Competition> competitions = competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                .orderByDesc(Competition::getCompetitionDate)
                .orderByDesc(Competition::getId));

        // 2) 聚合每场比赛的配置检查与统计摘要
        return competitions.stream()
                .map(competition -> {
                    CompetitionDetailVO detail = buildDetail(competition);
                    return toCompetitionVO(competition, detail);
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO createCompetition(CompetitionCreateRequest request) {
        // 1) 参数规范化与唯一性校验
        String code = normalizeRequired(request.getCode(), "比赛编号不能为空");
        assertCompetitionCodeUnique(code, null);

        // 2) 构造草稿比赛主记录
        Competition competition = Competition.builder()
                .name(normalizeRequired(request.getName(), "比赛名称不能为空"))
                .code(code)
                .edition(normalizeRequired(request.getEdition(), "届次不能为空"))
                .competitionDate(request.getCompetitionDate())
                .registrationStart(request.getRegistrationStart())
                .registrationDeadline(request.getRegistrationDeadline())
                .status(CompetitionStatus.DRAFT.name())
                .entryFee(request.getEntryFee())
                .build();

        // 3) 写入数据库，数据库唯一约束兜底防重
        try {
            competitionMapper.insert(competition);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("比赛编号已存在");
        }

        // 4) 返回列表口径摘要
        Competition saved = competitionMapper.selectById(competition.getId());
        return toCompetitionVO(saved, buildDetail(saved));
    }

    @Override
    public CompetitionDetailVO getCompetitionDetail(Long id) {
        // 1) 查询比赛主数据
        Competition competition = getCompetitionOrThrow(id);

        // 2) 查询并聚合关联配置
        return buildDetail(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateBaseInfo(Long id, CompetitionBaseInfoUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertBaseInfoEditable(competition, request);
        String nextCode = normalizeRequired(request.getCode(), "比赛编号不能为空");
        assertCompetitionCodeUnique(nextCode, id);

        // 2) 应用允许修改的基础字段
        competition.setName(normalizeRequired(request.getName(), "比赛名称不能为空"));
        competition.setCode(nextCode);
        competition.setEdition(normalizeRequired(request.getEdition(), "届次不能为空"));
        competition.setCompetitionDate(request.getCompetitionDate());
        competition.setRegistrationStart(request.getRegistrationStart());
        competition.setRegistrationDeadline(request.getRegistrationDeadline());
        competition.setEntryFee(request.getEntryFee());

        // 3) 更新比赛主记录
        try {
            competitionMapper.updateById(competition);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("比赛编号已存在");
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateCategories(Long id, ConfigNameBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        List<ConfigNameItemRequest> items = normalizeNameItems(request.getItems(), "投递组别");

        // 2) 清理当前比赛旧组别
        competitionCategoryMapper.delete(new LambdaQueryWrapper<CompetitionCategory>()
                .eq(CompetitionCategory::getCompetitionId, id));

        // 3) 批量写入新组别
        int sort = 0;
        for (ConfigNameItemRequest item : items) {
            competitionCategoryMapper.insert(CompetitionCategory.builder()
                    .competitionId(id)
                    .name(item.getName())
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateStyles(Long id, ConfigNameBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        List<ConfigNameItemRequest> items = normalizeNameItems(request.getItems(), "基础风格");

        // 2) 清理当前比赛旧基础风格
        competitionStyleConfigMapper.delete(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, id));

        // 3) 批量写入新基础风格
        int sort = 0;
        for (ConfigNameItemRequest item : items) {
            competitionStyleConfigMapper.insert(CompetitionStyleConfig.builder()
                    .competitionId(id)
                    .name(item.getName())
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateEntryFields(Long id, EntryFieldBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertEntryStructureEditable(competition);
        List<EntryFieldItemRequest> items = normalizeEntryFields(request.getItems());

        // 2) 清理当前比赛旧报名字段
        entryFieldConfigMapper.delete(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, id));

        // 3) 批量写入新报名字段
        int sort = 0;
        for (EntryFieldItemRequest item : items) {
            entryFieldConfigMapper.insert(EntryFieldConfig.builder()
                    .competitionId(id)
                    .fieldKey(item.getFieldKey())
                    .fieldLabel(item.getFieldLabel())
                    .fieldType(item.getFieldType())
                    .requiredFlag(Boolean.TRUE.equals(item.getRequired()) ? 1 : 0)
                    .visibleToJudges(Boolean.TRUE.equals(item.getVisibleToJudges()) ? 1 : 0)
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO updateJudgeTables(Long id, JudgeTableBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(id);
        assertJudgeConfigEditable(competition);
        List<JudgeTableItemRequest> items = normalizeJudgeTables(request.getItems());

        // 2) 清理当前比赛旧评审桌
        judgeAssignmentMapper.delete(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, id));
        judgeTableMapper.delete(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, id));

        // 3) 批量写入新评审桌
        for (JudgeTableItemRequest item : items) {
            judgeTableMapper.insert(JudgeTable.builder()
                    .competitionId(id)
                    .tableName(item.getTableName())
                    .build());
        }

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO openRegistration(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = parseStatus(competition);
        if (status != CompetitionStatus.DRAFT) {
            throw new BaseException("只有草稿状态的比赛可以开放报名");
        }

        // 2) 执行完整性检查
        CompetitionDetailVO detail = buildDetail(competition);
        List<CompetitionCheckVO> blockingChecks = detail.getChecks().stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .toList();
        if (!blockingChecks.isEmpty()) {
            String message = blockingChecks.stream()
                    .map(CompetitionCheckVO::getLabel)
                    .collect(Collectors.joining("、"));
            throw new BaseException("配置未完整，暂不能开放报名：" + message);
        }

        // 3) 更新比赛状态为报名中
        competition.setStatus(CompetitionStatus.REGISTRATION_OPEN.name());
        competitionMapper.updateById(competition);

        // 4) 重新计算配置检查并返回详情
        return getCompetitionDetail(id);
    }

    @Override
    public List<ScoreConfigVO> getScoreConfigs(Long competitionId) {
        // 1) 校验比赛存在
        getCompetitionOrThrow(competitionId);

        // 2) 查询评分表配置
        return listScoreConfigs(competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ScoreConfigVO> updateScoreConfigs(Long competitionId, ScoreConfigBatchUpdateRequest request) {
        // 1) 参数规范化与阶段权限校验
        Competition competition = getCompetitionOrThrow(competitionId);
        assertJudgeConfigEditable(competition);
        validateScoreConfigs(request.getConfigs());

        // 2) 清理当前比赛旧评分表
        competitionScoreConfigMapper.delete(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, competitionId));

        // 3) 批量写入三类评分表
        for (ScoreConfigItemRequest item : request.getConfigs()) {
            CompetitionScoreConfig config = CompetitionScoreConfig.builder()
                    .competitionId(competitionId)
                    .judgeRoleType(item.getJudgeRoleType().name())
                    .dimensionsJson(writeDimensions(item.getDimensions()))
                    .build();
            competitionScoreConfigMapper.insert(config);
        }

        // 4) 返回最新评分表配置
        return listScoreConfigs(competitionId);
    }

    private CompetitionDetailVO buildDetail(Competition competition) {
        Long competitionId = competition.getId();
        List<CompetitionConfigNameVO> categories = listCategories(competitionId);
        List<CompetitionConfigNameVO> styles = listStyles(competitionId);
        List<EntryFieldConfigVO> entryFields = listEntryFields(competitionId);
        List<JudgeTableVO> judgeTables = listJudgeTables(competitionId);
        List<ScoreConfigVO> scoreConfigs = listScoreConfigs(competitionId);
        EntrySummaryVO entriesSummary = buildEntriesSummary(competitionId);
        ProgressSummaryVO progressSummary = buildProgressSummary(competitionId, entriesSummary);
        ResultSetupVO resultSetup = buildResultSetup(competition);
        List<CompetitionCheckVO> checks = buildChecks(competition, categories, styles, entryFields, judgeTables, scoreConfigs,
                entriesSummary, resultSetup);
        List<String> dataIntegrityIssues = buildDataIntegrityIssues(competition, checks);
        List<CompetitionStageCheckVO> stageChecks = buildStageChecks(competition, checks, dataIntegrityIssues);
        List<CompetitionAlertVO> alerts = buildAlerts(checks, entriesSummary, dataIntegrityIssues);
        CompetitionPrimaryActionVO primaryAction = buildPrimaryAction(competition, stageChecks, dataIntegrityIssues);

        return CompetitionDetailVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .edition(competition.getEdition())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .currentStageLabel(resolveStageLabel(parseStatus(competition)))
                .primaryAction(primaryAction)
                .categories(categories)
                .styles(styles)
                .entryFields(entryFields)
                .judgeTables(judgeTables)
                .scoreConfigs(scoreConfigs)
                .checks(checks)
                .stageChecks(stageChecks)
                .editableScopes(buildEditableScopes(competition))
                .entriesSummary(entriesSummary)
                .entries(listEntries(competitionId, categories))
                .progressSummary(progressSummary)
                .resultSetup(resultSetup)
                .alerts(alerts)
                .dataIntegrityIssues(dataIntegrityIssues)
                .build();
    }

    private CompetitionVO toCompetitionVO(Competition competition, CompetitionDetailVO detail) {
        int readyCount = countDoneChecks(detail.getChecks());
        int judgeCount = detail.getJudgeTables().stream()
                .mapToInt(table -> table.getCaptainCount() + table.getProfessionalCount() + table.getCrossCount())
                .sum();
        return CompetitionVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .edition(competition.getEdition())
                .competitionDate(competition.getCompetitionDate())
                .registrationStart(competition.getRegistrationStart())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .currentStageLabel(detail.getCurrentStageLabel())
                .primaryAction(detail.getPrimaryAction())
                .readyCount(readyCount)
                .checkTotal(detail.getChecks().size())
                .alertCount(detail.getAlerts().size())
                .nextAction(resolveNextAction(parseStatus(competition)))
                .dataIntegrityIssues(detail.getDataIntegrityIssues())
                .entriesSummary(detail.getEntriesSummary())
                .progressSummary(detail.getProgressSummary())
                .judgeTableCount(detail.getJudgeTables().size())
                .judgeCount(judgeCount)
                .build();
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
                        .orderByAsc(CompetitionStyleConfig::getSortOrder)
                        .orderByAsc(CompetitionStyleConfig::getId))
                .stream()
                .map(item -> CompetitionConfigNameVO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<EntryFieldConfigVO> listEntryFields(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                        .eq(EntryFieldConfig::getCompetitionId, competitionId)
                        .orderByAsc(EntryFieldConfig::getSortOrder)
                        .orderByAsc(EntryFieldConfig::getId))
                .stream()
                .map(item -> EntryFieldConfigVO.builder()
                        .id(item.getId())
                        .fieldKey(item.getFieldKey())
                        .fieldLabel(item.getFieldLabel())
                        .fieldType(item.getFieldType())
                        .required(Objects.equals(item.getRequiredFlag(), 1))
                        .visibleToJudges(Objects.equals(item.getVisibleToJudges(), 1))
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
    }

    private List<JudgeTableVO> listJudgeTables(Long competitionId) {
        List<JudgeTable> tables = judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
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

    private EntrySummaryVO buildEntriesSummary(Long competitionId) {
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId));
        int total = entries.size();
        int pendingPayment = countEntryStatus(entries, "PENDING_PAYMENT");
        int canceled = countEntryStatus(entries, "CANCELED");
        int stored = countEntryStatus(entries, "STORED");
        int resultPublished = countEntryStatus(entries, "RESULT_PUBLISHED") + countEntryStatus(entries, "PUBLISHED");
        int registered = Math.max(0, total - canceled);
        return EntrySummaryVO.builder()
                .total(total)
                .pendingPayment(pendingPayment)
                .registered(registered)
                .stored(stored)
                .canceled(canceled)
                .resultPublished(resultPublished)
                .build();
    }

    private List<CompetitionEntryVO> listEntries(Long competitionId, List<CompetitionConfigNameVO> categories) {
        Map<Long, String> categoryNameById = categories.stream()
                .collect(Collectors.toMap(CompetitionConfigNameVO::getId, CompetitionConfigNameVO::getName));
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getCompetitionId, competitionId)
                        .orderByDesc(BeerEntry::getCreateTime)
                        .last("LIMIT 20"))
                .stream()
                .map(entry -> CompetitionEntryVO.builder()
                        .id(entry.getId())
                        .uuid(entry.getUuid())
                        .categoryName(categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                        .style(entry.getStyle())
                        .status(entry.getStatus())
                        .stored("STORED".equals(entry.getStatus()) || "RESULT_PUBLISHED".equals(entry.getStatus()))
                        .build())
                .toList();
    }

    private ProgressSummaryVO buildProgressSummary(Long competitionId, EntrySummaryVO entriesSummary) {
        List<ScoreRecord> finalScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competitionId)
                .eq(ScoreRecord::getFinalFlag, 1));
        int advanced = (int) finalScores.stream()
                .filter(record -> Objects.equals(record.getAdvancedFlag(), 1))
                .count();
        return ProgressSummaryVO.builder()
                .finalized(finalScores.size())
                .total(entriesSummary.getRegistered())
                .advanced(advanced)
                .commentWarnings(0)
                .build();
    }

    private ResultSetupVO buildResultSetup(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        boolean published = status == CompetitionStatus.PUBLISHED || status == CompetitionStatus.ARCHIVED;
        boolean ready = status == CompetitionStatus.RESULT_CONFIRMING || published;
        return ResultSetupVO.builder()
                .awardsReady(ready)
                .published(published)
                .championReady(published)
                .build();
    }

    private List<CompetitionCheckVO> buildChecks(Competition competition,
                                                 List<CompetitionConfigNameVO> categories,
                                                 List<CompetitionConfigNameVO> styles,
                                                 List<EntryFieldConfigVO> entryFields,
                                                 List<JudgeTableVO> judgeTables,
                                                 List<ScoreConfigVO> scoreConfigs,
                                                 EntrySummaryVO entriesSummary,
                                                 ResultSetupVO resultSetup) {
        List<CompetitionCheckVO> checks = new ArrayList<>();
        checks.add(check("baseInfo", "基础信息", isBaseInfoReady(competition), "名称、编号、日期、截止时间和报名费需要完整"));
        checks.add(check("categories", "投递组别", !categories.isEmpty(), "至少配置 1 个投递组别"));
        checks.add(check("styles", "基础风格", !styles.isEmpty(), "至少配置 1 个基础风格"));
        checks.add(check("entryFields", "报名字段", !entryFields.isEmpty(), "至少配置 1 个报名字段"));
        checks.add(check("judgeTables", "评审桌", !judgeTables.isEmpty(), "至少配置 1 张评审桌"));
        checks.add(check("scoreForms", "评分表", isScoreFormsReady(scoreConfigs), "跨界、专业、桌长三类评分表都必须为 50 分"));
        checks.add(check("storedEntries", "酒款入库", entriesSummary.getRegistered() > 0
                && entriesSummary.getStored() >= entriesSummary.getRegistered(), "报名酒款需要完成入库确认"));
        checks.add(check("resultSetup", "结果发布", Boolean.TRUE.equals(resultSetup.getPublished()), "奖项确认后才能发布结果"));

        if (!buildEditableScopes(competition).get("entryStructure")) {
            markLocked(checks, Set.of("categories", "styles", "entryFields"), "报名已开放，报名结构已锁定");
        }
        if (!buildEditableScopes(competition).get("judgeConfig")) {
            markLocked(checks, Set.of("judgeTables", "scoreForms"), "评审已开始，评审配置已锁定");
        }
        return checks;
    }

    private CompetitionCheckVO check(String key, String label, boolean ready, String message) {
        return CompetitionCheckVO.builder()
                .key(key)
                .label(label)
                .state(ready ? CHECK_DONE : CHECK_PENDING)
                .message(ready ? "已完成" : message)
                .build();
    }

    private void markLocked(List<CompetitionCheckVO> checks, Set<String> keys, String message) {
        checks.stream()
                .filter(check -> keys.contains(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .forEach(check -> {
                    check.setState(CHECK_LOCKED);
                    check.setMessage(message);
                });
    }

    private List<String> buildDataIntegrityIssues(Competition competition, List<CompetitionCheckVO> checks) {
        if (parseStatus(competition) == CompetitionStatus.DRAFT) {
            return List.of();
        }
        return checks.stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(check -> "比赛已进入" + resolveStageLabel(parseStatus(competition)) + "，但" + check.getLabel() + "未完成")
                .toList();
    }

    private List<CompetitionStageCheckVO> buildStageChecks(Competition competition,
                                                           List<CompetitionCheckVO> checks,
                                                           List<String> dataIntegrityIssues) {
        CompetitionStatus status = parseStatus(competition);
        boolean hasDataIssue = !dataIntegrityIssues.isEmpty();
        return checks.stream()
                .map(check -> CompetitionStageCheckVO.builder()
                        .key(check.getKey())
                        .label(check.getLabel())
                        .state(hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())
                                ? CHECK_INVALID
                                : check.getState())
                        .group(resolveCheckGroup(status, check, hasDataIssue))
                        .message(resolveStageCheckMessage(status, check, hasDataIssue))
                        .targetTab(resolveCheckTargetTab(check.getKey()))
                        .build())
                .toList();
    }

    private List<CompetitionAlertVO> buildAlerts(List<CompetitionCheckVO> checks,
                                                 EntrySummaryVO entriesSummary,
                                                 List<String> dataIntegrityIssues) {
        if (!dataIntegrityIssues.isEmpty()) {
            return List.of(CompetitionAlertVO.builder()
                    .level("danger")
                    .text("当前比赛数据不一致：比赛已开放报名，但缺少开放报名必需配置。请管理员修正数据。")
                    .build());
        }
        List<CompetitionAlertVO> alerts = checks.stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(check -> CompetitionAlertVO.builder()
                        .level(CHECK_LOCKED.equals(check.getState()) ? "warning" : "danger")
                        .text(resolveAlertText(check))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        if (entriesSummary.getPendingPayment() > 0) {
            alerts.add(CompetitionAlertVO.builder()
                    .level("warning")
                    .text("还有 " + entriesSummary.getPendingPayment() + " 款酒等待付款；请进入参赛酒款跟进支付。")
                    .build());
        }
        return alerts;
    }

    private CompetitionPrimaryActionVO buildPrimaryAction(Competition competition,
                                                          List<CompetitionStageCheckVO> stageChecks,
                                                          List<String> dataIntegrityIssues) {
        if (!dataIntegrityIssues.isEmpty()) {
            return CompetitionPrimaryActionVO.builder()
                    .text("查看数据异常")
                    .targetTab("overview")
                    .enabled(true)
                    .build();
        }
        CompetitionStatus status = parseStatus(competition);
        return switch (status) {
            case DRAFT -> {
                boolean canOpen = stageChecks.stream()
                        .filter(check -> GROUP_REQUIRED.equals(check.getGroup()))
                        .allMatch(check -> CHECK_DONE.equals(check.getState()));
                yield CompetitionPrimaryActionVO.builder()
                        .text(canOpen ? "开放报名" : "补齐开放报名配置")
                        .targetTab(canOpen ? "overview" : resolveFirstPendingTarget(stageChecks))
                        .enabled(true)
                        .build();
            }
            case REGISTRATION_OPEN, REGISTRATION_CLOSED -> CompetitionPrimaryActionVO.builder()
                    .text("查看报名酒款")
                    .targetTab("entries")
                    .enabled(true)
                    .build();
            case JUDGING_PREP -> CompetitionPrimaryActionVO.builder()
                    .text("校准评审配置")
                    .targetTab("judges")
                    .enabled(true)
                    .build();
            case JUDGING -> CompetitionPrimaryActionVO.builder()
                    .text("查看现场进度")
                    .targetTab("progress")
                    .enabled(true)
                    .build();
            case RESULT_CONFIRMING -> CompetitionPrimaryActionVO.builder()
                    .text("确认结果发布")
                    .targetTab("results")
                    .enabled(true)
                    .build();
            case PUBLISHED, ARCHIVED -> CompetitionPrimaryActionVO.builder()
                    .text("查看结果与导出")
                    .targetTab("results")
                    .enabled(true)
                    .build();
        };
    }

    private String resolveCheckGroup(CompetitionStatus status, CompetitionCheckVO check, boolean hasDataIssue) {
        if (hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())) {
            return GROUP_DATA_ISSUE;
        }
        if (CHECK_LOCKED.equals(check.getState())) {
            return GROUP_LOCKED;
        }
        if (status == CompetitionStatus.DRAFT && isBlockingCheck(check.getKey())) {
            return GROUP_REQUIRED;
        }
        if ((status == CompetitionStatus.REGISTRATION_OPEN || status == CompetitionStatus.REGISTRATION_CLOSED)
                && Set.of("judgeTables", "scoreForms").contains(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.JUDGING_PREP
                && Set.of("judgeTables", "scoreForms", "storedEntries").contains(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.JUDGING && "storedEntries".equals(check.getKey())) {
            return GROUP_CURRENT;
        }
        if (status == CompetitionStatus.RESULT_CONFIRMING && "resultSetup".equals(check.getKey())) {
            return GROUP_CURRENT;
        }
        return GROUP_FUTURE;
    }

    private String resolveStageCheckMessage(CompetitionStatus status, CompetitionCheckVO check, boolean hasDataIssue) {
        if (hasDataIssue && isBlockingCheck(check.getKey()) && !CHECK_DONE.equals(check.getState())) {
            return "系统数据需修正：" + check.getLabel() + "缺失，请管理员处理。";
        }
        if (CHECK_DONE.equals(check.getState())) {
            return "已完成";
        }
        if (CHECK_LOCKED.equals(check.getState())) {
            return check.getMessage() + "。若需调整，请先关闭报名或由管理员修正数据。";
        }
        if (status == CompetitionStatus.DRAFT && isBlockingCheck(check.getKey())) {
            return check.getMessage() + "，完成后才能开放报名。";
        }
        if ("scoreForms".equals(check.getKey())) {
            return "评分表未完成，会影响评审开始；请进入评分表配置。";
        }
        return check.getMessage();
    }

    private String resolveAlertText(CompetitionCheckVO check) {
        String target = switch (check.getKey()) {
            case "categories", "styles", "entryFields" -> "报名配置";
            case "judgeTables" -> "评审配置";
            case "scoreForms" -> "评分表";
            default -> "概览";
        };
        return check.getLabel() + "未完成，会影响比赛推进；请进入" + target + "处理。";
    }

    private String resolveCheckTargetTab(String key) {
        return switch (key) {
            case "categories", "styles", "entryFields" -> "entryConfig";
            case "judgeTables" -> "judges";
            case "scoreForms" -> "score";
            case "storedEntries" -> "entries";
            case "resultSetup" -> "results";
            default -> "overview";
        };
    }

    private String resolveFirstPendingTarget(List<CompetitionStageCheckVO> stageChecks) {
        return stageChecks.stream()
                .filter(check -> GROUP_REQUIRED.equals(check.getGroup()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .findFirst()
                .map(CompetitionStageCheckVO::getTargetTab)
                .orElse("overview");
    }

    private Map<String, Boolean> buildEditableScopes(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        Map<String, Boolean> scopes = new LinkedHashMap<>();
        boolean draft = status == CompetitionStatus.DRAFT;
        boolean registrationStage = status == CompetitionStatus.REGISTRATION_OPEN || status == CompetitionStatus.REGISTRATION_CLOSED;
        boolean judgePrep = status == CompetitionStatus.JUDGING_PREP;
        boolean judgeConfig = draft || registrationStage || judgePrep;
        scopes.put("baseInfo", draft || registrationStage);
        scopes.put("entryStructure", draft);
        scopes.put("categories", draft);
        scopes.put("styles", draft);
        scopes.put("entryFields", draft);
        scopes.put("judgeConfig", judgeConfig);
        scopes.put("judgeTables", judgeConfig);
        scopes.put("scoreConfigs", judgeConfig);
        scopes.put("resultConfig", status == CompetitionStatus.RESULT_CONFIRMING);
        return scopes;
    }

    private void assertBaseInfoEditable(Competition competition, CompetitionBaseInfoUpdateRequest request) {
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT) {
            return;
        }
        if (status != CompetitionStatus.REGISTRATION_OPEN && status != CompetitionStatus.REGISTRATION_CLOSED) {
            throw new BaseException("当前阶段基础信息已锁定");
        }
        if (!Objects.equals(competition.getName(), request.getName())
                || !Objects.equals(competition.getCode(), request.getCode())
                || !Objects.equals(competition.getEdition(), request.getEdition())
                || !Objects.equals(competition.getRegistrationStart(), request.getRegistrationStart())) {
            throw new BaseException("报名已开放，仅允许修改比赛日期、报名截止时间和报名费");
        }
        if (competition.getEntryFee().compareTo(request.getEntryFee()) != 0 && hasEntries(competition.getId())) {
            throw new BaseException("已有报名酒款，报名费已锁定");
        }
    }

    private void assertEntryStructureEditable(Competition competition) {
        if (parseStatus(competition) != CompetitionStatus.DRAFT) {
            throw new BaseException("报名已开放，报名结构已锁定");
        }
    }

    private void assertJudgeConfigEditable(Competition competition) {
        CompetitionStatus status = parseStatus(competition);
        if (status == CompetitionStatus.DRAFT
                || status == CompetitionStatus.REGISTRATION_OPEN
                || status == CompetitionStatus.REGISTRATION_CLOSED
                || status == CompetitionStatus.JUDGING_PREP) {
            return;
        }
        throw new BaseException("评审已开始，评审桌和评分表已锁定");
    }

    private void validateScoreConfigs(List<ScoreConfigItemRequest> configs) {
        Set<JudgeRoleType> roles = configs.stream()
                .map(ScoreConfigItemRequest::getJudgeRoleType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(JudgeRoleType.class)));
        if (!roles.equals(EnumSet.allOf(JudgeRoleType.class)) || configs.size() != JudgeRoleType.values().length) {
            throw new BaseException("评分表需要同时配置跨界评审、专业评审和桌长");
        }
        for (ScoreConfigItemRequest config : configs) {
            BigDecimal total = config.getDimensions().stream()
                    .map(DimensionRequest::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(SCORE_FORM_TOTAL) != 0) {
                throw new BaseException("评分表总分必须等于 50 分：" + config.getJudgeRoleType().name());
            }
            assertUniqueKeys(config.getDimensions().stream().map(DimensionRequest::getKey).toList(), "评分维度 key");
        }
    }

    private boolean isScoreFormsReady(List<ScoreConfigVO> scoreConfigs) {
        if (scoreConfigs.size() != JudgeRoleType.values().length) {
            return false;
        }
        Set<String> roles = scoreConfigs.stream()
                .map(ScoreConfigVO::getJudgeRoleType)
                .collect(Collectors.toSet());
        if (!roles.containsAll(EnumSet.allOf(JudgeRoleType.class).stream().map(Enum::name).toList())) {
            return false;
        }
        return scoreConfigs.stream().allMatch(config -> getScoreTotal(config.getDimensions()).compareTo(SCORE_FORM_TOTAL) == 0);
    }

    private BigDecimal getScoreTotal(List<DimensionRequest> dimensions) {
        return dimensions.stream()
                .map(DimensionRequest::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ConfigNameItemRequest> normalizeNameItems(List<ConfigNameItemRequest> items, String label) {
        List<ConfigNameItemRequest> normalized = items.stream()
                .peek(item -> item.setName(normalizeRequired(item.getName(), label + "名称不能为空")))
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUniqueKeys(normalized.stream().map(ConfigNameItemRequest::getName).toList(), label + "名称");
        return normalized;
    }

    private List<EntryFieldItemRequest> normalizeEntryFields(List<EntryFieldItemRequest> items) {
        List<EntryFieldItemRequest> normalized = items.stream()
                .peek(item -> {
                    item.setFieldKey(normalizeRequired(item.getFieldKey(), "字段 key 不能为空"));
                    item.setFieldLabel(normalizeRequired(item.getFieldLabel(), "字段名称不能为空"));
                    item.setFieldType(normalizeRequired(item.getFieldType(), "字段类型不能为空"));
                })
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUniqueKeys(normalized.stream().map(EntryFieldItemRequest::getFieldKey).toList(), "报名字段 key");
        return normalized;
    }

    private List<JudgeTableItemRequest> normalizeJudgeTables(List<JudgeTableItemRequest> items) {
        List<JudgeTableItemRequest> normalized = items.stream()
                .peek(item -> item.setTableName(normalizeRequired(item.getTableName(), "评审桌名称不能为空")))
                .toList();
        assertUniqueKeys(normalized.stream().map(JudgeTableItemRequest::getTableName).toList(), "评审桌名称");
        return normalized;
    }

    private void assertUniqueKeys(List<String> values, String label) {
        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String value : values) {
            if (!uniqueValues.add(value)) {
                throw new BaseException(label + "不能重复：" + value);
            }
        }
    }

    private void assertCompetitionCodeUnique(String code, Long currentId) {
        LambdaQueryWrapper<Competition> query = new LambdaQueryWrapper<Competition>()
                .eq(Competition::getCode, code);
        if (currentId != null) {
            query.ne(Competition::getId, currentId);
        }
        if (competitionMapper.selectCount(query) > 0) {
            throw new BaseException("比赛编号已存在");
        }
    }

    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private boolean isBaseInfoReady(Competition competition) {
        return StringUtils.hasText(competition.getName())
                && StringUtils.hasText(competition.getCode())
                && competition.getCompetitionDate() != null
                && competition.getRegistrationDeadline() != null
                && competition.getEntryFee() != null
                && competition.getEntryFee().compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean hasEntries(Long competitionId) {
        return beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)) > 0;
    }

    private boolean isBlockingCheck(String key) {
        return Set.of("baseInfo", "categories", "styles", "entryFields", "judgeTables", "scoreForms").contains(key);
    }

    private int countDoneChecks(List<CompetitionCheckVO> checks) {
        return (int) checks.stream()
                .filter(check -> CHECK_DONE.equals(check.getState()))
                .count();
    }

    private int countRole(List<JudgeAssignment> assignments, String role) {
        return (int) assignments.stream()
                .filter(assignment -> role.equals(assignment.getRole()))
                .count();
    }

    private int countEntryStatus(List<BeerEntry> entries, String status) {
        return (int) entries.stream()
                .filter(entry -> status.equals(entry.getStatus()))
                .count();
    }

    private int resolveSort(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String resolveNextAction(CompetitionStatus status) {
        return switch (status) {
            case DRAFT -> "完善配置并开放报名";
            case REGISTRATION_OPEN -> "查看报名酒款";
            case REGISTRATION_CLOSED -> "检查入库情况";
            case JUDGING_PREP -> "校准评审配置";
            case JUDGING -> "查看现场进度";
            case RESULT_CONFIRMING -> "确认结果发布";
            case PUBLISHED, ARCHIVED -> "查看归档数据";
        };
    }

    private String resolveStageLabel(CompetitionStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case REGISTRATION_OPEN -> "报名中";
            case REGISTRATION_CLOSED -> "报名截止";
            case JUDGING_PREP -> "评审准备";
            case JUDGING -> "评审中";
            case RESULT_CONFIRMING -> "结果确认";
            case PUBLISHED -> "已发布";
            case ARCHIVED -> "已归档";
        };
    }

    private ScoreConfigVO toScoreConfigVO(CompetitionScoreConfig config) {
        return ScoreConfigVO.builder()
                .competitionId(config.getCompetitionId())
                .judgeRoleType(config.getJudgeRoleType())
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

    private String writeDimensions(List<DimensionRequest> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存评分维度失败");
        }
    }
}
