package com.beercompetition.service;

import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
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
    private CompetitionService competitionService;

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
        List<Long> displayedIds = competitionService.listPortalCompetitions().stream()
                .map(PortalCompetitionVO::getId)
                .filter(competitionIds::contains)
                .toList();

        assertThat(displayedIds).containsExactly(
                openSooner.getId(),
                openLater.getId(),
                registrationClosed.getId());
    }

    private void updateCompetitionSchedule(Competition competition, LocalDateTime registrationDeadline,
                                           LocalDate competitionDate) {
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ?, competition_date = ? WHERE id = ?",
                registrationDeadline, competitionDate, competition.getId());
    }
}
