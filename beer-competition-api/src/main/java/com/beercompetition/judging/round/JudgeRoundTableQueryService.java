package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundJudgeRankingDraftMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.RankingResultItemRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundJudgeRankingDraft;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.CompetitionEntryVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.JudgeRoundTableVO;
import com.beercompetition.pojo.vo.RankingConfirmationSlotVO;
import com.beercompetition.pojo.vo.RankingConfirmationVO;
import com.beercompetition.pojo.vo.RoundRankingSlotVO;
import com.beercompetition.pojo.vo.ScoreConfirmationEntryVO;
import com.beercompetition.pojo.vo.ScoreConfirmationVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.ReviewStatsService;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import com.beercompetition.service.impl.round.RoundValidationPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.beercompetition.service.impl.round.RoundConstants.FLAG_TRUE;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_BRONZE;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_GOLD;
import static com.beercompetition.service.impl.round.RoundConstants.SLOT_SILVER;

/**
 * 校验当前评委的评分桌访问权限，并组装评分、排序和确认详情。
 */
@Service
@RequiredArgsConstructor
public class JudgeRoundTableQueryService {

    private final CompetitionMapper competitionMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundResultMapper roundResultMapper;

    private final RoundTableConfirmationMapper roundTableConfirmationMapper;

    private final RoundJudgeRankingDraftMapper roundJudgeRankingDraftMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final ReviewStatsService reviewStatsService;

    private final ObjectMapper objectMapper;

    private final RoundQuerySupport roundQuerySupport;

    private final RoundValidationPolicy roundValidationPolicy;

