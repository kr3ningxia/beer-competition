package com.beercompetition.competition.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.service.EntryScanLabelService;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.beercompetition.competition.query.CompetitionScoringExportService;
import com.beercompetition.common.util.SimpleXlsxBuilder;

/**
 * 从已持久化的评审数据生成评分归档，不修改赛事和评分状态。
 */
@Service
@RequiredArgsConstructor
public class CompetitionScoringExportServiceImpl implements CompetitionScoringExportService {

    private static final int FLAG_TRUE = 1;

    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CompetitionMapper competitionMapper;

    private final CompetitionAccessService competitionAccessService;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final JudgeAccountMapper judgeAccountMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final BreweryMapper breweryMapper;

    private final AwardResultMapper awardResultMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final RoundResultMapper roundResultMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ObjectMapper objectMapper;

    @Override
    public byte[] exportScoringData(Long competitionId) {
        // 1) 查询比赛、酒款和导出关联数据
        competitionAccessService.requireCompetitionAccess(competitionId);
        getCompetitionOrThrow(competitionId);
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())
                .orderByAsc(BeerEntry::getCategoryId)
                .orderByAsc(BeerEntry::getId));
        Map<Long, CompetitionCategory> categoryById = competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, Function.identity(), (left, right) -> left));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entries.stream().map(BeerEntry::getId).toList());
        Map<Long, List<ScoreRecord>> scoresByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, competitionId)
                        .orderByAsc(ScoreRecord::getBeerEntryId)
                        .orderByAsc(ScoreRecord::getFinalFlag)
                        .orderByAsc(ScoreRecord::getId))
                .stream()
                .collect(Collectors.groupingBy(ScoreRecord::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, JudgeAccount> judgeById = loadJudgeAccounts(scoresByEntry.values().stream()
                .flatMap(List::stream)
                .map(ScoreRecord::getJudgeAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<RoundResult> roundResults = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .orderByAsc(RoundResult::getRoundId)
                .orderByAsc(RoundResult::getRoundTableId)
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
        Map<Long, List<RoundResult>> roundResultsByEntry = roundResults.stream()
                .collect(Collectors.groupingBy(RoundResult::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<AwardResult>> awardsByEntry = awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                        .eq(AwardResult::getCompetitionId, competitionId)
                        .orderByDesc(AwardResult::getChampionFlag)
                        .orderByAsc(AwardResult::getRankNo)
                        .orderByAsc(AwardResult::getId))
                .stream()
                .collect(Collectors.groupingBy(AwardResult::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> exportRoundIds = roundResults.stream()
                .map(RoundResult::getRoundId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        awardsByEntry.values().stream()
                .flatMap(List::stream)
                .map(AwardResult::getSourceRoundId)
                .filter(Objects::nonNull)
                .forEach(exportRoundIds::add);
        Set<Long> exportTableIds = roundResults.stream()
                .map(RoundResult::getRoundTableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        awardsByEntry.values().stream()
                .flatMap(List::stream)
                .map(AwardResult::getSourceRoundTableId)
                .filter(Objects::nonNull)
                .forEach(exportTableIds::add);
        Map<Long, CompetitionRound> roundById = loadRounds(exportRoundIds);
        Map<Long, RoundTable> tableById = loadRoundTables(exportTableIds);
        Map<Long, Brewery> breweryById = loadBreweries(entries.stream()
                .map(BeerEntry::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, List<BeerEntryExtraField>> extraFieldsByEntry = loadExtraFields(entries.stream()
                .map(BeerEntry::getId)
                .collect(Collectors.toSet()));
        Map<Long, BeerEntry> entryById = entries.stream()
                .collect(Collectors.toMap(BeerEntry::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        // 2) 按工作人员实际复盘场景拆分工作表
        List<SimpleXlsxBuilder.Sheet> sheets = List.of(
                new SimpleXlsxBuilder.Sheet("酒款总览", buildEntryOverviewRows(entries, categoryById, labelByEntryId, breweryById,
                        extraFieldsByEntry, scoresByEntry, roundResultsByEntry, roundById, tableById, awardsByEntry)),
                new SimpleXlsxBuilder.Sheet("评审原始评分", buildPersonalScoreRows(entries, categoryById, labelByEntryId, scoresByEntry, judgeById)),
                new SimpleXlsxBuilder.Sheet("评分维度明细", buildScoreDimensionRows(entries, labelByEntryId, scoresByEntry, judgeById)),
                new SimpleXlsxBuilder.Sheet("桌长汇总", buildCaptainSummaryRows(entries, categoryById, labelByEntryId, scoresByEntry)),
                new SimpleXlsxBuilder.Sheet("轮次与奖项", buildRoundAwardRows(roundResults, awardsByEntry, entryById, categoryById,
                        labelByEntryId, roundById, tableById, breweryById))
        );

        // 3) 返回可被 Excel 直接打开的 xlsx 文件
        return SimpleXlsxBuilder.build(sheets);
    }

    private List<List<String>> buildEntryOverviewRows(List<BeerEntry> entries,
                                                       Map<Long, CompetitionCategory> categoryById,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, Brewery> breweryById,
                                                       Map<Long, List<BeerEntryExtraField>> extraFieldsByEntry,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry,
                                                       Map<Long, List<RoundResult>> roundResultsByEntry,
                                                       Map<Long, CompetitionRound> roundById,
                                                       Map<Long, RoundTable> tableById,
                                                       Map<Long, List<AwardResult>> awardsByEntry) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "厂牌", "联系人", "手机号", "微信号", "投递组别", "基础风格", "酒精度",
                "额外报名信息", "报名状态", "入库状态", "发布状态", "原始评分数", "桌长共识分",
                "桌长综合评语", "轮次记录", "奖项结果"));
        for (BeerEntry entry : entries) {
            Brewery brewery = breweryById.get(entry.getBreweryId());
            List<ScoreRecord> scoreRecords = scoresByEntry.getOrDefault(entry.getId(), List.of());
            ScoreRecord finalScore = findFinalScore(scoreRecords);
            rows.add(List.of(
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    brewery == null ? "" : firstText(brewery.getCompanyName(), ""),
                    brewery == null ? "" : firstText(brewery.getContactName(), ""),
                    brewery == null ? "" : firstText(brewery.getPhone(), ""),
                    brewery == null ? "" : firstText(brewery.getWechat(), ""),
                    categoryName(entry, categoryById),
                    firstText(entry.getStyle(), ""),
                    toPlain(entry.getAbv()),
                    formatExtraFields(extraFieldsByEntry.getOrDefault(entry.getId(), List.of())),
                    formatEntryStatus(entry.getStatus()),
                    Objects.equals(entry.getStoredFlag(), FLAG_TRUE) ? "已入库" : "未入库",
                    EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) ? "已发布" : "未发布",
                    String.valueOf(scoreRecords.stream().filter(record -> !Objects.equals(record.getFinalFlag(), FLAG_TRUE)).count()),
                    finalScore == null ? "" : toPlain(firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore())),
                    finalScore == null ? "" : firstText(finalScore.getComments(), ""),
                    formatRoundResults(roundResultsByEntry.getOrDefault(entry.getId(), List.of()), roundById, tableById),
                    formatAwards(awardsByEntry.getOrDefault(entry.getId(), List.of()))
            ));
        }
        return rows;
    }

    private List<List<String>> buildPersonalScoreRows(List<BeerEntry> entries,
                                                      Map<Long, CompetitionCategory> categoryById,
                                                      Map<Long, EntryScanLabel> labelByEntryId,
                                                      Map<Long, List<ScoreRecord>> scoresByEntry,
                                                      Map<Long, JudgeAccount> judgeById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "投递组别", "基础风格", "评审", "评审角色",
                "总分", "各维度评分", "文字反馈", "提交时间"));
        for (BeerEntry entry : entries) {
            for (ScoreRecord record : scoresByEntry.getOrDefault(entry.getId(), List.of())) {
                if (Objects.equals(record.getFinalFlag(), FLAG_TRUE)) {
                    continue;
                }
                JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
                rows.add(List.of(
                        anonymousCode(entry, labelByEntryId),
                        shortCode(entry, labelByEntryId),
                        firstText(entry.getName(), ""),
                        categoryName(entry, categoryById),
                        firstText(entry.getStyle(), ""),
                        judge == null ? "未知评审" : firstText(judge.getName(), "未知评审"),
                        feedbackRoleLabel(record.getJudgeRoleType()),
                        toPlain(record.getTotalScore()),
                        formatScoreDimensions(record.getDimensionsJson()),
                        firstText(record.getComments(), ""),
                        formatDateTime(firstTime(record.getUpdateTime(), record.getCreateTime()))
                ));
            }
        }
        return rows;
    }

    private List<List<String>> buildScoreDimensionRows(List<BeerEntry> entries,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry,
                                                       Map<Long, JudgeAccount> judgeById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "评审", "评审角色", "评分维度", "得分", "满分", "维度备注"));
        for (BeerEntry entry : entries) {
            for (ScoreRecord record : scoresByEntry.getOrDefault(entry.getId(), List.of())) {
                if (Objects.equals(record.getFinalFlag(), FLAG_TRUE)) {
                    continue;
                }
                JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
                String judgeName = judge == null ? "未知评审" : firstText(judge.getName(), "未知评审");
                for (DimensionRequest dimension : readDimensions(record.getDimensionsJson())) {
                    rows.add(List.of(
                            anonymousCode(entry, labelByEntryId),
                            shortCode(entry, labelByEntryId),
                            firstText(entry.getName(), ""),
                            judgeName,
                            feedbackRoleLabel(record.getJudgeRoleType()),
                            firstText(dimension.getLabel(), ""),
                            toPlain(dimension.getScore()),
                            toPlain(dimension.getMaxScore()),
                            firstText(dimension.getNote(), "")
                    ));
                }
            }
        }
        return rows;
    }

    private List<List<String>> buildCaptainSummaryRows(List<BeerEntry> entries,
                                                       Map<Long, CompetitionCategory> categoryById,
                                                       Map<Long, EntryScanLabel> labelByEntryId,
                                                       Map<Long, List<ScoreRecord>> scoresByEntry) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("匿名编号", "短编号", "酒款名称", "投递组别", "基础风格", "桌长共识分", "是否晋级",
                "桌长综合评语", "提交时间"));
        for (BeerEntry entry : entries) {
            ScoreRecord finalScore = findFinalScore(scoresByEntry.getOrDefault(entry.getId(), List.of()));
            rows.add(List.of(
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    categoryName(entry, categoryById),
                    firstText(entry.getStyle(), ""),
                    finalScore == null ? "" : toPlain(firstScore(finalScore.getConsensusScore(), finalScore.getTotalScore())),
                    finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE) ? "是" : "否",
                    finalScore == null ? "" : firstText(finalScore.getComments(), ""),
                    finalScore == null ? "" : formatDateTime(firstTime(finalScore.getUpdateTime(), finalScore.getCreateTime()))
            ));
        }
        return rows;
    }

    private List<List<String>> buildRoundAwardRows(List<RoundResult> roundResults,
                                                   Map<Long, List<AwardResult>> awardsByEntry,
                                                   Map<Long, BeerEntry> entryById,
                                                   Map<Long, CompetitionCategory> categoryById,
                                                   Map<Long, EntryScanLabel> labelByEntryId,
                                                   Map<Long, CompetitionRound> roundById,
                                                   Map<Long, RoundTable> tableById,
                                                   Map<Long, Brewery> breweryById) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("记录类型", "匿名编号", "短编号", "酒款名称", "厂牌", "联系人", "手机号", "微信号", "投递组别", "轮次", "评审桌",
                "结果口径", "排名", "锁定状态", "奖项名称", "奖项状态", "发布时间"));
        for (RoundResult result : roundResults) {
            BeerEntry entry = entryById.get(result.getBeerEntryId());
            if (entry == null) {
                continue;
            }
            CompetitionRound round = roundById.get(result.getRoundId());
            RoundTable table = tableById.get(result.getRoundTableId());
            Brewery brewery = breweryById.get(entry.getBreweryId());
            rows.add(List.of(
                    "轮次记录",
                    anonymousCode(entry, labelByEntryId),
                    shortCode(entry, labelByEntryId),
                    firstText(entry.getName(), ""),
                    brewery == null ? "" : firstText(brewery.getCompanyName(), ""),
                    brewery == null ? "" : firstText(brewery.getContactName(), ""),
                    brewery == null ? "" : firstText(brewery.getPhone(), ""),
                    brewery == null ? "" : firstText(brewery.getWechat(), ""),
                    categoryName(entry, categoryById),
                    round == null ? "" : firstText(round.getRoundName(), ""),
                    table == null ? "" : firstText(table.getTableName(), ""),
                    formatRoundResultType(result.getResultType(), result.getSlotLabel()),
                    result.getRankNo() == null ? "" : String.valueOf(result.getRankNo()),
                    Objects.equals(result.getLockedFlag(), FLAG_TRUE) ? "已锁定" : "待确认",
                    "",
                    "",
                    ""
            ));
        }
        for (List<AwardResult> awards : awardsByEntry.values()) {
            for (AwardResult award : awards) {
                BeerEntry entry = entryById.get(award.getBeerEntryId());
                if (entry == null) {
                    continue;
                }
                CompetitionRound round = roundById.get(award.getSourceRoundId());
                RoundTable table = tableById.get(award.getSourceRoundTableId());
                Brewery brewery = breweryById.get(entry.getBreweryId());
                rows.add(List.of(
                        "奖项结果",
                        anonymousCode(entry, labelByEntryId),
                        shortCode(entry, labelByEntryId),
                        firstText(entry.getName(), ""),
                        brewery == null ? "" : firstText(brewery.getCompanyName(), ""),
                        brewery == null ? "" : firstText(brewery.getContactName(), ""),
                        brewery == null ? "" : firstText(brewery.getPhone(), ""),
                        brewery == null ? "" : firstText(brewery.getWechat(), ""),
                        categoryName(entry, categoryById),
                        round == null ? "" : firstText(round.getRoundName(), ""),
                        table == null ? "" : firstText(table.getTableName(), ""),
                        Objects.equals(award.getChampionFlag(), FLAG_TRUE) ? "全场总冠军" : "组别奖项",
                        award.getRankNo() == null ? "" : String.valueOf(award.getRankNo()),
                        "",
                        firstText(award.getAwardName(), ""),
                        formatAwardResultStatus(award.getStatus()),
                        formatDateTime(award.getPublishedTime())
                ));
            }
        }
        return rows;
    }

    private Map<Long, Brewery> loadBreweries(Set<Long> breweryIds) {
        if (breweryIds.isEmpty()) {
            return Map.of();
        }
        return breweryMapper.selectBatchIds(breweryIds).stream()
                .collect(Collectors.toMap(Brewery::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, List<BeerEntryExtraField>> loadExtraFields(Set<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .in(BeerEntryExtraField::getBeerEntryId, entryIds)
                        .orderByAsc(BeerEntryExtraField::getBeerEntryId)
                        .orderByAsc(BeerEntryExtraField::getId))
                .stream()
                .collect(Collectors.groupingBy(BeerEntryExtraField::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
    }

    private BigDecimal firstScore(BigDecimal primary, BigDecimal fallback) {
        return primary == null ? fallback : primary;
    }

    private LocalDateTime firstTime(LocalDateTime primary, LocalDateTime fallback) {
        return primary == null ? fallback : primary;
    }

    private String feedbackRoleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长个人评分";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return "评审";
    }

    private Map<Long, JudgeAccount> loadJudgeAccounts(Set<Long> judgeIds) {
        if (judgeIds.isEmpty()) {
            return Map.of();
        }
        return judgeAccountMapper.selectBatchIds(judgeIds).stream()
                .collect(Collectors.toMap(JudgeAccount::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, CompetitionRound> loadRounds(Set<Long> roundIds) {
        if (roundIds.isEmpty()) {
            return Map.of();
        }
        return competitionRoundMapper.selectBatchIds(roundIds).stream()
                .collect(Collectors.toMap(CompetitionRound::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, RoundTable> loadRoundTables(Set<Long> tableIds) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }
        return roundTableMapper.selectBatchIds(tableIds).stream()
                .collect(Collectors.toMap(RoundTable::getId, Function.identity(), (left, right) -> left));
    }

    private ScoreRecord findFinalScore(List<ScoreRecord> scoreRecords) {
        return scoreRecords.stream()
                .filter(record -> Objects.equals(record.getFinalFlag(), FLAG_TRUE))
                .findFirst()
                .orElse(null);
    }

    private String anonymousCode(BeerEntry entry, Map<Long, EntryScanLabel> labelByEntryId) {
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        return firstText(label == null ? null : label.getLabelCode(), entry.getUuid());
    }

    private String shortCode(BeerEntry entry, Map<Long, EntryScanLabel> labelByEntryId) {
        EntryScanLabel label = labelByEntryId.get(entry.getId());
        return label == null ? "" : firstText(label.getShortCode(), "");
    }

    private String categoryName(BeerEntry entry, Map<Long, CompetitionCategory> categoryById) {
        CompetitionCategory category = categoryById.get(entry.getCategoryId());
        return category == null ? "" : firstText(category.getName(), "");
    }

    private String formatExtraFields(List<BeerEntryExtraField> extraFields) {
        return extraFields.stream()
                .filter(field -> StringUtils.hasText(field.getFieldValue()))
                .map(field -> firstText(field.getFieldLabel(), field.getFieldKey()) + "：" + normalizeInline(field.getFieldValue()))
                .collect(Collectors.joining("；"));
    }

    private String formatEntryStatus(String status) {
        if (EntryStatus.PENDING_PAYMENT.name().equals(status)) {
            return "待支付";
        }
        if (EntryStatus.REGISTERED.name().equals(status)) {
            return "已支付，报名成功";
        }
        if (EntryStatus.STORED.name().equals(status)) {
            return "已入库";
        }
        if (EntryStatus.CANCELED.name().equals(status)) {
            return "已取消";
        }
        if (EntryStatus.RESULT_PUBLISHED.name().equals(status)) {
            return "结果已出";
        }
        return firstText(status, "");
    }

    private String formatRoundResultType(String resultType, String slotLabel) {
        String label = switch (firstText(resultType, "")) {
            case "ADVANCE" -> "晋级";
            case "RANK" -> "排序";
            case "AWARD_CANDIDATE" -> "奖项候选";
            case "CHAMPION" -> "总冠军候选";
            default -> firstText(resultType, "");
        };
        return StringUtils.hasText(slotLabel) ? slotLabel + "（" + label + "）" : label;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMAT);
    }

    private String formatScoreDimensions(String dimensionsJson) {
        return readDimensions(dimensionsJson).stream()
                .map(item -> item.getLabel() + " " + toPlain(item.getScore()) + "/" + toPlain(item.getMaxScore()))
                .collect(Collectors.joining("，"));
    }

    private String formatRoundResults(List<RoundResult> results,
                                      Map<Long, CompetitionRound> roundById,
                                      Map<Long, RoundTable> tableById) {
        return results.stream()
                .map(result -> {
                    CompetitionRound round = roundById.get(result.getRoundId());
                    RoundTable table = tableById.get(result.getRoundTableId());
                    return firstText(round == null ? null : round.getRoundName(), "轮次")
                            + " " + firstText(table == null ? null : table.getTableName(), "评审桌")
                            + " " + firstText(result.getSlotLabel(), result.getResultType())
                            + (result.getRankNo() == null ? "" : " 第" + result.getRankNo() + "名")
                            + (Objects.equals(result.getLockedFlag(), 1) ? " 已锁定" : " 待确认");
                })
                .collect(Collectors.joining("; "));
    }

    private String formatAwards(List<AwardResult> awards) {
        return awards.stream()
                .map(award -> award.getAwardName() + " " + formatAwardResultStatus(award.getStatus()))
                .collect(Collectors.joining("; "));
    }

    private String formatAwardResultStatus(String status) {
        if (AwardResultStatus.PUBLISHED.name().equals(status)) {
            return "已发布";
        }
        if (AwardResultStatus.CONFIRMED.name().equals(status)) {
            return "已确认";
        }
        return "待确认";
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : (fallback == null ? "" : fallback);
    }

    private String normalizeInline(String text) {
        return text == null ? "" : text.replace("\r", " ").replace("\n", " ").trim();
    }

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private List<DimensionRequest> readDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分维度失败");
        }
    }
}
