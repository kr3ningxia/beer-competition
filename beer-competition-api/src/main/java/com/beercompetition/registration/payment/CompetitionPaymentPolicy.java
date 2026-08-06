package com.beercompetition.registration.payment;

/**
 * 解析比赛报名资金由平台还是主办方管理。
 *
 * <p>支付和退款用例依据该策略选择一期平台自动支付链路或二期主办方线下链路，
 * Controller 不直接判断主办方类型。比赛主办方归属落地前不提供实现。</p>
 */
public interface CompetitionPaymentPolicy {

    /**
     * 查询指定比赛的报名资金管理模式。
     *
     * @param competitionId 比赛 ID
     * @return 资金管理模式
     */
    PaymentManagementMode resolvePaymentManagementMode(Long competitionId);

    /**
     * 报名资金的管理主体。
     */
    enum PaymentManagementMode {
        /** 平台管理，继续使用一期自动支付和退款能力。 */
        PLATFORM_MANAGED,
        /** 主办方管理，使用收款码、银行账户和线下退款记录。 */
        ORGANIZER_MANAGED
    }
}
