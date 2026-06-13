package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BankTransferPaymentEntryMapper;
import com.beercompetition.mapper.BankTransferPaymentMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.enums.BankTransferPaymentStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryScanLabelStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BankTransferPayment;
import com.beercompetition.pojo.po.BankTransferPaymentEntry;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.BankTransferAccountVO;
import com.beercompetition.pojo.vo.BankTransferEntryVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.pojo.vo.BankTransferVoucherVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.service.BankTransferPaymentService;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
public class BankTransferPaymentServiceImpl implements BankTransferPaymentService {

    private static final String BUSINESS_TYPE_BANK_TRANSFER_VOUCHER = "BANK_TRANSFER_VOUCHER";
    private static final String OWNER_TYPE_PORTAL_ACCOUNT = "PORTAL_ACCOUNT";
    private static final String TARGET_ENTRY = "BEER_ENTRY";
    private static final long MAX_VOUCHER_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> VOUCHER_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final BankTransferPaymentMapper bankTransferPaymentMapper;
    private final BankTransferPaymentEntryMapper bankTransferPaymentEntryMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final BreweryMapper breweryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryScanLabelMapper entryScanLabelMapper;
    private final FileAssetMapper fileAssetMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Override
    public BankTransferAccountVO getAccount() {
        // 1) 返回固定收款账户信息
        return BankTransferAccountVO.builder()
                .accountName("上海啤事文化传播有限公司")
                .bankName("招商银行上海分行马当路支行")
                .accountNo("121941020210501")
                .remarkTip("转账时请务必备注厂牌名")
                .invoiceTip("如需开发票或有任何问题，请联系啤酒事务局小秘书微信")
                .serviceWechat("beerxms")
                .build();
    }

    @Override
    public BankTransferVoucherVO uploadVoucher(MultipartFile file) {
        // 1) 校验当前厂商账号和凭证文件
        PortalAccount account = requirePortalAccount();
        validateVoucherFile(file);
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "bank-transfer-voucher.pdf");
        byte[] bytes = readUploadBytes(file);

