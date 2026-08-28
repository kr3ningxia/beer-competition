package com.beercompetition.service;

import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.pojo.vo.StyleLibraryVO;

import java.util.List;

public interface StyleLibraryService {

    List<StyleLibraryVO> listLibraries();

    StyleLibraryVO getLibrary(String code);

    /**
     * 返回创建比赛时可引用的启用风格库，只读且不授予公共风格库维护权限。
     */
    List<StyleLibraryVO> listEnabledLibraries();

    StyleLibraryVO saveLibrary(StyleLibraryUpsertRequest request);

    StyleLibraryVO setVisibility(String code, String visibility);

    List<StyleItemVO> listEnabledStyles(String code);
}
