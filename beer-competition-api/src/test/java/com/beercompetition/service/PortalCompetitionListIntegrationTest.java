package com.beercompetition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PortalCompetitionListIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionQueryService competitionQueryService;

    @Autowired
    private OrganizerMapper organizerMapper;

    @Test
    void portalListsOpenCompetitionsFirstByEarliestRegistrationDeadline() {
        Competition openLater = testData.createCompetition(testRun + "-open-later", CompetitionStatus.REGISTRATION_OPEN);
        Competition openSooner = testData.createCompetition(testRun + "-open-sooner", CompetitionStatus.REGISTRATION_OPEN);
        Competition registrationClosed = testData.createCompetition(testRun + "-closed", CompetitionStatus.REGISTRATION_CLOSED);
        LocalDateTime now = LocalDateTime.now();

        updateCompetitionSchedule(openLater, now.plusDays(7), LocalDate.now().plusDays(80));
        updateCompetitionSchedule(openSooner, now.plusDays(2), LocalDate.now().plusDays(10));
        updateCompetitionSchedule(registrationClosed, now.plusDays(1), LocalDate.now().plusDays(100));

        Set<Long> competitionIds = Set.of(openLater.getId(), openSooner.getId(), registrationClosed.getId());
        List<Long> displayedIds = competitionQueryService.listPortalCompetitions().stream()
                .map(PortalCompetitionVO::getId)
                .filter(competitionIds::contains)
                .toList();

        assertThat(displayedIds).containsExactly(
                openSooner.getId(),
                openLater.getId(),
                registrationClosed.getId());
    }

    @Test
    void portalUsesTenantOrganizationNameAsThirdPartyInitiator() {
        Organizer tenant = organizerMapper.selectOne(new LambdaQueryWrapper<Organizer>()
                .eq(Organizer::getOrganizerType, OrganizerType.TENANT.name())
                .last("LIMIT 1"));
        assertThat(tenant).isNotNull();
        assertThat(tenant.getName()).isNotEqualTo(tenant.getContactName());

        Competition competition = testData.createCompetition(testRun + "-tenant", CompetitionStatus.REGISTRATION_CLOSED);
        jdbcTemplate.update("UPDATE competition SET organizer_id = ? WHERE id = ?", tenant.getId(), competition.getId());

        PortalCompetitionVO result = competitionQueryService.getPortalCompetitionDetail(competition.getId());

        assertThat(result.getOrganizerType()).isEqualTo(OrganizerType.TENANT.name());
        assertThat(result.getOrganizerName()).isEqualTo(tenant.getName());
    }

    private void updateCompetitionSchedule(Competition competition, LocalDateTime registrationDeadline,
                                           LocalDate competitionDate) {
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ?, competition_date = ? WHERE id = ?",
                registrationDeadline, competitionDate, competition.getId());
    }
}
