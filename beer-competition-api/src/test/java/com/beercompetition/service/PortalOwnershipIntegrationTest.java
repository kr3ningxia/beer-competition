package com.beercompetition.service;

import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortalOwnershipIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private EntryService entryService;

    @Test
    void portalCanOnlyReadOwnEntries() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);

        asPortal(fixture.portalA().account().getId());
        EntryDetailVO own = entryService.getPortalEntry(fixture.entryA1().getId());
        assertThat(own.getId()).isEqualTo(fixture.entryA1().getId());

        assertThatThrownBy(() -> entryService.getPortalEntry(fixture.entryB1().getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("无权查看");
    }

    @Test
    void portalCannotMutateAnotherBreweryEntry() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);

        asPortal(fixture.portalA().account().getId());
        PortalEntryRefundRequest request = new PortalEntryRefundRequest();
        request.setReason("测试退款");

        assertThatThrownBy(() -> entryService.requestPortalEntryRefund(fixture.entryB1().getId(), request))
                .isInstanceOf(ForbiddenException.class);
    }
}
