package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.ScoreProgressStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {

    @Select("""
            SELECT COUNT(DISTINCT beer_entry_id) AS finalized_count,
                   COUNT(DISTINCT CASE WHEN is_advanced = 1 THEN beer_entry_id END) AS advanced_count
            FROM score_record
            WHERE competition_id = #{competitionId}
              AND is_final = 1
            """)
    ScoreProgressStatsVO selectFinalProgressStats(@Param("competitionId") Long competitionId);

    @Select("""
            SELECT COUNT(DISTINCT CASE WHEN sr.is_final = 1 THEN sr.beer_entry_id END) AS finalized_count,
                   COUNT(DISTINCT CASE WHEN sr.is_final = 1 AND sr.is_advanced = 1
                                       THEN sr.beer_entry_id END) AS advanced_count,
                   SUM(CASE WHEN sr.is_final = 1
                                 AND CHAR_LENGTH(TRIM(COALESCE(sr.comments, ''))) < #{minCommentLength}
                            THEN 1 ELSE 0 END) AS comment_warning_count,
                   AVG(CASE WHEN sr.round_id = #{roundId} AND sr.is_final = 0
                            THEN sr.duration_seconds END) AS average_review_seconds,
                   AVG(CASE WHEN sr.round_id = #{roundId} AND sr.is_final = 0
                            THEN sr.comment_char_count END) AS average_comment_chars,
                   AVG(CASE WHEN sr.round_id = #{roundId}
                                 AND #{publishedTime} IS NOT NULL
                                 AND sr.create_time >= #{publishedTime}
                            THEN TIMESTAMPDIFF(SECOND, #{publishedTime}, sr.create_time) / 60.0 END)
                       AS average_review_minutes
            FROM score_record sr
            JOIN (
                SELECT DISTINCT beer_entry_id
                FROM round_table_entry
                WHERE round_id = #{roundId}
            ) current_entry ON current_entry.beer_entry_id = sr.beer_entry_id
            WHERE sr.competition_id = #{competitionId}
            """)
    ScoreProgressStatsVO selectRoundProgressStats(
            @Param("competitionId") Long competitionId,
            @Param("roundId") Long roundId,
            @Param("minCommentLength") Integer minCommentLength,
            @Param("publishedTime") LocalDateTime publishedTime);
}
