package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.ScoreRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首轮评分结果的派生态同步。
 *
 * <p>按桌长汇总结果推导桌内酒款的晋级标记与轮次晋级结果，并以结果版本号作废过期确认。
 * 由评分汇总和进行中酒款调整共同复用，保证两条链路的派生逻辑一致。</p>
 */
@Component
@RequiredArgsConstructor
public class ScoreRoundResultSupport {

    private static final int FLAG_FALSE = 0;
    private static final int FLAG_TRUE = 1;

    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundResultMapper roundResultMapper;
    private final RoundTableMapper roundTableMapper;

    public void syncFirstRoundAdvance(RoundTableEntry roundEntry, ScoreRecord finalRecord) {
        boolean advanced = Integer.valueOf(FLAG_TRUE).equals(finalRecord.getAdvancedFlag());
        roundEntry.setStatus(advanced ? RoundEntryStatus.ADVANCED.name() : RoundEntryStatus.ELIMINATED.name());
        roundTableEntryMapper.updateById(roundEntry);
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundTableId, roundEntry.getRoundTableId())
                .eq(RoundResult::getBeerEntryId, roundEntry.getBeerEntryId())
                .eq(RoundResult::getResultType, RoundResultType.ADVANCE.name()));
        if (advanced) {
            roundResultMapper.insert(RoundResult.builder()
                    .competitionId(roundEntry.getCompetitionId())
                    .roundId(roundEntry.getRoundId())
                    .roundTableId(roundEntry.getRoundTableId())
                    .beerEntryId(roundEntry.getBeerEntryId())
                    .resultType(RoundResultType.ADVANCE.name())
                    .submittedBy(finalRecord.getJudgeAccountId())
                    .submittedTime(LocalDateTime.now())
                    .lockedFlag(FLAG_FALSE)
                    .build());
        }
    }

    public void bumpResultVersion(Long roundTableId) {
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            return;
        }
        roundTableMapper.update(null, new LambdaUpdateWrapper<RoundTable>()
                .eq(RoundTable::getId, roundTableId)
                .set(RoundTable::getResultVersion, (table.getResultVersion() == null ? 0 : table.getResultVersion()) + 1)
                .set(RoundTable::getConfirmationOverrideFlag, FLAG_FALSE)
                .set(RoundTable::getConfirmationOverrideReason, null)
                .set(RoundTable::getConfirmationOverrideBy, null)
                .set(RoundTable::getConfirmationOverrideTime, null));
    }
}
