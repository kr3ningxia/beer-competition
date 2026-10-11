package com.beercompetition.judging.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.judging.scoring.ScoreConfirmationService;
import com.beercompetition.judging.scoring.ScoreRoundResultSupport;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundResultType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.service.impl.round.RoundConstants;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首轮评审进行中调整桌内酒款：摘除、回补已摘除酒款、补充新入库酒款。
 *
 * <p>评分数据按 beerEntryId 保留，摘除只移除本轮的分配行并作废该酒在本轮的晋级结果；
 * 因此重新加入（回补）时会按保留的桌长汇总结果重建晋级状态，实现「摘除即可逆」。</p>
 */
@Service
@RequiredArgsConstructor
public class RoundEntryAdjustmentService {

    private static final int FLAG_TRUE = 1;

    private final RoundQuerySupport roundQuerySupport;
    private final RoundTableEntryMapper roundTableEntryMapper;
    private final RoundTableMapper roundTableMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundResultMapper roundResultMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ScoreRoundResultSupport scoreRoundResultSupport;
    private final ScoreConfirmationService scoreConfirmationService;

    /**
     * 摘除：把某款酒从首轮评审桌移除，保留其评分数据以便回补还原。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeEntryFromRoundTable(Long competitionId, Long roundId, Long roundTableId, String entryUuid, String reason) {
        AdjustmentContext context = requireAdjustableScoreRound(competitionId, roundId, roundTableId);
        BeerEntry entry = requireCompetitionEntry(competitionId, entryUuid);
        RoundTableEntry roundEntry = findRoundEntry(roundId, entry.getId());
        if (roundEntry == null || !Objects.equals(roundEntry.getRoundTableId(), roundTableId)) {
            throw new BaseException("该酒款不在当前评审桌");
        }
        long tableEntryCount = roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundTableId, roundTableId));
        if (tableEntryCount <= 1) {
            throw new BaseException("每张评审桌至少保留 1 款酒");
        }

        rollbackSubmittedTable(context);
        // 作废该酒在本轮的晋级结果（评分记录保留），否则仍会被当作下一轮候选。
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getRoundId, roundId)
                .eq(RoundResult::getBeerEntryId, entry.getId())
                .eq(RoundResult::getResultType, RoundResultType.ADVANCE.name()));
        roundTableEntryMapper.deleteById(roundEntry.getId());
        recomputeTableCategory(context.table());
        scoreRoundResultSupport.bumpResultVersion(roundTableId);
        scoreConfirmationService.refreshAfterEntryChange(roundTableId);
        writeLog(competitionId, "ROUND_ENTRY_REMOVE", entry.getUuid(),
                "首轮摘除酒款：" + entry.getName() + reasonSuffix(reason));
    }

    /**
     * 加入：回补此前摘除的酒款，或补充新入库酒款（新酒款需在本轮重新评分）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addEntryToRoundTable(Long competitionId, Long roundId, Long roundTableId, String entryUuid, String reason) {
        AdjustmentContext context = requireAdjustableScoreRound(competitionId, roundId, roundTableId);
        BeerEntry entry = requireCompetitionEntry(competitionId, entryUuid);
        if (!Objects.equals(entry.getStoredFlag(), FLAG_TRUE) || EntryStatus.CANCELED.name().equals(entry.getStatus())) {
            throw new BaseException("只能加入已入库且未取消的酒款");
        }
        if (findRoundEntry(roundId, entry.getId()) != null) {
            throw new BaseException("该酒款已在当前轮次");
        }
        assertCategoryCompatible(context.table(), entry);

        rollbackSubmittedTable(context);
        int nextSortOrder = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, roundTableId))
                .stream()
                .map(RoundTableEntry::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        RoundTableEntry roundEntry = RoundTableEntry.builder()
                .competitionId(competitionId)
                .roundId(roundId)
                .roundTableId(roundTableId)
                .beerEntryId(entry.getId())
                .status(RoundEntryStatus.ASSIGNED.name())
                .sortOrder(nextSortOrder)
                .build();
        roundTableEntryMapper.insert(roundEntry);
        recomputeTableCategory(context.table());
        // 回补：若该酒款保留了桌长汇总结果，按其重建晋级标记与轮次结果，等价于还原摘除前状态。
        ScoreRecord finalRecord = scoreRecordMapper.selectOne(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entry.getId())
                .eq(ScoreRecord::getFinalFlag, FLAG_TRUE)
                .last("LIMIT 1"));
        if (finalRecord != null) {
            scoreRoundResultSupport.syncFirstRoundAdvance(roundEntry, finalRecord);
        }
        scoreRoundResultSupport.bumpResultVersion(roundTableId);
        scoreConfirmationService.refreshAfterEntryChange(roundTableId);
        writeLog(competitionId, "ROUND_ENTRY_ADD", entry.getUuid(),
                "首轮加入酒款：" + entry.getName() + reasonSuffix(reason));
    }

    private AdjustmentContext requireAdjustableScoreRound(Long competitionId, Long roundId, Long roundTableId) {
        Competition competition = roundQuerySupport.requireCompetition(competitionId);
        if (CompetitionStatus.ARCHIVED.name().equals(competition.getStatus())) {
            throw new BaseException("比赛已归档");
        }
        CompetitionRound round = roundQuerySupport.requireRoundForUpdate(competitionId, roundId);
        if (!RoundType.SCORE.name().equals(round.getRoundType())) {
            throw new BaseException("只有首轮评分轮支持摘除或加入酒款");
        }
        if (!Set.of(RoundStatus.PUBLISHED.name(), RoundStatus.SUBMITTED.name()).contains(round.getStatus())) {
            throw new BaseException("只有已发布且未锁定的首轮可以调整酒款");
        }
        RoundTable table = roundQuerySupport.requireRoundTable(roundTableId);
        if (!Objects.equals(table.getRoundId(), roundId) || !Objects.equals(table.getCompetitionId(), competitionId)) {
            throw new ResourceNotFoundException("评审桌不存在");
        }
        if (RoundStatus.LOCKED.name().equals(table.getStatus())) {
            throw new BaseException("本桌已锁定，不能调整酒款");
        }
        return new AdjustmentContext(round, table);
    }

    /**
     * 已提交的桌退回进行中；随后由版本号递增统一作废同桌确认。
     */
    private void rollbackSubmittedTable(AdjustmentContext context) {
        RoundTable table = context.table();
        if (RoundStatus.SUBMITTED.name().equals(table.getStatus())) {
            table.setStatus(RoundStatus.PUBLISHED.name());
            roundTableMapper.updateById(table);
        }
        CompetitionRound round = context.round();
        if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
            round.setStatus(RoundStatus.PUBLISHED.name());
            round.setSubmittedTime(null);
            // updateById 默认跳过 null 字段，显式 set 才能清掉提交时间。
            competitionRoundMapper.update(null, new LambdaUpdateWrapper<CompetitionRound>()
                    .eq(CompetitionRound::getId, round.getId())
                    .set(CompetitionRound::getStatus, RoundStatus.PUBLISHED.name())
                    .set(CompetitionRound::getSubmittedTime, null));
        }
    }

    private BeerEntry requireCompetitionEntry(Long competitionId, String entryUuid) {
        if (!StringUtils.hasText(entryUuid)) {
            throw new BaseException("请选择酒款");
        }
        String uuid = entryUuid.trim();
        return roundQuerySupport.loadEntryByUuids(competitionId, Set.of(uuid)).get(uuid);
    }

    private RoundTableEntry findRoundEntry(Long roundId, Long beerEntryId) {
        return roundTableEntryMapper.selectOne(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getRoundId, roundId)
                .eq(RoundTableEntry::getBeerEntryId, beerEntryId)
                .last("LIMIT 1"));
    }

    private void assertCategoryCompatible(RoundTable table, BeerEntry entry) {
        if (RoundConstants.CATEGORY_MODE_CATEGORY.equals(table.getCategoryMode())
                && table.getCategoryId() != null
                && !Objects.equals(table.getCategoryId(), entry.getCategoryId())) {
            throw new BaseException("该酒款分组与当前评审桌不一致");
        }
    }

    /**
     * 按桌内实际酒款重算桌的分类范围，与编排保存时的推导口径保持一致。
     */
    private void recomputeTableCategory(RoundTable table) {
        Set<Long> entryIds = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                        .eq(RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(RoundTableEntry::getBeerEntryId)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = roundQuerySupport.loadEntries(entryIds).values().stream()
                .map(BeerEntry::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.size() == 1) {
            table.setCategoryId(categoryIds.iterator().next());
            table.setCategoryMode(RoundConstants.CATEGORY_MODE_CATEGORY);
        } else if (categoryIds.isEmpty()) {
            table.setCategoryId(null);
            table.setCategoryMode(RoundConstants.CATEGORY_MODE_EMPTY);
        } else {
            table.setCategoryId(null);
            table.setCategoryMode(RoundConstants.CATEGORY_MODE_MIXED);
        }
        roundTableMapper.updateById(table);
    }

    private String reasonSuffix(String reason) {
        return StringUtils.hasText(reason) ? "，原因：" + reason.trim() : "";
    }

    private void writeLog(Long competitionId, String action, String targetPublicId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .competitionId(competitionId)
                .action(action)
                .targetType("BEER_ENTRY")
                .targetPublicId(targetPublicId)
                .summary(summary)
                .build());
    }

    private record AdjustmentContext(CompetitionRound round, RoundTable table) {
    }
}
