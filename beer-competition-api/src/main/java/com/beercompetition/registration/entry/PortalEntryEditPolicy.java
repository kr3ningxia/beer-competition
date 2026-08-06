package com.beercompetition.registration.entry;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.judging.scoring.EntryEvaluationDataService;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.EntryRefund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * 统一判断厂商是否仍可自助修改报名资料。
 *
 * <p>命令和视图必须复用同一判定，避免页面显示可修改但提交时被拒绝，或评审
 * 数据生成后仍允许覆盖酒款资料。</p>
 */
@Service
@RequiredArgsConstructor
public class PortalEntryEditPolicy {

    private static final Set<String> EDITABLE_ENTRY_STATUSES = Set.of(
            EntryStatus.PENDING_PAYMENT.name(),
            EntryStatus.REGISTERED.name()
    );

    private static final Set<String> EDITABLE_COMPETITION_STATUSES = Set.of(
            CompetitionStatus.REGISTRATION_OPEN.name(),
            CompetitionStatus.REGISTRATION_CLOSED.name()
    );

    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
            EntryRefundStatus.REQUESTED.name(),
            EntryRefundStatus.APPROVED.name(),
            EntryRefundStatus.PROCESSING.name()
    );

    private final EntryEvaluationDataService entryEvaluationDataService;

    /**
     * 返回修改资格及面向厂商的禁用原因。
     */
    public EditDecision evaluate(BeerEntry entry,
                                 Competition competition,
                                 EntryRefund latestRefund,
                                 boolean resultPublished) {
        if (entry == null || competition == null) {
            return EditDecision.denied("当前酒款不能修改资料");
        }
        if (Objects.equals(entry.getStoredFlag(), 1)) {
            return EditDecision.denied("样品已入库，不能自助修改报名资料");
        }
        if (!EDITABLE_ENTRY_STATUSES.contains(entry.getStatus())) {
            return EditDecision.denied("当前状态不能修改报名资料");
        }
        if (!EDITABLE_COMPETITION_STATUSES.contains(competition.getStatus())) {
            return EditDecision.denied("赛事已进入评审准备，不能自助修改报名资料");
        }
        if (latestRefund != null && ACTIVE_REFUND_STATUSES.contains(latestRefund.getStatus())) {
            return EditDecision.denied("退款处理中，不能修改报名资料");
        }
        if (hasJudgingData(entry.getId())) {
            return EditDecision.denied("酒款已进入评审流程，不能自助修改报名资料");
        }
        if (resultPublished) {
            return EditDecision.denied("结果已发布，不能修改报名资料");
        }
        return EditDecision.allow();
    }

    /**
     * 校验修改资格，供写入事务在更新前调用。
     */
    public void requireEditable(BeerEntry entry,
                                Competition competition,
                                EntryRefund latestRefund,
                                boolean resultPublished) {
        EditDecision decision = evaluate(entry, competition, latestRefund, resultPublished);
        if (!decision.allowed()) {
            throw new BaseException(decision.disabledReason());
        }
    }

    private boolean hasJudgingData(Long entryId) {
        return entryEvaluationDataService.hasEntryEvaluationData(entryId);
    }

    /**
     * 厂商报名资料修改资格。
     *
     * @param allowed 是否允许修改
     * @param disabledReason 禁用原因；允许修改时为空
     */
    public record EditDecision(boolean allowed, String disabledReason) {

        private static EditDecision allow() {
            return new EditDecision(true, null);
        }

        private static EditDecision denied(String reason) {
            return new EditDecision(false, reason);
        }
    }
}
