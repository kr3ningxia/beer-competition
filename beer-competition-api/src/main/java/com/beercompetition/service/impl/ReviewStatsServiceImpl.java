package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.JudgeScoreSessionMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.JudgeScoreSession;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.ReviewStatsVO;
import com.beercompetition.service.ReviewStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewStatsServiceImpl implements ReviewStatsService {

    private static final String CACHE_PREFIX = "review:stats:";
    private static final int CACHE_TTL_SECONDS = 15;

    private final CompetitionRoundMapper competitionRoundMapper;
    private final CompetitionScoreConfigMapper competitionScoreConfigMapper;
    private final JudgeScoreSessionMapper judgeScoreSessionMapper;
    private final RoundTableMapper roundTableMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ReviewStatsVO getMyRoundTableStats(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        String cacheKey = buildJudgeKey(roundTableId, judgeId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ReviewStatsVO stats) {
            return stats;
        }
        if (cached instanceof Map<?, ?> statsMap) {
            return fromCachedMap(statsMap);
        }
        ReviewStatsVO stats = computeMyRoundTableStats(roundTableId);
        redisTemplate.opsForValue().set(cacheKey, stats, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
        return stats;
    }

    @Override
    public void evictReviewStats(Long roundId, Long roundTableId, Long judgeId, String judgeRoleType) {
        redisTemplate.delete(buildRoundKey(roundId));
        redisTemplate.delete(buildRoundRoleKey(roundId, judgeRoleType));
        redisTemplate.delete(buildTableKey(roundTableId));
        redisTemplate.delete(buildJudgeKey(roundTableId, judgeId));
    }

    private ReviewStatsVO computeMyRoundTableStats(Long roundTableId) {
        Long judgeId = BaseContext.getCurrentId();
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (table == null) {
            return emptyStats(0);
        }
        RoundTableMember member = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId)
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .eq(RoundTableMember::getSystemTaskRequired, 1)
                .last("LIMIT 1"));
        if (member == null) {
            return emptyStats(0);
        }
        CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
        String statsRole = JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                ? JudgeRoleType.PROFESSIONAL.name()
                : member.getRole();
        int commentMinLength = resolveCommentMinLength(round, statsRole);

        List<JudgeScoreSession> sessions = judgeScoreSessionMapper.selectList(new LambdaQueryWrapper<JudgeScoreSession>()
                .eq(JudgeScoreSession::getRoundTableId, roundTableId)
                .eq(JudgeScoreSession::getJudgeAccountId, judgeId)
                .eq(JudgeScoreSession::getJudgeRoleType, statsRole)
                .orderByAsc(JudgeScoreSession::getId));
        int submittedCount = (int) sessions.stream().filter(session -> session.getFirstSubmittedAt() != null).count();
        int averageDurationSeconds = averageInt(sessions.stream()
                .map(JudgeScoreSession::getDurationSeconds)
                .filter(Objects::nonNull)
                .toList());
        int averageCommentChars = averageInt(sessions.stream()
                .map(JudgeScoreSession::getCommentCharCount)
                .filter(Objects::nonNull)
                .toList());

        List<ScoreRecord> tableScores = scoreRecordMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getRoundId, table.getRoundId())
                .eq(ScoreRecord::getJudgeRoleType, statsRole)
                .eq(ScoreRecord::getFinalFlag, 0));
        int siteAverageDurationSeconds = averageInt(tableScores.stream()
                .map(ScoreRecord::getDurationSeconds)
                .filter(Objects::nonNull)
                .toList());
        int siteAverageCommentChars = averageInt(tableScores.stream()
                .map(ScoreRecord::getCommentCharCount)
                .filter(Objects::nonNull)
                .toList());
        return ReviewStatsVO.builder()
                .submittedCount(submittedCount)
                .averageDurationSeconds(averageDurationSeconds)
                .averageCommentChars(averageCommentChars)
                .siteAverageDurationSeconds(siteAverageDurationSeconds)
                .siteAverageCommentChars(siteAverageCommentChars)
                .commentMinLength(commentMinLength)
                .build();
    }

    private ReviewStatsVO emptyStats(int commentMinLength) {
        return ReviewStatsVO.builder()
                .submittedCount(0)
                .averageDurationSeconds(0)
                .averageCommentChars(0)
                .siteAverageDurationSeconds(0)
                .siteAverageCommentChars(0)
                .commentMinLength(commentMinLength)
                .build();
    }

    private ReviewStatsVO fromCachedMap(Map<?, ?> statsMap) {
        return ReviewStatsVO.builder()
                .submittedCount(readInteger(statsMap, "submittedCount"))
                .averageDurationSeconds(readInteger(statsMap, "averageDurationSeconds"))
                .averageCommentChars(readInteger(statsMap, "averageCommentChars"))
                .siteAverageDurationSeconds(readInteger(statsMap, "siteAverageDurationSeconds"))
                .siteAverageCommentChars(readInteger(statsMap, "siteAverageCommentChars"))
                .commentMinLength(readInteger(statsMap, "commentMinLength"))
                .build();
    }

    private int readInteger(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    private int resolveCommentMinLength(CompetitionRound round, String statsRole) {
        if (round == null) {
            return 0;
        }
        CompetitionScoreConfig config = competitionScoreConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionScoreConfig>()
                .eq(CompetitionScoreConfig::getCompetitionId, round.getCompetitionId())
                .eq(CompetitionScoreConfig::getJudgeRoleType, statsRole)
                .last("LIMIT 1"));
        return config == null || config.getMinCommentLength() == null ? 0 : config.getMinCommentLength();
    }

    private int averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String buildRoundKey(Long roundId) {
        return CACHE_PREFIX + "round:" + roundId;
    }

    private String buildRoundRoleKey(Long roundId, String role) {
        return CACHE_PREFIX + "round:" + roundId + ":role:" + role;
    }

    private String buildTableKey(Long roundTableId) {
        return CACHE_PREFIX + "table:" + roundTableId;
    }

    private String buildJudgeKey(Long roundTableId, Long judgeId) {
        return CACHE_PREFIX + "judge:" + roundTableId + ":" + judgeId;
    }
}
