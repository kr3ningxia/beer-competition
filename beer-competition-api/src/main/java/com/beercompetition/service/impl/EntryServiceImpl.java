package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.AdminOperationLogMapper;
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
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.EntryDeliveryMethod;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.JudgeTaskType;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AdminOperationLog;
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
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryLogVO;
import com.beercompetition.pojo.vo.AdminEntryTraceVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.EntryDeliveryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.EntryPaymentVO;
import com.beercompetition.pojo.vo.EntryRefundVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.CompetitionLogisticsVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.PortalAwardEntryVO;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.pojo.vo.PortalResultGroupVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import com.beercompetition.pojo.vo.PortalRoundResultVO;
import com.beercompetition.pojo.vo.PortalScoreDimensionVO;
import com.beercompetition.pojo.vo.PortalScoreRecordVO;
import com.beercompetition.service.CompetitionService;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.WechatPaymentService;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {

    private static final Set<String> LABEL_ALLOWED_STATUSES = Set.of(
            EntryStatus.REGISTERED.name(),
            EntryStatus.STORED.name(),
            EntryStatus.RESULT_PUBLISHED.name()
    );
    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
            EntryRefundStatus.REQUESTED.name(),
            EntryRefundStatus.APPROVED.name(),
            EntryRefundStatus.PROCESSING.name()
    );
    private static final Set<String> OPTION_FIELD_TYPES = Set.of("select", "multi_select");
    private static final Set<String> AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String TARGET_ENTRY = "BEER_ENTRY";
    private static final String BUSINESS_TYPE_BREWERY_AVATAR = "BREWERY_AVATAR";
    private static final int EXTRA_FIELD_VALUE_MAX_LENGTH = 255;
    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final AwardResultMapper awardResultMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryRefundMapper entryRefundMapper;
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
    private final WechatPaymentService wechatPaymentService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final WechatPayProperties wechatPayProperties;

    @Override
    public PageResult<AdminEntryVO> listAdminEntries(Long competitionId, String status, String paymentStatus,
                                                     String deliveryStatus, Long categoryId, Boolean assigned,
                                                     String refundStatus, String keyword, Integer page, Integer pageSize) {
        // 1) 参数规范化与基础查询
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 30 : pageSize, 1), 100);
        String normalizedKeyword = normalizeNullable(keyword);
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(competitionId != null, BeerEntry::getCompetitionId, competitionId)
                .eq(StringUtils.hasText(status), BeerEntry::getStatus, status)
                .eq(categoryId != null, BeerEntry::getCategoryId, categoryId)
                .orderByDesc(BeerEntry::getId));
        
        // 2) 组装运营列表并执行复合筛选
        List<AdminEntryVO> filtered = entries.stream()
                .map(this::toAdminEntryVO)
                .filter(item -> !StringUtils.hasText(paymentStatus) || paymentStatus.equals(item.getPaymentStatus()))
                .filter(item -> !StringUtils.hasText(deliveryStatus) || deliveryStatus.equals(item.getDeliveryStatus()))
                .filter(item -> !StringUtils.hasText(refundStatus) || refundStatus.equals(item.getRefundStatus()))
                .filter(item -> assigned == null || assigned.equals(Boolean.TRUE.equals(item.getAssigned())))
                .filter(item -> !StringUtils.hasText(normalizedKeyword) || matchesEntryKeyword(item, normalizedKeyword))
                .sorted(Comparator.comparingInt(this::adminEntryDisplayPriority)
                        .thenComparing(AdminEntryVO::getLastModifiedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminEntryVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // 3) 内存分页并返回
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + currentPageSize, filtered.size());
        return new PageResult<>(filtered.size(), filtered.subList(fromIndex, toIndex));
    }

    private int adminEntryDisplayPriority(AdminEntryVO entry) {
        if (entry == null) {
            return 1;
        }
        if (EntryRefundStatus.REQUESTED.name().equals(entry.getRefundStatus())
                || EntryRefundStatus.FAILED.name().equals(entry.getRefundStatus())) {
            return 0;
        }
        if (EntryRefundStatus.SUCCESS.name().equals(entry.getRefundStatus())
                || EntryPaymentStatus.REFUNDED.name().equals(entry.getPaymentStatus())) {
            return 2;
        }
        return 1;
    }

    @Override
    public AdminEntryDetailVO getAdminEntry(Long entryId) {
        // 1) 查询酒款主体
        BeerEntry entry = requireEntry(entryId);

        // 2) 组装后台详情
        return toAdminEntryDetailVO(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminEntryDetailVO updateAdminEntry(Long entryId, AdminEntryUpdateRequest request) {
        // 1) 查询上下文与前置校验
        BeerEntry entry = requireEntry(entryId);
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        if (EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) || isResultPublished(competition, entry)) {
            throw new BaseException("结果已发布，不能直接修改报名信息");
        }
        CompetitionCategory category = requireCompetitionCategory(entry.getCompetitionId(), request.getCategoryId());
        requireCompetitionStyle(entry.getCompetitionId(), request.getStyle());
        List<EntryFieldConfig> fieldConfigs = listEntryFieldConfigs(entry.getCompetitionId());
        Map<String, String> normalizedExtraFields = normalizeExtraFields(fieldConfigs, request.getExtraFields());
        String reason = normalizeRequired(request.getReason(), "请填写修改原因");

        // 2) 收集变更并更新主记录
        List<Map<String, String>> changes = new ArrayList<>();
        addChange(changes, "酒名", entry.getName(), normalizeRequired(request.getName(), "酒款名称不能为空"));
        addChange(changes, "投递组别", resolveCategoryName(entry.getCategoryId()), category.getName());
        addChange(changes, "基础风格", entry.getStyle(), normalizeRequired(request.getStyle(), "基础风格不能为空"));
        addChange(changes, "ABV", entry.getAbv() == null ? null : entry.getAbv().stripTrailingZeros().toPlainString(),
                request.getAbv() == null ? null : request.getAbv().stripTrailingZeros().toPlainString());
        addExtraFieldChanges(changes, entry.getId(), fieldConfigs, normalizedExtraFields);

        entry.setName(normalizeRequired(request.getName(), "酒款名称不能为空"));
        entry.setCategoryId(category.getId());
        entry.setStyle(normalizeRequired(request.getStyle(), "基础风格不能为空"));
        entry.setAbv(request.getAbv());
        entry.setExtraFieldsJson(writeJson(normalizedExtraFields));
        beerEntryMapper.updateById(entry);

        // 3) 重写补充字段并记录审计
        beerEntryExtraFieldMapper.delete(new LambdaQueryWrapper<BeerEntryExtraField>()
                .eq(BeerEntryExtraField::getBeerEntryId, entry.getId()));
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
        writeEntryLog("ENTRY_UPDATE", entry.getUuid(), buildEntryLogSummary(reason, changes));

        // 4) 返回最新详情
        return toAdminEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

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
        requireRulesAcceptedIfConfigured(competition, request);
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
                .extraFieldsJson(writeJson(normalizedExtraFields))
                .status(EntryStatus.PENDING_PAYMENT.name())
                .storedFlag(0)
                .build();
        beerEntryMapper.insert(entry);
        entryScanLabelService.createActiveLabel(entry, BaseContext.getCurrentId());

        // 3) 初始化支付与送样记录
        entryPaymentMapper.insert(EntryPayment.builder()
                .beerEntryId(entry.getId())
                .amount(resolveEntryFee(competition, LocalDateTime.now()))
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

    private void requireRulesAcceptedIfConfigured(Competition competition, PortalEntrySubmitRequest request) {
        if (StringUtils.hasText(competition.getRulesUrl()) && !Boolean.TRUE.equals(request.getRulesAccepted())) {
            throw new BaseException("请先阅读并同意本次大赛参赛细则");
        }
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
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能提交寄样信息");
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
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO requestPortalEntryRefund(Long entryId, PortalEntryRefundRequest request) {
        // 1) 查询厂商酒款并校验退款资格
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        assertCanRequestRefund(entry, competition, payment);

        // 2) 创建退款申请记录
        EntryRefund refund = EntryRefund.builder()
                .beerEntryId(entry.getId())
                .entryPaymentId(payment.getId())
                .refundNo(generateRefundNo())
                .amount(payment.getAmount())
                .status(EntryRefundStatus.REQUESTED.name())
                .reason(normalizeRequired(request.getReason(), "请填写退款原因"))
                .requestedByPortalId(account.getId())
                .requestedTime(LocalDateTime.now())
                .build();
        entryRefundMapper.insert(refund);

        // 3) 返回最新酒款详情
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
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能下载现场标签");
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

        brewery.setCompanyName(normalizeRequired(request.getCompanyName(), "品牌名不能为空"));
        brewery.setContactName(normalizeRequired(request.getContactName(), "联系人不能为空"));
        brewery.setWechat(normalizeNullable(request.getWechat()));
        breweryMapper.updateById(brewery);

        // 3) 返回更新后的资料
        return toPortalProfileVO(portalAccountMapper.selectById(account.getId()), breweryMapper.selectById(brewery.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProfileVO uploadPortalAvatar(MultipartFile file) {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }

        // 2) 校验并上传头像
        validateAvatarFile(file);
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "avatar.png");
        byte[] bytes = readUploadBytes(file, "读取头像文件失败");
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_BREWERY_AVATAR, filename, bytes);
        String publicUrl = resolveUploadPublicUrl(storagePath);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_BREWERY_AVATAR)
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);

        // 3) 绑定头像到厂牌资料
        brewery.setAvatarAssetId(asset.getId());
        brewery.setAvatarUrl(publicUrl);
        breweryMapper.updateById(brewery);

        // 4) 返回更新后的资料
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
    public List<PortalCompetitionResultVO> listPublishedCompetitionResults() {
        // 1) 查询已发布奖项
        List<AwardResult> awards = listPublishedAwards(null);
        if (awards.isEmpty()) {
            return List.of();
        }

        // 2) 批量读取关联赛事、酒款、厂牌和组别
        PublishedResultContext context = buildPublishedResultContext(awards);

        // 3) 按赛事组装公开获奖结果
        return awards.stream()
                .map(AwardResult::getCompetitionId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(competitionId -> !isCompetitionArchived(competitionId))
                .map(competitionId -> buildCompetitionResult(competitionId, awards, context))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public PortalCompetitionResultVO getPublishedCompetitionResult(Long competitionId) {
        // 1) 查询指定赛事的已发布奖项
        List<AwardResult> awards = listPublishedAwards(competitionId);
        if (awards.isEmpty()) {
            throw new ResourceNotFoundException("暂无已发布赛事结果");
        }
        if (isCompetitionArchived(competitionId)) {
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
                .contentType(CONTENT_TYPE_PDF)
                .content(fileStorageService.download(asset.getStoragePath()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO simulatePayment(Long entryId) {
        // 1) 查询并校验当前厂商酒款
        if (wechatPayProperties.isWechatMode()) {
            throw new BaseException("当前已启用微信支付，请扫码完成报名费支付");
        }
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        if (LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            return toEntryDetailVO(entry);
        }
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("当前酒款不能支付报名费");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待主办方核对到账");
        }

        // 2) 模拟支付到账并推进报名状态
        payment.setStatus(EntryPaymentStatus.PAID.name());
        payment.setPayMethod(EntryPayMethod.MOCK.name());
        payment.setOutTradeNo(StringUtils.hasText(payment.getOutTradeNo()) ? payment.getOutTradeNo() : generateMockOutTradeNo());
        payment.setPaidTime(LocalDateTime.now());
        payment.setConfirmRemark("mock payment");
        entryPaymentMapper.updateById(payment);

        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);

        // 3) 返回更新后的酒款详情
        return toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long entryId) {
        confirmPayment(entryId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("只有待付款确认的酒款可以确认付款");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账记录请在转账确认页面处理");
        }

        // 2) 更新支付记录和报名状态
        payment.setStatus(EntryPaymentStatus.PAID.name());
        payment.setPayMethod(resolvePayMethod(payment.getPayMethod()));
        payment.setPaidTime(LocalDateTime.now());
        payment.setConfirmedByAdminId(BaseContext.getCurrentId());
        payment.setConfirmRemark(normalizeStatusReason(request));
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_CONFIRM_PAYMENT", entry.getUuid(), buildStatusLogSummary("确认付款", normalizeStatusReason(request)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStored(Long entryId) {
        markStored(entryId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStored(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!EntryStatus.REGISTERED.name().equals(entry.getStatus())) {
            throw new BaseException("只有报名成功的酒款可以确认入库");
        }
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能确认入库");
        }
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());

        // 2) 更新送样记录和入库状态
        delivery.setDeliveryStatus(EntryDeliveryStatus.RECEIVED.name());
        delivery.setReceivedTime(LocalDateTime.now());
        delivery.setReceivedByAdminId(BaseContext.getCurrentId());
        delivery.setReceiveRemark(normalizeStatusReason(request));
        if (delivery.getSubmittedTime() == null) {
            delivery.setSubmittedTime(LocalDateTime.now());
        }
        entryDeliveryMapper.updateById(delivery);
        entry.setStoredFlag(1);
        entry.setStatus(EntryStatus.STORED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_MARK_STORED", entry.getUuid(), buildStatusLogSummary("确认入库", normalizeStatusReason(request)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unmarkStored(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验撤销条件
        BeerEntry entry = requireEntry(entryId);
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryRefund refund = findLatestRefund(entry.getId());
        if (!canUnmarkStored(entry, competition, refund)) {
            throw new BaseException(resolveUnmarkStoredUnavailableReason(entry, competition, refund));
        }
        String reason = normalizeRequired(request == null ? null : request.getReason(), "请填写撤销入库原因");
        EntryDelivery delivery = ensureEntryDelivery(entry.getId());

        // 2) 回退入库状态并保留寄样信息
        delivery.setDeliveryStatus(EntryDeliveryStatus.SUBMITTED.name());
        delivery.setReceivedTime(null);
        delivery.setReceivedByAdminId(null);
        delivery.setReceiveRemark(reason);
        entryDeliveryMapper.updateById(delivery);

        entry.setStoredFlag(0);
        entry.setStatus(EntryStatus.REGISTERED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_UNMARK_STORED", entry.getUuid(), buildStatusLogSummary("撤销入库", reason));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEntry(Long entryId) {
        cancelEntry(entryId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEntry(Long entryId, AdminEntryStatusRequest request) {
        // 1) 查询作品并校验状态
        BeerEntry entry = requireEntry(entryId);
        if (!Set.of(EntryStatus.PENDING_PAYMENT.name(), EntryStatus.REGISTERED.name()).contains(entry.getStatus())) {
            throw new BaseException("当前状态不能取消报名");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        if (EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            throw new BaseException("已付款报名请通过退款申请处理");
        }
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账确认中，请先处理转账记录");
        }

        // 2) 更新支付记录并标记取消
        payment.setStatus(EntryPaymentStatus.CANCELED.name());
        payment.setConfirmRemark(normalizeStatusReason(request));
        entryPaymentMapper.updateById(payment);
        entry.setStatus(EntryStatus.CANCELED.name());
        beerEntryMapper.updateById(entry);
        writeEntryLog("ENTRY_CANCEL", entry.getUuid(), buildStatusLogSummary("取消报名", normalizeStatusReason(request)));
    }

    @Override
    public PageResult<AdminEntryVO> listAdminRefunds(String status, Integer page, Integer pageSize) {
        // 1) 查询退款记录关联酒款
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 30 : pageSize, 1), 100);
        List<EntryRefund> refunds = entryRefundMapper.selectList(new LambdaQueryWrapper<EntryRefund>()
                .eq(StringUtils.hasText(status), EntryRefund::getStatus, status)
                .orderByDesc(EntryRefund::getRequestedTime)
                .orderByDesc(EntryRefund::getId));

        // 2) 组装酒款列表并分页
        List<AdminEntryVO> records = refunds.stream()
                .map(refund -> beerEntryMapper.selectById(refund.getBeerEntryId()))
                .filter(Objects::nonNull)
                .map(this::toAdminEntryVO)
                .toList();
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, records.size());
        int toIndex = Math.min(fromIndex + currentPageSize, records.size());
        return new PageResult<>(records.size(), records.subList(fromIndex, toIndex));
    }

    @Override
    public void approveRefund(Long refundId, AdminEntryStatusRequest request) {
        // 1) 委托微信支付服务受理退款并根据返回结果推进状态
        wechatPaymentService.approveRefund(refundId, normalizeStatusReason(request), BaseContext.getCurrentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long refundId, AdminEntryStatusRequest request) {
        // 1) 查询退款申请并校验状态
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.REQUESTED.name().equals(refund.getStatus())) {
            throw new BaseException("只有待处理退款可以驳回");
        }
        BeerEntry entry = requireEntry(refund.getBeerEntryId());

        // 2) 驳回退款并保留报名
        refund.setStatus(EntryRefundStatus.REJECTED.name());
        refund.setProcessedByAdminId(BaseContext.getCurrentId());
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(normalizeStatusReason(request));
        entryRefundMapper.updateById(refund);
        writeEntryLog("ENTRY_REFUND_REJECT", entry.getUuid(), buildStatusLogSummary("驳回退款", normalizeStatusReason(request)));
    }

    @Override
    public void retryRefund(Long refundId, AdminEntryStatusRequest request) {
        // 1) 委托微信支付服务重试失败退款
        wechatPaymentService.retryRefund(refundId, normalizeStatusReason(request), BaseContext.getCurrentId());
    }

    @Override
    public JudgeEntryVO getJudgeEntry(String uuid) {
        // 1) 查询匿名作品并校验评审权限
        BeerEntry entry = beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, uuid));
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        assertCompetitionNotArchived(entry.getCompetitionId());
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
        assertCompetitionNotArchived(entry.getCompetitionId());
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
                .extraFields(listJudgeVisibleExtraFields(entry))
                .build();
    }

    private AdminEntryVO toAdminEntryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        Brewery brewery = breweryMapper.selectById(entry.getBreweryId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        List<AdminEntryTraceVO> traces = listAdminEntryTraces(entry.getId());
        boolean assigned = hasRoundAssignment(entry.getId());
        boolean resultPublished = isResultPublished(competition, entry);
        return AdminEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .breweryCompanyName(brewery == null ? null : brewery.getCompanyName())
                .breweryContactName(brewery == null ? null : brewery.getContactName())
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .status(entry.getStatus())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .assigned(assigned)
                .pathText(formatEntryPathText(traces))
                .submittedAt(entry.getCreateTime())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .deliveryReceivedAt(delivery == null ? null : delivery.getReceivedTime())
                .lastModifiedAt(resolveEntryLastModifiedAt(entry, payment, delivery, refund))
                .canConfirmPayment(canConfirmPayment(entry, payment))
                .canMarkStored(EntryStatus.REGISTERED.name().equals(entry.getStatus()) && !isActiveRefund(refund))
                .canUnmarkStored(canUnmarkStored(entry, competition, refund))
                .canCancel(canCancelEntry(entry, payment))
                .canApproveRefund(canApproveRefund(refund))
                .canRejectRefund(canRejectRefund(refund))
                .canEdit(!EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) && !resultPublished)
                .traces(traces)
                .build();
    }

    private AdminEntryDetailVO toAdminEntryDetailVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        Brewery brewery = breweryMapper.selectById(entry.getBreweryId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean resultPublished = isResultPublished(competition, entry);
        boolean assigned = hasRoundAssignment(entry.getId());
        return AdminEntryDetailVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .name(entry.getName())
                .breweryCompanyName(brewery == null ? null : brewery.getCompanyName())
                .breweryContactName(brewery == null ? null : brewery.getContactName())
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .status(entry.getStatus())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .refund(toEntryRefundVO(refund))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .delivery(toEntryDeliveryVO(delivery))
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .assigned(assigned)
                .resultPublished(resultPublished)
                .canConfirmPayment(canConfirmPayment(entry, payment))
                .canMarkStored(EntryStatus.REGISTERED.name().equals(entry.getStatus()) && !isActiveRefund(refund))
                .canUnmarkStored(canUnmarkStored(entry, competition, refund))
                .canCancel(canCancelEntry(entry, payment))
                .canApproveRefund(canApproveRefund(refund))
                .canRejectRefund(canRejectRefund(refund))
                .canEdit(!EntryStatus.RESULT_PUBLISHED.name().equals(entry.getStatus()) && !resultPublished)
                .submittedAt(entry.getCreateTime())
                .extraFields(listExtraFields(entry.getId()))
                .traces(listAdminEntryTraces(entry.getId()))
                .logs(listEntryLogs(entry.getUuid()))
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
                .matchDate(competition.getCompetitionDate())
                .publishedAt(resolvePublishedAt(competitionAwards))
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

    private LocalDateTime resolvePublishedAt(List<AwardResult> awards) {
        return awards.stream()
                .map(AwardResult::getPublishedTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private EntryDetailVO toEntryDetailVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean activeRefund = isActiveRefund(refund);
        boolean resultPublished = isResultPublished(competition, entry);
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
                .categoryId(entry.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionDate(competition == null ? null : competition.getCompetitionDate())
                .competitionLogistics(toEntryCompetitionLogisticsVO(competition, entry, payment))
                .status(entry.getStatus())
                .published(resultPublished)
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .stored(Objects.equals(entry.getStoredFlag(), 1))
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .refund(toEntryRefundVO(refund))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .canRequestRefund(canRequestRefund(entry, competition, payment, refund, resultPublished))
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .deliveryNote(delivery == null ? null : delivery.getDeliveryNote())
                .deliverySubmittedAt(delivery == null ? null : delivery.getSubmittedTime())
                .deliveryReceivedAt(delivery == null ? null : delivery.getReceivedTime())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()) && !activeRefund)
                .submittedAt(entry.getCreateTime())
                .extraFields(listExtraFields(entry.getId()))
                .build();
    }

    private EntrySummaryVO toEntrySummaryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryPayment payment = findEntryPayment(entry.getId());
        EntryDelivery delivery = findEntryDelivery(entry.getId());
        EntryRefund refund = findLatestRefund(entry.getId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        boolean activeRefund = isActiveRefund(refund);
        boolean resultPublished = isResultPublished(competition, entry);
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
                .published(resultPublished)
                .abv(entry.getAbv())
                .entryFee(competition == null ? null : competition.getEntryFee())
                .storedFlag(entry.getStoredFlag())
                .paymentStatus(resolvePaymentStatus(entry, payment))
                .payment(toEntryPaymentVO(payment, competition))
                .refund(toEntryRefundVO(refund))
                .refundStatus(refund == null ? null : refund.getStatus())
                .refundReason(refund == null ? null : refund.getReason())
                .refundRequestedAt(refund == null ? null : refund.getRequestedTime())
                .refundProcessedAt(refund == null ? null : refund.getProcessedTime())
                .canRequestRefund(canRequestRefund(entry, competition, payment, refund, resultPublished))
                .delivery(toEntryDeliveryVO(delivery))
                .deliveryMethod(delivery == null ? null : delivery.getDeliveryMethod())
                .deliveryStatus(resolveDeliveryStatus(delivery))
                .carrier(delivery == null ? null : delivery.getCarrier())
                .trackingNo(delivery == null ? null : delivery.getTrackingNo())
                .canDownloadLabel(LABEL_ALLOWED_STATUSES.contains(entry.getStatus()) && !activeRefund)
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

    private List<AdminEntryTraceVO> listAdminEntryTraces(Long beerEntryId) {
        List<AdminEntryTraceVO> traces = new ArrayList<>();
        roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getBeerEntryId, beerEntryId)
                        .orderByAsc(RoundTableEntry::getRoundId)
                        .orderByAsc(RoundTableEntry::getSortOrder)
                        .orderByAsc(RoundTableEntry::getId))
                .forEach(item -> {
                    CompetitionRound round = competitionRoundMapper.selectById(item.getRoundId());
                    RoundTable table = roundTableMapper.selectById(item.getRoundTableId());
                    traces.add(AdminEntryTraceVO.builder()
                            .type("ROUND")
                            .roundId(item.getRoundId())
                            .roundName(round == null ? null : round.getRoundName())
                            .roundTableId(item.getRoundTableId())
                            .tableName(table == null ? null : table.getTableName())
                            .status(item.getStatus())
                            .build());
                });
        roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                        .eq(RoundResult::getBeerEntryId, beerEntryId)
                        .orderByAsc(RoundResult::getRoundId)
                        .orderByAsc(RoundResult::getRankNo)
                        .orderByAsc(RoundResult::getId))
                .forEach(item -> {
                    CompetitionRound round = competitionRoundMapper.selectById(item.getRoundId());
                    RoundTable table = roundTableMapper.selectById(item.getRoundTableId());
                    traces.add(AdminEntryTraceVO.builder()
                            .type("RESULT")
                            .roundId(item.getRoundId())
                            .roundName(round == null ? null : round.getRoundName())
                            .roundTableId(item.getRoundTableId())
                            .tableName(table == null ? null : table.getTableName())
                            .resultType(item.getResultType())
                            .rankNo(item.getRankNo())
                            .slotLabel(item.getSlotLabel())
                            .advanced(true)
                            .build());
                });
        awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                        .eq(AwardResult::getBeerEntryId, beerEntryId)
                        .orderByDesc(AwardResult::getChampionFlag)
                        .orderByAsc(AwardResult::getRankNo)
                        .orderByAsc(AwardResult::getId))
                .forEach(item -> traces.add(AdminEntryTraceVO.builder()
                        .type("AWARD")
                        .roundId(item.getSourceRoundId())
                        .roundTableId(item.getSourceRoundTableId())
                        .awardName(item.getAwardName())
                        .awardType(item.getAwardType())
                        .rankNo(item.getRankNo())
                        .advanced(true)
                        .build()));
        return traces;
    }

    private List<AdminEntryLogVO> listEntryLogs(String uuid) {
        return adminOperationLogMapper.selectList(new LambdaQueryWrapper<AdminOperationLog>()
                        .eq(AdminOperationLog::getTargetType, TARGET_ENTRY)
                        .eq(AdminOperationLog::getTargetPublicId, uuid)
                        .orderByDesc(AdminOperationLog::getId))
                .stream()
                .map(item -> AdminEntryLogVO.builder()
                        .id(item.getId())
                        .adminUserId(item.getAdminUserId())
                        .action(item.getAction())
                        .summary(item.getSummary())
                        .createTime(item.getCreateTime())
                        .build())
                .toList();
    }

    private boolean matchesEntryKeyword(AdminEntryVO item, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        List<String> candidates = new ArrayList<>();
        candidates.add(item.getName());
        candidates.add(item.getBreweryCompanyName());
        candidates.add(item.getCompetitionName());
        candidates.add(item.getUuid());
        candidates.add(item.getLabelCode());
        candidates.add(item.getShortCode());
        candidates.add(item.getCategoryName());
        candidates.add(item.getStyle());
        candidates.add(item.getCarrier());
        candidates.add(item.getTrackingNo());
        return candidates.stream()
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .anyMatch(value -> value.contains(lowerKeyword));
    }

    private boolean hasRoundAssignment(Long beerEntryId) {
        return roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, beerEntryId)) > 0;
    }

    private String formatEntryPathText(List<AdminEntryTraceVO> traces) {
        List<String> parts = traces.stream()
                .filter(item -> "ROUND".equals(item.getType()))
                .map(item -> {
                    List<String> labels = new ArrayList<>();
                    labels.add(item.getRoundName());
                    labels.add(item.getTableName());
                    return labels.stream()
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(" "));
                })
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (parts.isEmpty()) {
            return "未分桌";
        }
        return String.join(" -> ", parts);
    }

    private LocalDateTime resolveEntryLastModifiedAt(BeerEntry entry, EntryPayment payment, EntryDelivery delivery) {
        return resolveEntryLastModifiedAt(entry, payment, delivery, null);
    }

    private LocalDateTime resolveEntryLastModifiedAt(BeerEntry entry, EntryPayment payment, EntryDelivery delivery,
                                                     EntryRefund refund) {
        List<LocalDateTime> times = new ArrayList<>();
        times.add(entry.getUpdateTime());
        times.add(payment == null ? null : payment.getUpdateTime());
        times.add(delivery == null ? null : delivery.getUpdateTime());
        times.add(refund == null ? null : refund.getUpdateTime());
        return times.stream()
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(entry.getCreateTime());
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        CompetitionCategory category = competitionCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private void addChange(List<Map<String, String>> changes, String field, String before, String after) {
        String normalizedBefore = normalizeNullable(before);
        String normalizedAfter = normalizeNullable(after);
        if (Objects.equals(normalizedBefore, normalizedAfter)) {
            return;
        }
        Map<String, String> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("before", normalizedBefore == null ? "" : normalizedBefore);
        change.put("after", normalizedAfter == null ? "" : normalizedAfter);
        changes.add(change);
    }

    private void addExtraFieldChanges(List<Map<String, String>> changes, Long beerEntryId,
                                      List<EntryFieldConfig> fieldConfigs, Map<String, String> normalizedExtraFields) {
        Map<String, String> current = beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .collect(Collectors.toMap(BeerEntryExtraField::getFieldKey, BeerEntryExtraField::getFieldValue, (left, right) -> right));
        for (EntryFieldConfig config : fieldConfigs) {
            addChange(changes, config.getFieldLabel(), current.get(config.getFieldKey()), normalizedExtraFields.get(config.getFieldKey()));
        }
    }

    private String buildEntryLogSummary(String reason, List<Map<String, String>> changes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        payload.put("changes", changes);
        return writeObjectJson(payload, "保存修改记录失败");
    }

    private String buildStatusLogSummary(String action, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("reason", StringUtils.hasText(reason) ? reason : "");
        return writeObjectJson(payload, "保存状态记录失败");
    }

    private String normalizeStatusReason(AdminEntryStatusRequest request) {
        return request == null ? null : normalizeNullable(request.getReason());
    }

    private void writeEntryLog(String action, String targetPublicId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetType(TARGET_ENTRY)
                .targetPublicId(targetPublicId)
                .summary(summary)
                .build());
    }

    private EntryPayment findEntryPayment(Long beerEntryId) {
        return entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private EntryRefund findLatestRefund(Long beerEntryId) {
        return entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getBeerEntryId, beerEntryId)
                .orderByDesc(EntryRefund::getId)
                .last("LIMIT 1"));
    }

    private EntryRefund requireRefund(Long refundId) {
        EntryRefund refund = entryRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new ResourceNotFoundException("退款记录不存在");
        }
        return refund;
    }

    private boolean hasActiveRefund(Long beerEntryId) {
        return isActiveRefund(findLatestRefund(beerEntryId));
    }

    private boolean isActiveRefund(EntryRefund refund) {
        return refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private boolean canApproveRefund(EntryRefund refund) {
        return refund != null && (EntryRefundStatus.REQUESTED.name().equals(refund.getStatus())
                || EntryRefundStatus.FAILED.name().equals(refund.getStatus()));
    }

    private boolean canRejectRefund(EntryRefund refund) {
        return refund != null && EntryRefundStatus.REQUESTED.name().equals(refund.getStatus());
    }

    private boolean canUnmarkStored(BeerEntry entry, Competition competition, EntryRefund refund) {
        if (entry == null || !EntryStatus.STORED.name().equals(entry.getStatus()) || !Objects.equals(entry.getStoredFlag(), 1)) {
            return false;
        }
        if (isActiveRefund(refund) || isResultPublished(competition, entry)) {
            return false;
        }
        return !hasRoundAssignment(entry.getId())
                && !hasScoreRecord(entry.getId())
                && !hasRoundResult(entry.getId())
                && !hasAwardResult(entry.getId());
    }

    private String resolveUnmarkStoredUnavailableReason(BeerEntry entry, Competition competition, EntryRefund refund) {
        if (entry == null || !EntryStatus.STORED.name().equals(entry.getStatus()) || !Objects.equals(entry.getStoredFlag(), 1)) {
            return "只有已入库酒款可以撤销入库";
        }
        if (isActiveRefund(refund)) {
            return "退款处理中，不能撤销入库";
        }
        if (isResultPublished(competition, entry)) {
            return "结果已发布，不能撤销入库";
        }
        if (hasRoundAssignment(entry.getId()) || hasScoreRecord(entry.getId()) || hasRoundResult(entry.getId()) || hasAwardResult(entry.getId())) {
            return "酒款已进入评审或结果流程，不能撤销入库";
        }
        return "当前酒款不能撤销入库";
    }

    private boolean hasScoreRecord(Long beerEntryId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)) > 0;
    }

    private boolean hasRoundResult(Long beerEntryId) {
        return roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getBeerEntryId, beerEntryId)) > 0;
    }

    private boolean hasAwardResult(Long beerEntryId) {
        return awardResultMapper.selectCount(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, beerEntryId)) > 0;
    }

    private void assertCanRequestRefund(BeerEntry entry, Competition competition, EntryPayment payment) {
        boolean resultPublished = isResultPublished(competition, entry);
        EntryRefund latestRefund = findLatestRefund(entry.getId());
        if (!canRequestRefund(entry, competition, payment, latestRefund, resultPublished)) {
            throw new BaseException(resolveRefundUnavailableReason(entry, competition, payment, latestRefund, resultPublished));
        }
    }

    private boolean canRequestRefund(BeerEntry entry, Competition competition, EntryPayment payment,
                                     EntryRefund refund, boolean resultPublished) {
        if (entry == null || competition == null || payment == null) {
            return false;
        }
        if (!EntryStatus.REGISTERED.name().equals(entry.getStatus())) {
            return false;
        }
        if (!EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return false;
        }
        if (competition.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            return false;
        }
        if (Objects.equals(entry.getStoredFlag(), 1) || hasRoundAssignment(entry.getId()) || resultPublished) {
            return false;
        }
        return refund == null || !ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private String resolveRefundUnavailableReason(BeerEntry entry, Competition competition, EntryPayment payment,
                                                  EntryRefund refund, boolean resultPublished) {
        if (entry == null || competition == null || payment == null) {
            return "当前酒款不能申请退款";
        }
        if (!EntryStatus.REGISTERED.name().equals(entry.getStatus())) {
            return "只有报名成功的酒款可以申请退款";
        }
        if (!EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return "只有已付款酒款可以申请退款";
        }
        if (competition.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            return "报名截止后不能申请退款";
        }
        if (Objects.equals(entry.getStoredFlag(), 1)) {
            return "酒样已入库，不能申请退款";
        }
        if (hasRoundAssignment(entry.getId())) {
            return "酒款已进入评审编排，不能申请退款";
        }
        if (resultPublished) {
            return "结果已发布，不能申请退款";
        }
        if (refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus())) {
            return "退款正在处理中，请勿重复申请";
        }
        return "当前酒款不能申请退款";
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
            if (value != null && value.length() > EXTRA_FIELD_VALUE_MAX_LENGTH) {
                throw new BaseException(config.getFieldLabel() + "不能超过" + EXTRA_FIELD_VALUE_MAX_LENGTH + "个字符");
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
                .amount(resolveEntryFee(competition, LocalDateTime.now()))
                .status(EntryPaymentStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .build();
        entryPaymentMapper.insert(created);
        return created;
    }

    private BigDecimal resolveEntryFee(Competition competition, LocalDateTime now) {
        if (competition == null) {
            return BigDecimal.ZERO;
        }
        if (competition.getEarlyBirdFee() != null
                && competition.getEarlyBirdDeadline() != null
                && !now.isAfter(competition.getEarlyBirdDeadline())) {
            return competition.getEarlyBirdFee();
        }
        return competition.getEntryFee() == null ? BigDecimal.ZERO : competition.getEntryFee();
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
                .bankTransferId(payment == null ? null : payment.getBankTransferId())
                .codeUrl(payment == null ? null : payment.getCodeUrl())
                .expireTime(payment == null ? null : payment.getExpireTime())
                .paidAmount(payment == null ? null : payment.getPaidAmount())
                .wechatTradeState(payment == null ? null : payment.getWechatTradeState())
                .wechatTradeStateDesc(payment == null ? null : payment.getWechatTradeStateDesc())
                .paidTime(payment == null ? null : payment.getPaidTime())
                .build();
    }

    private EntryRefundVO toEntryRefundVO(EntryRefund refund) {
        if (refund == null) {
            return null;
        }
        return EntryRefundVO.builder()
                .id(refund.getId())
                .beerEntryId(refund.getBeerEntryId())
                .entryPaymentId(refund.getEntryPaymentId())
                .refundNo(refund.getRefundNo())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .requestedByPortalId(refund.getRequestedByPortalId())
                .requestedTime(refund.getRequestedTime())
                .processedByAdminId(refund.getProcessedByAdminId())
                .processedTime(refund.getProcessedTime())
                .successTime(refund.getSuccessTime())
                .failReason(refund.getFailReason())
                .wechatRefundId(refund.getWechatRefundId())
                .wechatRefundStatus(refund.getWechatRefundStatus())
                .outRefundNo(refund.getOutRefundNo())
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
                .avatarUrl(brewery == null ? null : brewery.getAvatarUrl())
                .build();
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BaseException("头像图片不能超过 5MB");
        }
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "avatar.png").toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        boolean allowedExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp");
        boolean allowedContentType = contentType != null && AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        if (!allowedExtension || !allowedContentType) {
            throw new BaseException("头像仅支持 JPG、PNG、WebP 图片");
        }
    }

    private byte[] readUploadBytes(MultipartFile file, String errorMessage) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private String sanitizeUploadFilename(String originalFilename, String defaultFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim() : defaultFilename;
        filename = filename.replace("\\", "/");
        int index = filename.lastIndexOf('/');
        return index >= 0 ? filename.substring(index + 1) : filename;
    }

    private String resolveUploadPublicUrl(String storagePath) {
        if (!"local".equalsIgnoreCase(storageProperties.getProvider())) {
            return storagePath;
        }
        Path baseDir = Path.of(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize();
        Path storedFile = Path.of(storagePath).toAbsolutePath().normalize();
        String relativePath = baseDir.relativize(storedFile).toString().replace("\\", "/");
        return "/uploads/" + relativePath;
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

    private boolean isCompetitionArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        return competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus());
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        if (isCompetitionArchived(competitionId)) {
            throw new ResourceNotFoundException("赛事不存在");
        }
    }

    private String resolvePaymentStatus(BeerEntry entry, EntryPayment payment) {
        if (payment != null && StringUtils.hasText(payment.getStatus())) {
            return payment.getStatus();
        }
        if (EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return EntryPaymentStatus.UNPAID.name();
        }
        return LABEL_ALLOWED_STATUSES.contains(entry.getStatus())
                ? EntryPaymentStatus.PAID.name()
                : EntryPaymentStatus.UNPAID.name();
    }

    private boolean canConfirmPayment(BeerEntry entry, EntryPayment payment) {
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return false;
        }
        return payment == null || EntryPaymentStatus.UNPAID.name().equals(payment.getStatus());
    }

    private boolean canCancelEntry(BeerEntry entry, EntryPayment payment) {
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return false;
        }
        return payment == null || !EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus());
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

    private String generateMockOutTradeNo() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }

    private String generateRefundNo() {
        return "RF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
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

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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

    private String writeObjectJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private record PublishedResultContext(
            Map<Long, Competition> competitionById,
            Map<Long, BeerEntry> entryById,
            Map<Long, Brewery> breweryById,
            Map<Long, CompetitionCategory> categoryById
    ) {
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
