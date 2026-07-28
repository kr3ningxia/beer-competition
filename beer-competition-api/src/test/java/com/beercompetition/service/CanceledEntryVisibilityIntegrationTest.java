package com.beercompetition.service;

import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CanceledEntryVisibilityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private EntryService entryService;

    @Test
    void analyticsAndEntryPagesExcludeCanceledEntries() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var canceledEntry = testData.createEntry(testRun, fixture.competition().getId(),
                fixture.portalA().brewery().getId(), fixture.category().getId(),
                testRun + "-已取消测试酒款", EntryStatus.CANCELED, false);
        asAdmin(1L);

        var analytics = competitionService.getCompetitionAnalytics(fixture.competition().getId());
        assertThat(analytics.getSummary().getTotalEntries()).isEqualTo(3);
        assertThat(analytics.getSummary().getRegisteredEntries()).isEqualTo(3);
        assertThat(analytics.getSummary().getPendingPaymentEntries()).isZero();
        assertThat(analytics.getSummary().getTestRecordCount()).isZero();
        assertThat(analytics.getRegistration().getCategories())
                .singleElement()
                .satisfies(bucket -> assertThat(bucket.getCount()).isEqualTo(3));

        CompetitionDetailVO detail = competitionService.getCompetitionDetail(fixture.competition().getId());
        assertThat(detail.getEntriesSummary().getTotal()).isEqualTo(3);
        assertThat(detail.getEntriesSummary().getRegistered()).isEqualTo(3);
        assertThat(detail.getEntriesSummary().getCanceled()).isZero();
        assertThat(detail.getEntries())
                .extracting(entry -> entry.getId())
                .doesNotContain(canceledEntry.getId());

        var adminEntries = entryService.listAdminEntries(fixture.competition().getId(), null,
                null, null, null, null, null, null, 1, 30);
        assertThat(adminEntries.getTotal()).isEqualTo(3);
        assertThat(adminEntries.getRecords())
                .extracting(AdminEntryVO::getId)
                .doesNotContain(canceledEntry.getId());

        CompetitionVO listItem = competitionService.listCompetitions(false).stream()
                .filter(item -> item.getId().equals(fixture.competition().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(listItem.getEntriesSummary().getTotal()).isEqualTo(3);
        assertThat(listItem.getEntriesSummary().getCanceled()).isZero();
    }
}
