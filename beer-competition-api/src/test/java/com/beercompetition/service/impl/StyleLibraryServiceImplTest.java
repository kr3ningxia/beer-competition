package com.beercompetition.service.impl;

import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.StyleCategoryMapper;
import com.beercompetition.mapper.StyleItemMapper;
import com.beercompetition.mapper.StyleLibraryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
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
    void styleLibraryListRequiresPlatformSuperAdmin() {
        when(styleLibraryMapper.selectList(any())).thenReturn(List.of());

        assertThat(styleLibraryService.listLibraries()).isEmpty();
        verify(competitionAccessService).requirePlatformSuperAdmin();
    }

    @Test
    void organizerCannotMaintainStyleLibrary() {
        doThrow(new ForbiddenException("当前账号无平台超级管理员权限"))
                .when(competitionAccessService).requirePlatformSuperAdmin();

        assertThatThrownBy(styleLibraryService::listLibraries)
                .isInstanceOf(ForbiddenException.class);
        verify(competitionAccessService).requirePlatformSuperAdmin();
    }
}