        // 2) 上传 OSS/本地存储并登记 file_asset
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_BANK_TRANSFER_VOUCHER, filename, bytes);
        String publicUrl = resolveUploadPublicUrl(storagePath);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_BANK_TRANSFER_VOUCHER)
                .ownerType(OWNER_TYPE_PORTAL_ACCOUNT)
                .ownerId(account.getId())
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);

        // 3) 返回前端可展示的文件信息
        return BankTransferVoucherVO.builder()
                .fileAssetId(asset.getId())
                .fileName(asset.getFileName())
                .publicUrl(asset.getPublicUrl())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankTransferVO submitPortalTransfer(PortalBankTransferSubmitRequest request) {
        // 1) 参数规范化与账号校验
        PortalAccount account = requirePortalAccount();
        Brewery brewery = requireBrewery(account.getBreweryId());
        List<Long> entryIds = normalizeEntryIds(request.getEntryIds());
        BigDecimal submittedAmount = normalizeAmount(request.getAmount());
        String payerName = normalizeNullable(request.getPayerName());
        String remark = normalizeRequired(request.getRemark(), "请填写转账备注");
        FileAsset voucher = resolveVoucherAsset(request.getVoucherAssetId(), account.getId());

        // 2) 读取并校验所选酒款与付款记录
        List<BeerEntry> entries = requireOwnedPayableEntries(entryIds, brewery.getId());
        Long competitionId = requireSingleCompetition(entries);
        Map<Long, EntryPayment> paymentByEntryId = ensurePayments(entries);
        BigDecimal expectedAmount = entries.stream()
                .map(entry -> paymentByEntryId.get(entry.getId()).getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (normalizeAmount(expectedAmount).compareTo(submittedAmount) != 0) {
            throw new BaseException("转账金额需与所选酒款应付金额一致");
        }

        // 3) 创建转账批次并锁定关联付款
        BankTransferPayment transfer = BankTransferPayment.builder()
                .transferNo(generateTransferNo())
                .breweryId(brewery.getId())
                .portalAccountId(account.getId())
                .competitionId(competitionId)
                .amount(submittedAmount)
                .payerName(payerName)
                .transferTime(request.getTransferTime())
                .remark(remark)
                .voucherAssetId(voucher == null ? null : voucher.getId())
                .status(BankTransferPaymentStatus.SUBMITTED.name())
                .submittedTime(LocalDateTime.now())
                .build();
        bankTransferPaymentMapper.insert(transfer);
        for (BeerEntry entry : entries) {
            EntryPayment payment = paymentByEntryId.get(entry.getId());
            bankTransferPaymentEntryMapper.insert(BankTransferPaymentEntry.builder()
                    .bankTransferPaymentId(transfer.getId())
                    .beerEntryId(entry.getId())
                    .entryPaymentId(payment.getId())
                    .amount(payment.getAmount())
                    .build());
            payment.setStatus(EntryPaymentStatus.PENDING_CONFIRM.name());
            payment.setPayMethod(EntryPayMethod.BANK_TRANSFER.name());
            payment.setBankTransferId(transfer.getId());
            entryPaymentMapper.updateById(payment);
        }

        // 4) 返回批次详情
        return toBankTransferVO(bankTransferPaymentMapper.selectById(transfer.getId()), true);
    }

    @Override
    public BankTransferVO getPortalTransfer(Long id) {
        // 1) 校验当前厂商归属
        PortalAccount account = requirePortalAccount();
        BankTransferPayment transfer = requireTransfer(id);
        if (!Objects.equals(transfer.getPortalAccountId(), account.getId())) {
            throw new ForbiddenException("无权查看该转账记录");
        }

        // 2) 返回批次详情
        return toBankTransferVO(transfer, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankTransferVO cancelPortalTransfer(Long id) {
        // 1) 校验当前厂商归属和批次状态
        PortalAccount account = requirePortalAccount();
        BankTransferPayment transfer = requireTransfer(id);
        if (!Objects.equals(transfer.getPortalAccountId(), account.getId())) {
            throw new ForbiddenException("无权取消该转账记录");
        }
        if (!BankTransferPaymentStatus.SUBMITTED.name().equals(transfer.getStatus())) {
            throw new BaseException("当前转账记录不能取消");
        }

        // 2) 取消批次并释放关联付款
        transfer.setStatus(BankTransferPaymentStatus.CANCELED.name());
        transfer.setProcessedTime(LocalDateTime.now());
        bankTransferPaymentMapper.updateById(transfer);
        resetRelatedPayments(transfer.getId(), BankTransferPaymentStatus.CANCELED.name(), null);

        // 3) 返回批次详情
        return toBankTransferVO(bankTransferPaymentMapper.selectById(id), true);
    }

    @Override
    public PageResult<BankTransferVO> listAdminTransfers(String status, Long competitionId, String keyword,
                                                         Integer page, Integer pageSize) {
        // 1) 参数规范化并查询批次
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 30 : pageSize, 1), 100);
        String normalizedKeyword = normalizeNullable(keyword);
        List<BankTransferVO> filtered = bankTransferPaymentMapper.selectList(new LambdaQueryWrapper<BankTransferPayment>()
                        .eq(StringUtils.hasText(status), BankTransferPayment::getStatus, status)
                        .eq(competitionId != null, BankTransferPayment::getCompetitionId, competitionId)
                        .orderByDesc(BankTransferPayment::getSubmittedTime)
                        .orderByDesc(BankTransferPayment::getId))
                .stream()
                .map(transfer -> toBankTransferVO(transfer, false))
                .filter(item -> !StringUtils.hasText(normalizedKeyword) || matchesKeyword(item, normalizedKeyword))
                .toList();

        // 2) 内存分页并返回
        int fromIndex = Math.min((currentPage - 1) * currentPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + currentPageSize, filtered.size());
        return new PageResult<>(filtered.size(), filtered.subList(fromIndex, toIndex));
    }

    @Override
    public BankTransferVO getAdminTransfer(Long id) {
        // 1) 查询批次详情
        return toBankTransferVO(requireTransfer(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankTransferVO confirmTransfer(Long id, AdminBankTransferProcessRequest request) {
        // 1) 校验批次状态
        BankTransferPayment transfer = requireTransfer(id);
        if (!BankTransferPaymentStatus.SUBMITTED.name().equals(transfer.getStatus())) {
            throw new BaseException("只有待确认的转账可以确认到账");
        }

        // 2) 推进批次、付款和报名状态
        LocalDateTime now = LocalDateTime.now();
        transfer.setStatus(BankTransferPaymentStatus.CONFIRMED.name());
        transfer.setAdminId(BaseContext.getCurrentId());
        transfer.setAdminNote(normalizeNullable(request == null ? null : request.getAdminNote()));
        transfer.setProcessedTime(now);
        bankTransferPaymentMapper.updateById(transfer);
        List<BankTransferPaymentEntry> relations = listTransferEntries(id);
        for (BankTransferPaymentEntry relation : relations) {
            EntryPayment payment = entryPaymentMapper.selectById(relation.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(relation.getBeerEntryId());
            if (payment == null || entry == null) {
                throw new BaseException("转账关联酒款数据异常");
            }
            payment.setStatus(EntryPaymentStatus.PAID.name());
            payment.setPayMethod(EntryPayMethod.BANK_TRANSFER.name());
            payment.setPaidAmount(payment.getAmount());
            payment.setPaidTime(now);
            payment.setConfirmedByAdminId(BaseContext.getCurrentId());
            payment.setConfirmRemark(transfer.getAdminNote());
            entryPaymentMapper.updateById(payment);
            if (EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
                entry.setStatus(EntryStatus.REGISTERED.name());
                beerEntryMapper.updateById(entry);
            }
            writeEntryLog("ENTRY_BANK_TRANSFER_CONFIRM", entry.getUuid(), "银行转账确认到账");
        }

        // 3) 返回批次详情
        return toBankTransferVO(bankTransferPaymentMapper.selectById(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankTransferVO rejectTransfer(Long id, AdminBankTransferProcessRequest request) {
        // 1) 校验批次状态
        BankTransferPayment transfer = requireTransfer(id);
        if (!BankTransferPaymentStatus.SUBMITTED.name().equals(transfer.getStatus())) {
            throw new BaseException("只有待确认的转账可以驳回");
        }

        // 2) 驳回批次并释放关联付款
        String adminNote = normalizeRequired(request == null ? null : request.getAdminNote(), "请填写驳回原因");
        transfer.setStatus(BankTransferPaymentStatus.REJECTED.name());
        transfer.setAdminId(BaseContext.getCurrentId());
        transfer.setAdminNote(adminNote);
        transfer.setProcessedTime(LocalDateTime.now());
        bankTransferPaymentMapper.updateById(transfer);
        resetRelatedPayments(id, BankTransferPaymentStatus.REJECTED.name(), adminNote);

        // 3) 返回批次详情
        return toBankTransferVO(bankTransferPaymentMapper.selectById(id), true);
    }

    @Override
    public FileDownloadVO downloadVoucher(Long id) {
        // 1) 查询批次与凭证资产
        BankTransferPayment transfer = requireTransfer(id);
        if (transfer.getVoucherAssetId() == null) {
            throw new ResourceNotFoundException("该转账记录没有上传凭证");
        }
        FileAsset asset = fileAssetMapper.selectById(transfer.getVoucherAssetId());
        if (asset == null) {
            throw new ResourceNotFoundException("凭证文件不存在");
        }

        // 2) 读取文件内容并返回
        return FileDownloadVO.builder()
                .fileName(asset.getFileName())
                .contentType(resolveContentType(asset.getFileName()))
                .content(fileStorageService.download(asset.getStoragePath()))
                .build();
    }

    private void resetRelatedPayments(Long transferId, String actionStatus, String note) {
        List<BankTransferPaymentEntry> relations = listTransferEntries(transferId);
        for (BankTransferPaymentEntry relation : relations) {
            EntryPayment payment = entryPaymentMapper.selectById(relation.getEntryPaymentId());
            BeerEntry entry = beerEntryMapper.selectById(relation.getBeerEntryId());
            if (payment != null && EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())
                    && Objects.equals(payment.getBankTransferId(), transferId)) {
                entryPaymentMapper.update(null, new LambdaUpdateWrapper<EntryPayment>()
                        .eq(EntryPayment::getId, payment.getId())
                        .set(EntryPayment::getStatus, EntryPaymentStatus.UNPAID.name())
                        .set(EntryPayment::getPayMethod, EntryPayMethod.MANUAL.name())
                        .set(EntryPayment::getBankTransferId, null)
                        .set(EntryPayment::getConfirmRemark, note));
            }
            if (entry != null && BankTransferPaymentStatus.REJECTED.name().equals(actionStatus)) {
                writeEntryLog("ENTRY_BANK_TRANSFER_REJECT", entry.getUuid(), "银行转账被驳回");
            }
        }
    }

    private List<Long> normalizeEntryIds(List<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            throw new BaseException("请选择需要付款的酒款");
        }
        return entryIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private List<BeerEntry> requireOwnedPayableEntries(List<Long> entryIds, Long breweryId) {
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .in(BeerEntry::getId, entryIds)
                .eq(BeerEntry::getBreweryId, breweryId));
        if (entries.size() != entryIds.size()) {
            throw new BaseException("所选酒款不存在或不属于当前厂牌");
        }
        Map<Long, BeerEntry> entryById = entries.stream().collect(Collectors.toMap(BeerEntry::getId, Function.identity()));
        return entryIds.stream()
                .map(entryById::get)
                .peek(entry -> {
                    if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
                        throw new BaseException("只有待付款酒款可以提交银行转账");
                    }
                })
                .toList();
    }

    private Long requireSingleCompetition(List<BeerEntry> entries) {
        Set<Long> competitionIds = entries.stream().map(BeerEntry::getCompetitionId).collect(Collectors.toSet());
        if (competitionIds.size() != 1) {
            throw new BaseException("一次银行转账只能选择同一场赛事的酒款");
        }
        return competitionIds.iterator().next();
    }

    private Map<Long, EntryPayment> ensurePayments(List<BeerEntry> entries) {
        List<Long> entryIds = entries.stream().map(BeerEntry::getId).toList();
        Map<Long, EntryPayment> paymentByEntryId = entryPaymentMapper.selectList(new LambdaQueryWrapper<EntryPayment>()
                        .in(EntryPayment::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryPayment::getBeerEntryId, Function.identity(), (left, right) -> left));
        for (BeerEntry entry : entries) {
            EntryPayment payment = paymentByEntryId.get(entry.getId());
            if (payment == null) {
                Competition competition = competitionMapper.selectById(entry.getCompetitionId());
                payment = EntryPayment.builder()
                        .beerEntryId(entry.getId())
                        .amount(resolveEntryFee(competition, LocalDateTime.now()))
                        .status(EntryPaymentStatus.UNPAID.name())
                        .payMethod(EntryPayMethod.MANUAL.name())
                        .build();
                entryPaymentMapper.insert(payment);
                paymentByEntryId.put(entry.getId(), payment);
            }
            if (!EntryPaymentStatus.UNPAID.name().equals(payment.getStatus())) {
                throw new BaseException("所选酒款包含已付款或等待确认的记录");
            }
        }
        return paymentByEntryId;
    }

    private BankTransferVO toBankTransferVO(BankTransferPayment transfer, boolean includeEntries) {
        Competition competition = competitionMapper.selectById(transfer.getCompetitionId());
        Brewery brewery = breweryMapper.selectById(transfer.getBreweryId());
        FileAsset voucher = transfer.getVoucherAssetId() == null ? null : fileAssetMapper.selectById(transfer.getVoucherAssetId());
        List<BankTransferPaymentEntry> relations = listTransferEntries(transfer.getId());
        List<BankTransferEntryVO> entries = includeEntries
                ? relations.stream().map(this::toTransferEntryVO).filter(Objects::nonNull).toList()
                : List.of();
        return BankTransferVO.builder()
                .id(transfer.getId())
                .transferNo(transfer.getTransferNo())
                .competitionId(transfer.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .breweryId(transfer.getBreweryId())
                .breweryName(brewery == null ? null : brewery.getCompanyName())
                .portalAccountId(transfer.getPortalAccountId())
                .amount(transfer.getAmount())
                .entryCount(relations.size())
                .payerName(transfer.getPayerName())
                .transferTime(transfer.getTransferTime())
                .remark(transfer.getRemark())
                .voucherAssetId(transfer.getVoucherAssetId())
                .voucherFileName(voucher == null ? null : voucher.getFileName())
                .voucherPublicUrl(voucher == null ? null : voucher.getPublicUrl())
                .status(transfer.getStatus())
                .adminId(transfer.getAdminId())
                .adminNote(transfer.getAdminNote())
                .submittedTime(transfer.getSubmittedTime())
                .processedTime(transfer.getProcessedTime())
                .entries(entries)
                .build();
    }

    private BankTransferEntryVO toTransferEntryVO(BankTransferPaymentEntry relation) {
        BeerEntry entry = beerEntryMapper.selectById(relation.getBeerEntryId());
        if (entry == null) {
            return null;
        }
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entryScanLabelMapper.selectOne(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getBeerEntryId, entry.getId())
                .eq(EntryScanLabel::getStatus, EntryScanLabelStatus.ACTIVE.name())
                .last("LIMIT 1"));
        return BankTransferEntryVO.builder()
                .entryId(entry.getId())
                .paymentId(relation.getEntryPaymentId())
                .entryName(entry.getName())
                .shortCode(label == null ? null : label.getShortCode())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .amount(relation.getAmount())
                .build();
    }

    private boolean matchesKeyword(BankTransferVO item, String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(item.getTransferNo(), normalized)
                || contains(item.getBreweryName(), normalized)
                || contains(item.getCompetitionName(), normalized)
                || contains(item.getRemark(), normalized)
                || contains(item.getPayerName(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private List<BankTransferPaymentEntry> listTransferEntries(Long transferId) {
        return bankTransferPaymentEntryMapper.selectList(new LambdaQueryWrapper<BankTransferPaymentEntry>()
                .eq(BankTransferPaymentEntry::getBankTransferPaymentId, transferId)
                .orderByAsc(BankTransferPaymentEntry::getId));
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ForbiddenException("请先登录厂商账号");
        }
        return account;
    }

    private Brewery requireBrewery(Long breweryId) {
        Brewery brewery = breweryMapper.selectById(breweryId);
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return brewery;
    }

    private BankTransferPayment requireTransfer(Long id) {
        BankTransferPayment transfer = bankTransferPaymentMapper.selectById(id);
        if (transfer == null) {
            throw new ResourceNotFoundException("转账记录不存在");
        }
        return transfer;
    }

    private FileAsset resolveVoucherAsset(Long assetId, Long portalAccountId) {
        if (assetId == null) {
            return null;
        }
        FileAsset asset = fileAssetMapper.selectById(assetId);
        if (asset == null || !BUSINESS_TYPE_BANK_TRANSFER_VOUCHER.equals(asset.getBusinessType())) {
            throw new BaseException("转账凭证不存在");
        }
        if (!OWNER_TYPE_PORTAL_ACCOUNT.equals(asset.getOwnerType())
                || !Objects.equals(asset.getOwnerId(), portalAccountId)) {
            throw new BaseException("转账凭证不存在");
        }
        return asset;
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

    private void validateVoucherFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择转账凭证");
        }
        if (file.getSize() > MAX_VOUCHER_SIZE) {
            throw new BaseException("转账凭证不能超过 10MB");
        }
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "bank-transfer-voucher.pdf").toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        boolean allowedExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp")
                || filename.endsWith(".pdf");
        boolean allowedContentType = contentType != null && VOUCHER_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        if (!allowedExtension || !allowedContentType) {
            throw new BaseException("转账凭证仅支持 JPG、PNG、WebP 或 PDF");
        }
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException("读取转账凭证失败");
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

    private String resolveContentType(String filename) {
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BaseException("请填写转账金额");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
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

    private String generateTransferNo() {
        return "BT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
    }

    private void writeEntryLog(String action, String targetId, String summary) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .action(action)
                .targetType(TARGET_ENTRY)
                .targetPublicId(targetId)
                .summary(summary)
                .createTime(LocalDateTime.now())
                .build());
    }
}
