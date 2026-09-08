package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.CompetitionRoundVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.ResultDraftVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import com.beercompetition.judging.round.RoundQueryService;

/**
 * 提供比赛候选池、结果草稿和轮次总览查询。
 */
@Service
@RequiredArgsConstructor
public class RoundQueryServiceImpl implements RoundQueryService {

    private final BeerEntryMapper beerEntryMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final RoundResultMapper roundResultMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundOverviewQueryService roundOverviewQueryService;

    @Override
    public List<CompetitionEntryVO> listEntryPool(Long competitionId) {
        roundQuerySupport.requireCompetition(competitionId);
        List<BeerEntry> entries = beerEntryMapper.selectList(entryPoolWrapper(competitionId));
        return buildEntryPool(competitionId, entries);
    }

    @Override
    public PageResult<CompetitionEntryVO> listEntryPoolPage(Long competitionId, Integer page, Integer pageSize) {
        roundQuerySupport.requireCompetition(competitionId);
        long currentPage = page == null || page < 1 ? 1 : page;
        long currentPageSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        Page<BeerEntry> result = beerEntryMapper.selectPage(new Page<>(currentPage, currentPageSize), entryPoolWrapper(competitionId));
        return new PageResult<>(result.getTotal(), buildEntryPool(competitionId, result.getRecords()));
    }

