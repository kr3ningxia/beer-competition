package com.beercompetition.registration.refund;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.file.FileAccessService;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminOfflineRefundRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RefundApprovalMode;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.RefundPreviewVO;
import com.beercompetition.service.WechatPaymentService;
import com.beercompetition.registration.payment.CompetitionPricingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.beercompetition.registration.refund.EntryRefundService;
import com.beercompetition.registration.entry.PortalEntryViewAssembler;
import com.beercompetition.registration.entry.AdminEntryListAssembler;
import com.beercompetition.registration.entry.PortalAccountAccessService;

/**
 * 处理退款申请、审批、线下登记和失败重试。
 */
@Service
@RequiredArgsConstructor
public class EntryRefundServiceImpl implements EntryRefundService {

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

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final OrganizerMapper organizerMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final PaymentOrderItemMapper paymentOrderItemMapper;

    private final WechatPaymentService wechatPaymentService;

    private final ObjectMapper objectMapper;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    private final AdminEntryListAssembler adminEntryListAssembler;

    private final PortalAccountAccessService portalAccountAccessService;

    private final OfflineRefundRegistrationService offlineRefundRegistrationService;

    private final CompetitionAccessService competitionAccessService;
    private final FileAccessService fileAccessService;

    private final CompetitionPricingService competitionPricingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO requestPortalEntryRefund(Long entryId, PortalEntryRefundRequest request) {
        // 1) 查询厂商酒款并校验退款资格
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryPayment payment = lockEntryPayment(ensureEntryPayment(entry.getId(), entry.getCompetitionId()).getId());
        assertCanRequestRefund(entry, competition, payment);
        int activeCount = countActiveEntries(entry.getCompetitionId(), entry.getBreweryId());
        BigDecimal refundAmount = calculateRefundAmount(competition, payment, activeCount);
        RefundApprovalMode approvalMode = resolveRefundApprovalMode(competition);

        // 2) 创建退款申请记录
        EntryRefund refund = EntryRefund.builder()
                .beerEntryId(entry.getId())
                .entryPaymentId(payment.getId())
                .paymentOrderItemId(findPaymentOrderItemId(payment))
                .refundNo(generateRefundNo())
                .amount(refundAmount)
                .status(EntryRefundStatus.REQUESTED.name())
                .approvalModeSnapshot(approvalMode.name())
                .reason(normalizeRequired(request.getReason(), "请填写退款原因"))
                .requestedByPortalId(account.getId())
                .requestedTime(LocalDateTime.now())
                .build();
        entryRefundMapper.insert(refund);
        if (approvalMode == RefundApprovalMode.AUTO_APPROVE) {
            scheduleRefundAutoApproval(refund.getId());
        }

        // 3) 返回最新酒款详情
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    @Override
    public RefundPreviewVO previewPortalEntryRefund(Long entryId) {
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        assertCanRequestRefund(entry, competition, payment);
        int before = countActiveEntries(entry.getCompetitionId(), entry.getBreweryId());
        BigDecimal amount = calculateRefundAmount(competition, payment, before);
        return RefundPreviewVO.builder()
                .beerEntryId(entry.getId()).competitionId(entry.getCompetitionId())
                .activeEntryCountBefore(before).activeEntryCountAfter(Math.max(0, before - 1))
                .currentPaidAmount(payment.getAmount()).refundAmount(amount)
                .remainingAmountAfterRefund(payment.getAmount().subtract(amount).max(BigDecimal.ZERO))
                .pricingNote("退款按退款前累计数量的最后一档边际价格计算，不按被退酒款原价计算")
                .build();
    }

    private int countActiveEntries(Long competitionId, Long breweryId) {
        return Math.toIntExact(beerEntryMapper.selectCount(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .eq(BeerEntry::getBreweryId, breweryId)
                .ne(BeerEntry::getStatus, EntryStatus.CANCELED.name())));
    }

    private BigDecimal calculateRefundAmount(Competition competition, EntryPayment payment, int activeCount) {
        if (activeCount <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = payment.getPricingBaseAmount();
        if (base == null) {
            base = payment.getAmount();
        }
        BigDecimal amount = competitionPricingService.priceForSequence(competition, base, activeCount);
        return amount.min(payment.getAmount()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Long findPaymentOrderItemId(EntryPayment payment) {
        if (payment == null || payment.getPaymentOrderId() == null) {
            return null;
        }
        PaymentOrderItem item = paymentOrderItemMapper.selectOne(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getEntryPaymentId, payment.getId())
                .last("LIMIT 1"));
        return item == null ? null : item.getId();
    }

    @Override
    public PageResult<AdminEntryVO> listAdminRefunds(String status, Integer page, Integer pageSize) {
        // 1) 查询退款记录关联酒款
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 30 : pageSize, 1), 100);
        List<Long> visibleCompetitionIds = competitionAccessService.canAccessAllOrganizers()
                ? null : visibleCompetitionIds();
        List<EntryRefund> refunds = entryRefundMapper.selectAdminRefunds(status, visibleCompetitionIds);

        // 2) 组装酒款列表并分页
        List<AdminEntryVO> records = refunds.stream()
                .map(refund -> beerEntryMapper.selectById(refund.getBeerEntryId()))
                .filter(Objects::nonNull)
                .map(adminEntryListAssembler::toAdminEntryVO)
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
        BeerEntry entry = requireAdminEntry(refund.getBeerEntryId());

        // 2) 驳回退款并保留报名
        refund.setStatus(EntryRefundStatus.REJECTED.name());
        refund.setProcessedByAdminId(BaseContext.getCurrentId());
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(normalizeStatusReason(request));
        entryRefundMapper.updateById(refund);
        writeEntryLog("ENTRY_REFUND_REJECT", entry.getUuid(), buildStatusLogSummary("驳回退款", normalizeStatusReason(request)));
    }

    @Override
    public void completeOfflineRefund(Long refundId, AdminEntryStatusRequest request) {
        wechatPaymentService.completeOfflineRefund(refundId, request.getReason(), BaseContext.getCurrentId());
    }

    @Override
    public void registerOfflineRefund(Long refundId, AdminOfflineRefundRequest request, MultipartFile voucher) {
        offlineRefundRegistrationService.register(refundId, request, voucher);
    }

    @Override
    public void retryRefund(Long refundId, AdminEntryStatusRequest request) {
        // 1) 委托微信支付服务重试失败退款
        wechatPaymentService.retryRefund(refundId, normalizeStatusReason(request), BaseContext.getCurrentId());
    }

    @Override
    public com.beercompetition.pojo.vo.FileDownloadVO downloadOfflineVoucher(Long refundId) {
        EntryRefund refund = requireRefund(refundId);
        if (refund.getOfflineRefundVoucherAssetId() == null) {
            throw new ResourceNotFoundException("该退款记录没有打款凭证");
        }
        return fileAccessService.download(refund.getOfflineRefundVoucherAssetId());
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
        requireAdminEntry(refund.getBeerEntryId());
        return refund;
    }

    private void assertCanRequestRefund(BeerEntry entry, Competition competition, EntryPayment payment) {
        EntryRefund latestRefund = findLatestRefund(entry.getId());
        if (!canRequestRefund(entry, competition, payment, latestRefund)) {
            throw new BaseException(resolveRefundUnavailableReason(entry, competition, payment, latestRefund));
        }
    }

    private boolean canRequestRefund(BeerEntry entry, Competition competition, EntryPayment payment,
                                     EntryRefund refund) {
        if (entry == null || competition == null || payment == null) {
            return false;
        }
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            return false;
        }
        if (!EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return false;
        }
        if (competition.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            return false;
        }
        return refund == null || EntryRefundStatus.REJECTED.name().equals(refund.getStatus());
    }

