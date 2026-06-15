package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.vo.StyleLibraryVO;
import com.beercompetition.service.StyleLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台风格库接口，维护比赛可引用的啤酒风格分类体系。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/style-libraries")
public class AdminStyleLibraryController {

    private final StyleLibraryService styleLibraryService;

    /**
     * 查询后台风格库列表。
     */
    @GetMapping
    public Result<List<StyleLibraryVO>> styleLibraries() {
        return Result.success(styleLibraryService.listLibraries());
    }

    /**
     * 查询单个风格库详情。
     */
    @GetMapping("/{code}")
    public Result<StyleLibraryVO> styleLibraryDetail(@PathVariable String code) {
        return Result.success(styleLibraryService.getLibrary(code));
    }

    /**
     * 新建风格库。
     */
    @PostMapping
    public Result<StyleLibraryVO> createStyleLibrary(@RequestBody @Valid StyleLibraryUpsertRequest request) {
        return Result.success(styleLibraryService.saveLibrary(request));
    }

    /**
     * 更新指定编码的风格库。
     */
    @PutMapping("/{code}")
    public Result<StyleLibraryVO> updateStyleLibrary(@PathVariable String code,
                                                     @RequestBody @Valid StyleLibraryUpsertRequest request) {
        request.setCode(code);
        return Result.success(styleLibraryService.saveLibrary(request));
    }
}