    private LambdaQueryWrapper<BeerEntry> entryPoolWrapper(Long competitionId) {
        return new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())
                .orderByDesc(BeerEntry::getCreateTime)
                .orderByAsc(BeerEntry::getId);
    }

    private List<CompetitionEntryVO> buildEntryPool(Long competitionId, List<BeerEntry> entries) {
        // 读取当前页报名及其关联的支付、物流、退款、扫码标签和最新轮次结果
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(competitionId);
        Map<String, CompetitionStyleConfig> styleByName = roundQuerySupport.listStyleSnapshot(competitionId);
        Map<Long, RoundResult> latestResultByEntry = roundQuerySupport.latestResultByEntry(competitionId);
        Map<Long, CompetitionStyleConfig> styleById = roundQuerySupport.loadStyleSnapshots(entries.stream()
                .map(BeerEntry::getStyleConfigId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, RoundTable> tableById = roundQuerySupport.loadRoundTables(latestResultByEntry.values().stream()
                .map(RoundResult::getRoundTableId)
                .collect(Collectors.toSet()));
        Map<Long, Brewery> breweryById = roundQuerySupport.loadBreweries(entries.stream().map(BeerEntry::getBreweryId).collect(Collectors.toSet()));
        Map<Long, EntryPayment> paymentByEntryId = roundQuerySupport.loadPayments(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryDelivery> deliveryByEntryId = roundQuerySupport.loadDeliveries(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryRefund> refundByEntryId = roundQuerySupport.loadLatestRefunds(entries.stream().map(BeerEntry::getId).collect(Collectors.toSet()));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entries.stream().map(BeerEntry::getId).toList());

        // 组装后台酒款池视图，保留报名、入库、支付与评审结果的综合状态
        return entries.stream()
                .map(entry -> toEntryVO(entry, categoryNameById, styleById, styleByName, latestResultByEntry.get(entry.getId()), tableById,
                        breweryById.get(entry.getBreweryId()), paymentByEntryId.get(entry.getId()), deliveryByEntryId.get(entry.getId()),
                        refundByEntryId.get(entry.getId()), labelByEntryId.get(entry.getId())))
                .toList();
    }

    @Override
    public List<ResultDraftVO> buildResultDrafts(Long competitionId) {
        // 1) 取最新锁定轮次作为奖项草稿来源
        roundQuerySupport.requireCompetition(competitionId);
        List<CompetitionRound> rounds = roundQuerySupport.listRounds(competitionId);
        if (rounds.isEmpty()) {
            return List.of();
        }
        CompetitionRound lastLocked = rounds.stream()
                .filter(round -> RoundStatus.LOCKED.name().equals(round.getStatus()))
                .max(Comparator.comparing(CompetitionRound::getRoundNo))
                .orElse(null);
        if (lastLocked == null) {
            return List.of();
        }
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .eq(RoundResult::getRoundId, lastLocked.getId()))
                .stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet()));
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(competitionId);

        // 2) 只输出正式名次、奖项候选和总冠军结果，供发布前人工核对
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getCompetitionId, competitionId)
                        .eq(RoundResult::getRoundId, lastLocked.getId())
                        .in(RoundResult::getResultType, RoundResultType.RANK.name(), RoundResultType.AWARD_CANDIDATE.name(), RoundResultType.CHAMPION.name())
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByAsc(RoundResult::getId))
                .stream()
                .map(result -> {
                    BeerEntry entry = entryById.get(result.getBeerEntryId());
                    return ResultDraftVO.builder()
                            .category(entry == null ? "-" : categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                            .slot(resolveSlotLabel(result))
                            .uuid(entry == null ? null : entry.getUuid())
                            .beerEntryId(result.getBeerEntryId())
                            .build();
                })
                .toList();
    }

    private CompetitionEntryVO toEntryVO(BeerEntry entry,
                                         Map<Long, String> categoryNameById,
                                         Map<Long, CompetitionStyleConfig> styleById,
                                         Map<String, CompetitionStyleConfig> styleByName,
                                         RoundResult latestResult,
                                         Map<Long, RoundTable> tableById,
                                         Brewery brewery,
                                         EntryPayment payment,
                                         EntryDelivery delivery,
                                         EntryRefund refund,
                                         EntryScanLabel label) {
        if (entry == null) {
            return CompetitionEntryVO.builder().build();
        }
        CompetitionStyleConfig style = entry.getStyleConfigId() == null ? null : styleById.get(entry.getStyleConfigId());
        if (style == null) {
            style = styleByName.get(entry.getStyle());
        }
        boolean canConfirmPayment = EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus());
        boolean canMarkStored = EntryStatus.REGISTERED.name().equals(entry.getStatus());
        boolean canCancel = Set.of(EntryStatus.PENDING_PAYMENT.name(), EntryStatus.REGISTERED.name()).contains(entry.getStatus());
        return CompetitionEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label == null ? null : label.getLabelCode())
                .shortCode(label == null ? null : label.getShortCode())
                .scanToken(label == null ? null : label.getScanToken())
                .name(entry.getName())
                .breweryId(entry.getBreweryId())
                .breweryCompanyName(brewery == null ? null : brewery.getCompanyName())
                .breweryContactName(brewery == null ? null : brewery.getContactName())
                .categoryId(entry.getCategoryId())
                .categoryName(categoryNameById.getOrDefault(entry.getCategoryId(), "-"))
                .style(entry.getStyle())
                .boxNumber(entry.getBoxNumber())
                .styleCategoryName(style == null ? null : style.getCategoryName())
                .styleCode(style == null ? null : style.getStyleCode())
                .styleDescription(style == null ? null : style.getDescription())
                .abv(entry.getAbv())
                .extraFields(listJudgeVisibleExtraFields(entry))
                .status(entry.getStatus())
                .paymentStatus(payment == null ? EntryPaymentStatus.UNPAID.name() : payment.getStatus())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(delivery == null ? null : delivery.getDeliveryStatus())
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                .stored(Objects.equals(entry.getStoredFlag(), FLAG_TRUE))
                .advanced(latestResult != null && !RoundResultType.EVALUATED.name().equals(latestResult.getResultType()))
                .canConfirmPayment(canConfirmPayment)
                .canMarkStored(canMarkStored)
                .canCancel(canCancel)
                .sourceTable(latestResult == null || tableById.get(latestResult.getRoundTableId()) == null ? "" : tableById.get(latestResult.getRoundTableId()).getTableName())
                .sourceResult(latestResult == null ? "" : resolveSlotLabel(latestResult))
                .build();
    }

    private List<EntryExtraFieldVO> listJudgeVisibleExtraFields(BeerEntry entry) {
        Set<String> visibleKeys = listEntryFieldConfigs(entry.getCompetitionId()).stream()
                .filter(item -> Objects.equals(item.getVisibleToJudges(), FLAG_TRUE))
                .map(EntryFieldConfig::getFieldKey)
                .collect(Collectors.toSet());
        if (visibleKeys.isEmpty()) {
            return List.of();
        }
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, entry.getId()))
                .stream()
                .filter(item -> visibleKeys.contains(item.getFieldKey()))
                .map(this::toEntryExtraFieldVO)
                .toList();
    }

    private EntryExtraFieldVO toEntryExtraFieldVO(BeerEntryExtraField item) {
        return EntryExtraFieldVO.builder()
                .key(item.getFieldKey())
                .label(item.getFieldLabel())
                .value(item.getFieldValue())
                .build();
    }

    private List<EntryFieldConfig> listEntryFieldConfigs(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId)
                .orderByAsc(EntryFieldConfig::getSortOrder)
                .orderByAsc(EntryFieldConfig::getId));
    }

    private String resolveSlotLabel(RoundResult result) {
        if (StringUtils.hasText(result.getSlotLabel())) {
            return result.getSlotLabel();
        }
        if (result.getRankNo() != null) {
            return "第 " + result.getRankNo() + " 名";
        }
        if (RoundResultType.ADVANCE.name().equals(result.getResultType())) {
            return "晋级";
        }
        return result.getResultType();
    }

    @Override
    public List<CompetitionRoundVO> listCompetitionRounds(Long competitionId) {
        return roundOverviewQueryService.listCompetitionRounds(competitionId);
    }
}
