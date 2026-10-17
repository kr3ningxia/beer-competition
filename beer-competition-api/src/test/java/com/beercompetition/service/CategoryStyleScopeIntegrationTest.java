package com.beercompetition.service;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.registration.entry.PortalEntryService;
import com.beercompetition.testsupport.BeerCompetitionTestData;
import com.beercompetition.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryStyleScopeIntegrationTest extends IntegrationTestBase {

    @Autowired
    private BeerCompetitionTestData testData;

    @Autowired
    private PortalEntryService portalEntryService;

    @Test
    void entrySubmissionEnforcesCategoryStyleScope() {
        BeerCompetitionTestData.Fixture fixture = testData.createFixture(testRun);
        var allowedStyle = testData.createStyle(fixture.competition().getId(), testRun + "-受限风格");
        jdbcTemplate.update("""
                INSERT INTO competition_category_style
                    (competition_id, category_id, style_name, sort_order)
                VALUES (?, ?, ?, 1)
                """, fixture.competition().getId(), fixture.category().getId(), allowedStyle.getName());
        jdbcTemplate.update("UPDATE competition SET status = ? WHERE id = ?",
                CompetitionStatus.REGISTRATION_OPEN.name(), fixture.competition().getId());

        PortalEntrySubmitRequest request = new PortalEntrySubmitRequest();
        request.setName(testRun + "-范围外风格");
        request.setCategoryId(fixture.category().getId());
        request.setStyle(testRun + "-风格");
        request.setAbv(new BigDecimal("5.0"));
        asPortal(fixture.portalA().account().getId());

        assertThatThrownBy(() -> portalEntryService.submitPortalEntry(fixture.competition().getId(), request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("不在所选投递组别的可选范围内");

        request.setName(testRun + "-范围内风格");
        request.setStyle(allowedStyle.getName());
        var submitted = portalEntryService.submitPortalEntry(fixture.competition().getId(), request);
        assertThat(submitted.getStyle()).isEqualTo(allowedStyle.getName());
    }
}
