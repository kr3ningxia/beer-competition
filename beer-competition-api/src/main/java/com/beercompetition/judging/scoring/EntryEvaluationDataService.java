package com.beercompetition.judging.scoring;

import com.beercompetition.pojo.vo.AdminEntryTraceVO;

import java.util.List;

/**
 * 向报名模块提供酒款评审轨迹、删除影响和关联数据清理能力。
 *
 * <p>报名用例不直接访问分桌、评分、名次和奖项 Mapper。清理操作必须由上层
 * 报名删除事务调用，确保付款、报名和评审数据同时回滚。</p>
 */
public interface EntryEvaluationDataService {

    /**
     * 加载管理端报名视图需要的评审轨迹和存在性摘要。
     */
    EntryEvaluationData loadEntryEvaluationData(Long entryId);

    /**
     * 判断酒款是否已经形成分桌、评分、轮次结果或奖项数据。
     *
     * <p>报名模块在修改或回退酒款状态前使用该结果保护评审数据不被覆盖。</p>
     */
    boolean hasEntryEvaluationData(Long entryId);

    /**
     * 汇总管理端物理删除前需要展示的评审数据影响。
     */
    EntryEvaluationImpact summarizeDeleteImpact(Long entryId);

    /**
     * 删除指定酒款的分桌、评分会话、评分、轮次结果和奖项关联。
     */
    void deleteEntryEvaluationData(Long entryId);

    record EntryEvaluationData(List<AdminEntryTraceVO> traces,
                               boolean assigned,
                               boolean hasScoreRecord,
                               boolean hasRoundResult,
                               boolean hasAwardResult) {
    }

    record EntryEvaluationImpact(int assignmentCount,
                                 int scoreSessionCount,
                                 int scoreRecordCount,
                                 int roundResultCount,
                                 int awardResultCount,
                                 boolean publishedAward) {

        public boolean hasEvaluationData() {
            return assignmentCount + scoreSessionCount + scoreRecordCount
                    + roundResultCount + awardResultCount > 0;
        }
    }
}
