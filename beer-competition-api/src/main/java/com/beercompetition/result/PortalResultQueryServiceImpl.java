package com.beercompetition.result;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.PortalAwardEntryVO;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalResultGroupVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import com.beercompetition.pojo.vo.PortalRoundResultVO;
import com.beercompetition.pojo.vo.PortalScoreDimensionVO;
import com.beercompetition.pojo.vo.PortalScoreRecordVO;
import com.beercompetition.service.support.AwardCertificateFileType;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.beercompetition.result.PortalResultQueryService;

/**
 * 组装已经发布的赛事结果、评分反馈和证书下载内容。
 */
@Service
@RequiredArgsConstructor
public class PortalResultQueryServiceImpl implements PortalResultQueryService {

    private final PortalAccountMapper portalAccountMapper;

    private final AwardResultMapper awardResultMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final FileAssetMapper fileAssetMapper;

    private final BreweryMapper breweryMapper;

    private final RoundResultMapper roundResultMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final ObjectMapper objectMapper;

    private final FileStorageService fileStorageService;

    @Override
    public List<PortalCompetitionResultVO> listPublishedCompetitionResults() {
        // 1) 查询正式评奖结果和风格对齐会诊断结果
        List<PortalCompetitionResultVO> results = new ArrayList<>();
        List<AwardResult> awards = listPublishedAwards(null);
        if (!awards.isEmpty()) {
            PublishedResultContext context = buildPublishedResultContext(awards);
            results.addAll(awards.stream()
                    .map(AwardResult::getCompetitionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(competitionId -> !isCompetitionArchived(competitionId))
                    .map(competitionId -> buildCompetitionResult(competitionId, awards, context))
                    .filter(Objects::nonNull)
                    .toList());
        }
        results.addAll(listPublishedFeedbackOnlyCompetitionResults(null));

        // 2) 按发布时间倒序返回公开结果
        return results.stream()
                .sorted(Comparator.comparing(
                        PortalCompetitionResultVO::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public PortalCompetitionResultVO getPublishedCompetitionResult(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null || CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new ResourceNotFoundException("暂无已发布赛事结果");
        }
        if (resolveCompetitionType(competition) == CompetitionType.FEEDBACK_ONLY) {
            return listPublishedFeedbackOnlyCompetitionResults(competitionId).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("暂无已发布赛事结果"));
        }

        // 1) 查询指定赛事的已发布奖项
        List<AwardResult> awards = listPublishedAwards(competitionId);
        if (awards.isEmpty()) {
            throw new ResourceNotFoundException("暂无已发布赛事结果");
        }

        // 2) 批量读取关联赛事、酒款、厂牌和组别
        PublishedResultContext context = buildPublishedResultContext(awards);

        // 3) 组装并返回公开获奖结果
        PortalCompetitionResultVO result = buildCompetitionResult(competitionId, awards, context);
        if (result == null) {
            throw new ResourceNotFoundException("暂无已发布赛事结果");
        }
        return result;
    }

    @Override
    public List<PortalResultSummaryVO> listPortalResults() {
        // 1) 查询当前厂商作品
        PortalAccount account = requirePortalAccount();

        // 2) 组装成绩列表摘要
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getBreweryId, account.getBreweryId())
                        .orderByDesc(BeerEntry::getId))
                .stream()
                .filter(entry -> !isCompetitionArchived(entry.getCompetitionId()))
                .map(this::toResultSummaryVO)
                .toList();
    }

