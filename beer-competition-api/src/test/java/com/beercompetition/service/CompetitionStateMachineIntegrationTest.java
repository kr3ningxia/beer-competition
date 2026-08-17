package com.beercompetition.service;

import com.beercompetition.judging.round.RoundLifecycleService;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.lifecycle.CompetitionLifecycleService;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.pojo.dto.CompetitionReopenRegistrationRequest;
import com.beercompetition.pojo.dto.CompetitionReturnToSampleCheckRequest;
import com.beercompetition.pojo.dto.CompetitionStyleLibraryUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldBatchUpdateRequest;
import com.beercompetition.pojo.dto.EntryFieldItemRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class CompetitionStateMachineIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionConfigurationService competitionConfigurationService;

    @Autowired
    private CompetitionLifecycleService competitionLifecycleService;

    @Autowired
    private RoundLifecycleService roundLifecycleService;

    @Autowired
    private CompetitionRoundMapper competitionRoundMapper;

    @Test
    void cannotPrepareJudgingBeforeRegistrationClosed() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);

        assertThatThrownBy(() -> competitionLifecycleService.prepareJudging(fixture.competition().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("报名截止");
    }

    @Test
    void prepareJudgingAllowsPaidEntriesThatHaveNotArrived() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        testData.createEntry(testRun, fixture.competition().getId(), fixture.portalB().brewery().getId(),
                fixture.category().getId(), testRun + "-未到场作品", EntryStatus.REGISTERED, true);
        jdbcTemplate.update("""
                        UPDATE competition_score_config
                        SET dimensions_json = ?
                        WHERE competition_id = ? AND judge_role_type = 'PROFESSIONAL'
                        """,
                "[{\"key\":\"aroma\",\"label\":\"香气\",\"maxScore\":12},{\"key\":\"appearance\",\"label\":\"外观\",\"maxScore\":3},{\"key\":\"flavor\",\"label\":\"味道\",\"maxScore\":20},{\"key\":\"mouthfeel\",\"label\":\"口感\",\"maxScore\":5},{\"key\":\"overall\",\"label\":\"整体印象\",\"maxScore\":10}]",
                fixture.competition().getId());
        jdbcTemplate.update("""
                        UPDATE competition_score_config
                        SET dimensions_json = ?
                        WHERE competition_id = ? AND judge_role_type = 'CROSS'
                        """,
                "[{\"key\":\"preference\",\"label\":\"喜好度\",\"maxScore\":25},{\"key\":\"impression\",\"label\":\"整体印象\",\"maxScore\":25}]",
                fixture.competition().getId());
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), fixture.competition().getId());
        asAdmin(1L);

        var detail = competitionLifecycleService.prepareJudging(fixture.competition().getId());

        assertThat(detail.getStatus()).isEqualTo(CompetitionStatus.JUDGING_PREP.name());
        assertThat(detail.getEntriesSummary().getStored()).isEqualTo(3);
        assertThat(detail.getEntriesSummary().getRegistered()).isEqualTo(4);
    }

    @Test
    void registrationOpenAllowsStyleLibraryAndEntryFieldUpdates() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());
        asAdmin(1L);

        CompetitionStyleLibraryUpdateRequest styleRequest = new CompetitionStyleLibraryUpdateRequest();
        styleRequest.setStyleLibraryVersion(jdbcTemplate.queryForObject("""
                SELECT l.code
                FROM style_library l
                WHERE l.status = 1
                  AND EXISTS (SELECT 1 FROM style_item i WHERE i.library_id = l.id AND i.status = 1)
                ORDER BY l.id
                LIMIT 1
                """, String.class));
        assertThatCode(() -> competitionConfigurationService.updateStyles(fixture.competition().getId(), styleRequest))
                .doesNotThrowAnyException();

        EntryFieldItemRequest field = new EntryFieldItemRequest();
        field.setFieldKey("servingTemp");
        field.setFieldLabel("饮用温度");
        field.setFieldType("select");
        field.setOptions(List.of("冷藏", "常温"));
        field.setRequired(false);
        field.setVisibleToJudges(true);
        field.setSortOrder(0);
        EntryFieldBatchUpdateRequest fieldRequest = new EntryFieldBatchUpdateRequest();
        fieldRequest.setItems(List.of(field));
        competitionConfigurationService.updateEntryFields(fixture.competition().getId(), fieldRequest);

        assertThat(jdbcTemplate.queryForObject("SELECT active_flag FROM entry_field_config WHERE competition_id = ? AND field_key = ?",
                Integer.class, fixture.competition().getId(), "servingTemp")).isEqualTo(1);
    }

    @Test
    void optionFieldRequiresAtLeastTwoChoices() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);
        EntryFieldItemRequest field = new EntryFieldItemRequest();
        field.setFieldKey("single");
        field.setFieldLabel("单选字段");
        field.setFieldType("select");
        field.setOptions(List.of("唯一选项"));
        field.setRequired(false);
        field.setVisibleToJudges(false);
        EntryFieldBatchUpdateRequest request = new EntryFieldBatchUpdateRequest();
        request.setItems(List.of(field));
        assertThatThrownBy(() -> competitionConfigurationService.updateEntryFields(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("至少需要 2 个候选项");
    }

    @Test
    void publishingFirstRoundMovesCompetitionToJudgingAndRejectsRepeatPublish() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.JUDGING_PREP.name(), fixture.competition().getId());
        BeerCompetitionTestData.ScoreRound scoreRound = testData.createPublishedScoreRound(
                fixture, List.of(fixture.entryA1(), fixture.entryA2()), 1);
        jdbcTemplate.update("UPDATE competition_round SET status = ? WHERE id = ?",
                RoundStatus.DRAFT.name(), scoreRound.round().getId());
        jdbcTemplate.update("UPDATE round_table SET status = ? WHERE id = ?",
                RoundStatus.DRAFT.name(), scoreRound.table().getId());

        asAdmin(1L);
        roundLifecycleService.publishRound(fixture.competition().getId(), scoreRound.round().getId());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition WHERE id = ?",
                String.class, fixture.competition().getId())).isEqualTo(CompetitionStatus.JUDGING.name());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition_round WHERE id = ?",
                String.class, scoreRound.round().getId())).isEqualTo(RoundStatus.PUBLISHED.name());

        assertThatThrownBy(() -> roundLifecycleService.publishRound(fixture.competition().getId(), scoreRound.round().getId()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("草稿");
    }

    @Test
    void reopenRegistrationRequiresReasonAndCanExtendDeadline() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);
        jdbcTemplate.update("UPDATE competition SET status = ?, registration_deadline = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), java.time.LocalDateTime.now().minusHours(1), fixture.competition().getId());
        CompetitionReopenRegistrationRequest request = new CompetitionReopenRegistrationRequest();
        request.setReason("误点截止报名");
        request.setRegistrationDeadline(java.time.LocalDateTime.now().plusDays(3));

        competitionLifecycleService.reopenRegistration(fixture.competition().getId(), request);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition WHERE id = ?",
                String.class, fixture.competition().getId())).isEqualTo(CompetitionStatus.REGISTRATION_OPEN.name());
        assertThat(jdbcTemplate.queryForObject("SELECT registration_deadline FROM competition WHERE id = ?",
                java.time.LocalDateTime.class, fixture.competition().getId())).isAfter(java.time.LocalDateTime.now());
        assertThat(countCompetitionLog("COMPETITION_REOPEN_REGISTRATION", fixture.competition().getId())).isEqualTo(1);
    }

    @Test
    void reopenRegistrationRejectsMissingNewDeadlineWhenOriginalExpired() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);
        jdbcTemplate.update("UPDATE competition SET status = ?, registration_deadline = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_CLOSED.name(), java.time.LocalDateTime.now().minusHours(1), fixture.competition().getId());
        CompetitionReopenRegistrationRequest request = new CompetitionReopenRegistrationRequest();
        request.setReason("临时延长");

        assertThatThrownBy(() -> competitionLifecycleService.reopenRegistration(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("新的报名截止时间");
    }

    @Test
    void returnToSampleCheckKeepsDraftRoundAndRejectsPublishedRound() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.JUDGING_PREP.name(), fixture.competition().getId());
        CompetitionRound draftRound = CompetitionRound.builder()
                .competitionId(fixture.competition().getId())
                .roundNo(1)
                .roundName("第一轮")
                .roundType("SCORE")
                .status(RoundStatus.DRAFT.name())
                .sortOrder(1)
                .build();
        competitionRoundMapper.insert(draftRound);
        CompetitionReturnToSampleCheckRequest request = new CompetitionReturnToSampleCheckRequest();
        request.setReason("样品入库状态待复核");

        competitionLifecycleService.returnToSampleCheck(fixture.competition().getId(), request);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM competition WHERE id = ?",
                String.class, fixture.competition().getId())).isEqualTo(CompetitionStatus.REGISTRATION_CLOSED.name());
        assertThat(competitionRoundMapper.selectById(draftRound.getId()).getStatus()).isEqualTo(RoundStatus.DRAFT.name());
        assertThat(countCompetitionLog("COMPETITION_RETURN_TO_SAMPLE_CHECK", fixture.competition().getId())).isEqualTo(1);
    }

    @Test
    void returnToSampleCheckRejectsWhenFirstRoundAlreadyPublished() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        asAdmin(1L);
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.JUDGING_PREP.name(), fixture.competition().getId());
        CompetitionRound publishedRound = CompetitionRound.builder()
                .competitionId(fixture.competition().getId())
                .roundNo(1)
                .roundName("第一轮")
                .roundType("SCORE")
                .status(RoundStatus.PUBLISHED.name())
                .sortOrder(1)
                .build();
        competitionRoundMapper.insert(publishedRound);
        CompetitionReturnToSampleCheckRequest request = new CompetitionReturnToSampleCheckRequest();
        request.setReason("误点恢复");

        assertThatThrownBy(() -> competitionLifecycleService.returnToSampleCheck(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("首轮已发布");
    }

    private Long countCompetitionLog(String action, Long competitionId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM admin_operation_log
                WHERE action = ?
                  AND target_type = 'COMPETITION'
                  AND target_public_id = ?
                """, Long.class, action, String.valueOf(competitionId));
    }
}
