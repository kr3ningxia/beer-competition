package com.beercompetition.registration.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.properties.WechatPayProperties;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.beercompetition.registration.payment.EntryPaymentAdminService;
import com.beercompetition.registration.entry.PortalEntryViewAssembler;

/**
 * 保留一期单酒款支付模拟和后台到账确认行为。
 */
@Service
@RequiredArgsConstructor
public class EntryPaymentAdminServiceImpl implements EntryPaymentAdminService {

    private static final Set<String> LABEL_ALLOWED_STATUSES = Set.of(
                EntryStatus.REGISTERED.name(),
                EntryStatus.STORED.name(),
                EntryStatus.RESULT_PUBLISHED.name()
        );

    private static final String TARGET_ENTRY = "BEER_ENTRY";

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final PortalAccountMapper portalAccountMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final EntryPaymentMapper entryPaymentMapper;

    private final BreweryMapper breweryMapper;

    private final ObjectMapper objectMapper;

    private final WechatPayProperties wechatPayProperties;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    private final CompetitionAccessService competitionAccessService;

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
            return portalEntryViewAssembler.toEntryDetailVO(entry);
        }
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("当前酒款不能支付报名费");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        assertStandalonePaymentAction(payment, "该酒款已加入统一付款订单，请按整批完成付款");
        if (EntryPaymentStatus.PENDING_CONFIRM.name().equals(payment.getStatus())) {
            throw new BaseException("银行转账信息已提交，请等待组委会核对到账");
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
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
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
        BeerEntry entry = requireAdminEntry(entryId);
        if (!EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            throw new BaseException("只有待支付确认的酒款可以确认支付");
        }
        EntryPayment payment = ensureEntryPayment(entry.getId(), entry.getCompetitionId());
        assertStandalonePaymentAction(payment, "该酒款已加入统一付款订单，不能单独确认付款");
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
        writeEntryLog("ENTRY_CONFIRM_PAYMENT", entry.getUuid(), buildStatusLogSummary("确认支付", normalizeStatusReason(request)));
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

    private void assertStandalonePaymentAction(EntryPayment payment, String message) {
        if (payment != null && payment.getPaymentOrderId() != null) {
            throw new BaseException(message);
        }
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

    private String generateMockOutTradeNo() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolvePayMethod(String payMethod) {
        return StringUtils.hasText(payMethod) ? payMethod : EntryPayMethod.MANUAL.name();
    }

    private String writeObjectJson(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }
}