    @Override
    public PortalResultDetailVO getPortalResultDetail(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        assertCompetitionNotArchived(entry.getCompetitionId());
        PortalResultSummaryVO summary = toResultSummaryVO(entry);

        // 2) 未发布时只返回锁定摘要
        if (!Boolean.TRUE.equals(summary.getPublished())) {
            return PortalResultDetailVO.builder()
                    .summary(summary)
                    .scores(List.of())
                    .roundResults(List.of())
                    .build();
        }

        // 3) 读取评分和轮次结果
        List<PortalScoreRecordVO> scores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getBeerEntryId, entry.getId())
                        .orderByAsc(ScoreRecord::getFinalFlag)
                        .orderByAsc(ScoreRecord::getJudgeRoleType)
                        .orderByAsc(ScoreRecord::getId))
                .stream()
                .map(this::toPortalScoreRecordVO)
                .toList();
        List<PortalRoundResultVO> roundResults = listRoundResults(entry.getId());

        // 4) 组装成绩详情
        return PortalResultDetailVO.builder()
                .summary(summary)
                .scores(scores)
                .roundResults(roundResults)
                .build();
    }

    @Override
    public FileDownloadVO downloadPortalResultCertificate(Long entryId) {
        // 1) 查询并校验当前厂商酒款
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        if (!isResultPublished(competition, entry)) {
            throw new ResourceNotFoundException("奖状暂未开放下载");
        }

        // 2) 查询已发布奖项和奖状文件
        AwardResult award = findDownloadableAward(entry.getId());
        FileAsset asset = requireCertificateAsset(award);

        // 3) 读取并返回文件内容
        return FileDownloadVO.builder()
                .fileName(resolveCertificateFilename(award, asset))
                .contentType(AwardCertificateFileType.resolveContentType(resolveCertificateFilename(award, asset)))
                .content(fileStorageService.download(asset.getStoragePath()))
                .build();
    }

    private List<AwardResult> listPublishedAwards(Long competitionId) {
        return awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                .eq(competitionId != null, AwardResult::getCompetitionId, competitionId)
                .eq(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name())
                .orderByDesc(AwardResult::getPublishedTime)
                .orderByDesc(AwardResult::getCompetitionId)
                .orderByDesc(AwardResult::getChampionFlag)
                .orderByAsc(AwardResult::getCategoryId)
                .orderByAsc(AwardResult::getRankNo)
                .orderByAsc(AwardResult::getId));
    }

    private PublishedResultContext buildPublishedResultContext(List<AwardResult> awards) {
        Set<Long> competitionIds = awards.stream()
                .map(AwardResult::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> beerEntryIds = awards.stream()
                .map(AwardResult::getBeerEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return buildPublishedResultContext(competitionIds, beerEntryIds);
    }

    private PublishedResultContext buildPublishedResultContext(Set<Long> competitionIds, Set<Long> beerEntryIds) {
        Map<Long, Competition> competitionById = competitionIds.isEmpty()
                ? Map.of()
                : competitionMapper.selectBatchIds(competitionIds).stream()
                        .collect(Collectors.toMap(Competition::getId, Function.identity(), (left, right) -> left));
        Map<Long, BeerEntry> entryById = beerEntryIds.isEmpty()
                ? Map.of()
                : beerEntryMapper.selectBatchIds(beerEntryIds).stream()
                        .collect(Collectors.toMap(BeerEntry::getId, Function.identity(), (left, right) -> left));
        Set<Long> breweryIds = entryById.values().stream()
                .map(BeerEntry::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> categoryIds = entryById.values().stream()
                .map(BeerEntry::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Brewery> breweryById = breweryIds.isEmpty()
                ? Map.of()
                : breweryMapper.selectBatchIds(breweryIds).stream()
                        .collect(Collectors.toMap(Brewery::getId, Function.identity(), (left, right) -> left));
        Map<Long, CompetitionCategory> categoryById = categoryIds.isEmpty()
                ? Map.of()
                : competitionCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(CompetitionCategory::getId, Function.identity(), (left, right) -> left));
        return new PublishedResultContext(competitionById, entryById, breweryById, categoryById);
    }

    private List<PortalCompetitionResultVO> listPublishedFeedbackOnlyCompetitionResults(Long competitionId) {
        // 1) 查询已发布的风格对齐会
        List<Competition> competitions = competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                .eq(competitionId != null, Competition::getId, competitionId)
                .eq(Competition::getCompetitionType, CompetitionType.FEEDBACK_ONLY.name())
                .eq(Competition::getStatus, CompetitionStatus.PUBLISHED.name())
                .orderByDesc(Competition::getUpdateTime)
                .orderByDesc(Competition::getId));
        if (competitions.isEmpty()) {
            return List.of();
        }
        Set<Long> competitionIds = competitions.stream()
                .map(Competition::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 2) 查询首轮诊断结果
        List<RoundResult> evaluatedResults = roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .in(RoundResult::getCompetitionId, competitionIds)
                .eq(RoundResult::getResultType, RoundResultType.EVALUATED.name())
                .eq(RoundResult::getLockedFlag, 1)
                .orderByDesc(RoundResult::getId));
        if (evaluatedResults.isEmpty()) {
            return List.of();
        }
        Set<Long> beerEntryIds = evaluatedResults.stream()
                .map(RoundResult::getBeerEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        PublishedResultContext context = buildPublishedResultContext(competitionIds, beerEntryIds);

        // 3) 按赛事组装公开诊断名单
        Map<Long, List<RoundResult>> resultsByCompetition = evaluatedResults.stream()
                .collect(Collectors.groupingBy(RoundResult::getCompetitionId));
        return competitions.stream()
                .map(competition -> buildFeedbackOnlyCompetitionResult(
                        competition,
                        resultsByCompetition.getOrDefault(competition.getId(), List.of()),
                        context))
                .filter(Objects::nonNull)
                .toList();
    }

    private PortalCompetitionResultVO buildCompetitionResult(Long competitionId, List<AwardResult> allAwards,
                                                            PublishedResultContext context) {
        Competition competition = context.competitionById().get(competitionId);
        if (competition == null) {
            return null;
        }
        List<AwardResult> competitionAwards = allAwards.stream()
                .filter(award -> Objects.equals(award.getCompetitionId(), competitionId))
                .toList();
        List<PortalAwardEntryVO> entries = competitionAwards.stream()
                .map(award -> toPortalAwardEntryVO(award, context))
                .filter(Objects::nonNull)
                .toList();
        if (entries.isEmpty()) {
            return null;
        }
        Map<Long, PortalResultGroupVO> groups = new LinkedHashMap<>();
        entries.forEach(entry -> {
            if (entry.getGroupId() != null) {
                groups.putIfAbsent(entry.getGroupId(), PortalResultGroupVO.builder()
                        .id(entry.getGroupId())
                        .name(firstText(entry.getGroupName(), "未分组"))
                        .build());
            }
        });
        return PortalCompetitionResultVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .matchDate(competition.getCompetitionDate())
                .publishedAt(resolvePublishedAt(competitionAwards))
                .groups(new ArrayList<>(groups.values()))
                .entries(entries)
                .build();
    }

    private PortalCompetitionResultVO buildFeedbackOnlyCompetitionResult(Competition competition,
                                                                        List<RoundResult> results,
                                                                        PublishedResultContext context) {
        if (competition == null || results.isEmpty()) {
            return null;
        }
        List<PortalAwardEntryVO> entries = results.stream()
                .map(result -> toPortalFeedbackEntryVO(result, context))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        PortalAwardEntryVO::getGroupName,
                        Comparator.nullsLast(String::compareTo))
                        .thenComparing(PortalAwardEntryVO::getBeerName, Comparator.nullsLast(String::compareTo)))
                .toList();
        if (entries.isEmpty()) {
            return null;
        }
        Map<Long, PortalResultGroupVO> groups = new LinkedHashMap<>();
        entries.forEach(entry -> {
            if (entry.getGroupId() != null) {
                groups.putIfAbsent(entry.getGroupId(), PortalResultGroupVO.builder()
                        .id(entry.getGroupId())
                        .name(firstText(entry.getGroupName(), "未分组"))
                        .build());
            }
        });
        return PortalCompetitionResultVO.builder()
                .id(competition.getId())
                .code(competition.getCode())
                .name(competition.getName())
                .competitionType(CompetitionType.FEEDBACK_ONLY.name())
                .matchDate(competition.getCompetitionDate())
                .publishedAt(competition.getUpdateTime())
                .groups(new ArrayList<>(groups.values()))
                .entries(entries)
                .build();
    }

    private PortalAwardEntryVO toPortalAwardEntryVO(AwardResult award, PublishedResultContext context) {
        BeerEntry entry = context.entryById().get(award.getBeerEntryId());
        if (entry == null) {
            return null;
        }
        Brewery brewery = context.breweryById().get(entry.getBreweryId());
        CompetitionCategory category = context.categoryById().get(entry.getCategoryId());
        return PortalAwardEntryVO.builder()
                .id(entry.getId())
                .awardResultId(award.getId())
                .resultType(award.getAwardType())
                .slotLabel(award.getAwardName())
                .awardType(award.getAwardType())
                .awardName(award.getAwardName())
                .rankNo(award.getRankNo())
                .champion(Objects.equals(award.getChampionFlag(), 1))
                .beerEntryId(entry.getId())
                .beerName(entry.getName())
                .breweryName(brewery == null ? null : brewery.getCompanyName())
                .groupId(entry.getCategoryId())
                .groupName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .build();
    }

    private PortalAwardEntryVO toPortalFeedbackEntryVO(RoundResult result, PublishedResultContext context) {
        BeerEntry entry = context.entryById().get(result.getBeerEntryId());
        if (entry == null || EntryStatus.CANCELED.name().equals(entry.getStatus())) {
            return null;
        }
        Brewery brewery = context.breweryById().get(entry.getBreweryId());
        CompetitionCategory category = context.categoryById().get(entry.getCategoryId());
        return PortalAwardEntryVO.builder()
                .id(entry.getId())
                .roundResultId(result.getId())
                .resultType(result.getResultType())
                .slotLabel("风格诊断")
                .champion(false)
                .beerEntryId(entry.getId())
                .beerName(entry.getName())
                .breweryName(brewery == null ? null : brewery.getCompanyName())
                .groupId(entry.getCategoryId())
                .groupName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .build();
    }

    private LocalDateTime resolvePublishedAt(List<AwardResult> awards) {
        return awards.stream()
                .map(AwardResult::getPublishedTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private PortalResultSummaryVO toResultSummaryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        boolean published = isResultPublished(competition, entry);
        PortalRoundResultVO formalAward = published ? findPublishedAwardResult(entry.getId()) : null;
        return PortalResultSummaryVO.builder()
                .entryId(entry.getId())
                .entryName(entry.getName())
                .competitionId(entry.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionType(resolveCompetitionType(competition).name())
                .categoryName(category == null ? null : category.getName())
                .categoryEntryCount(resolveCategoryEntryCount(entry))
                .style(entry.getStyle())
                .status(entry.getStatus())
                .published(published)
                .lockReason(published ? null : "比赛结果暂未发布")
                .awardName(published && formalAward != null ? formalAward.getAwardName() : null)
                .awardType(published && formalAward != null ? formalAward.getAwardType() : null)
                .champion(published && formalAward != null && Boolean.TRUE.equals(formalAward.getChampion()))
                .certificateAvailable(published && formalAward != null && Boolean.TRUE.equals(formalAward.getCertificateAvailable()))
                .certificateFilename(published && formalAward != null ? formalAward.getCertificateFilename() : null)
                .roundResult(published ? (formalAward == null ? listRoundResults(entry.getId()).stream().findFirst().orElse(null) : formalAward) : null)
                .build();
    }

    private Integer resolveCategoryEntryCount(BeerEntry entry) {
        if (entry.getCompetitionId() == null || entry.getCategoryId() == null) {
            return 0;
        }
        return Math.toIntExact(beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, entry.getCompetitionId())
                .eq(BeerEntry::getCategoryId, entry.getCategoryId())
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())));
    }

    private PortalScoreRecordVO toPortalScoreRecordVO(ScoreRecord record) {
        return PortalScoreRecordVO.builder()
                .judgeLabel(resolveJudgeLabel(record))
                .judgeRoleType(record.getJudgeRoleType())
                .totalScore(record.getTotalScore())
                .consensusScore(record.getConsensusScore())
                .comments(record.getComments())
                .finalScore(Objects.equals(record.getFinalFlag(), 1))
                .advanced(Objects.equals(record.getAdvancedFlag(), 1))
                .dimensions(readScoreDimensions(record.getDimensionsJson()))
                .build();
    }

    private List<PortalRoundResultVO> listRoundResults(Long entryId) {
        List<PortalRoundResultVO> formalAwards = listPublishedAwardResults(entryId);
        if (!formalAwards.isEmpty()) {
            return formalAwards;
        }
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getBeerEntryId, entryId)
                        .orderByAsc(RoundResult::getResultType)
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByDesc(RoundResult::getId))
                .stream()
                .map(result -> PortalRoundResultVO.builder()
                        .resultType(result.getResultType())
                        .rankNo(result.getRankNo())
                        .slotLabel(RoundResultType.EVALUATED.name().equals(result.getResultType()) ? "风格诊断" : result.getSlotLabel())
                        .locked(Objects.equals(result.getLockedFlag(), 1))
                        .build())
                .toList();
    }

    private PortalRoundResultVO findPublishedAwardResult(Long entryId) {
        return listPublishedAwardResults(entryId).stream().findFirst().orElse(null);
    }

    private List<PortalRoundResultVO> listPublishedAwardResults(Long entryId) {
        return awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                        .eq(AwardResult::getBeerEntryId, entryId)
                        .eq(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name())
                        .orderByDesc(AwardResult::getChampionFlag)
                        .orderByAsc(AwardResult::getRankNo)
                        .orderByAsc(AwardResult::getId))
                .stream()
                .map(result -> PortalRoundResultVO.builder()
                        .resultType(result.getAwardType())
                        .rankNo(result.getRankNo())
                        .slotLabel(result.getAwardName())
                        .awardType(result.getAwardType())
                        .awardName(result.getAwardName())
                        .champion(Objects.equals(result.getChampionFlag(), 1))
                        .certificateAvailable(result.getCertificateAssetId() != null)
                        .certificateFilename(result.getCertificateFilename())
                        .locked(true)
                        .build())
                .toList();
    }

    private AwardResult findDownloadableAward(Long entryId) {
        AwardResult award = awardResultMapper.selectOne(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, entryId)
                .eq(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name())
                .isNotNull(AwardResult::getCertificateAssetId)
                .orderByDesc(AwardResult::getChampionFlag)
                .orderByAsc(AwardResult::getRankNo)
                .orderByAsc(AwardResult::getId)
                .last("LIMIT 1"));
        if (award == null) {
            throw new ResourceNotFoundException("奖状暂未上传");
        }
        return award;
    }

    private FileAsset requireCertificateAsset(AwardResult award) {
        FileAsset asset = fileAssetMapper.selectById(award.getCertificateAssetId());
        if (asset == null) {
            throw new ResourceNotFoundException("奖状文件不存在");
        }
        return asset;
    }

    private String resolveCertificateFilename(AwardResult award, FileAsset asset) {
        if (StringUtils.hasText(award.getCertificateFilename())) {
            return award.getCertificateFilename();
        }
        return StringUtils.hasText(asset.getFileName()) ? asset.getFileName() : "certificate.pdf";
    }

    private List<PortalScoreDimensionVO> readScoreDimensions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
            return items.stream()
                    .map(item -> PortalScoreDimensionVO.builder()
                            .key(asString(item.get("key")))
                            .label(asString(item.get("label")))
                            .score(asBigDecimal(item.get("score")))
                            .maxScore(asBigDecimal(item.get("maxScore")))
                            .note(asString(item.get("note")))
                            .build())
                    .toList();
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析评分明细失败");
        }
    }

    private BeerEntry requireOwnedEntry(Long entryId, Long breweryId) {
        BeerEntry entry = requireEntry(entryId);
        if (!entry.getBreweryId().equals(breweryId)) {
            throw new ForbiddenException("无权查看该酒款");
        }
        return entry;
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
    }

    private boolean isResultPublished(Competition competition, BeerEntry entry) {
        return EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())
                || (competition != null && CompetitionStatus.PUBLISHED.name().equals(competition.getStatus()));
    }

    private CompetitionType resolveCompetitionType(Competition competition) {
        return CompetitionType.of(competition == null ? null : competition.getCompetitionType());
    }

    private boolean isCompetitionArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        return competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus());
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        if (isCompetitionArchived(competitionId)) {
            throw new ResourceNotFoundException("赛事不存在");
        }
    }

    private String resolveJudgeLabel(ScoreRecord record) {
        String role = switch (record.getJudgeRoleType()) {
            case "CAPTAIN" -> "桌长";
            case "PROFESSIONAL" -> "专业评审";
            case "CROSS" -> "跨界评审";
            default -> "评审";
        };
        return role + " #" + record.getId();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BaseException("数字格式不正确");
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record PublishedResultContext(
            Map<Long, Competition> competitionById,
            Map<Long, BeerEntry> entryById,
            Map<Long, Brewery> breweryById,
            Map<Long, CompetitionCategory> categoryById
    ) {
    }
}
