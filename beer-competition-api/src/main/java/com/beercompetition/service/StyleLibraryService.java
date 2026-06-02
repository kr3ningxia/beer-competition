package com.beercompetition.service;

import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.pojo.vo.StyleLibraryVO;

import java.util.List;

public interface StyleLibraryService {

    List<StyleLibraryVO> listLibraries();

    StyleLibraryVO getLibrary(String code);

    StyleLibraryVO saveLibrary(StyleLibraryUpsertRequest request);

    List<StyleItemVO> listEnabledStyles(String code);
}
