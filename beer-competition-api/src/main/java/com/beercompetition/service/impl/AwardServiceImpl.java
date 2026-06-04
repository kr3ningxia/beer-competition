package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.AwardRuleMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.pojo.dto.AwardConfirmItemRequest;
import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.AwardType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.AwardRule;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.service.AwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {

    private static final int FLAG_TRUE = 1;
    private static final int FLAG_FALSE = 0;
    private static final String GOLD = "金奖";
    private static final String SILVER = "银奖";
    private static final String BRONZE = "铜奖";
    private static final String CHAMPION = "总冠军";

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundResultMapper roundResultMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final AwardRuleMapper awardRuleMapper;
    private final AwardResultMapper awardResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AwardRuleVO> listAwardRules(Long competitionId) {
        requireCompetition(competitionId);
        ensureDefaultRules(competitionId);
        Map<Long, String> categoryNameById = categoryNameById(competitionId);
        return listRules(competitionId).stream()
                .map(rule -> toRuleVO(rule, categoryNameById))
                .toList();
    }

    @Override
    public List<AwardResultVO> listAwardResults(Long competitionId) {
        requireCompetition(competitionId);
        return toResultVOs(listPersistedResults(competitionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AwardResultVO> generateAwardDrafts(Long competitionId) {
        // 1) 查询比赛、默认规则与最终锁定轮次
        Competition competition = requireCompetition(competitionId);
        if (!CompetitionStatus.RESULT_CONFIRMING.name().equals(competition.getStatus())) {
            throw new BaseException("请先锁定最终结果轮次，再生成奖项");
        }
        ensureDefaultRules(competitionId);
        if (hasPublishedAwards(competitionId)) {
            throw new BaseException("结果已发布，不能重新生成奖项");
        }
        CompetitionRound medalRound = findLastLockedFinalRound(competitionId, RoundTargetMode.MEDALS.name());
        CompetitionRound championRound = findLastLockedFinalRound(competitionId, RoundTargetMode.CHAMPION.name());
        if (medalRound == null) {
            throw new BaseException("没有已锁定的奖项轮次");
        }
        if (championRound == null) {
            throw new BaseException("请先完成并锁定跨类总冠军轮次");
        }

        // 2) 从奖项轮次写入可调整草稿
        awardResultMapper.delete(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCompetitionId, competitionId)
                .ne(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name()));
        List<AwardResult> drafts = new ArrayList<>();
        drafts.addAll(buildDraftResults(competitionId, medalRound, RoundTargetMode.MEDALS.name()));
        if (championRound != null && !championRound.getId().equals(medalRound.getId())) {
            drafts.addAll(buildDraftResults(competitionId, championRound, RoundTargetMode.CHAMPION.name()));
        } else if (championRound != null) {
            drafts.addAll(buildDraftResults(competitionId, championRound, RoundTargetMode.CHAMPION.name()));
        }
        if (drafts.isEmpty()) {
            throw new BaseException("最终轮次没有可生成的奖项结果");
        }
        validateMedalDraftCompleteness(drafts);
        if (drafts.stream().noneMatch(result -> Objects.equals(result.getChampionFlag(), FLAG_TRUE))) {
            throw new BaseException("跨类总冠军轮次没有总冠军结果");
        }
        drafts.forEach(awardResultMapper::insert);
        return toResultVOs(drafts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AwardResultVO> confirmAwards(Long competitionId, AwardConfirmRequest request) {
        // 1) 校验比赛阶段和奖项结果
        Competition competition = requireCompetition(competitionId);
        if (!CompetitionStatus.RESULT_CONFIRMING.name().equals(competition.getStatus())) {
            throw new BaseException("只有结果确认阶段可以确认奖项");
        }
        if (hasPublishedAwards(competitionId)) {
            throw new BaseException("结果已发布，不能调整奖项");
        }
        ensureDefaultRules(competitionId);
        validateConfirmItems(competitionId, request.getAwards());

        // 2) 覆盖保存已确认奖项
        awardResultMapper.delete(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCompetitionId, competitionId)
                .ne(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name()));
        LocalDateTime now = LocalDateTime.now();
        Long adminId = BaseContext.getCurrentId();
        List<AwardResult> confirmed = request.getAwards().stream()
                .map(item -> toConfirmedResult(competitionId, item, adminId, now))
                .toList();
        confirmed.forEach(awardResultMapper::insert);
        return toResultVOs(confirmed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAwards(Long competitionId) {
        // 1) 校验确认结果
        Competition competition = requireCompetition(competitionId);
        if (!CompetitionStatus.RESULT_CONFIRMING.name().equals(competition.getStatus())) {
            throw new BaseException("请先进入结果确认阶段");
        }
        List<AwardResult> results = listPersistedResults(competitionId);
        if (results.isEmpty()) {
            throw new BaseException("请先生成并确认奖项结果");
        }
        if (results.stream().anyMatch(result -> !AwardResultStatus.CONFIRMED.name().equals(result.getStatus()))) {
            throw new BaseException("所有奖项都需要确认后才能发布");
        }
        validateChampionReady(results);

        // 2) 发布奖项并写回酒款结果状态
        LocalDateTime now = LocalDateTime.now();
        results.forEach(result -> {
            result.setStatus(AwardResultStatus.PUBLISHED.name());
            result.setPublishedTime(now);
            awardResultMapper.updateById(result);
        });
        beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getCompetitionId, competitionId)
                        .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name()))
                .forEach(entry -> {
                    entry.setStatus(EntryStatus.RESULT_PUBLISHED.name());
                    beerEntryMapper.updateById(entry);
                });
    }

    private List<AwardResult> buildDraftResults(Long competitionId, CompetitionRound finalRound, String targetModeFilter) {
        Map<Long, RoundTable> tableById = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                        .eq(RoundTable::getRoundId, finalRound.getId()))
                .stream()
                .collect(Collectors.toMap(RoundTable::getId, Function.identity()));
        List<RoundResult> sourceResults = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getCompetitionId, competitionId)
                .eq(RoundResult::getRoundId, finalRound.getId())
                .eq(RoundResult::getResultType, RoundResultType.RANK.name())
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
        Map<Long, BeerEntry> entryById = loadEntryById(sourceResults.stream()
                .map(RoundResult::getBeerEntryId)
                .collect(Collectors.toSet()));
        Map<String, AwardRule> ruleByKey = listRules(competitionId).stream()
                .collect(Collectors.toMap(this::ruleKey, Function.identity(), (left, right) -> left));

        List<AwardResult> drafts = new ArrayList<>();
        for (RoundResult result : sourceResults) {
            RoundTable table = tableById.get(result.getRoundTableId());
            BeerEntry entry = entryById.get(result.getBeerEntryId());
            if (table == null || entry == null) {
                continue;
            }
            if (!targetModeFilter.equals(table.getTargetMode())) {
                continue;
            }
            if (RoundTargetMode.MEDALS.name().equals(table.getTargetMode())) {
                if (table.getCategoryId() == null || !table.getCategoryId().equals(entry.getCategoryId())) {
                    throw new BaseException(table.getTableName() + "奖牌轮酒款组别不一致");
                }
                String awardName = medalName(result.getRankNo(), result.getSlotLabel());
                AwardRule rule = ruleByKey.get(ruleKey(entry.getCategoryId(), AwardType.MEDAL.name(), result.getRankNo()));
                drafts.add(newDraft(competitionId, entry, rule, AwardType.MEDAL.name(), awardName, result, FLAG_FALSE));
            }
            if (RoundTargetMode.CHAMPION.name().equals(table.getTargetMode()) && Objects.equals(result.getRankNo(), 1)) {
                AwardRule rule = ruleByKey.get(ruleKey(null, AwardType.CHAMPION.name(), 1));
                drafts.add(newDraft(competitionId, entry, rule, AwardType.CHAMPION.name(), CHAMPION, result, FLAG_TRUE));
            }
        }
        return drafts;
    }

    private void buildChampionCandidatesFromGolds(Long competitionId, List<AwardResult> drafts, Map<String, AwardRule> ruleByKey) {
        AwardRule championRule = ruleByKey.get(ruleKey(null, AwardType.CHAMPION.name(), 1));
        List<AwardResult> golds = drafts.stream()
                .filter(result -> AwardType.MEDAL.name().equals(result.getAwardType()))
                .filter(result -> Objects.equals(result.getRankNo(), 1) || GOLD.equals(result.getAwardName()))
                .toList();
        if (!golds.isEmpty()) {
            AwardResult gold = golds.get(0);
            drafts.add(newChampionDraft(competitionId, gold, championRule));
        }
    }

    private AwardResult newChampionDraft(Long competitionId, AwardResult sourceGold, AwardRule championRule) {
        return AwardResult.builder()
                .competitionId(competitionId)
                .categoryId(null)
                .beerEntryId(sourceGold.getBeerEntryId())
                .awardRuleId(championRule == null ? null : championRule.getId())
                .awardType(AwardType.CHAMPION.name())
                .awardName(CHAMPION)
                .rankNo(1)
                .sourceRoundId(sourceGold.getSourceRoundId())
                .sourceRoundTableId(sourceGold.getSourceRoundTableId())
                .sourceResultId(sourceGold.getSourceResultId())
                .championFlag(FLAG_TRUE)
                .status(AwardResultStatus.DRAFT.name())
                .build();
    }

    private void validateMedalDraftCompleteness(List<AwardResult> drafts) {
        Map<Long, List<AwardResult>> medalsByCategory = drafts.stream()
                .filter(result -> AwardType.MEDAL.name().equals(result.getAwardType()))
                .collect(Collectors.groupingBy(AwardResult::getCategoryId));
        for (Map.Entry<Long, List<AwardResult>> entry : medalsByCategory.entrySet()) {
            Set<Integer> ranks = entry.getValue().stream()
                    .map(AwardResult::getRankNo)
                    .collect(Collectors.toSet());
            if (!ranks.containsAll(Set.of(1, 2, 3)) || entry.getValue().size() != 3) {
                throw new BaseException("每个投递组别必须生成金、银、铜三个奖项");
            }
        }
    }

    private AwardResult newDraft(Long competitionId, BeerEntry entry, AwardRule rule, String awardType, String awardName,
                                 RoundResult source, int championFlag) {
        return AwardResult.builder()
                .competitionId(competitionId)
                .categoryId(AwardType.CHAMPION.name().equals(awardType) ? null : entry.getCategoryId())
                .beerEntryId(entry.getId())
                .awardRuleId(rule == null ? null : rule.getId())
                .awardType(awardType)
                .awardName(awardName)
                .rankNo(source.getRankNo())
                .sourceRoundId(source.getRoundId())
                .sourceRoundTableId(source.getRoundTableId())
                .sourceResultId(source.getId())
                .championFlag(championFlag)
                .status(AwardResultStatus.DRAFT.name())
                .build();
    }

    private void ensureDefaultRules(Long competitionId) {
        List<CompetitionCategory> categories = competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                .eq(CompetitionCategory::getCompetitionId, competitionId)
                .orderByAsc(CompetitionCategory::getSortOrder)
                .orderByAsc(CompetitionCategory::getId));
        Set<Long> activeCategoryIds = categories.stream().map(CompetitionCategory::getId).collect(Collectors.toSet());
        awardRuleMapper.selectList(new LambdaQueryWrapper<AwardRule>()
                        .eq(AwardRule::getCompetitionId, competitionId)
                        .eq(AwardRule::getAwardType, AwardType.MEDAL.name())
                        .eq(AwardRule::getEnabledFlag, FLAG_TRUE))
                .stream()
                .filter(rule -> rule.getCategoryId() == null || !activeCategoryIds.contains(rule.getCategoryId()))
                .forEach(rule -> {
                    rule.setEnabledFlag(FLAG_FALSE);
                    awardRuleMapper.updateById(rule);
                });
        Map<String, AwardRule> existing = listRules(competitionId).stream()
                .collect(Collectors.toMap(this::ruleKey, Function.identity(), (left, right) -> left));
        for (CompetitionCategory category : categories) {
            insertRuleIfAbsent(existing, competitionId, category.getId(), AwardType.MEDAL.name(), GOLD, 1, category.getSortOrder() * 10 + 1);
            insertRuleIfAbsent(existing, competitionId, category.getId(), AwardType.MEDAL.name(), SILVER, 2, category.getSortOrder() * 10 + 2);
            insertRuleIfAbsent(existing, competitionId, category.getId(), AwardType.MEDAL.name(), BRONZE, 3, category.getSortOrder() * 10 + 3);
        }
        insertRuleIfAbsent(existing, competitionId, null, AwardType.CHAMPION.name(), CHAMPION, 1, 9999);
    }

    private void insertRuleIfAbsent(Map<String, AwardRule> existing, Long competitionId, Long categoryId, String awardType,
                                    String awardName, Integer rankNo, Integer sortOrder) {
        String key = ruleKey(categoryId, awardType, rankNo);
        if (existing.containsKey(key)) {
            return;
        }
        AwardRule rule = AwardRule.builder()
                .competitionId(competitionId)
                .categoryId(categoryId)
                .awardType(awardType)
                .awardName(awardName)
                .rankNo(rankNo)
                .enabledFlag(FLAG_TRUE)
                .sortOrder(sortOrder)
                .build();
        awardRuleMapper.insert(rule);
        existing.put(key, rule);
    }

    private void validateConfirmItems(Long competitionId, List<AwardConfirmItemRequest> items) {
        Map<Long, BeerEntry> entryById = loadEntryById(items.stream()
                .map(AwardConfirmItemRequest::getBeerEntryId)
                .collect(Collectors.toSet()));
        if (items.stream().anyMatch(item -> !entryById.containsKey(item.getBeerEntryId()))) {
            throw new BaseException("存在无效获奖酒款");
        }
        Map<String, Long> slotKeys = new LinkedHashMap<>();
        Set<Long> goldEntryIds = items.stream()
                .filter(item -> AwardType.MEDAL.name().equals(item.getAwardType()))
                .filter(item -> Objects.equals(item.getRankNo(), 1) || GOLD.equals(item.getAwardName()))
                .map(AwardConfirmItemRequest::getBeerEntryId)
                .collect(Collectors.toSet());
        Map<Long, Set<Integer>> medalRanksByCategory = new LinkedHashMap<>();
        int championCount = 0;
        for (AwardConfirmItemRequest item : items) {
            AwardType type = AwardType.of(item.getAwardType());
            BeerEntry entry = entryById.get(item.getBeerEntryId());
            if (!competitionId.equals(entry.getCompetitionId())) {
                throw new BaseException("获奖酒款不属于当前比赛：" + entry.getUuid());
            }
            if (item.getRankNo() == null || item.getRankNo() <= 0) {
                throw new BaseException("奖项名次必须大于 0");
            }
            if (type == AwardType.MEDAL && item.getCategoryId() != null && !item.getCategoryId().equals(entry.getCategoryId())) {
                throw new BaseException("组别奖项酒款与组别不匹配：" + entry.getUuid());
            }
            if (type == AwardType.CHAMPION) {
                championCount++;
                if (!goldEntryIds.contains(item.getBeerEntryId())) {
                    throw new BaseException("总冠军必须从各组别金奖中选择");
                }
            } else {
                medalRanksByCategory
                        .computeIfAbsent(entry.getCategoryId(), key -> new java.util.HashSet<>())
                        .add(item.getRankNo());
            }
            String key = item.getAwardType() + ":" + (type == AwardType.CHAMPION ? "ALL" : entry.getCategoryId()) + ":" + item.getRankNo();
            if (slotKeys.putIfAbsent(key, item.getBeerEntryId()) != null) {
                throw new BaseException("同一奖项名次不能重复确认");
            }
        }
        if (championCount != 1) {
            throw new BaseException("必须确认且只能确认 1 个总冠军");
        }
        for (Set<Integer> ranks : medalRanksByCategory.values()) {
            if (!ranks.containsAll(Set.of(1, 2, 3)) || ranks.size() != 3) {
                throw new BaseException("每个投递组别必须确认金、银、铜三个奖项");
            }
        }
    }

    private AwardResult toConfirmedResult(Long competitionId, AwardConfirmItemRequest item, Long adminId, LocalDateTime now) {
        AwardType type = AwardType.of(item.getAwardType());
        BeerEntry entry = beerEntryMapper.selectById(item.getBeerEntryId());
        return AwardResult.builder()
                .competitionId(competitionId)
                .categoryId(type == AwardType.CHAMPION ? null : entry.getCategoryId())
                .beerEntryId(item.getBeerEntryId())
                .awardRuleId(item.getAwardRuleId())
                .awardType(type.name())
                .awardName(item.getAwardName().trim())
                .rankNo(item.getRankNo())
                .sourceRoundId(item.getSourceRoundId())
                .sourceRoundTableId(item.getSourceRoundTableId())
                .sourceResultId(item.getSourceResultId())
                .championFlag(type == AwardType.CHAMPION ? FLAG_TRUE : FLAG_FALSE)
                .confirmedBy(adminId)
                .confirmedTime(now)
                .status(AwardResultStatus.CONFIRMED.name())
                .build();
    }

    private void validateChampionReady(List<AwardResult> results) {
        long championCount = results.stream()
                .filter(result -> AwardType.CHAMPION.name().equals(result.getAwardType()))
                .count();
        if (championCount != 1) {
            throw new BaseException("发布前必须确认 1 个总冠军");
        }
    }

    private boolean hasPublishedAwards(Long competitionId) {
        return awardResultMapper.selectCount(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCompetitionId, competitionId)
                .eq(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name())) > 0;
    }

    private List<AwardRule> listRules(Long competitionId) {
        return awardRuleMapper.selectList(new LambdaQueryWrapper<AwardRule>()
                .eq(AwardRule::getCompetitionId, competitionId)
                .eq(AwardRule::getEnabledFlag, FLAG_TRUE)
                .orderByAsc(AwardRule::getSortOrder)
                .orderByAsc(AwardRule::getId));
    }

    private List<AwardResult> listPersistedResults(Long competitionId) {
        return awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCompetitionId, competitionId)
                .orderByAsc(AwardResult::getAwardType)
                .orderByAsc(AwardResult::getCategoryId)
                .orderByAsc(AwardResult::getRankNo)
                .orderByAsc(AwardResult::getId));
    }

    private CompetitionRound findLastLockedFinalRound(Long competitionId, String targetMode) {
        List<CompetitionRound> rounds = competitionRoundMapper.selectList(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, competitionId)
                .eq(CompetitionRound::getRoundType, RoundType.RANKING.name())
                .eq(CompetitionRound::getStatus, RoundStatus.LOCKED.name())
                .orderByDesc(CompetitionRound::getRoundNo)
                .orderByDesc(CompetitionRound::getId));
        return rounds.stream()
                .filter(round -> hasTargetMode(round, targetMode))
                .findFirst()
                .orElse(null);
    }

    private boolean hasTargetMode(CompetitionRound round, String targetMode) {
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getRoundId, round.getId()));
        return !tables.isEmpty() && tables.stream().anyMatch(table -> targetMode.equals(table.getTargetMode()));
    }

    private List<AwardResultVO> toResultVOs(List<AwardResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNameById = categoryNameById(results.get(0).getCompetitionId());
        Map<Long, BeerEntry> entryById = loadEntryById(results.stream().map(AwardResult::getBeerEntryId).collect(Collectors.toSet()));
        Map<Long, CompetitionRound> roundById = loadRoundById(results.stream().map(AwardResult::getSourceRoundId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, RoundTable> tableById = loadTableById(results.stream().map(AwardResult::getSourceRoundTableId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return results.stream()
                .sorted(Comparator.comparing(AwardResult::getAwardType)
                        .thenComparing(result -> result.getCategoryId() == null ? Long.MAX_VALUE : result.getCategoryId())
                        .thenComparing(result -> result.getRankNo() == null ? Integer.MAX_VALUE : result.getRankNo()))
                .map(result -> {
                    BeerEntry entry = entryById.get(result.getBeerEntryId());
                    CompetitionRound round = roundById.get(result.getSourceRoundId());
                    RoundTable table = tableById.get(result.getSourceRoundTableId());
                    return AwardResultVO.builder()
                            .id(result.getId())
                            .competitionId(result.getCompetitionId())
                            .categoryId(result.getCategoryId())
                            .categoryName(result.getCategoryId() == null ? "全场" : categoryNameById.getOrDefault(result.getCategoryId(), "-"))
                            .beerEntryId(result.getBeerEntryId())
                            .uuid(entry == null ? null : entry.getUuid())
                            .beerName(entry == null ? null : entry.getName())
                            .style(entry == null ? null : entry.getStyle())
                            .awardRuleId(result.getAwardRuleId())
                            .awardType(result.getAwardType())
                            .awardName(result.getAwardName())
                            .rankNo(result.getRankNo())
                            .sourceRoundId(result.getSourceRoundId())
                            .sourceRoundName(round == null ? null : round.getRoundName())
                            .sourceRoundTableId(result.getSourceRoundTableId())
                            .sourceTableName(table == null ? null : table.getTableName())
                            .sourceResultId(result.getSourceResultId())
                            .champion(Objects.equals(result.getChampionFlag(), FLAG_TRUE))
                            .status(result.getStatus())
                            .build();
                })
                .toList();
    }

    private AwardRuleVO toRuleVO(AwardRule rule, Map<Long, String> categoryNameById) {
        return AwardRuleVO.builder()
                .id(rule.getId())
                .competitionId(rule.getCompetitionId())
                .categoryId(rule.getCategoryId())
                .categoryName(rule.getCategoryId() == null ? "全场" : categoryNameById.getOrDefault(rule.getCategoryId(), "-"))
                .awardType(rule.getAwardType())
                .awardName(rule.getAwardName())
                .rankNo(rule.getRankNo())
                .enabled(Objects.equals(rule.getEnabledFlag(), FLAG_TRUE))
                .sortOrder(rule.getSortOrder())
                .build();
    }

    private Map<Long, String> categoryNameById(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, CompetitionCategory::getName, (left, right) -> left));
    }

    private Map<Long, BeerEntry> loadEntryById(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return beerEntryMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(BeerEntry::getId, Function.identity()));
    }

    private Map<Long, CompetitionRound> loadRoundById(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return competitionRoundMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(CompetitionRound::getId, Function.identity()));
    }

    private Map<Long, RoundTable> loadTableById(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return roundTableMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(RoundTable::getId, Function.identity()));
    }

    private Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private String medalName(Integer rankNo, String slotLabel) {
        if (StringUtils.hasText(slotLabel)) {
            return slotLabel.trim();
        }
        return switch (rankNo == null ? 0 : rankNo) {
            case 1 -> GOLD;
            case 2 -> SILVER;
            case 3 -> BRONZE;
            default -> "第 " + rankNo + " 名";
        };
    }

    private String ruleKey(AwardRule rule) {
        return ruleKey(rule.getCategoryId(), rule.getAwardType(), rule.getRankNo());
    }

    private String ruleKey(Long categoryId, String awardType, Integer rankNo) {
        return awardType + ":" + (categoryId == null ? "ALL" : categoryId) + ":" + rankNo;
    }
}