    public JudgeRoundTableVO getMyRoundTable(Long roundTableId) {
        // 1) 校验当前评审对轮次桌的访问权限和轮次可见状态
        Long judgeId = BaseContext.getCurrentId();
        roundQuerySupport.requireActiveJudge(judgeId);
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        if (round == null) {
            throw new ResourceNotFoundException("轮次不存在");
        }
        if (RoundType.SCORE.name().equals(round.getRoundType()) && !isScoreRoundVisibleStatus(round.getStatus())) {
            throw new BaseException("当前评分轮次未发布或已锁定");
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && !isRankingVisibleStatus(round.getStatus())) {
            throw new BaseException("当前排序轮次未发布或已锁定");
        }
        assertCompetitionNotArchived(table.getCompetitionId());
        Competition competition = roundQuerySupport.requireCompetition(table.getCompetitionId());
        RoundTableMember member = requireRoundTableMember(roundTableId, judgeId);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<String, CompetitionStyleConfig> styleByName = roundQuerySupport.listStyleSnapshot(table.getCompetitionId());
        List<RoundTableEntry> entries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, roundTableId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet()));
        Map<Long, CompetitionStyleConfig> styleById = roundQuerySupport.loadStyleSnapshots(entryById.values().stream()
                .map(BeerEntry::getStyleConfigId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryById.keySet());
        List<RoundResult> results = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, roundTableId)
                .orderByAsc(RoundResult::getRankNo));

        // 2) 组装评分、排序、确认状态和个人参考排序
        return JudgeRoundTableVO.builder()
                .roundTableId(roundTableId)
                .roundId(round.getId())
                .roundName(round.getRoundName())
                .roundType(round.getRoundType())
                .competitionType(resolveCompetitionType(competition).name())
                .tableName(table.getTableName())
                .targetCount(table.getTargetCount())
                .expectedJudgeCount(resolveExpectedJudgeCount(table.getId()))
                .targetMode(table.getTargetMode())
                .status(table.getStatus())
                .canSubmitTableScore(canSubmitScoreRoundTable(member, round, table))
                .canSubmitRanking(canSubmitRanking(member, round, table))
                .scoreConfirmation(RoundType.SCORE.name().equals(round.getRoundType()) ? buildScoreConfirmation(table, member) : null)
                .rankingConfirmation(RoundType.RANKING.name().equals(round.getRoundType()) ? buildRankingConfirmation(table, member) : null)
                .myReviewStats(RoundType.SCORE.name().equals(round.getRoundType()) ? reviewStatsService.getMyRoundTableStats(roundTableId) : null)
                .entries(entries.stream()
                        .map(item -> toEntryVO(
                                entryById.get(item.getBeerEntryId()),
                                categoryNameById,
                                styleById,
                                styleByName,
                                null,
                                Map.of(),
                                null,
                                null,
                                null,
                                null,
                                labelByEntryId.get(item.getBeerEntryId())))
                        .toList())
                .rankings(buildRankings(table, results, entryById))
                .myRankingDraft(RoundType.RANKING.name().equals(round.getRoundType()) ? buildMyRankingDraft(table, entryById, judgeId) : List.of())
                .build();
    }

    private Integer resolveExpectedJudgeCount(Long roundTableId) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
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

    private RoundTableMember requireRoundTableMember(Long roundTableId, Long judgeId) {
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId));
        if (member == null) {
            throw new ForbiddenException("无权查看该轮次桌");
        }
        return member;
    }

    private boolean canSubmitRanking(RoundTableMember member, CompetitionRound round, RoundTable table) {
        return RoundType.RANKING.name().equals(round.getRoundType())
                && isRankingEditableStatus(round.getStatus())
                && !RoundStatus.SUBMITTED.name().equals(table.getStatus())
                && !RoundStatus.LOCKED.name().equals(table.getStatus())
                && JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE);
    }

    private boolean canSubmitScoreRoundTable(RoundTableMember member, CompetitionRound round, RoundTable table) {
        return RoundType.SCORE.name().equals(round.getRoundType())
                && RoundStatus.PUBLISHED.name().equals(round.getStatus())
                && RoundStatus.PUBLISHED.name().equals(table.getStatus())
                && JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                && Objects.equals(member.getSystemTaskRequired(), FLAG_TRUE);
    }

    private boolean isScoreRoundVisibleStatus(String status) {
        return RoundStatus.PUBLISHED.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new BaseException("比赛已归档");
        }
    }

    private boolean isRankingVisibleStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status)
                || RoundStatus.LOCKED.name().equals(status);
    }

    private boolean isRankingEditableStatus(String status) {
        return RoundStatus.IN_PROGRESS.name().equals(status)
                || RoundStatus.SUBMITTED.name().equals(status);
    }

    private ScoreConfirmationVO buildScoreConfirmation(RoundTable table, RoundTableMember currentMember) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId())
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
        Set<Long> entryIds = tableEntries.stream().map(RoundTableEntry::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entryIds);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Map<Long, ScoreRecord> finalScoreByEntry = entryIds.isEmpty()
                ? Map.of()
                : scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        int version = currentResultVersion(table);
        Long judgeId = BaseContext.getCurrentId();
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version)) > 0;
        List<ScoreConfirmationEntryVO> entries = tableEntries.stream()
                .map(item -> {
                    BeerEntry entry = entryById.get(item.getBeerEntryId());
                    ScoreRecord finalScore = finalScoreByEntry.get(item.getBeerEntryId());
                    EntryScanLabel label = labelByEntryId.get(item.getBeerEntryId());
                    return ScoreConfirmationEntryVO.builder()
                            .beerEntryId(item.getBeerEntryId())
                            .uuid(entry == null ? "" : entry.getUuid())
                            .shortCode(label == null ? "" : label.getShortCode())
                            .categoryName(entry == null ? "" : categoryNameById.getOrDefault(entry.getCategoryId(), ""))
                            .style(entry == null ? "" : entry.getStyle())
                            .consensusScore(finalScore == null ? null : finalScore.getConsensusScore())
                            .comments(finalScore == null ? "" : finalScore.getComments())
                            .advanced(finalScore != null && Objects.equals(finalScore.getAdvancedFlag(), FLAG_TRUE))
                            .build();
                })
                .toList();
        return ScoreConfirmationVO.builder()
                .roundTableId(table.getId())
                .competitionType(resolveCompetitionType(competitionMapper.selectById(table.getCompetitionId())).name())
                .tableName(table.getTableName())
                .status(table.getStatus())
                .resultVersion(version)
                .confirmedCount(resolveConfirmationConfirmedCount(table))
                .requiredCount(resolveConfirmationRequiredCount(table))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(isScoreRoundTableReady(table))
                .overrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .overrideReason(table.getConfirmationOverrideReason())
                .overrideTime(table.getConfirmationOverrideTime())
                .entries(entries)
                .build();
    }

    private RankingConfirmationVO buildRankingConfirmation(RoundTable table, RoundTableMember currentMember) {
        List<RoundResult> results = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, table.getId())
                .orderByAsc(RoundResult::getRankNo));
        Set<Long> entryIds = results.stream().map(RoundResult::getBeerEntryId).collect(Collectors.toSet());
        Map<Long, BeerEntry> entryById = roundQuerySupport.loadEntries(entryIds);
        Map<Long, String> categoryNameById = roundQuerySupport.listCategoryNames(table.getCompetitionId());
        Map<Long, EntryScanLabel> labelByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Long judgeId = currentMember == null ? null : currentMember.getJudgeAccountId();
        int version = currentResultVersion(table);
        boolean mineConfirmed = judgeId != null && roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)
                .eq(RoundTableConfirmation::getResultVersion, version)) > 0;
        List<RankingConfirmationSlotVO> slots = results.stream()
                .map(result -> {
                    BeerEntry entry = entryById.get(result.getBeerEntryId());
                    EntryScanLabel label = labelByEntryId.get(result.getBeerEntryId());
                    return RankingConfirmationSlotVO.builder()
                            .rank(result.getRankNo())
                            .label(resolveSlotLabel(result))
                            .beerEntryId(result.getBeerEntryId())
                            .uuid(entry == null ? "" : entry.getUuid())
                            .shortCode(label == null ? "" : label.getShortCode())
                            .categoryName(entry == null ? "" : categoryNameById.getOrDefault(entry.getCategoryId(), ""))
                            .style(entry == null ? "" : entry.getStyle())
                            .build();
                })
                .toList();
        return RankingConfirmationVO.builder()
                .roundTableId(table.getId())
                .tableName(table.getTableName())
                .status(table.getStatus())
                .targetMode(table.getTargetMode())
                .resultVersion(version)
                .confirmedCount(resolveRankingConfirmationConfirmedCount(table))
                .requiredCount(resolveRankingConfirmationRequiredCount(table))
                .mineConfirmed(mineConfirmed)
                .readyForConfirmation(roundValidationPolicy.isRankingRoundTableReady(table))
                .readyForFinalSubmit(isRankingConfirmationReady(table))
                .overrideFlag(Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE))
                .overrideReason(table.getConfirmationOverrideReason())
                .overrideTime(table.getConfirmationOverrideTime())
                .slots(slots)
                .build();
    }

    private List<RoundRankingSlotVO> buildMyRankingDraft(RoundTable table, Map<Long, BeerEntry> entryById, Long judgeId) {
        RoundJudgeRankingDraft draft = roundJudgeRankingDraftMapper.selectOne(new LambdaQueryWrapper<RoundJudgeRankingDraft>()
                .eq(RoundJudgeRankingDraft::getRoundTableId, table.getId())
                .eq(RoundJudgeRankingDraft::getJudgeAccountId, judgeId));
        if (draft == null || !StringUtils.hasText(draft.getRankingsJson())) {
            return emptyRankingSlots(table);
        }
        Map<Integer, RankingResultItemRequest> itemByRank = readRankingDraft(draft.getRankingsJson()).stream()
                .filter(item -> item.getRankNo() != null)
                .collect(Collectors.toMap(RankingResultItemRequest::getRankNo, Function.identity(), (left, right) -> right));
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            RankingResultItemRequest item = itemByRank.get(rank);
            BeerEntry entry = item == null ? null : entryById.get(item.getBeerEntryId());
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(item == null || !StringUtils.hasText(item.getSlotLabel()) ? defaultSlotLabel(table.getTargetMode(), rank) : item.getSlotLabel())
                    .uuid(entry == null ? "" : entry.getUuid())
                    .beerEntryId(item == null ? null : item.getBeerEntryId())
                    .build());
        }
        return slots;
    }

    private List<RoundRankingSlotVO> emptyRankingSlots(RoundTable table) {
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(defaultSlotLabel(table.getTargetMode(), rank))
                    .uuid("")
                    .beerEntryId(null)
                    .build());
        }
        return slots;
    }

    private int resolveConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .eq(RoundTableMember::getSystemTaskRequired, FLAG_TRUE)
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersion(table))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds)));
    }

    private int resolveRankingConfirmationRequiredCount(RoundTable table) {
        return Math.toIntExact(roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, table.getId())
                .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name())));
    }

    private int resolveRankingConfirmationConfirmedCount(RoundTable table) {
        Set<Long> requiredJudgeIds = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getRoundTableId, table.getId())
                        .ne(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                .stream()
                .map(RoundTableMember::getJudgeAccountId)
                .collect(Collectors.toSet());
        if (requiredJudgeIds.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getRoundTableId, table.getId())
                .eq(RoundTableConfirmation::getResultVersion, currentResultVersion(table))
                .eq(RoundTableConfirmation::getStatus, "AGREED")
                .in(RoundTableConfirmation::getJudgeAccountId, requiredJudgeIds)));
    }

    private boolean isRankingConfirmationReady(RoundTable table) {
        if (!roundValidationPolicy.isRankingRoundTableReady(table)) {
            return false;
        }
        if (Objects.equals(table.getConfirmationOverrideFlag(), FLAG_TRUE)) {
            return true;
        }
        int required = resolveRankingConfirmationRequiredCount(table);
        return required <= 0 || resolveRankingConfirmationConfirmedCount(table) >= required;
    }

    private int currentResultVersion(RoundTable table) {
        return table.getResultVersion() == null ? 0 : table.getResultVersion();
    }

    private boolean isScoreRoundTableReady(RoundTable table) {
        List<RoundTableEntry> tableEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, table.getId()));
        if (tableEntries.isEmpty()) {
            return false;
        }
        Set<Long> entryIds = tableEntries.stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, ScoreRecord> finalScoreByEntry = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getCompetitionId, table.getCompetitionId())
                        .in(ScoreRecord::getBeerEntryId, entryIds)
                        .eq(ScoreRecord::getFinalFlag, FLAG_TRUE))
                .stream()
                .collect(Collectors.toMap(ScoreRecord::getBeerEntryId, Function.identity(), (left, right) -> right));
        if (entryIds.stream().anyMatch(entryId -> !finalScoreByEntry.containsKey(entryId))) {
            return false;
        }
        if (isFeedbackOnlyCompetition(table.getCompetitionId())) {
            return true;
        }
        long advancedCount = finalScoreByEntry.values().stream()
                .filter(score -> Objects.equals(score.getAdvancedFlag(), FLAG_TRUE))
                .count();
        int targetCount = table.getTargetCount() == null ? 0 : table.getTargetCount();
        return advancedCount == targetCount;
    }

    private List<RankingResultItemRequest> readRankingDraft(String rankingsJson) {
        try {
            return objectMapper.readValue(rankingsJson, new TypeReference<List<RankingResultItemRequest>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<RoundRankingSlotVO> buildRankings(RoundTable table, List<RoundResult> results, Map<Long, BeerEntry> entryById) {
        Map<Integer, RoundResult> resultByRank = results.stream()
                .filter(result -> result.getRankNo() != null)
                .collect(Collectors.toMap(RoundResult::getRankNo, Function.identity(), (left, right) -> right));
        int count = table.getTargetCount() == null ? 0 : table.getTargetCount();
        List<RoundRankingSlotVO> slots = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            RoundResult result = resultByRank.get(rank);
            BeerEntry entry = result == null ? null : entryById.get(result.getBeerEntryId());
            slots.add(RoundRankingSlotVO.builder()
                    .rank(rank)
                    .label(result == null || !StringUtils.hasText(result.getSlotLabel()) ? defaultSlotLabel(table.getTargetMode(), rank) : result.getSlotLabel())
                    .uuid(entry == null ? "" : entry.getUuid())
                    .beerEntryId(result == null ? null : result.getBeerEntryId())
                    .build());
        }
        return slots;
    }

    private String defaultSlotLabel(String targetMode, int rank) {
        if (RoundTargetMode.MEDALS.name().equals(targetMode)) {
            return switch (rank) {
                case 1 -> SLOT_GOLD;
                case 2 -> SLOT_SILVER;
                case 3 -> SLOT_BRONZE;
                default -> "第 " + rank + " 名";
            };
        }
        if (RoundTargetMode.CHAMPION.name().equals(targetMode)) {
            return "总冠军";
        }
        return "第 " + rank + " 名";
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

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isFeedbackOnlyCompetition(Long competitionId) {
        return resolveCompetitionType(competitionMapper.selectById(competitionId)) == CompetitionType.FEEDBACK_ONLY;
    }
}
