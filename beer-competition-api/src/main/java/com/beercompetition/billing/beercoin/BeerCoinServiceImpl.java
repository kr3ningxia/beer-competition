package com.beercompetition.billing.beercoin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerCoinConsumptionAllocationMapper;
import com.beercompetition.mapper.BeerCoinLedgerMapper;
import com.beercompetition.mapper.BeerCoinLotMapper;
import com.beercompetition.mapper.BeerCoinProductMapper;
import com.beercompetition.mapper.BeerCoinProductTierMapper;
import com.beercompetition.mapper.BeerCoinPurchaseOrderMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCoinSettlementEntryMapper;
import com.beercompetition.mapper.CompetitionCoinSettlementMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EnterpriseAccountMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pay.WechatPayClient;
import com.beercompetition.pojo.dto.BeerCoinAdjustmentRequest;
import com.beercompetition.pojo.dto.BeerCoinPricingRequest;
import com.beercompetition.pojo.dto.BeerCoinPurchaseRequest;
import com.beercompetition.pojo.dto.BeerCoinTierRequest;
import com.beercompetition.pojo.enums.AdminType;
import com.beercompetition.pojo.enums.BeerCoinLedgerDirection;
import com.beercompetition.pojo.enums.BeerCoinProductStatus;
import com.beercompetition.pojo.enums.BeerCoinPurchaseOrderStatus;
import com.beercompetition.pojo.enums.BeerCoinSettlementStatus;
import com.beercompetition.pojo.enums.BeerCoinSettlementType;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.BeerCoinConsumptionAllocation;
import com.beercompetition.pojo.po.BeerCoinLedger;
import com.beercompetition.pojo.po.BeerCoinLot;
import com.beercompetition.pojo.po.BeerCoinProduct;
import com.beercompetition.pojo.po.BeerCoinProductTier;
import com.beercompetition.pojo.po.BeerCoinPurchaseOrder;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCoinSettlement;
import com.beercompetition.pojo.po.CompetitionCoinSettlementEntry;
import com.beercompetition.pojo.po.EnterpriseAccount;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.vo.BeerCoinLedgerVO;
import com.beercompetition.pojo.vo.BeerCoinLotVO;
import com.beercompetition.pojo.vo.BeerCoinAccountOptionVO;
import com.beercompetition.pojo.vo.BeerCoinOverviewVO;
import com.beercompetition.pojo.vo.BeerCoinPriceSegmentVO;
import com.beercompetition.pojo.vo.BeerCoinPricingVO;
import com.beercompetition.pojo.vo.BeerCoinPurchaseOrderVO;
import com.beercompetition.pojo.vo.BeerCoinPurchasePaymentVO;
import com.beercompetition.pojo.vo.BeerCoinSettlementVO;
import com.beercompetition.pojo.vo.BeerCoinTierVO;
import com.beercompetition.pojo.vo.BeerCoinWalletVO;
import com.beercompetition.pojo.vo.WechatJsapiPayVO;
import com.beercompetition.pojo.vo.WechatPayClientConfigVO;
import com.beercompetition.properties.BeerCoinPaymentProperties;
import com.beercompetition.security.AdminIdentityService;
import com.beercompetition.security.AdminSessionIdentity;
import com.beercompetition.service.WechatOAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 啤酒币定价、钱包、购买支付及赛事结算应用服务。
 */
@Service
@RequiredArgsConstructor
public class BeerCoinServiceImpl implements BeerCoinService, BeerCoinSettlementService {

    private static final String ACTIVE = BeerCoinProductStatus.ACTIVE.name();
    private static final String GLOBAL_PRICING_CODE = "BEER_COIN_GLOBAL";
    private static final String GLOBAL_PRICING_NAME = "啤酒币价格";
    private static final String WAITING_PAYMENT = BeerCoinPurchaseOrderStatus.WAITING_PAYMENT.name();
    private static final String PAID = BeerCoinPurchaseOrderStatus.PAID.name();
    private static final int PAYMENT_EXPIRE_MINUTES = 30;
    private static final long MAX_PURCHASE_QUANTITY = 1_000_000_000L;
    private static final int MAX_PRICING_TIERS = 100;
    private static final BigDecimal MAX_TIER_UNIT_PRICE = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("9999999999.99");
    private static final int EXPIRING_WINDOW_DAYS = 30;
    private static final String SOURCE_PURCHASE = "PURCHASE";
    private static final String BUSINESS_PURCHASE = "PURCHASE";
    private static final String BUSINESS_COMPETITION = "COMPETITION";
    private static final String SETTLEMENT_COMPLETED = "COMPLETED";
    private static final Set<String> WECHAT_TERMINAL_FAILURE_STATES = Set.of("CLOSED", "REVOKED", "PAYERROR");

    private final BeerCoinProductMapper productMapper;
    private final BeerCoinProductTierMapper tierMapper;
    private final BeerCoinPurchaseOrderMapper purchaseOrderMapper;
    private final BeerCoinLotMapper lotMapper;
    private final BeerCoinLedgerMapper ledgerMapper;
    private final BeerCoinConsumptionAllocationMapper allocationMapper;
    private final CompetitionCoinSettlementMapper settlementMapper;
    private final CompetitionCoinSettlementEntryMapper settlementEntryMapper;
    private final EnterpriseAccountMapper enterpriseAccountMapper;
    private final OrganizerMapper organizerMapper;
    private final CompetitionMapper competitionMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final AdminIdentityService adminIdentityService;
    private final WechatPayClient wechatPayClient;
    private final WechatOAuthService wechatOAuthService;
    private final com.beercompetition.properties.WechatPayProperties wechatPayProperties;
    private final BeerCoinPaymentProperties beerCoinPaymentProperties;
    private final ObjectMapper objectMapper;

    @Override
    public BeerCoinOverviewVO getOverview() {
        AdminSessionIdentity identity = requireSupportedIdentity();
        BeerCoinPricingVO pricing = toPricingVO(productMapper.selectActive());
        if (identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN) {
            return BeerCoinOverviewVO.builder()
                    .activePricing(pricing)
                    .wallet(null)
                    .purchaseOrders(listAllPurchaseOrders())
                    .ledger(listAllLedger())
                    .lots(listAllLots())
                    .accountOptions(listActiveAccountOptions())
                    .canConfigurePricing(true)
                    .canPurchase(false)
                    .build();
        }
        Long accountId = requireCurrentEnterpriseAccount(identity);
        return BeerCoinOverviewVO.builder()
                .activePricing(pricing)
                .wallet(toWallet(accountId))
                .purchaseOrders(listPurchaseOrders(accountId))
                .ledger(listLedger(accountId))
                .lots(listLots(accountId))
                .accountOptions(List.of())
                .canConfigurePricing(false)
                .canPurchase(pricing != null)
                .build();
    }

