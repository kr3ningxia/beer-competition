package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.JudgeScoreSessionMapper;
import com.beercompetition.mapper.RoundResultMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeScoreSession;
import com.beercompetition.pojo.po.RoundResult;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.AdminEntryTraceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 集中读取和清理单个酒款在评审域内形成的数据。
 */
@Service
@RequiredArgsConstructor
public class EntryEvaluationDataServiceImpl implements EntryEvaluationDataService {

    private final RoundTableEntryMapper roundTableEntryMapper;
    private final JudgeScoreSessionMapper judgeScoreSessionMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final RoundResultMapper roundResultMapper;
    private final AwardResultMapper awardResultMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;

    @Override
    public EntryEvaluationData loadEntryEvaluationData(Long entryId) {
        List<RoundTableEntry> assignments = listAssignments(entryId);
        List<RoundResult> results = listResults(entryId);
        List<AwardResult> awards = listAwards(entryId);
        List<AdminEntryTraceVO> traces = new ArrayList<>();

        assignments.forEach(item -> {
            CompetitionRound round = competitionRoundMapper.selectById(item.getRoundId());
            RoundTable table = roundTableMapper.selectById(item.getRoundTableId());
            traces.add(AdminEntryTraceVO.builder()
                    .type("ROUND")
                    .roundId(item.getRoundId())
                    .roundName(round == null ? null : round.getRoundName())
                    .roundTableId(item.getRoundTableId())
                    .tableName(table == null ? null : table.getTableName())
                    .status(item.getStatus())
                    .build());
        });
        results.forEach(item -> {
            CompetitionRound round = competitionRoundMapper.selectById(item.getRoundId());
            RoundTable table = roundTableMapper.selectById(item.getRoundTableId());
            traces.add(AdminEntryTraceVO.builder()
                    .type("RESULT")
                    .roundId(item.getRoundId())
                    .roundName(round == null ? null : round.getRoundName())
                    .roundTableId(item.getRoundTableId())
                    .tableName(table == null ? null : table.getTableName())
                    .resultType(item.getResultType())
                    .rankNo(item.getRankNo())
                    .slotLabel(item.getSlotLabel())
                    .advanced(true)
                    .build());
        });
        awards.forEach(item -> traces.add(AdminEntryTraceVO.builder()
                .type("AWARD")
                .roundId(item.getSourceRoundId())
                .roundTableId(item.getSourceRoundTableId())
                .awardName(item.getAwardName())
                .awardType(item.getAwardType())
                .rankNo(item.getRankNo())
                .advanced(true)
                .build()));

        boolean hasScoreRecord = scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entryId)) > 0;
        return new EntryEvaluationData(
                List.copyOf(traces),
                !assignments.isEmpty(),
                hasScoreRecord,
                !results.isEmpty(),
                !awards.isEmpty()
        );
    }

    @Override
    public boolean hasEntryEvaluationData(Long entryId) {
        return roundTableEntryMapper.selectCount(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, entryId)) > 0
                || scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entryId)) > 0
                || roundResultMapper.selectCount(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getBeerEntryId, entryId)) > 0
                || awardResultMapper.selectCount(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, entryId)) > 0;
    }

    @Override
    public EntryEvaluationImpact summarizeDeleteImpact(Long entryId) {
        int assignmentCount = count(roundTableEntryMapper.selectCount(
                new LambdaQueryWrapper<RoundTableEntry>().eq(RoundTableEntry::getBeerEntryId, entryId)));
        int scoreSessionCount = count(judgeScoreSessionMapper.selectCount(
                new LambdaQueryWrapper<JudgeScoreSession>().eq(JudgeScoreSession::getBeerEntryId, entryId)));
        int scoreRecordCount = count(scoreRecordMapper.selectCount(
                new LambdaQueryWrapper<ScoreRecord>().eq(ScoreRecord::getBeerEntryId, entryId)));
        int roundResultCount = count(roundResultMapper.selectCount(
                new LambdaQueryWrapper<RoundResult>().eq(RoundResult::getBeerEntryId, entryId)));
        int awardResultCount = count(awardResultMapper.selectCount(
                new LambdaQueryWrapper<AwardResult>().eq(AwardResult::getBeerEntryId, entryId)));
        boolean publishedAward = awardResultMapper.selectCount(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, entryId)
                .eq(AwardResult::getStatus, AwardResultStatus.PUBLISHED.name())) > 0;
        return new EntryEvaluationImpact(assignmentCount, scoreSessionCount, scoreRecordCount,
                roundResultCount, awardResultCount, publishedAward);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteEntryEvaluationData(Long entryId) {
        roundTableEntryMapper.delete(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, entryId));
        judgeScoreSessionMapper.delete(new LambdaQueryWrapper<JudgeScoreSession>()
                .eq(JudgeScoreSession::getBeerEntryId, entryId));
        scoreRecordMapper.delete(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, entryId));
        roundResultMapper.delete(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getBeerEntryId, entryId));
        awardResultMapper.delete(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, entryId));
    }

    private List<RoundTableEntry> listAssignments(Long entryId) {
        return roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, entryId)
                .orderByAsc(RoundTableEntry::getRoundId)
                .orderByAsc(RoundTableEntry::getSortOrder)
                .orderByAsc(RoundTableEntry::getId));
    }

    private List<RoundResult> listResults(Long entryId) {
        return roundResultMapper.selectList(new LambdaQueryWrapper<RoundResult>()
                .eq(RoundResult::getBeerEntryId, entryId)
                .orderByAsc(RoundResult::getRoundId)
                .orderByAsc(RoundResult::getRankNo)
                .orderByAsc(RoundResult::getId));
    }

    private List<AwardResult> listAwards(Long entryId) {
        return awardResultMapper.selectList(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getBeerEntryId, entryId)
                .orderByDesc(AwardResult::getChampionFlag)
                .orderByAsc(AwardResult::getRankNo)
                .orderByAsc(AwardResult::getId));
    }

    private int count(Long value) {
        return Math.toIntExact(value == null ? 0L : value);
    }
}
