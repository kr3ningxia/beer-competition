package com.beercompetition.registration.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.enums.EntryDeliveryMethod;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Set;
import com.beercompetition.registration.delivery.EntryDeliveryService;
import com.beercompetition.registration.entry.PortalEntryViewAssembler;

/**
 * 保存厂商送样信息并返回最新报名详情。
 */
@Service
@RequiredArgsConstructor
public class EntryDeliveryServiceImpl implements EntryDeliveryService {

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

    private final PortalAccountMapper portalAccountMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final EntryDeliveryMapper entryDeliveryMapper;

    private final BreweryMapper breweryMapper;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntryDetailVO submitPortalEntryDelivery(Long entryId, PortalEntryDeliverySubmitRequest request) {
        // 1) 参数规范化与前置校验
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            throw new BaseException("支付成功后才能提交送样信息");
        }
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能提交送样信息");
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
        return portalEntryViewAssembler.toEntryDetailVO(beerEntryMapper.selectById(entry.getId()));
    }

    private EntryRefund findLatestRefund(Long beerEntryId) {
        return entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getBeerEntryId, beerEntryId)
                .orderByDesc(EntryRefund::getId)
                .last("LIMIT 1"));
    }

    private boolean hasActiveRefund(Long beerEntryId) {
        return isActiveRefund(findLatestRefund(beerEntryId));
    }

    private boolean isActiveRefund(EntryRefund refund) {
        return refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private EntryDelivery findEntryDelivery(Long beerEntryId) {
        return entryDeliveryMapper.selectOne(new LambdaQueryWrapper<EntryDelivery>()
                .eq(EntryDelivery::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
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
}
