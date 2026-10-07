package com.beercompetition.service.impl;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.judging.access.JudgeAccessService;
import com.beercompetition.mapper.*;
import com.beercompetition.pojo.dto.JudgeRecruitmentRequest;
import com.beercompetition.pojo.dto.JudgeRecruitmentApplicationRequest;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeRecruitment;
import com.beercompetition.pojo.po.JudgeRecruitmentApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeRecruitmentServiceImplTest {
    @Mock private JudgeRecruitmentMapper recruitmentMapper;
    @Mock private JudgeRecruitmentApplicationMapper applicationMapper;
    @Mock private AdminOperationLogMapper adminOperationLogMapper;
    @Mock private CompetitionMapper competitionMapper;
    @Mock private JudgeAccountMapper judgeAccountMapper;
    @Mock private JudgeAssignmentMapper assignmentMapper;
    @Mock private BeerEntryMapper beerEntryMapper;
    @Mock private CompetitionJudgeEvaluationMapper competitionJudgeEvaluationMapper;
    @Mock private JudgeAccessService judgeAccessService;
    @Mock private PiiService piiService;
    @Mock private CompetitionAccessService competitionAccessService;
    @InjectMocks private JudgeRecruitmentServiceImpl service;

    private JudgeRecruitment recruitment;
    private JudgeRecruitmentRequest request;

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @BeforeEach
    void prepare() {
        recruitment = new JudgeRecruitment();
        recruitment.setId(1L);
        recruitment.setCompetitionId(10L);
        recruitment.setPublicId("JR_TEST");
        recruitment.setStatus("OPEN");
        request = new JudgeRecruitmentRequest();
        request.setCompetitionId(10L);
        request.setRecruitmentStart(LocalDateTime.of(2026, 10, 1, 10, 0));
        request.setRecruitmentDeadline(LocalDateTime.of(2026, 10, 8, 23, 59));
        request.setVenue(" updated venue ");
        request.setDescription("updated description");
        request.setRequirements("updated requirements");
        request.setExpectedCount(20);
        request.setJudgingStartTime(LocalTime.of(9, 0));
    }

    private void mockExisting() {
        when(recruitmentMapper.selectById(1L)).thenReturn(recruitment);
        when(competitionMapper.selectById(10L)).thenReturn(Competition.builder()
                .id(10L).name("Test competition").competitionDate(LocalDate.of(2026, 10, 9)).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "OPEN", "CLOSED"})
    void updatesRecruitmentWithoutChangingStatusOrApplications(String status) {
        recruitment.setStatus(status);
        mockExisting();
        when(applicationMapper.selectList(any())).thenReturn(List.of());

        var result = service.save(request, 1L);

        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getVenue()).isEqualTo("updated venue");
        assertThat(result.getDescription()).isEqualTo("updated description");
        assertThat(result.getRequirements()).isEqualTo("updated requirements");
        assertThat(result.getExpectedCount()).isEqualTo(20);
        assertThat(result.getJudgingStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.getCompetitionDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        verify(recruitmentMapper).updateById(recruitment);
        verify(applicationMapper, never()).updateById(any(com.beercompetition.pojo.po.JudgeRecruitmentApplication.class));
        verify(applicationMapper, never()).insert(any(com.beercompetition.pojo.po.JudgeRecruitmentApplication.class));
        verify(judgeAccessService, atLeastOnce()).requireCompetitionAccess(10L);
    }

    @Test
    void archivedRecruitmentCannotBeEdited() {
        recruitment.setStatus("ARCHIVED");
        mockExisting();
        assertThatThrownBy(() -> service.save(request, 1L)).isInstanceOf(BaseException.class);
        verify(recruitmentMapper, never()).updateById(any(JudgeRecruitment.class));
    }

    @Test
    void rejectsReversedRegistrationWindow() {
        request.setRecruitmentDeadline(request.getRecruitmentStart().minusMinutes(1));
        assertThatThrownBy(() -> service.save(request, 1L))
                .isInstanceOf(BaseException.class).hasMessage("截止时间不能早于开始时间");
        verifyNoInteractions(recruitmentMapper);
    }

    @Test
    void competitionCannotBeReassigned() {
        recruitment.setCompetitionId(11L);
        mockExisting();
        assertThatThrownBy(() -> service.save(request, 1L))
                .isInstanceOf(BaseException.class).hasMessage("招募对应的比赛不能修改");
        verify(recruitmentMapper, never()).updateById(any(JudgeRecruitment.class));
    }

    @Test
    void deniesUpdatesOutsideCompetitionScope() {
        doThrow(new BaseException("无权访问该比赛")).when(judgeAccessService).requireCompetitionAccess(10L);
        assertThatThrownBy(() -> service.save(request, 1L)).isInstanceOf(BaseException.class);
        verifyNoInteractions(recruitmentMapper, applicationMapper);
    }

    @Test
    void createsDraftWithCustomJudgingTime() {
        when(competitionMapper.selectById(10L)).thenReturn(Competition.builder().id(10L).build());
        when(recruitmentMapper.selectCount(any())).thenReturn(0L);
        when(applicationMapper.selectList(any())).thenReturn(List.of());

        var result = service.save(request, null);

        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getJudgingStartTime()).isEqualTo(LocalTime.of(9, 0));
        verify(recruitmentMapper).insert(any(JudgeRecruitment.class));
    }

    @Test
    void legacyRecruitmentHasNoInventedJudgingTime() {
        mockExisting();
        when(applicationMapper.selectList(any())).thenReturn(List.of());
        var result = service.adminGet(1L);
        assertThat(result.getJudgingStartTime()).isNull();
        assertThat(result.getCompetitionDate()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    @Test
    void expiredRecruitmentExposesClosedApplicationWindow() {
        recruitment.setRecruitmentDeadline(LocalDateTime.now().minusMinutes(1));
        mockExisting();
        when(applicationMapper.selectList(any())).thenReturn(List.of());

        var result = service.adminGet(1L);

        assertThat(result.getApplicationWindowOpen()).isFalse();
    }

    @Test
    void cannotWithdrawAfterRecruitmentDeadline() {
        recruitment.setRecruitmentDeadline(LocalDateTime.now().minusMinutes(1));
        JudgeRecruitmentApplication application = application("APPLIED");
        when(applicationMapper.selectById(4L)).thenReturn(application);
        when(recruitmentMapper.selectById(1L)).thenReturn(recruitment);
        BaseContext.setCurrentUser(SessionUser.builder().userId(7L).role("JUDGE").build());

        assertThatThrownBy(() -> service.withdraw(4L))
                .isInstanceOf(BaseException.class)
                .hasMessage("招募已关闭");
        verify(applicationMapper, never()).updateById(any(JudgeRecruitmentApplication.class));
    }

    @Test
    void cannotWithdrawAfterRecruitmentIsClosedBeforeDeadline() {
        recruitment.setStatus("CLOSED");
        recruitment.setRecruitmentDeadline(LocalDateTime.now().plusMinutes(1));
        JudgeRecruitmentApplication application = application("ACCEPTED");
        when(applicationMapper.selectById(4L)).thenReturn(application);
        when(recruitmentMapper.selectById(1L)).thenReturn(recruitment);
        BaseContext.setCurrentUser(SessionUser.builder().userId(7L).role("JUDGE").build());

        assertThatThrownBy(() -> service.withdraw(4L))
                .isInstanceOf(BaseException.class)
                .hasMessage("招募已关闭");
        verify(applicationMapper, never()).updateById(any(JudgeRecruitmentApplication.class));
    }

    @Test
    void withdrawnApplicationCanBeSubmittedAgain() {
        JudgeAccount judge = JudgeAccount.builder()
                .id(7L)
                .name("Judge")
                .qualification("BJCP")
                .status(1)
                .build();
        JudgeRecruitmentApplication application = application("WITHDRAWN");
        application.setReviewRemark("旧审核意见");
        application.setProcessedBy(99L);
        application.setProcessedTime(LocalDateTime.now().minusDays(1));
        JudgeRecruitmentApplicationRequest applyRequest = new JudgeRecruitmentApplicationRequest();
        applyRequest.setAvailabilityConfirmed(true);
        applyRequest.setNote("重新报名");
        when(judgeAccountMapper.selectById(7L)).thenReturn(judge);
        when(recruitmentMapper.selectById(1L)).thenReturn(recruitment);
        when(applicationMapper.selectOne(any())).thenReturn(application);
        when(competitionMapper.selectById(10L)).thenReturn(Competition.builder().id(10L).name("Test competition").build());
        BaseContext.setCurrentUser(SessionUser.builder().userId(7L).role("JUDGE").build());

        service.apply(1L, applyRequest);

        assertThat(application.getStatus()).isEqualTo("APPLIED");
        assertThat(application.getNote()).isEqualTo("重新报名");
        assertThat(application.getWithdrawnTime()).isNull();
        assertThat(application.getReviewRemark()).isNull();
        assertThat(application.getProcessedBy()).isNull();
        assertThat(application.getProcessedTime()).isNull();
        verify(applicationMapper).updateById(application);
    }

    private JudgeRecruitmentApplication application(String status) {
        JudgeRecruitmentApplication application = new JudgeRecruitmentApplication();
        application.setId(4L);
        application.setRecruitmentId(1L);
        application.setJudgeAccountId(7L);
        application.setStatus(status);
        return application;
    }
}
