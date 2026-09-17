package com.beercompetition.service;

import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
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

    private Long tenantEnterpriseId;
    private Long tenantOrganizerId;

    @AfterEach
    void cleanTenantOrganizer() {
        cleanupByPrefix(testRun);
        if (tenantOrganizerId != null) {
            jdbcTemplate.update("DELETE FROM organizer WHERE id = ?", tenantOrganizerId);
        }
        if (tenantEnterpriseId != null) {
            jdbcTemplate.update("DELETE FROM enterprise_account WHERE id = ?", tenantEnterpriseId);
        }
    }

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
        Organizer tenant = createTenantOrganizer();
        assertThat(tenant.getName()).isNotEqualTo(tenant.getContactName());

        Competition competition = testData.createCompetition(testRun + "-tenant", CompetitionStatus.REGISTRATION_CLOSED);
        jdbcTemplate.update("UPDATE competition SET organizer_id = ? WHERE id = ?", tenant.getId(), competition.getId());

        PortalCompetitionVO result = competitionQueryService.getPortalCompetitionDetail(competition.getId());

        assertThat(result.getOrganizerType()).isEqualTo(OrganizerType.TENANT.name());
        assertThat(result.getOrganizerName()).isEqualTo(tenant.getName());
    }

    private Organizer createTenantOrganizer() {
        String accountCode = testRun + "-EA";
        jdbcTemplate.update("""
                INSERT INTO enterprise_account (account_code, name, status)
                VALUES (?, ?, 'ACTIVE')
                """, accountCode, testRun + "-企业账户");
        tenantEnterpriseId = jdbcTemplate.queryForObject(
                "SELECT id FROM enterprise_account WHERE account_code = ?", Long.class, accountCode);
        jdbcTemplate.update("""
                INSERT INTO organizer (enterprise_account_id, name, organizer_type, status, contact_name)
                VALUES (?, ?, 'TENANT', 'ACTIVE', ?)
                """, tenantEnterpriseId, testRun + "-第三方主办方", testRun + "-联系人");
        tenantOrganizerId = jdbcTemplate.queryForObject(
                "SELECT id FROM organizer WHERE enterprise_account_id = ?", Long.class, tenantEnterpriseId);
        return organizerMapper.selectById(tenantOrganizerId);
    }

    private void updateCompetitionSchedule(Competition competition, LocalDateTime registrationDeadline,
                                           LocalDate competitionDate) {
        jdbcTemplate.update("UPDATE competition SET registration_deadline = ?, competition_date = ? WHERE id = ?",
                registrationDeadline, competitionDate, competition.getId());
    }
}
