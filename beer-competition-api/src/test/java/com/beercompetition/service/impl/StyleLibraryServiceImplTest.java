package com.beercompetition.service.impl;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.StyleCategoryMapper;
import com.beercompetition.mapper.StyleItemMapper;
import com.beercompetition.mapper.StyleLibraryMapper;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.StyleLibrary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StyleLibraryServiceImplTest {

    private final StyleLibraryMapper styleLibraryMapper = mock(StyleLibraryMapper.class);
    private final StyleCategoryMapper styleCategoryMapper = mock(StyleCategoryMapper.class);
    private final StyleItemMapper styleItemMapper = mock(StyleItemMapper.class);
    private final CompetitionAccessService competitionAccessService = mock(CompetitionAccessService.class);
    private final StyleLibraryServiceImpl styleLibraryService = new StyleLibraryServiceImpl(
            styleLibraryMapper, styleCategoryMapper, styleItemMapper, new ObjectMapper(), competitionAccessService);

    @Test
    void organizerCanListPublicAndOwnLibraries() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(64L);
        when(styleLibraryMapper.selectList(any())).thenReturn(List.of());

        assertThat(styleLibraryService.listLibraries()).isEmpty();
        verify(competitionAccessService).requireCurrentOrganizerId();
    }

    @Test
    void organizerListingDoesNotRequirePlatformSuperAdmin() {
        when(styleLibraryMapper.selectList(any())).thenReturn(List.of());
        verify(competitionAccessService, never()).requirePlatformSuperAdmin();
        styleLibraryService.listLibraries();
        verify(competitionAccessService, never()).requirePlatformSuperAdmin();
    }

    @Test
    void organizerCanReadEnabledLibrariesForCompetitionSetup() {
        when(competitionAccessService.canAccessAllOrganizers()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(64L);
        when(styleLibraryMapper.selectList(any())).thenReturn(List.of(StyleLibrary.builder()
                .id(1L)
                .code("BJCP_2021_CN")
                .name("BJCP 2021 中文标准库")
                .version("2021")
                .language("中文")
                .source("BJCP")
                .status(1)
                .build()));
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(27L)
                .role(UserRole.ADMIN.name())
                .build());

        assertThat(styleLibraryService.listEnabledLibraries()).hasSize(1);
        verify(competitionAccessService, never()).requirePlatformSuperAdmin();
        BaseContext.clear();
    }

    @Test
    void organizerCannotReadAnotherOrganizersPrivateLibrary() {
        when(competitionAccessService.isPlatformSuperAdmin()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(64L);
        when(styleLibraryMapper.selectOne(any())).thenReturn(StyleLibrary.builder()
                .id(9L).code("TENANT_99").organizerId(99L).visibility("PRIVATE").status(1).build());

        assertThatThrownBy(() -> styleLibraryService.getLibrary("TENANT_99"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void platformEventAdminCanReadButCannotEditPlatformPrivateLibrary() {
        when(competitionAccessService.isPlatformSuperAdmin()).thenReturn(false);
        when(competitionAccessService.isOrganizerAdmin()).thenReturn(false);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(1L);
        when(styleLibraryMapper.selectOne(any())).thenReturn(StyleLibrary.builder()
                .id(10L).code("PLATFORM_INTERNAL").organizerId(1L).visibility("PRIVATE").status(1).build());
        when(styleCategoryMapper.selectList(any())).thenReturn(List.of());
        when(styleItemMapper.selectList(any())).thenReturn(List.of());

        assertThat(styleLibraryService.getLibrary("PLATFORM_INTERNAL").getCanEdit()).isFalse();
    }

    @Test
    void organizerAdminCanEditOwnPrivateLibrary() {
        when(competitionAccessService.isPlatformSuperAdmin()).thenReturn(false);
        when(competitionAccessService.isOrganizerAdmin()).thenReturn(true);
        when(competitionAccessService.requireCurrentOrganizerId()).thenReturn(64L);
        when(styleLibraryMapper.selectOne(any())).thenReturn(StyleLibrary.builder()
                .id(11L).code("TENANT_PRIVATE").organizerId(64L).visibility("PRIVATE").status(1).build());
        when(styleCategoryMapper.selectList(any())).thenReturn(List.of());
        when(styleItemMapper.selectList(any())).thenReturn(List.of());

        assertThat(styleLibraryService.getLibrary("TENANT_PRIVATE").getCanEdit()).isTrue();
    }
}
