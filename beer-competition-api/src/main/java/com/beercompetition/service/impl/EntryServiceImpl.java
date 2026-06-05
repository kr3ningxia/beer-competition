package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.EntryDeliveryMethod;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.JudgeTaskType;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.EntryDeliveryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.EntryPaymentVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import com.beercompetition.pojo.vo.PortalRoundResultVO;
import com.beercompetition.pojo.vo.PortalScoreDimensionVO;
import com.beercompetition.pojo.vo.PortalScoreRecordVO;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {

    private static final Set<String> LABEL_ALLOWED_STATUSES = Set.of(
            EntryStatus.REGISTERED.name(),
            EntryStatus.STORED.name(),
            EntryStatus.RESULT_PUBLISHED.name()
    );
    private static final Set<String> OPTION_FIELD_TYPES = Set.of("select", "multi_select");
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final PortalAccountMapper portalAccountMapper;
    private final AwardResultMapper awardResultMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final EntryFieldConfigMapper entryFieldConfigMapper;
    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    private final FileAssetMapper fileAssetMapper;
    private final BreweryMapper breweryMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundResultMapper roundResultMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final CompetitionService competitionService;
    private final EntryScanLabelService entryScanLabelService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Override
    public List<EntrySummaryVO> listPortalEntries() {
        // 1) 查询当前厂商账号
        PortalAccount account = requirePortalAccount();

        // 2) 查询当前厂商全部作品
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getBreweryId, account.getBreweryId())
                        .orderByDesc(BeerEntry::getId))
                .stream()
                .map(this::toEntrySummaryVO)
                .toList();
    }

    @Override
    public EntryDetailVO getPortalEntry(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());

        // 2) 组装作品详情
        return toEntryDetailVO(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request) {
        // 1) 参数规范化与前置校验
        PortalAccount account = requirePortalAccount();
        Competition competition = requireOpenCompetition(competitionId);
        CompetitionCategory category = requireCompetitionCategory(competitionId, request.getCategoryId());
        requireCompetitionStyle(competitionId, request.getStyle());
        List<EntryFieldConfig> fieldConfigs = listEntryFieldConfigs(competitionId);
        Map<String, String> normalizedExtraFields = normalizeExtraFields(fieldConfigs, request.getExtraFields());

        // 2) 创建作品主记录
        BeerEntry entry = BeerEntry.builder()
                .uuid(generateEntryUuid())
                .competitionId(competition.getId())
                .breweryId(account.getBreweryId())
                .categoryId(category.getId())
                .name(normalizeRequired(request.getName(), "酒款名称不能为空"))
                .style(normalizeRequired(request.getStyle(), "基础风格不能为空"))
                .abv(request.getAbv())
                .description(normalizeRequired(request.getDescription(), "酒款简介不能为空"))
                .extraFieldsJson(writeJson(normalizedExtraFields))
                .status(EntryStatus.PENDING_PAYMENT.name())
                .storedFlag(0)
                .build();
        beerEntryMapper.insert(entry);
        entryScanLabelService.createActiveLabel(entry, BaseContext.getCurrentId());

        // 3) 初始化支付与送样记录
        entryPaymentMapper.insert(EntryPayment.builder()
                .beerEntryId(entry.getId())
                .amount(competition.getEntryFee())
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .build());
        entryDeliveryMapper.insert(EntryDelivery.builder()
                .beerEntryId(entry.getId())
                .deliveryStatus(EntryDeliveryStatus.NOT_SUBMITTED.name())
                .build());

        // 4) 写入赛事配置内的补充字段
        for (EntryFieldConfig fieldConfig : fieldConfigs) {
            String value = normalizedExtraFields.get(fieldConfig.getFieldKey());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            beerEntryExtraFieldMapper.insert(BeerEntryExtraField.builder()
                    .beerEntryId(entry.getId())
                    .fieldKey(fieldConfig.getFieldKey())
                    .fieldLabel(fieldConfig.getFieldLabel())
                    .fieldValue(value)
                    .build());
        }

        // 5) 返回新作品详情
        return toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO submitPortalEntryDelivery(Long entryId, PortalEntryDeliverySubmitRequest request) {
        // 1) 参数规范化与前置校验
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            throw new BaseException("付款确认后才能提交送样信息");
        }
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());
        String deliveryMethod = normalizeDeliveryMethod(request.getDeliveryMethod());
        String carrier = normalizeNullable(request.getCarrier());
        String trackingNo = normalizeNullable(request.getTrackingNo());
        String deliveryNote = normalizeNullable(request.getDeliveryNote());
        validateDeliveryRequest(deliveryMethod, carrier, trackingNo);

        // 2) 更新送样信息
        delivery.setDeliveryMethod(deliveryMethod);
        delivery.setCarrier(carrier);
        delivery.setTrackingNo(trackingNo);
        delivery.setDeliveryNote(deliveryNote);
        delivery.setDeliveryStatus(EntryDeliveryStatus.SUBMITTED.name());
        delivery.setSubmittedTime(LocalDateTime.now());
        entryDeliveryMapper.updateById(delivery);

        // 3) 返回更新后的酒款详情
        return toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    @Override
    public PortalEntryLabelVO getPortalEntryLabel(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            throw new BaseException("付款确认后才能下载现场标签");
        }

        // 2) 组装标签数据
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        return PortalEntryLabelVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .name(entry.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .build();
    }

    @Override
    public PortalProfileVO getPortalProfile() {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());

        // 2) 组装资料视图
        return toPortalProfileVO(account, brewery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProfileVO updatePortalProfile(PortalProfileUpdateRequest request) {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }

        // 2) 更新当前数据库已有字段
        account.setDisplayName(normalizeRequired(request.getDisplayName(), "账号名称不能为空"));
        account.setWechat(normalizeNullable(request.getWechat()));
        portalAccountMapper.updateById(account);

        brewery.setCompanyName(normalizeRequired(request.getCompanyName(), "厂牌名称不能为空"));
        brewery.setContactName(normalizeRequired(request.getContactName(), "联系人不能为空"));
        brewery.setWechat(normalizeNullable(request.getWechat()));
        breweryMapper.updateById(brewery);

        // 3) 返回更新后的资料
        return toPortalProfileVO(portalAccountMapper.selectById(account.getId()), breweryMapper.selectById(brewery.getId()));
    }

    @Override
    public PortalMyParticipationVO getPortalMyParticipation() {
        // 1) 查询当前资料和参赛作品
        PortalProfileVO profile = getPortalProfile();
        List<EntrySummaryVO> entries = listPortalEntries();

        // 2) 按厂商作品关联公开赛事
        Set<Long> competitionIds = entries.stream()
                .map(EntrySummaryVO::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PortalCompetitionVO> competitions = competitionService.listPortalCompetitions().stream()
                .filter(item -> competitionIds.contains(item.getId()))
                .toList();

        // 3) 组装我的参赛首页数据
        return PortalMyParticipationVO.builder()
                .profile(profile)
                .entries(entries)
                .competitions(competitions)
                .build();
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
                .map(this::toResultSummaryVO)
                .toList();
    }

    @Override
    public PortalResultDetailVO getPortalResultDetail(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
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
        if (!EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())) {
            throw new ResourceNotFoundException("奖状暂未开放下载");
        }

        // 2) 查询已发布奖项和奖状文件
        AwardResult award = findDownloadableAward(entry.getId());
        FileAsset asset = requireCertificateAsset(award);

        // 3) 读取并返回文件内容
        return FileDownloadVO.builder()
                .fileName(resolveCertificateFilename(award, asset))
                .contentType(CONTENT_TYPE_PDF)
                .content(fileStorageService.download(asset.getStoragePath()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long entryId) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("只有待付款确认的酒款可以确认付款");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());

        // 2) 更新支付记录和报名状态
        payment.setStatus(EntryPaymentStatus.PAID.name());
        payment.setPayMethod(resolvePayMethod(payment.getPayMethod()));
        payment.setPaidTime(LocalDateTime.now());
        payment.setConfirmedByAdminId(BaseContext.getCurrentId());
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStored(Long entryId) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!EntryStatus.REGISTERED.name().equals(entry.getStatus())) {
            throw new BaseException("只有报名成功的酒款可以确认入库");
        }
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());

        // 2) 更新送样记录和入库状态
        delivery.setDeliveryStatus(EntryDeliveryStatus.RECEIVED.name());
        delivery.setReceivedTime(LocalDateTime.now());
        delivery.setReceivedByAdminId(BaseContext.getCurrentId());
        if (delivery.getSubmittedTime() == null) {
            delivery.setSubmittedTime(LocalDateTime.now());
        }
        entryDeliveryMapper.updateById(delivery);
        entry.setStoredFlag(1);
        entry.setStatus(EntryStatus.STORED.name());
        beerEntryMapper.updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEntry(Long entryId) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!Set.of(EntryStatus.PENDING_PAYMENT.name(), EntryStatus.REGISTERED.name()).contains(entry.getStatus())) {
            throw new BaseException("当前状态不能取消报名");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());

        // 2) 更新支付记录并标记取消
        payment.setStatus(EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())
                ? EntryPaymentStatus.CANCELED.name()
                : EntryPaymentStatus.REFUNDED.name());
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
    }

    @Override
    public JudgeEntryVO getJudgeEntry(String uuid) {
        // 1) 查询匿名作品并校验评审权限
        BeerEntry entry = beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, uuid));
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        JudgeScanContext context = requireActiveJudgeRoundEntry(entry);
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());

        // 2) 组装评审可见匿名信息
        return buildJudgeEntryVO(entry, label, context);
    }

    @Override
    public JudgeEntryVO resolveJudgeScan(String code) {
        // 1) 解析二维码令牌、现场短编号或匿名标签编码
        EntryScanLabel label = entryScanLabelService.resolveActiveLabel(code);
        BeerEntry entry = requireEntry(label.getBeerEntryId());
        JudgeScanContext context = requireActiveJudgeRoundEntry(entry);

        // 2) 组装评审可见匿名信息
        return buildJudgeEntryVO(entry, label, context);
    }

    private JudgeEntryVO buildJudgeEntryVO(BeerEntry entry, EntryScanLabel label, JudgeScanContext context) {
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        CompetitionStyleConfig style = findStyleSnapshot(entry);
        boolean scoreTableLocked = RoundStatus.LOCKED.name().equals(context.table().getStatus());
        boolean finalScoreExists = hasFinalScore(entry.getId());
        boolean canScore = canSubmitPersonalScore(context) && !finalScoreExists;
        boolean canFinalize = canFinalizeTable(context) && !scoreTableLocked;
        return JudgeEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionName(context.competition() == null ? null : context.competition().getName())
                .roundId(context.round().getId())
                .roundName(context.round().getRoundName())
                .roundType(context.round().getRoundType())
                .roundTableId(context.table().getId())
                .tableName(context.table().getTableName())
                .judgeRoleType(context.member().getRole())
                .taskType(context.taskType())
                .action(resolveJudgeAction(context.taskType()))
                .canScore(canScore)
                .canFinalize(canFinalize)
                .scored(hasSubmittedJudgeScore(entry.getId(), context.judge().getId()))
                .locked(scoreTableLocked || finalScoreExists)
                .scoreRoleType(resolveScoreRoleType(context.member().getRole(), context.taskType()))
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .styleCategoryName(style == null ? null : style.getCategoryName())
                .styleCode(style == null ? null : style.getStyleCode())
                .styleDescription(style == null ? null : style.getDescription())
                .abv(entry.getAbv())
                .description(entry.getDescription())
                .extraFields(listJudgeVisibleExtraFields(entry))
                .build();
    }

    private EntryDetailVO toEntryDetailVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        return EntryDetailVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .description(entry.getDescription())
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionDate(competition == null ? null : competition.getCompetitionDate())
                .competitionLogistics(toEntryCompetitionLogisticsVO(competition, entry, payment))
                .status(entry.getStatus())
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliveryNote(delivery == null ? null : delivery.getDeliveryNote())
                .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                .deliveryReceivedAt(delivery == null ? null : delivery.getReceivedTime())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()))
                .submittedAt(entry.getCreateTime())
                .extraFields(listExtraFields(entry.getId()))
                .build();
    }

    private EntrySummaryVO toEntrySummaryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        return EntrySummaryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionLogistics(toEntryCompetitionLogisticsVO(competition, entry, payment))
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .status(entry.getStatus())
                .abv(entry.getAbv())
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()))
                .submittedAt(entry.getCreateTime())
                .build();
    }

    private CompetitionLogisticsVO toEntryCompetitionLogisticsVO(Competition competition, BeerEntry entry, EntryPayment payment) {
        if (competition == null) {
            return null;
        }
        CompetitionLogisticsVO logistics = CompetitionLogisticsVO.builder()
                .deliveryMethod(competition.getDeliveryMethod())
                .sampleArrivalStart(competition.getSampleArrivalStart())
                .sampleArrivalDeadline(competition.getSampleArrivalDeadline())
                .sampleQuantityNote(competition.getSampleQuantityNote())
                .deliveryRecipient(competition.getDeliveryRecipient())
                .deliveryPhone(competition.getDeliveryPhone())
                .deliveryAddress(competition.getDeliveryAddress())
                .deliveryNote(competition.getDeliveryNote())
                .venueName(competition.getVenueName())
                .venueAddress(competition.getVenueAddress())
                .venueTimeNote(competition.getVenueTimeNote())
                .venueContact(competition.getVenueContact())
                .venueMapUrl(competition.getVenueMapUrl())
                .logisticsVisibility(competition.getLogisticsVisibility())
                .build();
        if (canViewFullLogistics(logistics, entry, payment)) {
            return logistics;
        }
        logistics.setDeliveryRecipient(null);
        logistics.setDeliveryPhone(null);
        logistics.setDeliveryAddress(null);
        return logistics;
    }

    private boolean canViewFullLogistics(CompetitionLogisticsVO logistics, BeerEntry entry, EntryPayment payment) {
        if (logistics == null || !StringUtils.hasText(logistics.getLogisticsVisibility())) {
            return false;
        }
        if (LogisticsVisibility.PUBLIC.name().equals(logistics.getLogisticsVisibility())) {
            return true;
        }
        if (LogisticsVisibility.LOGIN_REQUIRED.name().equals(logistics.getLogisticsVisibility())) {
            return true;
        }
        return EntryPaymentStatus.PAID.name().equals(resolvePaymentStatus(entry, payment))
                || LABEL_ALLOWED_STATUSES.contains(entry.getStatus());
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
                        .slotLabel(result.getSlotLabel())
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

    private List<EntryExtraFieldVO> listExtraFields(Long beerEntryId) {
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .map(this::toEntryExtraFieldVO)
                .toList();
    }

    private EntryPayment findEntryPayment(Long beerEntryId) {
        return entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private EntryDelivery findEntryDelivery(Long beerEntryId) {
        return entryDeliveryMapper.selectOne(new LambdaQueryWrapper<EntryDelivery>()
                .eq(EntryDelivery::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private List<EntryExtraFieldVO> listJudgeVisibleExtraFields(BeerEntry entry) {
        Set<String> visibleKeys = listEntryFieldConfigs(entry.getCompetitionId()).stream()
                .filter(item -> Objects.equals(item.getVisibleToJudges(), 1))
                .map(EntryFieldConfig::getFieldKey)
                .collect(Collectors.toSet());
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

    private Map<String, String> normalizeExtraFields(List<EntryFieldConfig> configs, Map<String, Object> input) {
        Map<String, Object> source = input == null ? Map.of() : input;
        Map<String, String> normalized = new LinkedHashMap<>();
        for (EntryFieldConfig config : configs) {
            Object rawValue = source.get(config.getFieldKey());
            String value = normalizeFieldValue(config, rawValue);
            if (Objects.equals(config.getRequiredFlag(), 1) && !StringUtils.hasText(value)) {
                throw new BaseException("请填写" + config.getFieldLabel());
            }
            if (StringUtils.hasText(value)) {
                normalized.put(config.getFieldKey(), value);
            }
        }
        return normalized;
    }

    private String normalizeFieldValue(EntryFieldConfig config, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if ("multi_select".equals(config.getFieldType())) {
            List<String> values = normalizeMultiSelectValue(config, rawValue);
            return values.isEmpty() ? null : String.join("、", values);
        }
        if ("number".equals(config.getFieldType())) {
            BigDecimal number = asBigDecimal(rawValue);
            return number == null ? null : number.stripTrailingZeros().toPlainString();
        }
        String value = normalizeNullable(String.valueOf(rawValue));
        if (OPTION_FIELD_TYPES.contains(config.getFieldType()) && StringUtils.hasText(value)) {
            requireAllowedOption(config, value);
        }
        return value;
    }

    private List<String> normalizeMultiSelectValue(EntryFieldConfig config, Object rawValue) {
        List<?> rawItems;
        if (rawValue instanceof List<?> list) {
            rawItems = list;
        } else {
            rawItems = List.of(rawValue);
        }
        List<String> values = new ArrayList<>();
        for (Object item : rawItems) {
            String value = normalizeNullable(String.valueOf(item));
            if (!StringUtils.hasText(value)) {
                continue;
            }
            requireAllowedOption(config, value);
            values.add(value);
        }
        return values;
    }

    private void requireAllowedOption(EntryFieldConfig config, String value) {
        List<String> options = readOptions(config.getOptionsJson());
        if (!options.isEmpty() && !options.contains(value)) {
            throw new BaseException(config.getFieldLabel() + "选项不合法");
        }
    }

    private Competition requireOpenCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (!CompetitionStatus.REGISTRATION_OPEN.name().equals(competition.getStatus())) {
            throw new BaseException("当前赛事暂未开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (competition.getRegistrationDeadline() != null && now.isAfter(competition.getRegistrationDeadline())) {
            throw new BaseException("报名已截止");
        }
        return competition;
    }

    private EntryPayment ensureEntryPayment(Long beerEntryId, Long competitionId) {
        EntryPayment payment = findEntryPayment(beerEntryId);
        if (payment != null) {
            return payment;
        }
        Competition competition = competitionMapper.selectById(competitionId);
        EntryPayment created = EntryPayment.builder()
                .beerEntryId(beerEntryId)
                .amount(competition == null ? BigDecimal.ZERO : competition.getEntryFee())
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .build();
        entryPaymentMapper.insert(created);
        return created;
    }

    private EntryDelivery ensureEntryDelivery(Long beerEntryId) {
        EntryDelivery delivery = findEntryDelivery(beerEntryId);
        if (delivery != null) {
            return delivery;
        }
        EntryDelivery created = EntryDelivery.builder()
                .beerEntryId(beerEntryId)
                .deliveryStatus(EntryDeliveryStatus.NOT_SUBMITTED.name())
                .build();
        entryDeliveryMapper.insert(created);
        return created;
    }

    private CompetitionCategory requireCompetitionCategory(Long competitionId, Long categoryId) {
        CompetitionCategory category = competitionCategoryMapper.selectOne(new LambdaQueryWrapper<CompetitionCategory>()
                .eq(CompetitionCategory::getId, categoryId)
                .eq(CompetitionCategory::getCompetitionId, competitionId));
        if (category == null) {
            throw new BaseException("投递组别不属于当前赛事");
        }
        return category;
    }

    private void requireCompetitionStyle(Long competitionId, String styleName) {
        CompetitionStyleConfig style = competitionStyleConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, competitionId)
                .eq(CompetitionStyleConfig::getName, normalizeRequired(styleName, "基础风格不能为空"))
                .last("LIMIT 1"));
        if (style == null) {
            throw new BaseException("基础风格不属于当前赛事");
        }
    }

    private List<EntryFieldConfig> listEntryFieldConfigs(Long competitionId) {
        return entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                .eq(EntryFieldConfig::getCompetitionId, competitionId)
                .orderByAsc(EntryFieldConfig::getSortOrder)
                .orderByAsc(EntryFieldConfig::getId));
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

    private EntryPaymentVO toEntryPaymentVO(EntryPayment payment, Competition competition) {
        return EntryPaymentVO.builder()
                .status(payment == null ? EntryPaymentStatus.UNPAID.name() : payment.getStatus())
                .payMethod(resolvePayMethod(payment == null ? null : payment.getPayMethod()))
                .amount(payment == null ? competition == null ? null : competition.getEntryFee() : payment.getAmount())
                .outTradeNo(payment == null ? null : payment.getOutTradeNo())
                .wechatTransactionId(payment == null ? null : payment.getWechatTransactionId())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .build();
    }

    private EntryDeliveryVO toEntryDeliveryVO(EntryDelivery delivery) {
        return EntryDeliveryVO.builder()
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliveryNote(delivery == null ? null : delivery.getDeliveryNote())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .submittedTime(delivery == null ? null : delivery.getSubmittedTime())
                .receivedTime(delivery == null ? null : delivery.getReceivedTime())
                .build();
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂商账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
    }

    private PortalProfileVO toPortalProfileVO(PortalAccount account, Brewery brewery) {
        return PortalProfileVO.builder()
                .accountId(account.getId())
                .breweryId(account.getBreweryId())
                .displayName(account.getDisplayName())
                .companyName(brewery == null ? null : brewery.getCompanyName())
                .contactName(brewery == null ? null : brewery.getContactName())
                .phone(account.getPhone())
                .wechat(StringUtils.hasText(account.getWechat()) ? account.getWechat() : brewery == null ? null : brewery.getWechat())
                .build();
    }

    private CompetitionStyleConfig findStyleSnapshot(BeerEntry entry) {
        return competitionStyleConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, entry.getCompetitionId())
                .eq(CompetitionStyleConfig::getName, entry.getStyle())
                .last("LIMIT 1"));
    }

    private JudgeScanContext requireActiveJudgeRoundEntry(BeerEntry entry) {
        JudgeAccount account = judgeAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用，不能查看酒款");
        }
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, account.getId())
                .eq(RoundTableMember::getSystemTaskRequired, 1));
        if (members.isEmpty()) {
            throw new ForbiddenException("当前评审没有可查看的评审任务");
        }
        List<Long> roundTableIds = members.stream().map(RoundTableMember::getRoundTableId).toList();
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, entry.getId())
                .in(RoundTableEntry::getRoundTableId, roundTableIds)
                .orderByDesc(RoundTableEntry::getId));
        Map<Long, RoundTableMember> memberByTableId = members.stream()
                .collect(Collectors.toMap(RoundTableMember::getRoundTableId, item -> item, (left, right) -> left));
        for (RoundTableEntry roundEntry : roundEntries) {
            RoundTable table = roundTableMapper.selectById(roundEntry.getRoundTableId());
            if (table == null) {
                continue;
            }
            RoundTableMember member = memberByTableId.get(table.getId());
            CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
            if (round == null || !isRoundVisibleToJudge(round, table, member)) {
                continue;
            }
            String taskType = resolveJudgeTaskType(round, member);
            if (!StringUtils.hasText(taskType)) {
                continue;
            }
            Competition competition = competitionMapper.selectById(round.getCompetitionId());
            return new JudgeScanContext(account, member, roundEntry, table, round, competition, taskType);
        }
        throw new ForbiddenException("当前评审无权查看该酒款");
    }

    private boolean isRoundVisibleToJudge(CompetitionRound round, RoundTable table, RoundTableMember member) {
        if (member == null) {
            return false;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            if (RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
                return RoundStatus.PUBLISHED.name().equals(table.getStatus())
                        || JudgeRoleType.CAPTAIN.name().equals(member.getRole());
            }
            if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
                return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                        && !RoundStatus.LOCKED.name().equals(table.getStatus());
            }
            return RoundStatus.LOCKED.name().equals(round.getStatus())
                    && JudgeRoleType.CAPTAIN.name().equals(member.getRole());
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return RoundStatus.IN_PROGRESS.name().equals(round.getStatus())
                    || RoundStatus.SUBMITTED.name().equals(round.getStatus())
                    || RoundStatus.LOCKED.name().equals(round.getStatus());
        }
        return false;
    }

    private String resolveJudgeTaskType(CompetitionRound round, RoundTableMember member) {
        if (member == null) {
            return null;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                    ? JudgeTaskType.CAPTAIN_FINALIZE.name()
                    : JudgeTaskType.SCORE_ENTRY.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return JudgeTaskType.RANKING_ROUND.name();
        }
        return null;
    }

    private String resolveJudgeAction(String taskType) {
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            return "CAPTAIN";
        }
        if (JudgeTaskType.RANKING_ROUND.name().equals(taskType)) {
            return "RANKING";
        }
        return "SCORE";
    }

    private boolean canSubmitPersonalScore(JudgeScanContext context) {
        if (!RoundType.SCORE.name().equals(context.round().getRoundType())) {
            return false;
        }
        if (!RoundStatus.PUBLISHED.name().equals(context.round().getStatus())
                || !RoundStatus.PUBLISHED.name().equals(context.table().getStatus())) {
            return false;
        }
        return JudgeTaskType.SCORE_ENTRY.name().equals(context.taskType())
                || JudgeTaskType.CAPTAIN_FINALIZE.name().equals(context.taskType());
    }

    private boolean canFinalizeTable(JudgeScanContext context) {
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(context.taskType())) {
            return RoundType.SCORE.name().equals(context.round().getRoundType())
                    && JudgeRoleType.CAPTAIN.name().equals(context.member().getRole())
                    && (RoundStatus.PUBLISHED.name().equals(context.round().getStatus())
                    || RoundStatus.SUBMITTED.name().equals(context.round().getStatus()));
        }
        return JudgeTaskType.RANKING_ROUND.name().equals(context.taskType());
    }

    private String resolveScoreRoleType(String memberRole, String taskType) {
        if (!JudgeTaskType.SCORE_ENTRY.name().equals(taskType)
                && !JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            return null;
        }
        if (JudgeRoleType.CAPTAIN.name().equals(memberRole)) {
            return JudgeRoleType.PROFESSIONAL.name();
        }
        return memberRole;
    }

    private boolean hasSubmittedJudgeScore(Long beerEntryId, Long judgeId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getJudgeAccountId, judgeId)
                .eq(ScoreRecord::getFinalFlag, 0)) > 0;
    }

    private boolean hasFinalScore(Long beerEntryId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getFinalFlag, 1)) > 0;
    }

    private boolean isResultPublished(Competition competition, BeerEntry entry) {
        return EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus())
                || (competition != null && CompetitionStatus.PUBLISHED.name().equals(competition.getStatus()));
    }

    private String resolvePaymentStatus(BeerEntry entry, EntryPayment payment) {
        if (payment != null && StringUtils.hasText(payment.getStatus())) {
            return EntryPaymentStatus.PAID.name().equals(payment.getStatus())
                    ? EntryPaymentStatus.PAID.name()
                    : EntryPaymentStatus.UNPAID.name();
        }
        if (EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return EntryPaymentStatus.UNPAID.name();
        }
        return LABEL_ALLOWED_STATUSES.contains(entry.getStatus())
                ? EntryPaymentStatus.PAID.name()
                : EntryPaymentStatus.UNPAID.name();
    }

    private String resolveDeliveryStatus(EntryDelivery delivery) {
        if (delivery == null || !StringUtils.hasText(delivery.getDeliveryStatus())) {
            return EntryDeliveryStatus.NOT_SUBMITTED.name();
        }
        return delivery.getDeliveryStatus();
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

    private String generateEntryUuid() {
        String uuid;
        do {
            uuid = "BE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>().eq(BeerEntry::getUuid, uuid)) != null);
        return uuid;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeDeliveryMethod(String deliveryMethod) {
        String normalized = normalizeRequired(deliveryMethod, "送样方式不能为空").toUpperCase();
        try {
            return EntryDeliveryMethod.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BaseException("送样方式不合法");
        }
    }

    private void validateDeliveryRequest(String deliveryMethod, String carrier, String trackingNo) {
        if (EntryDeliveryMethod.EXPRESS.name().equals(deliveryMethod)) {
            if (!StringUtils.hasText(carrier)) {
                throw new BaseException("请填写快递公司");
            }
            if (!StringUtils.hasText(trackingNo)) {
                throw new BaseException("请填写快递单号");
            }
        }
    }

    private String resolvePayMethod(String payMethod) {
        return StringUtils.hasText(payMethod) ? payMethod : EntryPayMethod.MANUAL.name();
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

    private List<String> readOptions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BaseException("解析字段选项失败");
        }
    }

    private String writeJson(Map<String, String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存补充字段失败");
        }
    }

    private record JudgeScanContext(
            JudgeAccount judge,
            RoundTableMember member,
            RoundTableEntry roundEntry,
            RoundTable table,
            CompetitionRound round,
            Competition competition,
            String taskType
    ) {
    }
}
