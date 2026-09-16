package com.beercompetition.service.impl;

import com.beercompetition.mapper.CompetitionJudgePublicProfileMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.pojo.po.CompetitionJudgePublicProfile;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionJudgePublicProfileServiceImplTest {

    @Mock
    private CompetitionJudgePublicProfileMapper publicProfileMapper;
    @Mock
    private JudgeAssignmentMapper judgeAssignmentMapper;
    @Mock
    private JudgeAccountMapper judgeAccountMapper;
    @Mock
    private FileAssetMapper fileAssetMapper;

    @Test
    void snapshotsConsentedActiveJudgesOnceAndMergesRoles() {
        JudgeAccount captain = JudgeAccount.builder()
                .id(11L)
                .name("陈明轩")
                .qualification("BJCP 认证评审")
                .status(1)
                .publicProfileConsent(true)
                .build();
        JudgeAccount withdrawn = JudgeAccount.builder()
                .id(12L)
                .name("不公开评委")
                .qualification("内部资料")
                .status(1)
                .publicProfileConsent(false)
                .build();
        JudgeAccount disabled = JudgeAccount.builder()
                .id(13L)
                .name("停用评委")
                .qualification("内部资料")
                .status(0)
                .publicProfileConsent(true)
                .build();
        when(publicProfileMapper.selectCount(any())).thenReturn(0L);
        when(judgeAssignmentMapper.selectList(any())).thenReturn(List.of(
                JudgeAssignment.builder().id(8L).competitionId(100L).judgeAccountId(11L).role("CAPTAIN").status("ACTIVE").build(),
                JudgeAssignment.builder().id(9L).competitionId(100L).judgeAccountId(11L).role("PROFESSIONAL").status("ACTIVE").build(),
                JudgeAssignment.builder().id(10L).competitionId(100L).judgeAccountId(12L).role("CROSS").status("ACTIVE").build(),
                JudgeAssignment.builder().id(11L).competitionId(100L).judgeAccountId(13L).role("CAPTAIN").status("ACTIVE").build()
        ));
        when(judgeAccountMapper.selectBatchIds(anyCollection())).thenReturn(List.of(captain, withdrawn, disabled));

        CompetitionJudgePublicProfileServiceImpl service = new CompetitionJudgePublicProfileServiceImpl(
                publicProfileMapper,
                judgeAssignmentMapper,
                judgeAccountMapper,
                fileAssetMapper,
                new ObjectMapper());

        service.snapshotAtPublication(100L);

        ArgumentCaptor<CompetitionJudgePublicProfile> captor = ArgumentCaptor.forClass(CompetitionJudgePublicProfile.class);
        verify(publicProfileMapper).insert(captor.capture());
        CompetitionJudgePublicProfile snapshot = captor.getValue();
        assertThat(snapshot.getJudgeAccountId()).isEqualTo(11L);
        assertThat(snapshot.getName()).isEqualTo("陈明轩");
        assertThat(snapshot.getRolesJson()).contains("桌长", "专业评审");
    }

    @Test
    void onlyPublishedSnapshotCanAuthorizePublicAvatar() {
        CompetitionJudgePublicProfile snapshot = CompetitionJudgePublicProfile.builder()
                .id(3L)
                .competitionId(100L)
                .avatarAssetId(77L)
                .build();
        when(publicProfileMapper.selectPublishedByAvatarAssetId(77L)).thenReturn(snapshot);
        CompetitionJudgePublicProfileServiceImpl service = new CompetitionJudgePublicProfileServiceImpl(
                publicProfileMapper,
                judgeAssignmentMapper,
                judgeAccountMapper,
                fileAssetMapper,
                new ObjectMapper());

        assertThat(service.isPublishedAvatarReferenced(77L)).isTrue();
        assertThat(service.isPublishedAvatarReferenced(78L)).isFalse();
    }
}
