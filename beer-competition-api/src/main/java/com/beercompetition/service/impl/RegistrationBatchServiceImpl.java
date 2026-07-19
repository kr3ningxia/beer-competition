package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.PaymentOrderItemMapper;
import com.beercompetition.mapper.PaymentOrderMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RegistrationBatchMapper;
import com.beercompetition.pojo.dto.PortalEntryBatchQuoteRequest;
import com.beercompetition.pojo.dto.PortalEntryBatchSubmitRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.PaymentOrderStatus;
import com.beercompetition.pojo.enums.RegistrationBatchStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.PaymentOrder;
import com.beercompetition.pojo.po.PaymentOrderItem;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RegistrationBatch;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.RegistrationBatchQuoteVO;
import com.beercompetition.pojo.vo.RegistrationBatchVO;
import com.beercompetition.service.EntryService;
import com.beercompetition.service.RegistrationBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationBatchServiceImpl implements RegistrationBatchService {

    private final RegistrationBatchMapper registrationBatchMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderItemMapper paymentOrderItemMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final CompetitionMapper competitionMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryService entryService;

    @Override
    public RegistrationBatchQuoteVO quote(Long competitionId, PortalEntryBatchQuoteRequest request) {
        // 1) 校验账号、赛事和报名窗口
        requirePortalAccount();
        Competition competition = requireOpenCompetition(competitionId);
        LocalDateTime quotedAt = LocalDateTime.now();

        // 2) 按当前计价时点计算单价、优惠和总额
        BigDecimal unitAmount = resolveEntryFee(competition, quotedAt);
        BigDecimal standardAmount = defaultAmount(competition.getEntryFee());
        BigDecimal totalAmount = unitAmount.multiply(BigDecimal.valueOf(request.getEntryCount()));
        BigDecimal discountAmount = standardAmount.subtract(unitAmount)
                .max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(request.getEntryCount()));

        // 3) 返回服务端报价
        return RegistrationBatchQuoteVO.builder()
                .competitionId(competitionId)
                .entryCount(request.getEntryCount())
                .unitAmount(unitAmount)
                .standardUnitAmount(standardAmount)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .earlyBirdActive(isEarlyBirdActive(competition, quotedAt))
                .earlyBirdDeadline(competition.getEarlyBirdDeadline())
                .quotedAt(quotedAt)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationBatchVO submit(Long competitionId, PortalEntryBatchSubmitRequest request) {
        // 1) 校验账号、赛事并处理幂等重放
        PortalAccount account = requirePortalAccount();
        requireOpenCompetition(competitionId);
        RegistrationBatch existing = registrationBatchMapper.selectOne(new LambdaQueryWrapper<RegistrationBatch>()
                .eq(RegistrationBatch::getPortalAccountId, account.getId())
                .eq(RegistrationBatch::getIdempotencyKey, request.getIdempotencyKey())
                .last("LIMIT 1"));
        if (existing != null) {
            return toBatchVO(existing);
        }

        // 2) 创建批次并逐款复用现有报名校验和写入流程
        RegistrationBatch batch = RegistrationBatch.builder()
                .batchNo(generateBusinessNo("RB"))
                .competitionId(competitionId)
                .breweryId(account.getBreweryId())
                .portalAccountId(account.getId())
                .entryCount(request.getEntries().size())
                .totalAmount(BigDecimal.ZERO)
                .status(RegistrationBatchStatus.PENDING_PAYMENT.name())
                .idempotencyKey(request.getIdempotencyKey().trim())
                .rulesAccepted(Boolean.TRUE.equals(request.getRulesAccepted()) ? 1 : 0)
                .build();
        registrationBatchMapper.insert(batch);

        List<EntryPayment> entryPayments = new ArrayList<>();
        for (PortalEntrySubmitRequest entryRequest : request.getEntries()) {
            entryRequest.setRulesAccepted(request.getRulesAccepted());
            EntryDetailVO created = entryService.submitPortalEntry(competitionId, entryRequest);
            BeerEntry entry = beerEntryMapper.selectById(created.getId());
            entry.setRegistrationBatchId(batch.getId());
            beerEntryMapper.updateById(entry);
            EntryPayment payment = requireEntryPayment(entry.getId());
            entryPayments.add(payment);
        }

        // 3) 创建聚合订单与逐款分摊明细
        BigDecimal totalAmount = entryPayments.stream()
                .map(EntryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.getExpectedTotalAmount() != null
                && totalAmount.compareTo(request.getExpectedTotalAmount()) != 0) {
            throw new BaseException("报名费已更新，请重新确认应付金额后提交");
        }
        boolean freeRegistration = totalAmount.compareTo(BigDecimal.ZERO) == 0;
        LocalDateTime now = LocalDateTime.now();
        PaymentOrder order = PaymentOrder.builder()
                .orderNo(generateBusinessNo("PO"))
                .registrationBatchId(batch.getId())
                .amount(totalAmount)
                .paidAmount(freeRegistration ? BigDecimal.ZERO : null)
                .refundedAmount(BigDecimal.ZERO)
                .status(freeRegistration ? PaymentOrderStatus.PAID.name() : PaymentOrderStatus.UNPAID.name())
                .payMethod(EntryPayMethod.MANUAL.name())
                .paidTime(freeRegistration ? now : null)
                .build();
        paymentOrderMapper.insert(order);

        for (EntryPayment payment : entryPayments) {
            payment.setPaymentOrderId(order.getId());
            if (freeRegistration) {
                payment.setStatus(EntryPaymentStatus.PAID.name());
                payment.setPaidAmount(BigDecimal.ZERO);
                payment.setPaidTime(now);
                BeerEntry entry = beerEntryMapper.selectById(payment.getBeerEntryId());
                entry.setStatus(EntryStatus.REGISTERED.name());
                beerEntryMapper.updateById(entry);
            }
            entryPaymentMapper.updateById(payment);
            paymentOrderItemMapper.insert(PaymentOrderItem.builder()
                    .paymentOrderId(order.getId())
                    .beerEntryId(payment.getBeerEntryId())
                    .entryPaymentId(payment.getId())
                    .amount(payment.getAmount())
                    .refundedAmount(BigDecimal.ZERO)
                    .status(payment.getStatus())
                    .build());
        }

        batch.setTotalAmount(totalAmount);
        if (freeRegistration) {
            batch.setStatus(RegistrationBatchStatus.PAID.name());
        }
        registrationBatchMapper.updateById(batch);

        // 4) 返回包含酒款与支付订单的批次详情
        return toBatchVO(batch);
    }

    @Override
    public RegistrationBatchVO getPortalBatch(Long batchId) {
        // 1) 查询当前账号与报名批次
        PortalAccount account = requirePortalAccount();
        RegistrationBatch batch = requireBatch(batchId);

        // 2) 校验批次归属
        if (!Objects.equals(batch.getBreweryId(), account.getBreweryId())) {
            throw new ForbiddenException("无权查看该报名批次");
        }

        // 3) 返回批次详情
        return toBatchVO(batch);
    }

    private RegistrationBatchVO toBatchVO(RegistrationBatch batch) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getRegistrationBatchId, batch.getId())
                .last("LIMIT 1"));
        List<EntryDetailVO> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getRegistrationBatchId, batch.getId())
                        .orderByAsc(BeerEntry::getId))
                .stream()
                .map(entry -> entryService.getPortalEntry(entry.getId()))
                .toList();
        return toBatchVO(batch, order, entries);
    }

    private RegistrationBatchVO toBatchVO(RegistrationBatch batch, PaymentOrder order, List<EntryDetailVO> entries) {
        Competition competition = competitionMapper.selectById(batch.getCompetitionId());
        return RegistrationBatchVO.builder()
                .id(batch.getId())
                .batchNo(batch.getBatchNo())
                .competitionId(batch.getCompetitionId())
                .competitionName(competition == null ? null : competition.getName())
                .entryCount(batch.getEntryCount())
                .totalAmount(batch.getTotalAmount())
                .status(batch.getStatus())
                .paymentOrderId(order == null ? null : order.getId())
                .paymentOrderNo(order == null ? null : order.getOrderNo())
                .paymentStatus(order == null ? null : order.getStatus())
                .payMethod(order == null ? null : order.getPayMethod())
                .paidAmount(order == null ? null : order.getPaidAmount())
                .refundedAmount(order == null ? null : order.getRefundedAmount())
                .expireTime(order == null ? null : order.getExpireTime())
                .paidTime(order == null ? null : order.getPaidTime())
                .submittedAt(batch.getCreateTime())
                .entries(entries)
                .build();
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null || account.getBreweryId() == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        return account;
    }

    private RegistrationBatch requireBatch(Long batchId) {
        RegistrationBatch batch = registrationBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ResourceNotFoundException("报名批次不存在");
        }
        return batch;
    }

    private Competition requireOpenCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (!CompetitionStatus.REGISTRATION_OPEN.name().equals(competition.getStatus())) {
            throw new BaseException("当前赛事暂未开放报名");
        }
        if (competition.getRegistrationDeadline() != null
                && LocalDateTime.now().isAfter(competition.getRegistrationDeadline())) {
            throw new BaseException("报名已截止");
        }
        return competition;
    }

    private EntryPayment requireEntryPayment(Long entryId) {
        EntryPayment payment = entryPaymentMapper.selectOne(new LambdaQueryWrapper<EntryPayment>()
                .eq(EntryPayment::getBeerEntryId, entryId)
                .last("LIMIT 1"));
        if (payment == null) {
            throw new BaseException("酒款付款记录创建失败");
        }
        return payment;
    }

    private BigDecimal resolveEntryFee(Competition competition, LocalDateTime now) {
        return isEarlyBirdActive(competition, now)
                ? competition.getEarlyBirdFee()
                : defaultAmount(competition.getEntryFee());
    }

    private boolean isEarlyBirdActive(Competition competition, LocalDateTime now) {
        return competition.getEarlyBirdFee() != null
                && competition.getEarlyBirdDeadline() != null
                && !now.isAfter(competition.getEarlyBirdDeadline());
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String generateBusinessNo(String prefix) {
        return prefix + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
