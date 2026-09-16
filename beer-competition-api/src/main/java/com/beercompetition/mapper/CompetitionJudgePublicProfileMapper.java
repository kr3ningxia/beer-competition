package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.CompetitionJudgePublicProfile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CompetitionJudgePublicProfileMapper extends BaseMapper<CompetitionJudgePublicProfile> {

    @Select("""
            SELECT p.*
            FROM competition_judge_public_profile p
            JOIN competition c ON c.id = p.competition_id
            WHERE p.avatar_asset_id = #{fileAssetId}
              AND c.status = 'PUBLISHED'
            ORDER BY p.id
            LIMIT 1
            """)
    CompetitionJudgePublicProfile selectPublishedByAvatarAssetId(@Param("fileAssetId") Long fileAssetId);
}
