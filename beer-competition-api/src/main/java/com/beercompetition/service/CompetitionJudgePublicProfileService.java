package com.beercompetition.service;

import com.beercompetition.pojo.vo.PortalPublicJudgeVO;

import java.util.List;

public interface CompetitionJudgePublicProfileService {

    void snapshotAtPublication(Long competitionId);

    List<PortalPublicJudgeVO> listPublicProfiles(Long competitionId);

    boolean isPublishedAvatarReferenced(Long fileAssetId);
}
