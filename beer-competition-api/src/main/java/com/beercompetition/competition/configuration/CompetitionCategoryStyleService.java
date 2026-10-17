package com.beercompetition.competition.configuration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionCategoryStyleMapper;
import com.beercompetition.pojo.po.CompetitionCategoryStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 维护投递组别的可选风格范围，并作为报名时的统一校验入口。
 * 组别没有任何范围记录时表示全部风格可选。
 */
@Service
@RequiredArgsConstructor
public class CompetitionCategoryStyleService {

    private final CompetitionCategoryStyleMapper competitionCategoryStyleMapper;

    public Map<Long, List<String>> listStyleNamesByCategory(Long competitionId) {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        competitionCategoryStyleMapper.selectList(new LambdaQueryWrapper<CompetitionCategoryStyle>()
                        .eq(CompetitionCategoryStyle::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionCategoryStyle::getCategoryId)
                        .orderByAsc(CompetitionCategoryStyle::getSortOrder)
                        .orderByAsc(CompetitionCategoryStyle::getId))
                .forEach(item -> result.computeIfAbsent(item.getCategoryId(), key -> new ArrayList<>())
                        .add(item.getStyleName()));
        return result;
    }

    public void requireStyleAllowed(Long categoryId, String styleName) {
        long restrictedCount = competitionCategoryStyleMapper.selectCount(new LambdaQueryWrapper<CompetitionCategoryStyle>()
                .eq(CompetitionCategoryStyle::getCategoryId, categoryId));
        if (restrictedCount == 0) {
            return;
        }
        long allowedCount = competitionCategoryStyleMapper.selectCount(new LambdaQueryWrapper<CompetitionCategoryStyle>()
                .eq(CompetitionCategoryStyle::getCategoryId, categoryId)
                .eq(CompetitionCategoryStyle::getStyleName, styleName));
        if (allowedCount == 0) {
            throw new BaseException("该基础风格不在所选投递组别的可选范围内");
        }
    }

    public void replaceScopes(Long competitionId, Map<Long, List<String>> scopesByCategory) {
        competitionCategoryStyleMapper.delete(new LambdaQueryWrapper<CompetitionCategoryStyle>()
                .eq(CompetitionCategoryStyle::getCompetitionId, competitionId));
        scopesByCategory.forEach((categoryId, names) -> {
            int sort = 0;
            for (String name : normalizeNames(names)) {
                competitionCategoryStyleMapper.insert(CompetitionCategoryStyle.builder()
                        .competitionId(competitionId)
                        .categoryId(categoryId)
                        .styleName(name)
                        .sortOrder(sort++)
                        .build());
            }
        });
    }

    public List<String> normalizeKnownNames(List<String> names, Set<String> activeStyleNames, String categoryName) {
        Set<String> normalized = normalizeNames(names);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> unknown = normalized.stream().filter(name -> !activeStyleNames.contains(name)).toList();
        if (!unknown.isEmpty()) {
            throw new BaseException("投递组别「" + categoryName + "」包含当前风格库不存在的风格：" + String.join("、", unknown));
        }
        return new ArrayList<>(normalized);
    }

    public Set<String> normalizeNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        names.stream()
                .map(name -> StringUtils.hasText(name) ? name.trim() : null)
                .filter(name -> name != null)
                .forEach(normalized::add);
        return normalized;
    }
}