    @Override
    public BeerCoinPricingVO getActivePricing() {
        requireSupportedIdentity();
        return toPricingVO(productMapper.selectActive());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPricingVO savePricing(BeerCoinPricingRequest request) {
        AdminSessionIdentity identity = requireSupportedIdentity();
        if (identity.adminType() != AdminType.PLATFORM_SUPER_ADMIN) {
            throw new ForbiddenException("只有平台超级管理员可以配置啤酒币价格");
        }
        List<BeerCoinTierRequest> requests = validatePricingRequest(request);
        BeerCoinProduct activeProduct = productMapper.selectActiveForUpdate();
        if (activeProduct == null) {
            activeProduct = productMapper.selectByCodeForUpdate(GLOBAL_PRICING_CODE);
        }
        LocalDateTime now = LocalDateTime.now();
        BeerCoinProduct product;
        if (activeProduct == null) {
            product = BeerCoinProduct.builder()
                    .productCode(GLOBAL_PRICING_CODE)
                    .name(GLOBAL_PRICING_NAME)
                    .status(ACTIVE)
                    .effectiveTime(now)
                    .versionNo(1)
                    .createdByAdminId(BaseContext.getCurrentId())
                    .build();
            try {
                productMapper.insert(product);
            } catch (DuplicateKeyException ex) {
                throw new BaseException("价格设置正在被其他管理员保存，请刷新后重试");
            }
        } else {
            product = activeProduct;
            product.setName(GLOBAL_PRICING_NAME);
            product.setStatus(ACTIVE);
            product.setEffectiveTime(now);
            product.setVersionNo(activeProduct.getVersionNo() == null ? 1 : activeProduct.getVersionNo() + 1);
            productMapper.updateById(product);
            tierMapper.delete(new LambdaQueryWrapper<BeerCoinProductTier>()
                    .eq(BeerCoinProductTier::getProductId, product.getId()));
        }
        for (int index = 0; index < requests.size(); index++) {
            BeerCoinTierRequest item = requests.get(index);
            tierMapper.insert(BeerCoinProductTier.builder()
                    .productId(product.getId())
                    .startQuantity(item.getStartQuantity())
                    .endQuantity(item.getEndQuantity())
                    .unitPrice(item.getUnitPrice().setScale(2))
                    .sortOrder(index + 1)
                    .build());
        }
        return toPricingVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPurchaseOrderVO createPurchaseOrder(BeerCoinPurchaseRequest request) {
        AdminSessionIdentity identity = requireOrganizerIdentity();
        long quantity = requirePurchaseQuantity(request == null ? null : request.getQuantity());
        Long accountId = requireCurrentEnterpriseAccount(identity);
        BeerCoinProduct product = requireActiveProduct();
        List<BeerCoinProductTier> tiers = loadTiers(product.getId());
        PriceCalculation calculation = calculatePrice(quantity, tiers);
        BeerCoinPurchaseOrder order = BeerCoinPurchaseOrder.builder()
                .orderNo(generateOrderNo())
                .enterpriseAccountId(accountId)
                .productId(product.getId())
                .productSnapshotJson(writeProductSnapshot(product, tiers, calculation))
                .quantity(quantity)
                .amount(calculation.amount())
                .status(BeerCoinPurchaseOrderStatus.CREATED.name())
                .build();
        purchaseOrderMapper.insert(order);
        return toOrderVO(order);
    }

    @Override
    public List<BeerCoinPurchaseOrderVO> listPurchaseOrders() {
        AdminSessionIdentity identity = requireSupportedIdentity();
        return identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN
                ? listAllPurchaseOrders()
                : listPurchaseOrders(requireCurrentEnterpriseAccount(identity));
    }

    @Override
    public BeerCoinPurchaseOrderVO getPurchaseOrder(Long id) {
        BeerCoinPurchaseOrder order = requireAccessibleOrder(id);
        return toOrderVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPurchasePaymentVO createNativePayment(Long id) {
        BeerCoinPurchaseOrder order = preparePaymentOrder(id);
        if (PAID.equals(order.getStatus())) {
            return toPaymentVO(order, null);
        }
        if (!isWechatPaymentMode()) {
            return prepareMockPayment(order);
        }
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(order.getCodeUrl()) && order.getExpireTime() != null
                && order.getExpireTime().isAfter(now.plusSeconds(30))) {
            return toPaymentVO(order, null);
        }
        order = synchronizeWechatPayment(order, true);
        if (PAID.equals(order.getStatus())) {
            return toPaymentVO(order, null);
        }
        now = LocalDateTime.now();
        if (isExpired(order, now)) {
            expireOrder(order);
            throw new BaseException("支付订单已过期，请重新创建购买订单");
        }
        String outTradeNo = generateTradeNo();
        LocalDateTime expireTime = now.plusMinutes(PAYMENT_EXPIRE_MINUTES);
        WechatPayClient.NativePayResult result = wechatPayClient.createNativePayment(
                new WechatPayClient.NativePayRequest(outTradeNo, paymentDescription(order), order.getAmount(), expireTime));
        order.setOutTradeNo(outTradeNo);
        order.setCodeUrl(result.codeUrl());
        order.setExpireTime(expireTime);
        order.setStatus(WAITING_PAYMENT);
        order.setWechatTradeState("NOTPAY");
        order.setWechatTradeStateDesc("待支付");
        purchaseOrderMapper.updateById(order);
        return toPaymentVO(order, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPurchasePaymentVO createJsapiPayment(Long id, String code) {
        BeerCoinPurchaseOrder order = preparePaymentOrder(id);
        if (PAID.equals(order.getStatus())) {
            return toPaymentVO(order, null);
        }
        if (!isWechatPaymentMode()) {
            return prepareMockPayment(order);
        }
        if (!StringUtils.hasText(code)) {
            throw new BaseException("请先完成微信授权");
        }
        LocalDateTime now = LocalDateTime.now();
        order = synchronizeWechatPayment(order, true);
        if (PAID.equals(order.getStatus())) {
            return toPaymentVO(order, null);
        }
        now = LocalDateTime.now();
        if (isExpired(order, now)) {
            expireOrder(order);
            throw new BaseException("支付订单已过期，请重新创建购买订单");
        }
        String openid = wechatOAuthService.resolveOpenid(code);
        String outTradeNo = generateTradeNo();
        LocalDateTime expireTime = now.plusMinutes(PAYMENT_EXPIRE_MINUTES);
        WechatPayClient.JsapiPayResult result = wechatPayClient.createJsapiPayment(
                new WechatPayClient.JsapiPayRequest(outTradeNo, paymentDescription(order), order.getAmount(), expireTime, openid));
        order.setOutTradeNo(outTradeNo);
        order.setCodeUrl(null);
        order.setExpireTime(expireTime);
        order.setStatus(WAITING_PAYMENT);
        order.setWechatTradeState("NOTPAY");
        order.setWechatTradeStateDesc("待支付");
        purchaseOrderMapper.updateById(order);
        return toPaymentVO(order, result);
    }

    @Override
    public WechatPayClientConfigVO getWechatPayClientConfig() {
        requireSupportedIdentity();
        boolean wechatMode = isWechatPaymentMode();
        String appId = wechatMode && StringUtils.hasText(wechatPayProperties.getAppId())
                ? wechatPayProperties.getAppId().trim() : null;
        boolean jsapiConfigured = !wechatMode
                || (StringUtils.hasText(appId) && StringUtils.hasText(wechatPayProperties.getAppSecret()));
        return WechatPayClientConfigVO.builder()
                .mode(wechatMode ? "WECHAT" : "MOCK")
                .appId(appId)
                .jsapiConfigured(jsapiConfigured)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPurchaseOrderVO simulatePayment(Long id) {
        BeerCoinPurchaseOrder order = preparePaymentOrder(id);
        if (isWechatPaymentMode()) {
            throw new BaseException("当前已启用微信支付，请扫码完成啤酒币购买");
        }
        if (PAID.equals(order.getStatus())) {
            return toOrderVO(order);
        }
        if (!WAITING_PAYMENT.equals(order.getStatus())
                && !BeerCoinPurchaseOrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能模拟支付");
        }
        if (!StringUtils.hasText(order.getOutTradeNo())) {
            order.setOutTradeNo(generateTradeNo());
            purchaseOrderMapper.updateById(order);
        }
        applyWechatPaymentSuccess(new WechatPayClient.PaymentNotifyResult(
                "MOCK-NOTIFY-" + order.getOutTradeNo(), "TRANSACTION.SUCCESS", order.getOutTradeNo(),
                "MOCK-TX-" + order.getOutTradeNo(), "SUCCESS", "支付成功", order.getAmount(),
                LocalDateTime.now(), null));
        return toOrderVO(purchaseOrderMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinPurchaseOrderVO getPurchaseOrderStatus(Long id) {
        BeerCoinPurchaseOrder order = requireAccessibleOrder(id);
        if (PAID.equals(order.getStatus())) {
            return toOrderVO(order);
        }
        order = synchronizeWechatPayment(order, false);
        if (PAID.equals(order.getStatus())) {
            return toOrderVO(order);
        }
        if (isExpired(order, LocalDateTime.now())) {
            expireOrder(order);
            order = purchaseOrderMapper.selectById(id);
        }
        return toOrderVO(order);
    }

    @Override
    public List<BeerCoinLedgerVO> listLedger() {
        AdminSessionIdentity identity = requireSupportedIdentity();
        return identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN
                ? listAllLedger()
                : listLedger(requireCurrentEnterpriseAccount(identity));
    }

    @Override
    public List<BeerCoinLotVO> listLots() {
        AdminSessionIdentity identity = requireSupportedIdentity();
        return identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN
                ? listAllLots()
                : listLots(requireCurrentEnterpriseAccount(identity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BeerCoinLedgerVO adjust(BeerCoinAdjustmentRequest request) {
        AdminSessionIdentity identity = requireSupportedIdentity();
        if (identity.adminType() != AdminType.PLATFORM_SUPER_ADMIN) {
            throw new ForbiddenException("只有平台超级管理员可以调整啤酒币");
        }
        if (request == null || request.getEnterpriseAccountId() == null) {
            throw new BaseException("企业账户不能为空");
        }
        if (!StringUtils.hasText(request.getDirection())) {
            throw new BaseException("调整方向不能为空");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new BaseException("调整原因不能为空");
        }
        EnterpriseAccount account = requireActiveTenantAccount(request.getEnterpriseAccountId());
        long quantity = requirePurchaseQuantity(request.getQuantity());
        String direction = request.getDirection().trim().toUpperCase();
        String key = "MANUAL:" + UUID.randomUUID();
        BeerCoinLedger ledger;
        if (BeerCoinLedgerDirection.CREDIT.name().equals(direction)) {
            ledger = creditCoins(account.getId(), quantity, "MANUAL_ADJUSTMENT", null, key,
                    BaseContext.getCurrentId(), request.getReason(), BeerCoinLedgerDirection.CREDIT.name());
        } else if (BeerCoinLedgerDirection.DEBIT.name().equals(direction)) {
            ledger = consumeCoins(account.getId(), quantity, "MANUAL_ADJUSTMENT", null, key,
                    BaseContext.getCurrentId(), request.getReason(), BeerCoinLedgerDirection.DEBIT.name());
        } else if (BeerCoinLedgerDirection.REVERSAL.name().equals(direction)) {
            // 冲正用于补回此前已扣减的啤酒币，必须形成独立到账批次和反向流水。
            ledger = creditCoins(account.getId(), quantity, "MANUAL_ADJUSTMENT", null, key,
                    BaseContext.getCurrentId(), request.getReason(), BeerCoinLedgerDirection.REVERSAL.name());
        } else {
            throw new BaseException("调整方向不合法");
        }
        return toLedgerVO(ledger);
    }

    @Override
    public BeerCoinSettlementVO getSettlement(Long competitionId) {
        AdminSessionIdentity identity = adminIdentityService.requireCurrentIdentity();
        Competition competition = requireCompetition(competitionId);
        if (identity.adminType() == AdminType.PLATFORM_EVENT_ADMIN) {
            return BeerCoinSettlementVO.builder().applicable(false).completed(true).build();
        }
        if (identity.adminType() == AdminType.ORGANIZER_ADMIN
                && !Objects.equals(identity.organizerId(), competition.getOrganizerId())) {
            throw new ForbiddenException("无权查看其他组织的赛事结算");
        }
        Organizer organizer = organizerMapper.selectById(competition.getOrganizerId());
        if (organizer == null || !OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            return BeerCoinSettlementVO.builder().applicable(false).completed(true).build();
        }
        CompetitionCoinSettlement closing = settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionCoinSettlement>()
                .eq(CompetitionCoinSettlement::getCompetitionId, competitionId)
                .eq(CompetitionCoinSettlement::getSettlementType, BeerCoinSettlementType.REGISTRATION_CLOSE.name())
                .last("LIMIT 1"));
        CompetitionCoinSettlement opening = settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionCoinSettlement>()
                .eq(CompetitionCoinSettlement::getCompetitionId, competitionId)
                .eq(CompetitionCoinSettlement::getSettlementType, BeerCoinSettlementType.PUBLISH_MINIMUM.name())
                .last("LIMIT 1"));
        CompetitionCoinSettlement source = closing != null ? closing : opening;
        long charged = (opening == null ? 0 : nullToZero(opening.getChargedQuantity()))
                + (closing == null ? 0 : nullToZero(closing.getChargedQuantity()));
        int effectiveEntryCount = closing == null ? 0 : nullToZeroInt(closing.getEffectiveEntryCount());
        long required = closing == null ? (opening == null ? 1 : nullToZero(opening.getRequiredQuantity()))
                : nullToZero(closing.getRequiredQuantity());
        if (isSettlementCalculationStage(competition.getStatus())) {
            // 评审准备后仍可能补录有效酒款，详情页必须展示当前口径而非旧快照。
            effectiveEntryCount = Math.toIntExact(beerEntryMapper.countEffectiveEntries(competitionId));
            required = requiredQuantityForEntryCount(effectiveEntryCount);
        }
        long missingSnapshotCount = closing == null || closing.getId() == null
                ? effectiveEntryCount
                : settlementEntryMapper.countMissingEffectiveEntries(competitionId, closing.getId());
        boolean completed = closing != null
                && SETTLEMENT_COMPLETED.equals(closing.getStatus())
                && isFinalSettlementStage(competition.getStatus())
                && required <= charged
                && missingSnapshotCount == 0;
        return BeerCoinSettlementVO.builder()
                .applicable(true)
                .effectiveEntryCount(effectiveEntryCount)
                .requiredQuantity(required)
                .chargedQuantity(charged)
                .pendingQuantity(Math.max(0, required - charged))
                .completed(completed)
                .status(source == null ? null : source.getStatus())
                .build();
    }

    @Override
    public boolean isPurchaseOrder(String outTradeNo) {
        if (!StringUtils.hasText(outTradeNo)) {
            return false;
        }
        return purchaseOrderMapper.selectCount(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                .eq(BeerCoinPurchaseOrder::getOutTradeNo, outTradeNo)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyWechatPaymentSuccess(WechatPayClient.PaymentNotifyResult result) {
        if (result == null || !StringUtils.hasText(result.outTradeNo())) {
            return false;
        }
        BeerCoinPurchaseOrder order = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                .eq(BeerCoinPurchaseOrder::getOutTradeNo, result.outTradeNo())
                .last("FOR UPDATE"));
        if (order == null) {
            return false;
        }
        if (PAID.equals(order.getStatus())) {
            if (!"SUCCESS".equals(result.tradeState())) {
                throw new BaseException("微信支付状态与订单不一致");
            }
            if (!StringUtils.hasText(result.transactionId())) {
                throw new BaseException("微信支付交易号缺失");
            }
            if (result.paidAmount() == null || order.getAmount().compareTo(result.paidAmount()) != 0) {
                throw new BaseException("微信支付金额不一致");
            }
            if (StringUtils.hasText(order.getWechatTransactionId())
                    && !order.getWechatTransactionId().equals(result.transactionId())) {
                throw new BaseException("微信交易号与订单不一致");
            }
            return true;
        }
        if (!"SUCCESS".equals(result.tradeState())) {
            throw new BaseException("微信支付未成功");
        }
        if (!StringUtils.hasText(result.transactionId())) {
            throw new BaseException("微信支付交易号缺失");
        }
        if (result.paidAmount() != null && order.getAmount().compareTo(result.paidAmount()) != 0) {
            throw new BaseException("微信支付金额不一致");
        }
        if (result.paidAmount() == null) {
            throw new BaseException("微信支付金额缺失");
        }
        if (BeerCoinPurchaseOrderStatus.EXPIRED.name().equals(order.getStatus())
                || BeerCoinPurchaseOrderStatus.CLOSED.name().equals(order.getStatus())) {
            throw new BaseException("啤酒币订单已关闭");
        }
        BeerCoinPurchaseOrder duplicateTransaction = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                .eq(BeerCoinPurchaseOrder::getWechatTransactionId, result.transactionId())
                .ne(BeerCoinPurchaseOrder::getId, order.getId())
                .last("LIMIT 1"));
        if (duplicateTransaction != null) {
            throw new BaseException("微信交易号已关联其他啤酒币订单");
        }
        LocalDateTime paidTime = result.paidTime() == null ? LocalDateTime.now() : result.paidTime();
        order.setStatus(PAID);
        order.setWechatTransactionId(result.transactionId());
        order.setWechatTradeState(result.tradeState());
        order.setWechatTradeStateDesc(result.tradeStateDesc());
        order.setNotifyRawJson(result.rawJson());
        order.setPaidTime(paidTime);
        purchaseOrderMapper.updateById(order);

        String key = "PURCHASE:" + order.getOrderNo();
        BeerCoinLedger existingLedger = findLedger(order.getEnterpriseAccountId(), key);
        if (existingLedger == null) {
            BeerCoinLot existingLot = lotMapper.selectOne(new LambdaQueryWrapper<BeerCoinLot>()
                    .eq(BeerCoinLot::getPurchaseOrderId, order.getId())
                    .last("LIMIT 1"));
            if (existingLot == null) {
                BeerCoinLot lot = BeerCoinLot.builder()
                        .lotNo(generateLotNo())
                        .enterpriseAccountId(order.getEnterpriseAccountId())
                        .purchaseOrderId(order.getId())
                        .totalQuantity(order.getQuantity())
                        .remainingQuantity(order.getQuantity())
                        .availableFrom(paidTime)
                        .expiresAt(paidTime.plusYears(1))
                        .sourceType(SOURCE_PURCHASE)
                        .sourceOrderId(order.getOrderNo())
                        .build();
                lotMapper.insert(lot);
            }
            insertLedger(order.getEnterpriseAccountId(), BeerCoinLedgerDirection.CREDIT.name(), order.getQuantity(),
                    BUSINESS_PURCHASE, order.getId().toString(), key, null, "微信支付到账");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleRegistrationOpening(Long competitionId) {
        CompetitionContext context = requireTenantCompetitionForUpdate(competitionId);
        if (context.enterpriseAccountId() == null) {
            return;
        }
        CompetitionCoinSettlement existing = lockSettlement(competitionId, BeerCoinSettlementType.PUBLISH_MINIMUM);
        if (existing != null && SETTLEMENT_COMPLETED.equals(existing.getStatus())) {
            return;
        }
        String key = "COMPETITION:" + competitionId + ":PUBLISH_MINIMUM";
        BeerCoinLedger ledger = consumeCoins(context.enterpriseAccountId(), 1L, BUSINESS_COMPETITION,
                competitionId.toString(), key, BaseContext.getCurrentId(), "赛事开放报名最低消费", BeerCoinLedgerDirection.DEBIT.name());
        CompetitionCoinSettlement settlement = existing == null
                ? CompetitionCoinSettlement.builder().competitionId(competitionId)
                .enterpriseAccountId(context.enterpriseAccountId())
                .settlementType(BeerCoinSettlementType.PUBLISH_MINIMUM.name())
                .effectiveEntryCount(0)
                .billingTier("开放报名最低消费")
                .requiredQuantity(1L)
                .chargedQuantity(1L)
                .status(SETTLEMENT_COMPLETED)
                .ledgerId(ledger == null ? null : ledger.getId())
                .idempotencyKey(key)
                .settledByAdminId(BaseContext.getCurrentId())
                .settledTime(LocalDateTime.now())
                .build()
                : existing;
        if (existing == null) {
            settlementMapper.insert(settlement);
        } else {
            // 历史上可能存在未完成的占位记录，补扣成功后必须把完整结果持久化，
            // 否则下次请求仍会看到待结算状态并重复尝试扣款。
            settlement.setEnterpriseAccountId(context.enterpriseAccountId());
            settlement.setRequiredQuantity(1L);
            settlement.setChargedQuantity(1L);
            settlement.setLedgerId(ledger == null ? settlement.getLedgerId() : ledger.getId());
            settlement.setStatus(SETTLEMENT_COMPLETED);
            settlement.setSettledByAdminId(BaseContext.getCurrentId());
            settlement.setSettledTime(LocalDateTime.now());
            settlement.setIdempotencyKey(key);
            settlementMapper.updateById(settlement);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleJudgingPreparation(Long competitionId) {
        CompetitionContext context = requireTenantCompetitionForUpdate(competitionId);
        if (context.enterpriseAccountId() == null) {
            return;
        }
        CompetitionCoinSettlement existing = lockSettlement(competitionId, BeerCoinSettlementType.REGISTRATION_CLOSE);
        int effectiveCount = Math.toIntExact(beerEntryMapper.countEffectiveEntries(competitionId));
        long required = requiredQuantityForEntryCount(effectiveCount);
        CompetitionCoinSettlement opening = lockSettlement(competitionId, BeerCoinSettlementType.PUBLISH_MINIMUM);
        long openingCharged = opening == null ? 0 : nullToZero(opening.getChargedQuantity());
        long closingCharged = existing == null ? 0 : nullToZero(existing.getChargedQuantity());
        long priorCharged = openingCharged + closingCharged;
        long delta = Math.max(0L, required - priorCharged);
        String key = "COMPETITION:" + competitionId + ":REGISTRATION_CLOSE:" + required;
        BeerCoinLedger ledger = delta == 0 ? null : consumeCoins(context.enterpriseAccountId(), delta, BUSINESS_COMPETITION,
                competitionId.toString(), key, BaseContext.getCurrentId(), "报名截止结算补扣", BeerCoinLedgerDirection.DEBIT.name());
        LocalDateTime settledTime = LocalDateTime.now();
        CompetitionCoinSettlement settlement;
        if (existing == null) {
            settlement = CompetitionCoinSettlement.builder().competitionId(competitionId)
                    .enterpriseAccountId(context.enterpriseAccountId())
                    .settlementType(BeerCoinSettlementType.REGISTRATION_CLOSE.name())
                    .effectiveEntryCount(effectiveCount)
                    .billingTier("每 10 款有效酒款计 1 枚")
                    .requiredQuantity(required)
                    .chargedQuantity(delta)
                    .status(SETTLEMENT_COMPLETED)
                    .ledgerId(ledger == null ? null : ledger.getId())
                    .idempotencyKey("COMPETITION:" + competitionId + ":REGISTRATION_CLOSE")
                    .settledByAdminId(BaseContext.getCurrentId())
                    .settledTime(settledTime)
                    .build();
        } else {
            settlement = existing;
            settlement.setEffectiveEntryCount(effectiveCount);
            settlement.setRequiredQuantity(required);
            settlement.setChargedQuantity(closingCharged + delta);
            settlement.setStatus(SETTLEMENT_COMPLETED);
            if (ledger != null) {
                settlement.setLedgerId(ledger.getId());
            }
            settlement.setSettledByAdminId(BaseContext.getCurrentId());
            settlement.setSettledTime(settledTime);
            settlementMapper.updateById(settlement);
        }
        if (existing == null) {
            settlementMapper.insert(settlement);
        }
        snapshotEffectiveEntries(settlement, competitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requireJudgingSettlementCompleted(Long competitionId) {
        CompetitionContext context = requireTenantCompetition(competitionId);
        if (context.enterpriseAccountId() == null) {
            return;
        }
        // 评审准备后若补录酒款，所有进入评审的入口都先尝试完成增量结算。
        // 报名截止阶段仍必须通过正式的“进入评审准备”动作，避免绕过配置检查。
        if (isFinalSettlementStage(context.competition().getStatus())) {
            settleJudgingPreparation(competitionId);
        }
        CompetitionCoinSettlement settlement = settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionCoinSettlement>()
                .eq(CompetitionCoinSettlement::getCompetitionId, context.competition().getId())
                .eq(CompetitionCoinSettlement::getSettlementType, BeerCoinSettlementType.REGISTRATION_CLOSE.name())
                .last("LIMIT 1"));
        if (settlement == null || !BeerCoinSettlementStatus.COMPLETED.name().equals(settlement.getStatus())) {
            throw new BaseException("请先完成啤酒币结算，才能进入评审准备");
        }
        long effectiveCount = beerEntryMapper.countEffectiveEntries(competitionId);
        long required = requiredQuantityForEntryCount(Math.toIntExact(effectiveCount));
        CompetitionCoinSettlement opening = lockSettlement(competitionId, BeerCoinSettlementType.PUBLISH_MINIMUM);
        long charged = (opening == null ? 0 : nullToZero(opening.getChargedQuantity()))
                + nullToZero(settlement.getChargedQuantity());
        long missingSnapshotCount = settlementEntryMapper.countMissingEffectiveEntries(competitionId, settlement.getId());
        if (required > charged || missingSnapshotCount > 0) {
            throw new BaseException("请先完成啤酒币增量结算，才能进入评审流程");
        }
    }

    @Override
    public void requireEntryIncludedAfterSettlement(Long competitionId, Long beerEntryId) {
        CompetitionContext context = requireTenantCompetition(competitionId);
        if (context.enterpriseAccountId() == null || !isFinalSettlementStage(context.competition().getStatus())) {
            return;
        }
        BeerEntry entry = beerEntryMapper.selectById(beerEntryId);
        if (entry == null || !Objects.equals(entry.getCompetitionId(), competitionId)) {
            throw new ResourceNotFoundException("酒款不存在或不属于当前比赛");
        }
        CompetitionCoinSettlement settlement = settlementMapper.selectOne(
                new LambdaQueryWrapper<CompetitionCoinSettlement>()
                        .eq(CompetitionCoinSettlement::getCompetitionId, competitionId)
                        .eq(CompetitionCoinSettlement::getSettlementType,
                                BeerCoinSettlementType.REGISTRATION_CLOSE.name())
                        .last("LIMIT 1"));
        if (settlement == null || !SETTLEMENT_COMPLETED.equals(settlement.getStatus())
                || settlementEntryMapper.countSettlementEntry(settlement.getId(), beerEntryId) == 0) {
            throw new BaseException("该酒款尚未纳入赛事结算，请联系主办方完成增量结算");
        }
    }

    private AdminSessionIdentity requireSupportedIdentity() {
        AdminSessionIdentity identity = adminIdentityService.requireCurrentIdentity();
        if (identity.adminType() == AdminType.PLATFORM_EVENT_ADMIN) {
            throw new ForbiddenException("当前账号无啤酒币权限");
        }
        return identity;
    }

    private AdminSessionIdentity requireOrganizerIdentity() {
        AdminSessionIdentity identity = requireSupportedIdentity();
        if (identity.adminType() != AdminType.ORGANIZER_ADMIN) {
            throw new ForbiddenException("只有主办方管理员可以购买啤酒币");
        }
        return identity;
    }

    private Long requireCurrentEnterpriseAccount(AdminSessionIdentity identity) {
        if (identity.organizerId() == null) {
            throw new ForbiddenException("当前账号未关联主办方组织");
        }
        Organizer organizer = organizerMapper.selectById(identity.organizerId());
        if (organizer == null || !OrganizerType.TENANT.name().equals(organizer.getOrganizerType())
                || organizer.getEnterpriseAccountId() == null) {
            throw new ForbiddenException("当前组织不支持啤酒币业务");
        }
        requireActiveAccount(organizer.getEnterpriseAccountId());
        return organizer.getEnterpriseAccountId();
    }

    private EnterpriseAccount requireActiveAccount(Long id) {
        EnterpriseAccount account = enterpriseAccountMapper.selectById(id);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new BaseException("企业账户不存在或已停用");
        }
        return account;
    }

    private EnterpriseAccount requireActiveTenantAccount(Long id) {
        EnterpriseAccount account = requireActiveAccount(id);
        boolean tenantAccount = organizerMapper.selectCount(new LambdaQueryWrapper<Organizer>()
                .eq(Organizer::getOrganizerType, OrganizerType.TENANT.name())
                .eq(Organizer::getEnterpriseAccountId, id)) > 0;
        if (!tenantAccount) {
            throw new BaseException("只有第三方主办方账户可以使用啤酒币");
        }
        return account;
    }

    private BeerCoinProduct requireActiveProduct() {
        BeerCoinProduct product = productMapper.selectActive();
        if (product == null) {
            throw new BaseException("平台尚未设置啤酒币价格");
        }
        return product;
    }

    private BeerCoinPurchaseOrder preparePaymentOrder(Long id) {
        AdminSessionIdentity identity = requireOrganizerIdentity();
        BeerCoinPurchaseOrder order = requireOrderForUpdate(id);
        Long accountId = requireCurrentEnterpriseAccount(identity);
        if (!Objects.equals(accountId, order.getEnterpriseAccountId())) {
            throw new ForbiddenException("无权操作其他组织的啤酒币订单");
        }
        if (PAID.equals(order.getStatus())) {
            return order;
        }
        if (!WAITING_PAYMENT.equals(order.getStatus()) && !BeerCoinPurchaseOrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new BaseException("当前订单不能发起支付");
        }
        return order;
    }

    private BeerCoinPurchaseOrder requireOrderForUpdate(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("啤酒币订单不存在");
        }
        BeerCoinPurchaseOrder order = purchaseOrderMapper.selectOne(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                .eq(BeerCoinPurchaseOrder::getId, id)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new ResourceNotFoundException("啤酒币订单不存在");
        }
        return order;
    }

    private BeerCoinPurchaseOrder requireAccessibleOrder(Long id) {
        AdminSessionIdentity identity = requireSupportedIdentity();
        BeerCoinPurchaseOrder order = requireOrder(id);
        if (identity.adminType() == AdminType.PLATFORM_SUPER_ADMIN) {
            return order;
        }
        if (!Objects.equals(requireCurrentEnterpriseAccount(identity), order.getEnterpriseAccountId())) {
            throw new ForbiddenException("无权查看其他组织的啤酒币订单");
        }
        return order;
    }

    private BeerCoinPurchaseOrder requireOrder(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("啤酒币订单不存在");
        }
        BeerCoinPurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new ResourceNotFoundException("啤酒币订单不存在");
        }
        return order;
    }

    /**
     * 在重新生成支付参数前同步旧微信单，避免 Native 与 JSAPI 同时有效。
     */
    private BeerCoinPurchaseOrder synchronizeWechatPayment(BeerCoinPurchaseOrder order, boolean closeUnpaid) {
        if (order == null || !isWechatPaymentMode()
                || !StringUtils.hasText(order.getOutTradeNo())) {
            return order;
        }
        WechatPayClient.PaymentQueryResult result = wechatPayClient.queryPayment(order.getOutTradeNo());
        if ("SUCCESS".equals(result.tradeState())) {
            applyWechatPaymentSuccess(new WechatPayClient.PaymentNotifyResult(
                    "QUERY-" + order.getOutTradeNo(), "QUERY.SUCCESS", order.getOutTradeNo(),
                    result.transactionId(), result.tradeState(), result.tradeStateDesc(),
                    result.paidAmount(), result.paidTime(), null));
            return purchaseOrderMapper.selectById(order.getId());
        }
        order.setWechatTradeState(result.tradeState());
        order.setWechatTradeStateDesc(result.tradeStateDesc());
        if (WECHAT_TERMINAL_FAILURE_STATES.contains(result.tradeState())) {
            order.setStatus(BeerCoinPurchaseOrderStatus.CLOSED.name());
        }
        if (closeUnpaid && WAITING_PAYMENT.equals(order.getStatus())) {
            wechatPayClient.closePayment(order.getOutTradeNo());
            order.setWechatTradeState("CLOSED");
            order.setWechatTradeStateDesc("旧支付单已关闭");
        }
        purchaseOrderMapper.updateById(order);
        return order;
    }

    private void expireOrder(BeerCoinPurchaseOrder order) {
        if (order != null && !PAID.equals(order.getStatus())
                && !BeerCoinPurchaseOrderStatus.EXPIRED.name().equals(order.getStatus())) {
            order.setStatus(BeerCoinPurchaseOrderStatus.EXPIRED.name());
            order.setWechatTradeState("CLOSED");
            order.setWechatTradeStateDesc("支付已过期");
            purchaseOrderMapper.updateById(order);
        }
    }

    private boolean isExpired(BeerCoinPurchaseOrder order, LocalDateTime now) {
        return order != null && order.getExpireTime() != null && !order.getExpireTime().isAfter(now)
                && !PAID.equals(order.getStatus());
    }

    private List<BeerCoinPurchaseOrderVO> listPurchaseOrders(Long accountId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                        .eq(BeerCoinPurchaseOrder::getEnterpriseAccountId, accountId)
                        .orderByDesc(BeerCoinPurchaseOrder::getCreateTime)
                        .orderByDesc(BeerCoinPurchaseOrder::getId)
                        .last("LIMIT 100"))
                .stream().map(this::toOrderVO).toList();
    }

    private List<BeerCoinPurchaseOrderVO> listAllPurchaseOrders() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<BeerCoinPurchaseOrder>()
                        .orderByDesc(BeerCoinPurchaseOrder::getCreateTime)
                        .orderByDesc(BeerCoinPurchaseOrder::getId)
                        .last("LIMIT 200"))
                .stream().map(this::toOrderVO).toList();
    }

    private List<BeerCoinLedgerVO> listLedger(Long accountId) {
        return ledgerMapper.selectList(new LambdaQueryWrapper<BeerCoinLedger>()
                        .eq(BeerCoinLedger::getEnterpriseAccountId, accountId)
                        .orderByDesc(BeerCoinLedger::getCreateTime)
                        .orderByDesc(BeerCoinLedger::getId)
                        .last("LIMIT 200"))
                .stream().map(this::toLedgerVO).toList();
    }

    private List<BeerCoinLedgerVO> listAllLedger() {
        return ledgerMapper.selectList(new LambdaQueryWrapper<BeerCoinLedger>()
                        .orderByDesc(BeerCoinLedger::getCreateTime)
                        .orderByDesc(BeerCoinLedger::getId)
                        .last("LIMIT 500"))
                .stream().map(this::toLedgerVO).toList();
    }

    private List<BeerCoinLotVO> listLots(Long accountId) {
        return lotMapper.selectList(new LambdaQueryWrapper<BeerCoinLot>()
                        .eq(BeerCoinLot::getEnterpriseAccountId, accountId)
                        .orderByAsc(BeerCoinLot::getExpiresAt)
                        .orderByAsc(BeerCoinLot::getId)
                        .last("LIMIT 200"))
                .stream().map(this::toLotVO).toList();
    }

    private List<BeerCoinLotVO> listAllLots() {
        return lotMapper.selectList(new LambdaQueryWrapper<BeerCoinLot>()
                        .orderByAsc(BeerCoinLot::getExpiresAt)
                        .orderByAsc(BeerCoinLot::getId)
                        .last("LIMIT 500"))
                .stream().map(this::toLotVO).toList();
    }

    private List<BeerCoinAccountOptionVO> listActiveAccountOptions() {
        List<Long> tenantAccountIds = organizerMapper.selectList(new LambdaQueryWrapper<Organizer>()
                        .eq(Organizer::getOrganizerType, OrganizerType.TENANT.name())
                        .isNotNull(Organizer::getEnterpriseAccountId))
                .stream()
                .map(Organizer::getEnterpriseAccountId)
                .distinct()
                .toList();
        if (tenantAccountIds.isEmpty()) {
            return List.of();
        }
        return enterpriseAccountMapper.selectList(new LambdaQueryWrapper<EnterpriseAccount>()
                        .in(EnterpriseAccount::getId, tenantAccountIds)
                        .eq(EnterpriseAccount::getStatus, "ACTIVE")
                        .orderByAsc(EnterpriseAccount::getName)
                        .orderByAsc(EnterpriseAccount::getId))
                .stream()
                .map(account -> BeerCoinAccountOptionVO.builder()
                        .id(account.getId())
                        .name(account.getName())
                        .build())
                .toList();
    }

    private BeerCoinWalletVO toWallet(Long accountId) {
        EnterpriseAccount account = requireActiveAccount(accountId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiringBefore = now.plusDays(EXPIRING_WINDOW_DAYS);
        List<BeerCoinLot> lots = lotMapper.selectList(new LambdaQueryWrapper<BeerCoinLot>()
                .eq(BeerCoinLot::getEnterpriseAccountId, accountId)
                .gt(BeerCoinLot::getRemainingQuantity, 0)
                .le(BeerCoinLot::getAvailableFrom, now)
                .gt(BeerCoinLot::getExpiresAt, now)
                .orderByAsc(BeerCoinLot::getExpiresAt)
                .orderByAsc(BeerCoinLot::getId));
        long available = lots.stream().mapToLong(item -> nullToZero(item.getRemainingQuantity())).sum();
        long expiring = lots.stream().filter(item -> !item.getExpiresAt().isAfter(expiringBefore))
                .mapToLong(item -> nullToZero(item.getRemainingQuantity())).sum();
        return BeerCoinWalletVO.builder()
                .enterpriseAccountId(account.getId())
                .enterpriseAccountName(account.getName())
                .availableQuantity(available)
                .expiringQuantity(expiring)
                .nextExpiryTime(lots.isEmpty() ? null : lots.get(0).getExpiresAt())
                .build();
    }

    private BeerCoinPricingVO toPricingVO(BeerCoinProduct product) {
        if (product == null) {
            return null;
        }
        return BeerCoinPricingVO.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .status(product.getStatus())
                .effectiveTime(product.getEffectiveTime())
                .versionNo(product.getVersionNo())
                .tiers(loadTiers(product.getId()).stream().map(item -> BeerCoinTierVO.builder()
                        .id(item.getId())
                        .startQuantity(item.getStartQuantity())
                        .endQuantity(item.getEndQuantity())
                        .unitPrice(item.getUnitPrice())
                        .sortOrder(item.getSortOrder())
                        .build()).toList())
                .build();
    }

    private BeerCoinPurchaseOrderVO toOrderVO(BeerCoinPurchaseOrder order) {
        if (order == null) {
            return null;
        }
        JsonNode snapshot = readSnapshot(order.getProductSnapshotJson());
        String productName = snapshot == null ? null : text(snapshot, "name");
        return BeerCoinPurchaseOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .enterpriseAccountId(order.getEnterpriseAccountId())
                .enterpriseAccountName(resolveAccountName(order.getEnterpriseAccountId()))
                .productId(order.getProductId())
                .productName(productName)
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(order.getStatus())
                .outTradeNo(order.getOutTradeNo())
                .wechatTransactionId(order.getWechatTransactionId())
                .codeUrl(order.getCodeUrl())
                .expireTime(order.getExpireTime())
                .wechatTradeState(order.getWechatTradeState())
                .wechatTradeStateDesc(order.getWechatTradeStateDesc())
                .paidTime(order.getPaidTime())
                .createTime(order.getCreateTime())
                .priceSegments(readSegments(snapshot))
                .build();
    }

    private BeerCoinPurchasePaymentVO toPaymentVO(BeerCoinPurchaseOrder order, WechatPayClient.JsapiPayResult result) {
        WechatJsapiPayVO.JsapiPayParams params = result == null ? null : WechatJsapiPayVO.JsapiPayParams.builder()
                .appId(result.appId()).timeStamp(result.timeStamp()).nonceStr(result.nonceStr())
                .packageValue(result.packageValue()).signType(result.signType()).paySign(result.paySign()).build();
        return BeerCoinPurchasePaymentVO.builder()
                .mode(isWechatPaymentMode() ? "WECHAT" : "MOCK")
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .outTradeNo(order.getOutTradeNo())
                .amount(order.getAmount())
                .codeUrl(order.getCodeUrl())
                .expireTime(order.getExpireTime())
                .paymentStatus(order.getStatus())
                .payParams(params)
                .build();
    }

    private BeerCoinPurchasePaymentVO prepareMockPayment(BeerCoinPurchaseOrder order) {
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(order, now)) {
            expireOrder(order);
            throw new BaseException("支付订单已过期，请重新创建购买订单");
        }
        if (PAID.equals(order.getStatus())) {
            return toPaymentVO(order, null);
        }
        if (!StringUtils.hasText(order.getOutTradeNo())) {
            order.setOutTradeNo(generateTradeNo());
        }
        if (order.getExpireTime() == null || !order.getExpireTime().isAfter(now)) {
            order.setExpireTime(now.plusMinutes(PAYMENT_EXPIRE_MINUTES));
        }
        order.setCodeUrl(null);
        order.setStatus(WAITING_PAYMENT);
        order.setWechatTradeState("MOCK_WAITING");
        order.setWechatTradeStateDesc("等待模拟到账");
        purchaseOrderMapper.updateById(order);
        return toPaymentVO(order, null);
    }

    private boolean isWechatPaymentMode() {
        // 只有啤酒币和公共微信客户端都配置为 WECHAT 时才调用真实微信接口。
        return beerCoinPaymentProperties.isWechatMode() && wechatPayProperties.isWechatMode();
    }

    private BeerCoinLedgerVO toLedgerVO(BeerCoinLedger ledger) {
        return BeerCoinLedgerVO.builder()
                .id(ledger.getId()).ledgerNo(ledger.getLedgerNo())
                .enterpriseAccountId(ledger.getEnterpriseAccountId())
                .enterpriseAccountName(resolveAccountName(ledger.getEnterpriseAccountId()))
                .direction(ledger.getDirection()).quantity(ledger.getQuantity())
                .businessType(ledger.getBusinessType()).businessId(ledger.getBusinessId())
                .reason(ledger.getReason()).createTime(ledger.getCreateTime()).build();
    }

    private BeerCoinLotVO toLotVO(BeerCoinLot lot) {
        return BeerCoinLotVO.builder()
                .id(lot.getId()).lotNo(lot.getLotNo()).enterpriseAccountId(lot.getEnterpriseAccountId())
                .enterpriseAccountName(resolveAccountName(lot.getEnterpriseAccountId())).purchaseOrderId(lot.getPurchaseOrderId())
                .totalQuantity(lot.getTotalQuantity()).remainingQuantity(lot.getRemainingQuantity())
                .availableFrom(lot.getAvailableFrom()).expiresAt(lot.getExpiresAt())
                .sourceType(lot.getSourceType()).sourceOrderId(lot.getSourceOrderId()).build();
    }

    private List<BeerCoinProductTier> loadTiers(Long productId) {
        return tierMapper.selectList(new LambdaQueryWrapper<BeerCoinProductTier>()
                .eq(BeerCoinProductTier::getProductId, productId)
                .orderByAsc(BeerCoinProductTier::getSortOrder)
                .orderByAsc(BeerCoinProductTier::getId));
    }

    private List<BeerCoinTierRequest> validatePricingRequest(BeerCoinPricingRequest request) {
        if (request == null || request.getTiers() == null || request.getTiers().isEmpty()) {
            throw new BaseException("请至少配置一档啤酒币价格");
        }
        if (request.getTiers().size() > MAX_PRICING_TIERS) {
            throw new BaseException("价格阶梯最多配置 " + MAX_PRICING_TIERS + " 档");
        }
        List<BeerCoinTierRequest> tiers = new ArrayList<>(request.getTiers());
        tiers.sort(Comparator.comparing(BeerCoinTierRequest::getStartQuantity,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        long expectedStart = 1;
        for (BeerCoinTierRequest tier : tiers) {
            if (tier.getStartQuantity() == null || tier.getStartQuantity() != expectedStart) {
                throw new BaseException("价格区间必须从 1 开始且连续，不能重叠或断档");
            }
            if (tier.getEndQuantity() != null && tier.getEndQuantity() < tier.getStartQuantity()) {
                throw new BaseException("阶梯结束数量不能小于起始数量");
            }
            if (tier.getUnitPrice() == null || tier.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0
                    || tier.getUnitPrice().scale() > 2
                    || tier.getUnitPrice().compareTo(MAX_TIER_UNIT_PRICE) > 0) {
                throw new BaseException("单枚价格必须在 0.01 到 " + MAX_TIER_UNIT_PRICE + " 之间，且最多保留两位小数");
            }
            if (tier.getEndQuantity() == null) {
                if (tier != tiers.get(tiers.size() - 1)) {
                    throw new BaseException("开放区间只能作为最后一档");
                }
                expectedStart = Long.MAX_VALUE;
            } else {
                if (tier.getEndQuantity() == Long.MAX_VALUE) {
                    expectedStart = Long.MAX_VALUE;
                } else {
                    expectedStart = tier.getEndQuantity() + 1;
                }
            }
        }
        BeerCoinTierRequest lastTier = tiers.get(tiers.size() - 1);
        if (lastTier.getEndQuantity() != null && lastTier.getEndQuantity() < MAX_PURCHASE_QUANTITY) {
            throw new BaseException("最后一档必须设置为开放区间或覆盖完整购买范围");
        }
        return tiers;
    }

    private long requirePurchaseQuantity(Long quantity) {
        if (quantity == null || quantity <= 0 || quantity > MAX_PURCHASE_QUANTITY) {
            throw new BaseException("购买数量必须在 1 到 " + MAX_PURCHASE_QUANTITY + " 之间");
        }
        return quantity;
    }

    private PriceCalculation calculatePrice(long quantity, List<BeerCoinProductTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new BaseException("当前未配置啤酒币价格");
        }
        List<BeerCoinPriceSegmentVO> segments = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        long cursor = 1;
        for (BeerCoinProductTier tier : tiers) {
            long start = Math.max(cursor, tier.getStartQuantity());
            if (start > quantity) {
                break;
            }
            long end = tier.getEndQuantity() == null ? quantity : Math.min(quantity, tier.getEndQuantity());
            if (end < start) {
                continue;
            }
            long segmentQuantity = end - start + 1;
            BigDecimal segmentAmount = BigDecimal.valueOf(segmentQuantity).multiply(tier.getUnitPrice());
            amount = amount.add(segmentAmount);
            segments.add(BeerCoinPriceSegmentVO.builder()
                    .startQuantity(start).endQuantity(end)
                    .quantity(segmentQuantity).unitPrice(tier.getUnitPrice()).amount(segmentAmount).build());
            cursor = end == Long.MAX_VALUE ? Long.MAX_VALUE : end + 1;
        }
        if (cursor <= quantity) {
            throw new BaseException("购买数量超出当前计价范围");
        }
        BigDecimal normalizedAmount = amount.setScale(2);
        if (normalizedAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new BaseException("购买金额超过系统支持的最大金额 " + MAX_ORDER_AMOUNT + " 元");
        }
        return new PriceCalculation(normalizedAmount, segments);
    }

    private String writeProductSnapshot(BeerCoinProduct product, List<BeerCoinProductTier> tiers,
                                        PriceCalculation calculation) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("id", product.getId());
            root.put("productCode", product.getProductCode());
            root.put("name", product.getName());
            root.put("versionNo", product.getVersionNo());
            ArrayNode tierArray = root.putArray("tiers");
            for (BeerCoinProductTier tier : tiers) {
                ObjectNode node = tierArray.addObject();
                node.put("startQuantity", tier.getStartQuantity());
                if (tier.getEndQuantity() == null) node.putNull("endQuantity"); else node.put("endQuantity", tier.getEndQuantity());
                node.put("unitPrice", tier.getUnitPrice());
            }
            ArrayNode segmentArray = root.putArray("segments");
            for (BeerCoinPriceSegmentVO segment : calculation.segments()) {
                ObjectNode node = segmentArray.addObject();
                node.put("startQuantity", segment.getStartQuantity());
                if (segment.getEndQuantity() == null) node.putNull("endQuantity"); else node.put("endQuantity", segment.getEndQuantity());
                node.put("quantity", segment.getQuantity());
                node.put("unitPrice", segment.getUnitPrice());
                node.put("amount", segment.getAmount());
            }
            root.put("amount", calculation.amount());
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new BaseException("生成价格快照失败");
        }
    }

    private JsonNode readSnapshot(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<BeerCoinPriceSegmentVO> readSegments(JsonNode root) {
        if (root == null || !root.has("segments")) return List.of();
        try {
            return objectMapper.convertValue(root.get("segments"), new TypeReference<List<BeerCoinPriceSegmentVO>>() {});
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String resolveAccountName(Long accountId) {
        if (accountId == null) return null;
        EnterpriseAccount account = enterpriseAccountMapper.selectById(accountId);
        return account == null ? null : account.getName();
    }

    private BeerCoinLedger findLedger(Long accountId, String key) {
        return ledgerMapper.selectOne(new LambdaQueryWrapper<BeerCoinLedger>()
                .eq(BeerCoinLedger::getEnterpriseAccountId, accountId)
                .eq(BeerCoinLedger::getIdempotencyKey, key)
                .last("LIMIT 1"));
    }

    private BeerCoinLedger insertLedger(Long accountId, String direction, long quantity, String businessType,
                                        String businessId, String key, Long operatorId, String reason) {
        BeerCoinLedger ledger = BeerCoinLedger.builder()
                .ledgerNo(generateLedgerNo())
                .enterpriseAccountId(accountId)
                .direction(direction)
                .quantity(quantity)
                .businessType(businessType)
                .businessId(businessId)
                .idempotencyKey(key)
                .operatorAdminId(operatorId)
                .reason(reason)
                .build();
        ledgerMapper.insert(ledger);
        return ledger;
    }

    private BeerCoinLedger creditCoins(Long accountId, long quantity, String businessType, String businessId,
                                       String key, Long operatorId, String reason, String direction) {
        if (quantity <= 0) {
            return null;
        }
        BeerCoinLedger existing = findLedger(accountId, key);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        BeerCoinLot lot = BeerCoinLot.builder()
                .lotNo(generateLotNo())
                .enterpriseAccountId(accountId)
                .totalQuantity(quantity)
                .remainingQuantity(quantity)
                .availableFrom(now)
                .expiresAt(now.plusYears(1))
                .sourceType("MANUAL")
                .sourceOrderId(key)
                .build();
        lotMapper.insert(lot);
        return insertLedger(accountId, direction, quantity, businessType, businessId, key, operatorId, reason);
    }

    private BeerCoinLedger consumeCoins(Long accountId, long quantity, String businessType, String businessId,
                                        String key, Long operatorId, String reason, String direction) {
        if (quantity <= 0) return null;
        BeerCoinLedger existing = findLedger(accountId, key);
        if (existing != null) return existing;
        LocalDateTime now = LocalDateTime.now();
        List<BeerCoinLot> lots = lotMapper.selectList(new LambdaQueryWrapper<BeerCoinLot>()
                .eq(BeerCoinLot::getEnterpriseAccountId, accountId)
                .gt(BeerCoinLot::getRemainingQuantity, 0)
                .le(BeerCoinLot::getAvailableFrom, now)
                .gt(BeerCoinLot::getExpiresAt, now)
                .orderByAsc(BeerCoinLot::getExpiresAt)
                .orderByAsc(BeerCoinLot::getAvailableFrom)
                .orderByAsc(BeerCoinLot::getId)
                .last("FOR UPDATE"));
        // 并发请求可能在锁定批次前同时看不到流水；拿到批次锁后必须再次确认幂等键。
        existing = findLedger(accountId, key);
        if (existing != null) return existing;
        long available = lots.stream().mapToLong(item -> nullToZero(item.getRemainingQuantity())).sum();
        if (available < quantity) {
            throw new BaseException("啤酒币余额不足，请先购买啤酒币");
        }
        BeerCoinLedger ledger = insertLedger(accountId, direction, quantity, businessType, businessId, key, operatorId, reason);
        long remaining = quantity;
        for (BeerCoinLot lot : lots) {
            if (remaining == 0) break;
            long used = Math.min(remaining, nullToZero(lot.getRemainingQuantity()));
            lot.setRemainingQuantity(lot.getRemainingQuantity() - used);
            lotMapper.updateById(lot);
            allocationMapper.insert(BeerCoinConsumptionAllocation.builder()
                    .ledgerId(ledger.getId()).lotId(lot.getId()).quantity(used).build());
            remaining -= used;
        }
        return ledger;
    }

    private CompetitionContext requireTenantCompetition(Long competitionId) {
        return resolveTenantCompetition(requireCompetition(competitionId));
    }

    private CompetitionContext requireTenantCompetitionForUpdate(Long competitionId) {
        Competition competition = competitionMapper.selectOne(new LambdaQueryWrapper<Competition>()
                .eq(Competition::getId, competitionId)
                .last("FOR UPDATE"));
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return resolveTenantCompetition(competition);
    }

    private CompetitionContext resolveTenantCompetition(Competition competition) {
        Organizer organizer = organizerMapper.selectById(competition.getOrganizerId());
        if (organizer == null) {
            throw new BaseException("比赛所属组织不存在");
        }
        if (OrganizerType.PLATFORM.name().equals(organizer.getOrganizerType())) {
            return new CompetitionContext(competition, null);
        }
        if (!OrganizerType.TENANT.name().equals(organizer.getOrganizerType())
                || organizer.getEnterpriseAccountId() == null) {
            throw new BaseException("比赛所属租户账户配置不完整");
        }
        requireActiveAccount(organizer.getEnterpriseAccountId());
        return new CompetitionContext(competition, organizer.getEnterpriseAccountId());
    }

    private long requiredQuantityForEntryCount(int effectiveCount) {
        return Math.max(1L, (effectiveCount + 9L) / 10L);
    }

    private boolean isSettlementCalculationStage(String status) {
        return Set.of(
                CompetitionStatus.REGISTRATION_CLOSED.name(),
                CompetitionStatus.JUDGING_PREP.name(),
                CompetitionStatus.JUDGING.name(),
                CompetitionStatus.RESULT_CONFIRMING.name(),
                CompetitionStatus.PUBLISHED.name(),
                CompetitionStatus.ARCHIVED.name()
        ).contains(status);
    }

    private boolean isFinalSettlementStage(String status) {
        return Set.of(
                CompetitionStatus.JUDGING_PREP.name(),
                CompetitionStatus.JUDGING.name(),
                CompetitionStatus.RESULT_CONFIRMING.name(),
                CompetitionStatus.PUBLISHED.name(),
                CompetitionStatus.ARCHIVED.name()
        ).contains(status);
    }

    private void snapshotEffectiveEntries(CompetitionCoinSettlement settlement, Long competitionId) {
        List<BeerEntry> entries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .eq(BeerEntry::getDeletedFlag, 0)
                .in(BeerEntry::getStatus, EntryStatus.REGISTERED.name(), EntryStatus.STORED.name(), EntryStatus.RESULT_PUBLISHED.name())
                .orderByAsc(BeerEntry::getId));
        Set<Long> existingEntryIds = settlementEntryMapper.selectList(new LambdaQueryWrapper<CompetitionCoinSettlementEntry>()
                        .eq(CompetitionCoinSettlementEntry::getSettlementId, settlement.getId()))
                .stream()
                .map(CompetitionCoinSettlementEntry::getBeerEntryId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (BeerEntry entry : entries) {
            if (existingEntryIds.add(entry.getId())) {
                settlementEntryMapper.insert(CompetitionCoinSettlementEntry.builder()
                        .settlementId(settlement.getId())
                        .beerEntryId(entry.getId())
                        .entryStatusSnapshot(entry.getStatus())
                        .build());
            }
        }
    }

    private Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) throw new ResourceNotFoundException("比赛不存在");
        return competition;
    }

    private CompetitionCoinSettlement lockSettlement(Long competitionId, BeerCoinSettlementType type) {
        return settlementMapper.selectOne(new LambdaQueryWrapper<CompetitionCoinSettlement>()
                .eq(CompetitionCoinSettlement::getCompetitionId, competitionId)
                .eq(CompetitionCoinSettlement::getSettlementType, type.name())
                .last("FOR UPDATE"));
    }

    private String paymentDescription(BeerCoinPurchaseOrder order) {
        return "啤酒币充值-" + order.getOrderNo();
    }

    private String generateOrderNo() {
        return "BCO" + UUID.randomUUID().toString().replace("-", "").substring(0, 21).toUpperCase();
    }

    private String generateTradeNo() {
        return "BCT" + UUID.randomUUID().toString().replace("-", "").substring(0, 21).toUpperCase();
    }

    private String generateLotNo() {
        return "BCL" + UUID.randomUUID().toString().replace("-", "").substring(0, 21).toUpperCase();
    }

    private String generateLedgerNo() {
        return "BCLD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private int nullToZeroInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record PriceCalculation(BigDecimal amount, List<BeerCoinPriceSegmentVO> segments) { }

    private record CompetitionContext(Competition competition, Long enterpriseAccountId) { }
}