    private String resolveRefundUnavailableReason(BeerEntry entry, Competition competition, EntryPayment payment,
                                                  EntryRefund refund) {
        if (entry == null || competition == null || payment == null) {
            return "当前酒款不能申请退款";
        }
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            return "只有报名成功的酒款可以申请退款";
        }
        if (!EntryPaymentStatus.PAID.name().equals(payment.getStatus())) {
            return "只有已支付酒款可以申请退款";
        }
        if (competition.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            return "报名截止后不能申请退款";
        }
        if (refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus())) {
            return "退款正在处理中，请勿重复申请";
        }
        if (refund != null && EntryRefundStatus.FAILED.name().equals(refund.getStatus())) {
            return "退款暂未成功，请等待组委会处理或联系确认";
        }
        if (refund != null && !EntryRefundStatus.REJECTED.name().equals(refund.getStatus())) {
            return "这款酒已有退款记录，请勿重复申请";
        }
        return "当前酒款不能申请退款";
    }

    private void scheduleRefundAutoApproval(Long refundId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        wechatPaymentService.autoApproveRefund(refundId, "报名截止前自动受理");
                    } catch (RuntimeException ignored) {
                        // 退款申请已保存，自动受理失败时由后台退款列表继续处理。
                    }
                }
            });
            return;
        }
        wechatPaymentService.autoApproveRefund(refundId, "报名截止前自动受理");
    }

    private RefundApprovalMode resolveRefundApprovalMode(Competition competition) {
        Organizer organizer = competition == null ? null : organizerMapper.selectById(competition.getOrganizerId());
        if (organizer != null && OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            return RefundApprovalMode.MANUAL_REVIEW;
        }
        return RefundApprovalMode.of(competition == null ? null : competition.getRefundApprovalMode());
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

    private EntryPayment lockEntryPayment(Long paymentId) {
        EntryPayment payment = entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getId, paymentId)
                .last("FOR UPDATE"));
        if (payment == null) {
            throw new ResourceNotFoundException("支付记录不存在");
        }
        return payment;
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

    private BeerEntry requireOwnedEntry(Long entryId, Long breweryId) {
        BeerEntry entry = requirePortalEntry(entryId);
        if (!entry.getBreweryId().equals(breweryId)) {
            throw new ForbiddenException("无权查看该酒款");
        }
        return entry;
    }

    private BeerEntry requirePortalEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private BeerEntry requireAdminEntry(Long entryId) {
        BeerEntry entry = requirePortalEntry(entryId);
        competitionAccessService.requireCompetitionAccess(entry.getCompetitionId());
        return entry;
    }

    private List<Long> visibleCompetitionIds() {
        Long organizerId = competitionAccessService.requireCurrentOrganizerId();
        return competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                        .eq(Competition::getOrganizerId, organizerId))
                .stream()
                .map(Competition::getId)
                .toList();
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

    private String writeObjectJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }
}
