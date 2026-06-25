package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.DimensionRequest;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionAnalyticsBucketVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsDeliveryVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsFeedbackVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsPaymentVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsPhraseVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsRegistrationVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsSampleVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsSummaryVO;
import com.beercompetition.pojo.vo.CompetitionAnalyticsVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
public class CompetitionAnalyticsService {

    private static final List<String> ANALYTICS_TERMS = List.of(
            "风味单薄", "泡沫持久", "酒体偏轻", "酒体饱满", "香气强度", "风味层次", "苦甜平衡", "甜苦平衡",
            "清澈通透", "泡沫绵密", "干净无杂味", "麦芽香气", "酒花香气", "麦芽甜香", "酒花苦", "柑橘香",
            "松木香", "花香", "果香", "咖啡", "焦糖", "苦度", "甜感", "酸感", "回甘", "平衡",
            "层次", "杂味", "氧化", "硫味", "酚味", "粗糙", "寡淡", "不协调", "协调", "干净",
            "单薄", "不足", "欠缺", "偏轻", "偏淡", "偏弱", "清爽", "醇厚", "顺滑", "涩感",
            "收口", "余味", "饱满", "浑浊", "清澈", "金黄", "琥珀", "深色",
            "香气", "外观", "味道", "口感", "整体印象", "整体感受", "适饮性", "记忆点", "共识评分"
    );
    private static final List<String> ANALYTICS_TERMS_BY_LENGTH = ANALYTICS_TERMS.stream()
            .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Function.identity()))
            .toList();

    private static final Set<String> POSITIVE_TERMS = Set.of(
            "麦芽香气", "酒花香气", "麦芽甜香", "柑橘香", "松木香", "花香", "果香", "咖啡", "焦糖",
            "清澈通透", "泡沫绵密", "干净无杂味", "回甘", "平衡", "层次", "协调", "干净",
            "酒体饱满", "顺滑", "清爽", "醇厚", "饱满", "金黄", "琥珀"
    );

    private static final Set<String> NEGATIVE_TERMS = Set.of(
            "风味单薄", "酒体偏轻", "单薄", "不足", "欠缺", "偏轻", "偏淡", "偏弱",
            "氧化", "硫味", "酚味", "粗糙", "寡淡", "不协调", "杂味", "浑浊", "涩感"
    );

    private static final Map<String, String> DELIVERY_METHOD_LABELS = Map.of(
            "BOTH", "快递+现场",
            "EXPRESS", "仅快递",
            "ONSITE", "仅现场"
    );

    private static final Map<String, String> PAYMENT_STATUS_LABELS = Map.of(
            "UNPAID", "待支付",
            "PENDING_CONFIRM", "等待转账确认",
            "PAID", "已支付",
            "CANCELED", "已取消",
            "REFUNDED", "已退款"
    );

    private static final Map<String, String> DELIVERY_STATUS_LABELS = Map.of(
            "NOT_SUBMITTED", "未提交",
            "SUBMITTED", "已提交",
            "RECEIVED", "已入库"
    );

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final BreweryMapper breweryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final AwardResultMapper awardResultMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final ObjectMapper objectMapper;

    public CompetitionAnalyticsVO buildAnalytics(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }

        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId));
        Map<Long, BeerEntry> entryById = entries.stream()
                .collect(Collectors.toMap(BeerEntry::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<Long> entryIds = entries.stream().map(BeerEntry::getId).toList();
        List<EntryPayment> payments = entryIds.isEmpty()
                ? List.of()
                : entryPaymentMapper.selectList(new LambdaQueryWrapper<EntryPayment>()
                .in(EntryPayment::getBeerEntryId, entryIds));
        List<EntryDelivery> deliveries = entryIds.isEmpty()
                ? List.of()
                : entryDeliveryMapper.selectList(new LambdaQueryWrapper<EntryDelivery>()
                .in(EntryDelivery::getBeerEntryId, entryIds));

        Map<Long, String> categoryNameById = competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionCategory::getSortOrder)
                        .orderByAsc(CompetitionCategory::getId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, CompetitionCategory::getName, (left, right) -> left, LinkedHashMap::new));
        Map<String, CompetitionStyleConfig> styleByName = competitionStyleConfigMapper.selectList(new LambdaQueryWrapper<CompetitionStyleConfig>()
                        .eq(CompetitionStyleConfig::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionStyleConfig::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Set<Long> breweryIds = entries.stream()
                .map(BeerEntry::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Brewery> breweryById = breweryIds.isEmpty()
                ? Map.of()
                : breweryMapper.selectBatchIds(breweryIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Brewery::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<AwardResult> awardResults = awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCompetitionId, competitionId));
        List<ScoreRecord> scoreRecords = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getCompetitionId, competitionId));
        Set<Long> judgeIds = scoreRecords.stream()
                .map(ScoreRecord::getJudgeAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, JudgeAccount> judgeById = judgeIds.isEmpty()
                ? Map.of()
                : judgeAccountMapper.selectBatchIds(judgeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(JudgeAccount::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        int totalEntries = entries.size();
        int registeredEntries = (int) entries.stream()
                .filter(entry -> !Objects.equals(entry.getStatus(), EntryStatus.CANCELED.name()))
                .count();
        int storedEntries = (int) entries.stream().filter(entry -> Objects.equals(entry.getStoredFlag(), 1)).count();
        int paidEntries = (int) payments.stream().filter(payment -> "PAID".equals(payment.getStatus())).count();
        int pendingPaymentEntries = (int) payments.stream().filter(payment -> "UNPAID".equals(payment.getStatus())).count();
        int receivedEntries = (int) deliveries.stream().filter(delivery -> "RECEIVED".equals(delivery.getDeliveryStatus())).count();
        int reviewedEntries = (int) scoreRecords.stream().map(ScoreRecord::getBeerEntryId).distinct().count();
        int scoreRecordCount = scoreRecords.size();
        int awardCount = awardResults.size();
        int testRecordCount = (int) payments.stream().filter(payment -> EntryPayMethod.MOCK.name().equals(payment.getPayMethod())).count();
        int averageCommentChars = averageInt(scoreRecords.stream()
                .map(ScoreRecord::getCommentCharCount)
                .filter(Objects::nonNull)
                .toList());
        int averageReviewSeconds = averageInt(scoreRecords.stream()
                .map(ScoreRecord::getDurationSeconds)
                .filter(Objects::nonNull)
                .toList());

        CompetitionAnalyticsSummaryVO summary = CompetitionAnalyticsSummaryVO.builder()
                .totalEntries(totalEntries)
                .registeredEntries(registeredEntries)
                .storedEntries(storedEntries)
                .paidEntries(paidEntries)
                .pendingPaymentEntries(pendingPaymentEntries)
                .receivedEntries(receivedEntries)
                .reviewedEntries(reviewedEntries)
                .scoreRecords(scoreRecordCount)
                .awardCount(awardCount)
                .testRecordCount(testRecordCount)
                .averageCommentChars(averageCommentChars)
                .averageReviewSeconds(averageReviewSeconds)
                .build();

        CompetitionAnalyticsRegistrationVO registration = CompetitionAnalyticsRegistrationVO.builder()
                .categories(buildCategoryBuckets(entries, categoryNameById))
                .styles(buildStyleBuckets(entries, styleByName))
                .abvBuckets(buildAbvBuckets(entries))
                .breweries(buildBreweryBuckets(entries, breweryById))
                .build();

        CompetitionAnalyticsPaymentVO payment = CompetitionAnalyticsPaymentVO.builder()
                .channels(buildPaymentChannelBuckets(payments))
                .statuses(buildPaymentStatusBuckets(payments))
                .testRecordCount(testRecordCount)
                .notes(buildPaymentNotes(payments))
                .build();

        CompetitionAnalyticsDeliveryVO delivery = CompetitionAnalyticsDeliveryVO.builder()
                .statuses(buildDeliveryStatusBuckets(deliveries))
                .methods(buildDeliveryMethodBuckets(deliveries))
                .pendingEntries(buildPendingDeliverySamples(entries, deliveries, categoryNameById))
                .build();

        CompetitionAnalyticsFeedbackVO feedback = CompetitionAnalyticsFeedbackVO.builder()
                .commentCount(countCommentRecords(scoreRecords))
                .entryCount((int) scoreRecords.stream().map(ScoreRecord::getBeerEntryId).distinct().count())
                .wordCloud(buildPhraseCloud(scoreRecords, entryById, judgeById, null))
                .positivePhrases(buildPhraseCloud(scoreRecords, entryById, judgeById, "positive"))
                .negativePhrases(buildPhraseCloud(scoreRecords, entryById, judgeById, "negative"))
                .samples(buildFeedbackSamples(scoreRecords, entryById, judgeById))
                .build();

        List<String> warnings = new ArrayList<>();
        if (testRecordCount > 0) {
            warnings.add("检测到 " + testRecordCount + " 笔测试记录，不计入正式渠道统计");
        }
        long manualCount = payments.stream().filter(paymentItem -> EntryPayMethod.MANUAL.name().equals(paymentItem.getPayMethod())).count();
        if (manualCount > 0) {
            warnings.add("检测到 " + manualCount + " 笔人工确认记录，未计入渠道结构");
        }
        if (scoreRecordCount < 12) {
            warnings.add("评分样本较少，文本分析仅供参考");
        }
        if (feedback.getWordCloud().isEmpty()) {
            warnings.add("当前没有足够的词频样本，词云暂不显示");
        }

        return CompetitionAnalyticsVO.builder()
                .generatedAt(LocalDateTime.now())
                .summary(summary)
                .registration(registration)
                .payment(payment)
                .delivery(delivery)
                .feedback(feedback)
                .warnings(warnings)
                .build();
    }

    private List<CompetitionAnalyticsBucketVO> buildCategoryBuckets(List<BeerEntry> entries, Map<Long, String> categoryNameById) {
        return buildCountBuckets(entries.stream()
                .collect(Collectors.groupingBy(entry -> categoryNameById.getOrDefault(entry.getCategoryId(), "未分组"), LinkedHashMap::new, Collectors.counting())),
                entries.size(),
                null);
    }

    private List<CompetitionAnalyticsBucketVO> buildStyleBuckets(List<BeerEntry> entries, Map<String, CompetitionStyleConfig> styleByName) {
        return buildCountBuckets(entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    CompetitionStyleConfig style = styleByName.get(entry.getStyle());
                    return style == null ? StringUtils.hasText(entry.getStyle()) ? entry.getStyle() : "未填写风格" : style.getName();
                }, LinkedHashMap::new, Collectors.counting())),
                entries.size(),
                null);
    }

    private List<CompetitionAnalyticsBucketVO> buildAbvBuckets(List<BeerEntry> entries) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("< 4%", 0L);
        counts.put("4% - 5%", 0L);
        counts.put("5% - 6%", 0L);
        counts.put("6% - 7%", 0L);
        counts.put("7% - 8%", 0L);
        counts.put("8%+", 0L);
        counts.put("未填写", 0L);
        for (BeerEntry entry : entries) {
            BigDecimal abv = entry.getAbv();
            String bucket;
            if (abv == null) {
                bucket = "未填写";
            } else if (abv.compareTo(BigDecimal.valueOf(4)) < 0) {
                bucket = "< 4%";
            } else if (abv.compareTo(BigDecimal.valueOf(5)) < 0) {
                bucket = "4% - 5%";
            } else if (abv.compareTo(BigDecimal.valueOf(6)) < 0) {
                bucket = "5% - 6%";
            } else if (abv.compareTo(BigDecimal.valueOf(7)) < 0) {
                bucket = "6% - 7%";
            } else if (abv.compareTo(BigDecimal.valueOf(8)) < 0) {
                bucket = "7% - 8%";
            } else {
                bucket = "8%+";
            }
            counts.computeIfPresent(bucket, (key, value) -> value + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> CompetitionAnalyticsBucketVO.builder()
                        .key(entry.getKey())
                        .label(entry.getKey())
                        .count(entry.getValue().intValue())
                        .share(percent(entry.getValue().intValue(), entries.size()))
                        .tone(safeTone(entry.getKey()))
                        .build())
                .filter(item -> item.getCount() != null && item.getCount() > 0)
                .toList();
    }

    private List<CompetitionAnalyticsBucketVO> buildBreweryBuckets(List<BeerEntry> entries, Map<Long, Brewery> breweryById) {
        Map<String, Long> counts = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Brewery brewery = breweryById.get(entry.getBreweryId());
                    return brewery == null || !StringUtils.hasText(brewery.getCompanyName())
                            ? "未关联厂牌"
                            : brewery.getCompanyName();
                }, LinkedHashMap::new, Collectors.counting()));
        return buildCountBuckets(counts, entries.size(), 8);
    }

    private List<CompetitionAnalyticsBucketVO> buildPaymentChannelBuckets(List<EntryPayment> payments) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("WECHAT", 0L);
        counts.put("BANK_TRANSFER", 0L);
        Map<String, BigDecimal> amounts = new HashMap<>();
        amounts.put("WECHAT", BigDecimal.ZERO);
        amounts.put("BANK_TRANSFER", BigDecimal.ZERO);
        long formalCount = payments.stream().filter(payment -> isFormalChannel(payment.getPayMethod())).count();
        for (EntryPayment payment : payments) {
            if (!counts.containsKey(payment.getPayMethod())) {
                continue;
            }
            counts.compute(payment.getPayMethod(), (key, value) -> value == null ? 1L : value + 1);
            amounts.compute(payment.getPayMethod(), (key, value) -> value == null ? money(payment) : value.add(money(payment)));
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> CompetitionAnalyticsBucketVO.builder()
                        .key(entry.getKey())
                        .label(resolvePaymentChannelLabel(entry.getKey()))
                        .count(entry.getValue().intValue())
                        .amount(amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                        .share(percent(entry.getValue(), formalCount))
                        .tone("gold")
                        .build())
                .toList();
    }

    private List<CompetitionAnalyticsBucketVO> buildPaymentStatusBuckets(List<EntryPayment> payments) {
        Map<String, Long> counts = payments.stream()
                .collect(Collectors.groupingBy(payment -> StringUtils.hasText(payment.getStatus()) ? payment.getStatus() : "未记录", LinkedHashMap::new, Collectors.counting()));
        Map<String, BigDecimal> amounts = payments.stream()
                .collect(Collectors.groupingBy(payment -> StringUtils.hasText(payment.getStatus()) ? payment.getStatus() : "未记录",
                        LinkedHashMap::new,
                        Collectors.mapping(this::money, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .map(entry -> CompetitionAnalyticsBucketVO.builder()
                        .key(entry.getKey())
                        .label(PAYMENT_STATUS_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
                        .count(entry.getValue().intValue())
                        .amount(amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                        .share(percent(entry.getValue().intValue(), payments.size()))
                        .tone(paymentStatusTone(entry.getKey()))
                        .build())
                .toList();
    }

    private List<String> buildPaymentNotes(List<EntryPayment> payments) {
        List<String> notes = new ArrayList<>();
        long mockCount = payments.stream().filter(payment -> EntryPayMethod.MOCK.name().equals(payment.getPayMethod())).count();
        if (mockCount > 0) {
            notes.add("检测到 " + mockCount + " 笔测试记录，不计入正式渠道统计");
        }
        long manualCount = payments.stream().filter(payment -> EntryPayMethod.MANUAL.name().equals(payment.getPayMethod())).count();
        if (manualCount > 0) {
            notes.add("检测到 " + manualCount + " 笔人工确认记录，未计入付款构成");
        }
        if (payments.stream().noneMatch(payment -> isFormalChannel(payment.getPayMethod()))) {
            notes.add("当前没有正式支付渠道记录");
        }
        return notes;
    }

    private boolean isFormalChannel(String payMethod) {
        return EntryPayMethod.WECHAT.name().equals(payMethod) || EntryPayMethod.BANK_TRANSFER.name().equals(payMethod);
    }

    private List<CompetitionAnalyticsBucketVO> buildDeliveryStatusBuckets(List<EntryDelivery> deliveries) {
        Map<String, Long> counts = deliveries.stream()
                .collect(Collectors.groupingBy(delivery -> StringUtils.hasText(delivery.getDeliveryStatus()) ? delivery.getDeliveryStatus() : "未记录", LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .map(entry -> CompetitionAnalyticsBucketVO.builder()
                        .key(entry.getKey())
                        .label(DELIVERY_STATUS_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
                        .count(entry.getValue().intValue())
                        .share(percent(entry.getValue().intValue(), deliveries.size()))
                        .tone(deliveryTone(entry.getKey()))
                        .build())
                .toList();
    }

    private List<CompetitionAnalyticsBucketVO> buildDeliveryMethodBuckets(List<EntryDelivery> deliveries) {
        Map<String, Long> counts = deliveries.stream()
                .collect(Collectors.groupingBy(delivery -> StringUtils.hasText(delivery.getDeliveryMethod()) ? delivery.getDeliveryMethod() : "未记录", LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .map(entry -> CompetitionAnalyticsBucketVO.builder()
                        .key(entry.getKey())
                        .label(DELIVERY_METHOD_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
                        .count(entry.getValue().intValue())
                        .share(percent(entry.getValue().intValue(), deliveries.size()))
                        .tone("gold")
                        .build())
                .toList();
    }

    private List<CompetitionAnalyticsSampleVO> buildPendingDeliverySamples(List<BeerEntry> entries,
                                                                            List<EntryDelivery> deliveries,
                                                                            Map<Long, String> categoryNameById) {
        Map<Long, EntryDelivery> deliveryByEntryId = deliveries.stream()
                .collect(Collectors.toMap(EntryDelivery::getBeerEntryId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return entries.stream()
                .filter(entry -> {
                    EntryDelivery delivery = deliveryByEntryId.get(entry.getId());
                    return delivery == null
                            || "NOT_SUBMITTED".equals(delivery.getDeliveryStatus())
                            || "SUBMITTED".equals(delivery.getDeliveryStatus());
                })
                .limit(6)
                .map(entry -> {
                    EntryDelivery delivery = deliveryByEntryId.get(entry.getId());
                    String status = delivery == null ? "未提交送样" : DELIVERY_STATUS_LABELS.getOrDefault(delivery.getDeliveryStatus(), delivery.getDeliveryStatus());
                    String detail = delivery == null
                            ? "尚未提交送样信息"
                            : StringUtils.hasText(delivery.getCarrier()) || StringUtils.hasText(delivery.getTrackingNo())
                            ? List.of(delivery.getCarrier(), delivery.getTrackingNo()).stream().filter(StringUtils::hasText).collect(Collectors.joining(" · "))
                            : status;
                    return CompetitionAnalyticsSampleVO.builder()
                            .title(entry.getName())
                            .meta(List.of(categoryNameById.getOrDefault(entry.getCategoryId(), "未分组"), entry.getStyle()).stream().filter(StringUtils::hasText).collect(Collectors.joining(" · ")))
                            .detail(detail)
                            .tone("warning")
                            .build();
                })
                .toList();
    }

    private int countCommentRecords(List<ScoreRecord> scoreRecords) {
        return (int) scoreRecords.stream()
                .map(ScoreRecord::getComments)
                .filter(StringUtils::hasText)
                .count();
    }

    private List<CompetitionAnalyticsPhraseVO> buildPhraseCloud(List<ScoreRecord> scoreRecords,
                                                                Map<Long, BeerEntry> entryById,
                                                                Map<Long, JudgeAccount> judgeById,
                                                                String toneFilter) {
        Map<String, PhraseAccumulator> accumulatorByPhrase = new LinkedHashMap<>();
        for (ScoreRecord record : scoreRecords) {
            String tone = resolveTone(record);
            if (toneFilter != null && !toneFilter.equals(tone)) {
                continue;
            }
            Set<String> terms = new LinkedHashSet<>();
            collectTerms(record.getComments(), terms);
            readDimensions(record.getDimensionsJson()).stream()
                    .map(DimensionRequest::getNote)
                    .filter(StringUtils::hasText)
                    .forEach(note -> collectTerms(note, terms));
            if (terms.isEmpty()) {
                continue;
            }
            String sample = buildScoreSample(record, entryById, judgeById);
            Long entryId = record.getBeerEntryId();
            Long judgeId = record.getJudgeAccountId();
            for (String phrase : terms) {
                PhraseAccumulator accumulator = accumulatorByPhrase.computeIfAbsent(phrase, key -> new PhraseAccumulator(phrase));
                accumulator.count += 1;
                if (entryId != null) {
                    accumulator.entryIds.add(entryId);
                }
                if (judgeId != null) {
                    accumulator.judgeIds.add(judgeId);
                }
                if (sample != null && accumulator.sample == null) {
                    accumulator.sample = sample;
                }
            }
        }
        return accumulatorByPhrase.values().stream()
                .map(accumulator -> CompetitionAnalyticsPhraseVO.builder()
                        .text(accumulator.phrase)
                        .count(accumulator.count)
                        .entryCount(accumulator.entryIds.size())
                        .judgeCount(accumulator.judgeIds.size())
                        .weight(accumulator.weight())
                        .tone(resolvePhraseTone(accumulator.phrase, toneFilter))
                        .sample(accumulator.sample)
                        .build())
                .sorted(Comparator.comparing(CompetitionAnalyticsPhraseVO::getWeight, Comparator.reverseOrder())
                        .thenComparing(CompetitionAnalyticsPhraseVO::getText))
                .limit("positive".equals(toneFilter) || "negative".equals(toneFilter) ? 12 : 24)
                .toList();
    }

    private void collectTerms(String text, Set<String> terms) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        int index = 0;
        while (index < normalized.length()) {
            String matched = null;
            for (String term : ANALYTICS_TERMS_BY_LENGTH) {
                if (index + term.length() <= normalized.length() && normalized.startsWith(term, index)) {
                    matched = term;
                    break;
                }
            }
            if (matched != null) {
                terms.add(matched);
                index += matched.length();
                continue;
            }
            index += 1;
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("[^\\u4e00-\\u9fa5A-Za-z0-9]+", "");
    }

    private List<CompetitionAnalyticsSampleVO> buildFeedbackSamples(List<ScoreRecord> scoreRecords,
                                                                     Map<Long, BeerEntry> entryById,
                                                                     Map<Long, JudgeAccount> judgeById) {
        return scoreRecords.stream()
                .filter(record -> StringUtils.hasText(record.getComments()))
                .sorted(Comparator.comparing(ScoreRecord::getTotalScore, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(Comparator.comparing(ScoreRecord::getUpdateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()))
                .limit(6)
                .map(record -> {
                    BeerEntry entry = entryById.get(record.getBeerEntryId());
                    JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
                    String meta = List.of(
                            judge == null ? feedbackRoleLabel(record.getJudgeRoleType()) : judge.getName(),
                            formatScoreLabel(record.getTotalScore())
                    ).stream().filter(StringUtils::hasText).collect(Collectors.joining(" · "));
                    return CompetitionAnalyticsSampleVO.builder()
                            .title(entry == null ? "未命名酒款" : entry.getName())
                            .meta(meta)
                            .detail(snippet(record.getComments(), 120))
                            .tone(resolveTone(record))
                            .build();
                })
                .toList();
    }

    private String buildScoreSample(ScoreRecord record, Map<Long, BeerEntry> entryById, Map<Long, JudgeAccount> judgeById) {
        BeerEntry entry = entryById.get(record.getBeerEntryId());
        JudgeAccount judge = judgeById.get(record.getJudgeAccountId());
        return List.of(
                entry == null ? null : entry.getName(),
                judge == null ? feedbackRoleLabel(record.getJudgeRoleType()) : judge.getName(),
                snippet(record.getComments(), 64)
        ).stream().filter(StringUtils::hasText).collect(Collectors.joining(" · "));
    }

    private String feedbackRoleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return "专业评审";
    }

    private List<DimensionRequest> readDimensions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DimensionRequest>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String resolveTone(ScoreRecord record) {
        BigDecimal score = record.getTotalScore();
        if (score == null) {
            return "neutral";
        }
        if (score.compareTo(BigDecimal.valueOf(30)) >= 0) {
            return "positive";
        }
        if (score.compareTo(BigDecimal.valueOf(26)) <= 0) {
            return "negative";
        }
        return "neutral";
    }

    private String resolvePhraseTone(String phrase, String toneFilter) {
        if ("positive".equals(toneFilter)) {
            return "positive";
        }
        if ("negative".equals(toneFilter)) {
            return "negative";
        }
        if (POSITIVE_TERMS.contains(phrase)) {
            return "positive";
        }
        if (NEGATIVE_TERMS.contains(phrase)) {
            return "negative";
        }
        return "neutral";
    }

    private String formatScoreLabel(BigDecimal score) {
        if (score == null) {
            return "未评分";
        }
        return score.stripTrailingZeros().toPlainString() + " 分";
    }

    private String snippet(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private CompetitionAnalyticsBucketVO bucket(String key, String label, long count, int total, BigDecimal amount, String tone, String detail) {
        return CompetitionAnalyticsBucketVO.builder()
                .key(key)
                .label(label)
                .count((int) count)
                .amount(amount == null ? BigDecimal.ZERO : amount)
                .share(percent((int) count, total))
                .tone(tone)
                .detail(detail)
                .build();
    }

    private List<CompetitionAnalyticsBucketVO> buildCountBuckets(Map<String, Long> counts, int total, Integer limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .map(entry -> bucket(entry.getKey(), entry.getKey(), entry.getValue(), total, BigDecimal.ZERO, safeTone(entry.getKey()), null))
                .filter(item -> item.getCount() != null && item.getCount() > 0)
                .limit(limit == null ? Long.MAX_VALUE : limit.longValue())
                .toList();
    }

    private BigDecimal percent(long count, long total) {
        if (total <= 0 || count <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private BigDecimal money(EntryPayment payment) {
        if (payment == null) {
            return BigDecimal.ZERO;
        }
        if (payment.getPaidAmount() != null) {
            return payment.getPaidAmount();
        }
        return payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
    }

    private String paymentStatusTone(String status) {
        return switch (status) {
            case "PAID" -> "positive";
            case "UNPAID", "PENDING_CONFIRM" -> "warning";
            case "REFUNDED", "CANCELED" -> "neutral";
            default -> "neutral";
        };
    }

    private String deliveryTone(String status) {
        return switch (status) {
            case "RECEIVED" -> "positive";
            case "SUBMITTED" -> "warning";
            case "NOT_SUBMITTED" -> "neutral";
            default -> "neutral";
        };
    }

    private String safeTone(String label) {
        if (label == null) {
            return "neutral";
        }
        if ("未填写".equals(label) || "未关联厂牌".equals(label)) {
            return "warning";
        }
        return "gold";
    }

    private String resolvePaymentChannelLabel(String key) {
        return switch (key) {
            case "WECHAT" -> "微信支付";
            case "BANK_TRANSFER" -> "银行转账";
            default -> key;
        };
    }

    private static final class PhraseAccumulator {
        private final String phrase;
        private final Set<Long> entryIds = new LinkedHashSet<>();
        private final Set<Long> judgeIds = new LinkedHashSet<>();
        private int count = 0;
        private String sample;

        private PhraseAccumulator(String phrase) {
            this.phrase = phrase;
        }

        private int weight() {
            return count * 10 + entryIds.size() * 4 + judgeIds.size() * 2;
        }
    }
}
