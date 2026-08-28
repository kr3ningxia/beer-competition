package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.StyleCategoryMapper;
import com.beercompetition.mapper.StyleItemMapper;
import com.beercompetition.mapper.StyleLibraryMapper;
import com.beercompetition.pojo.dto.StyleCategoryRequest;
import com.beercompetition.pojo.dto.StyleItemRequest;
import com.beercompetition.pojo.dto.StyleLibraryUpsertRequest;
import com.beercompetition.pojo.po.StyleCategory;
import com.beercompetition.pojo.po.StyleItem;
import com.beercompetition.pojo.po.StyleLibrary;
import com.beercompetition.pojo.vo.StyleItemVO;
import com.beercompetition.pojo.vo.StyleLibraryVO;
import com.beercompetition.service.StyleLibraryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StyleLibraryServiceImpl implements StyleLibraryService {

    private static final int ENABLED_STATUS = 1;
    private static final String PUBLIC_VISIBILITY = "PUBLIC";
    private static final String PRIVATE_VISIBILITY = "PRIVATE";

    private final StyleLibraryMapper styleLibraryMapper;
    private final StyleCategoryMapper styleCategoryMapper;
    private final StyleItemMapper styleItemMapper;
    private final ObjectMapper objectMapper;
    private final CompetitionAccessService competitionAccessService;

    @Override
    public List<StyleLibraryVO> listLibraries() {
        final boolean platformSuperAdmin = competitionAccessService.isPlatformSuperAdmin();
        final Long organizerId = platformSuperAdmin ? null : competitionAccessService.requireCurrentOrganizerId();
        // 1) 查询风格库主记录
        LambdaQueryWrapper<StyleLibrary> query = new LambdaQueryWrapper<StyleLibrary>()
                .orderByDesc(StyleLibrary::getStatus)
                .orderByDesc(StyleLibrary::getUpdateTime)
                .orderByAsc(StyleLibrary::getId);
        if (!platformSuperAdmin) {
            query.and(w -> w.isNull(StyleLibrary::getVisibility)
                    .or().eq(StyleLibrary::getVisibility, PUBLIC_VISIBILITY)
                    .or().eq(StyleLibrary::getOrganizerId, organizerId));
        }
        List<StyleLibrary> libraries = styleLibraryMapper.selectList(query);

        // 2) 组装每套风格库的统计和预览
        return libraries.stream()
                .map(this::buildLibraryVO)
                .toList();
    }

    @Override
    public StyleLibraryVO getLibrary(String code) {
        // 1) 校验编码并查询风格库
        StyleLibrary library = getLibraryOrThrow(normalizeRequired(code, "风格库编码不能为空"));
        assertReadable(library);

        // 2) 组装完整风格库详情
        return buildLibraryVO(library);
    }

    @Override
    public List<StyleLibraryVO> listEnabledLibraries() {
        final boolean platformSuperAdmin = competitionAccessService.isPlatformSuperAdmin();
        final Long organizerId = platformSuperAdmin ? null : competitionAccessService.requireCurrentOrganizerId();
        LambdaQueryWrapper<StyleLibrary> query = new LambdaQueryWrapper<StyleLibrary>()
                        .eq(StyleLibrary::getStatus, ENABLED_STATUS);
        if (!platformSuperAdmin) {
            query.and(w -> w.isNull(StyleLibrary::getVisibility)
                    .or().eq(StyleLibrary::getVisibility, PUBLIC_VISIBILITY)
                    .or().eq(StyleLibrary::getOrganizerId, organizerId));
        }
        return styleLibraryMapper.selectList(query
                        .orderByAsc(StyleLibrary::getId))
                .stream()
                .map(this::buildLibraryVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StyleLibraryVO saveLibrary(StyleLibraryUpsertRequest request) {
        competitionAccessService.requireStyleLibraryWriteAccess();
        final boolean platformSuperAdmin = competitionAccessService.isPlatformSuperAdmin();
        final Long organizerId = competitionAccessService.requireCurrentOrganizerId();
        // 1) 参数规范化与重复校验
        String code = normalizeRequired(request.getCode(), "风格库编码不能为空");
        List<StyleCategoryRequest> categories = normalizeCategories(request.getCategories() == null ? List.of() : request.getCategories());
        List<StyleItemRequest> styles = normalizeStyles(request.getStyles());
        if (resolveStatus(request.getStatus()) == ENABLED_STATUS && categories.isEmpty()) {
            throw new BaseException("启用风格库至少需要 1 个分类");
        }
        assertStylesUseExistingCategories(styles, categories);

        // 2) 新建或更新风格库主记录
        StyleLibrary library = styleLibraryMapper.selectOne(new LambdaQueryWrapper<StyleLibrary>()
                .eq(StyleLibrary::getCode, code));
        if (library == null) {
            library = StyleLibrary.builder().code(code).organizerId(organizerId).build();
        } else if (!Objects.equals(library.getOrganizerId(), organizerId)) {
            throw new BaseException("当前账号不能修改该风格库");
        }
        library.setName(normalizeRequired(request.getName(), "风格库名称不能为空"));
        library.setVersion(normalizeRequired(request.getVersion(), "版本不能为空"));
        library.setLanguage(normalizeRequired(request.getLanguage(), "语言不能为空"));
        library.setSource(normalizeRequired(request.getSource(), "来源不能为空"));
        library.setStatus(resolveStatus(request.getStatus()));
        String requestedVisibility = request.getVisibility();
        if (library.getId() != null && requestedVisibility == null) {
            requestedVisibility = library.getVisibility();
        }
        library.setVisibility(platformSuperAdmin ? normalizeVisibility(requestedVisibility) : PRIVATE_VISIBILITY);
        library.setTagsJson(writeTags(request.getTags()));
        try {
            if (library.getId() == null) {
                styleLibraryMapper.insert(library);
            } else {
                styleLibraryMapper.updateById(library);
            }
        } catch (DuplicateKeyException ex) {
            throw new BaseException("风格库编码已存在");
        }

        // 3) 替换分类和风格条目
        replaceCategories(library.getId(), categories);
        replaceStyles(library.getId(), styles);

        // 4) 返回最新详情
        return getLibrary(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StyleLibraryVO setVisibility(String code, String visibility) {
        competitionAccessService.requirePlatformSuperAdmin();
        StyleLibrary library = getLibraryOrThrow(normalizeRequired(code, "风格库编码不能为空"));
        Long platformOrganizerId = competitionAccessService.requireCurrentOrganizerId();
        if (!Objects.equals(library.getOrganizerId(), platformOrganizerId)) {
            throw new BaseException("只能发布啤酒事务局创建的风格库");
        }
        String normalizedVisibility = normalizeVisibility(visibility);
        if (PUBLIC_VISIBILITY.equals(normalizedVisibility) && !Objects.equals(library.getStatus(), ENABLED_STATUS)) {
            throw new BaseException("启用风格库后才能设为公共库");
        }
        library.setVisibility(normalizedVisibility);
        styleLibraryMapper.updateById(library);
        return buildLibraryVO(library);
    }

    @Override
    public List<StyleItemVO> listEnabledStyles(String code) {
        // 1) 查询启用风格库
        StyleLibrary library = getLibraryOrThrow(normalizeRequired(code, "风格库编码不能为空"));
        assertReadable(library);
        if (!Objects.equals(library.getStatus(), ENABLED_STATUS)) {
            throw new BaseException("当前风格库未启用");
        }

        // 2) 返回启用风格详情
        Map<Long, StyleCategory> categoryMap = listCategories(library.getId()).stream()
                .collect(Collectors.toMap(StyleCategory::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<StyleItemVO> styles = styleItemMapper.selectList(new LambdaQueryWrapper<StyleItem>()
                        .eq(StyleItem::getLibraryId, library.getId())
                        .eq(StyleItem::getStatus, ENABLED_STATUS)
                        .orderByAsc(StyleItem::getSortOrder)
                        .orderByAsc(StyleItem::getId))
                .stream()
                .map(item -> StyleItemVO.builder()
                        .id(item.getId())
                        .categoryName(categoryMap.get(item.getCategoryId()) == null ? null : categoryMap.get(item.getCategoryId()).getName())
                        .name(item.getName())
                        .styleCode(item.getStyleCode())
                        .description(item.getDescription())
                        .status(item.getStatus())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
        if (styles.isEmpty()) {
            throw new BaseException("当前风格库没有可用风格");
        }
        if (styles.stream().anyMatch(item -> !StringUtils.hasText(item.getCategoryName()))) {
            throw new BaseException("当前风格库存在未归类风格，请先修正分类");
        }
        return styles;
    }

    private StyleLibrary getLibraryOrThrow(String code) {
        StyleLibrary library = styleLibraryMapper.selectOne(new LambdaQueryWrapper<StyleLibrary>()
                .eq(StyleLibrary::getCode, code));
        if (library == null) {
            throw new ResourceNotFoundException("风格库不存在");
        }
        return library;
    }

    private void assertReadable(StyleLibrary library) {
        if (PUBLIC_VISIBILITY.equals(normalizeVisibility(library.getVisibility()))) {
            return;
        }
        Long organizerId = library.getOrganizerId();
        if (organizerId == null) {
            return;
        }
        if (competitionAccessService.isPlatformSuperAdmin()) {
            return;
        }
        Long currentOrganizerId = competitionAccessService.requireCurrentOrganizerId();
        if (!Objects.equals(currentOrganizerId, organizerId)) {
            throw new com.beercompetition.common.exception.ForbiddenException("当前账号无权访问该风格库");
        }
    }

    private StyleLibraryVO buildLibraryVO(StyleLibrary library) {
        List<StyleCategory> categories = listCategories(library.getId());
        Map<Long, StyleCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(StyleCategory::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<StyleItemVO> styleItems = listStyles(library.getId()).stream()
                .map(item -> StyleItemVO.builder()
                        .id(item.getId())
                        .categoryName(categoryMap.get(item.getCategoryId()) == null ? null : categoryMap.get(item.getCategoryId()).getName())
                        .name(item.getName())
                        .styleCode(item.getStyleCode())
                        .description(item.getDescription())
                        .status(item.getStatus())
                        .sortOrder(item.getSortOrder())
                        .build())
                .toList();
        List<String> styleNames = styleItems.stream().map(StyleItemVO::getName).toList();
        return StyleLibraryVO.builder()
                .id(library.getId())
                .organizerId(library.getOrganizerId())
                .visibility(normalizeVisibility(library.getVisibility()))
                .visibilityLabel(PUBLIC_VISIBILITY.equals(normalizeVisibility(library.getVisibility())) ? "公共" : "内部")
                .canEdit(canEdit(library))
                .canPublish(canPublish(library))
                .value(library.getCode())
                .label(library.getName())
                .code(library.getCode())
                .name(library.getName())
                .version(library.getVersion())
                .language(library.getLanguage())
                .source(library.getSource())
                .status(library.getStatus())
                .statusLabel(Objects.equals(library.getStatus(), ENABLED_STATUS) ? "启用" : "停用")
                .categoryCount(categories.size())
                .styleCount(styleItems.size())
                .tags(readTags(library.getTagsJson()))
                .categories(categories.stream().map(StyleCategory::getName).toList())
                .styles(styleNames)
                .styleItems(styleItems)
                .updatedAt(library.getUpdateTime())
                .build();
    }

    private List<StyleCategory> listCategories(Long libraryId) {
        return styleCategoryMapper.selectList(new LambdaQueryWrapper<StyleCategory>()
                .eq(StyleCategory::getLibraryId, libraryId)
                .orderByAsc(StyleCategory::getSortOrder)
                .orderByAsc(StyleCategory::getId));
    }

    private List<StyleItem> listStyles(Long libraryId) {
        return styleItemMapper.selectList(new LambdaQueryWrapper<StyleItem>()
                .eq(StyleItem::getLibraryId, libraryId)
                .orderByAsc(StyleItem::getSortOrder)
                .orderByAsc(StyleItem::getId));
    }

    private void replaceCategories(Long libraryId, List<StyleCategoryRequest> categories) {
        styleCategoryMapper.delete(new LambdaQueryWrapper<StyleCategory>()
                .eq(StyleCategory::getLibraryId, libraryId));
        int sort = 0;
        for (StyleCategoryRequest item : categories) {
            styleCategoryMapper.insert(StyleCategory.builder()
                    .libraryId(libraryId)
                    .name(item.getName())
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }
    }

    private void replaceStyles(Long libraryId, List<StyleItemRequest> styles) {
        Map<String, StyleCategory> categoryMap = listCategories(libraryId).stream()
                .collect(Collectors.toMap(StyleCategory::getName, Function.identity()));
        styleItemMapper.delete(new LambdaQueryWrapper<StyleItem>()
                .eq(StyleItem::getLibraryId, libraryId));
        int sort = 0;
        for (StyleItemRequest item : styles) {
            StyleCategory category = categoryMap.get(item.getCategoryName());
            if (category == null) {
                throw new BaseException("风格分类不存在：" + item.getCategoryName());
            }
            styleItemMapper.insert(StyleItem.builder()
                    .libraryId(libraryId)
                    .categoryId(category.getId())
                    .name(item.getName())
                    .styleCode(normalizeNullable(item.getStyleCode()))
                    .description(normalizeNullable(item.getDescription()))
                    .status(resolveStatus(item.getStatus()))
                    .sortOrder(resolveSort(item.getSortOrder(), sort++))
                    .build());
        }
    }

    private List<StyleCategoryRequest> normalizeCategories(List<StyleCategoryRequest> categories) {
        List<StyleCategoryRequest> normalized = categories.stream()
                .peek(item -> item.setName(normalizeRequired(item.getName(), "分类名称不能为空")))
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUnique(normalized.stream().map(StyleCategoryRequest::getName).toList(), "分类名称");
        return normalized;
    }

    private List<StyleItemRequest> normalizeStyles(List<StyleItemRequest> styles) {
        List<StyleItemRequest> normalized = styles.stream()
                .peek(item -> {
                    item.setName(normalizeRequired(item.getName(), "风格名称不能为空"));
                    item.setCategoryName(normalizeRequired(item.getCategoryName(), "风格分类不能为空"));
                    item.setStyleCode(normalizeNullable(item.getStyleCode()));
                    item.setDescription(normalizeNullable(item.getDescription()));
                })
                .sorted(Comparator.comparing(item -> resolveSort(item.getSortOrder(), 0)))
                .toList();
        assertUnique(normalized.stream().map(StyleItemRequest::getName).toList(), "风格名称");
        assertUnique(normalized.stream()
                .map(StyleItemRequest::getStyleCode)
                .filter(StringUtils::hasText)
                .toList(), "风格编号");
        return normalized;
    }

    private void assertStylesUseExistingCategories(List<StyleItemRequest> styles, List<StyleCategoryRequest> categories) {
        Set<String> categoryNames = categories.stream()
                .map(StyleCategoryRequest::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (StyleItemRequest style : styles) {
            if (!categoryNames.contains(style.getCategoryName())) {
                throw new BaseException("风格分类不存在：" + style.getCategoryName());
            }
        }
    }

    private void assertUnique(List<String> values, String label) {
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (!set.add(value)) {
                throw new BaseException(label + "不能重复：" + value);
            }
        }
    }

    private Integer resolveStatus(Integer status) {
        return Objects.equals(status, ENABLED_STATUS) ? ENABLED_STATUS : 0;
    }

    private String normalizeVisibility(String visibility) {
        if (!StringUtils.hasText(visibility)) {
            return PRIVATE_VISIBILITY;
        }
        if (PUBLIC_VISIBILITY.equalsIgnoreCase(visibility.trim())) {
            return PUBLIC_VISIBILITY;
        }
        if (PRIVATE_VISIBILITY.equalsIgnoreCase(visibility.trim())) {
            return PRIVATE_VISIBILITY;
        }
        throw new BaseException("风格库可见范围不正确");
    }

    private boolean canEdit(StyleLibrary library) {
        if (library.getOrganizerId() == null) return false;
        boolean platformSuperAdmin = competitionAccessService.isPlatformSuperAdmin();
        if (!platformSuperAdmin && !competitionAccessService.isOrganizerAdmin()) return false;
        Long currentOrganizerId = competitionAccessService.requireCurrentOrganizerId();
        if (!Objects.equals(currentOrganizerId, library.getOrganizerId())) return false;
        return platformSuperAdmin
                || PRIVATE_VISIBILITY.equals(normalizeVisibility(library.getVisibility()));
    }

    private boolean canPublish(StyleLibrary library) {
        return competitionAccessService.isPlatformSuperAdmin()
                && Objects.equals(library.getOrganizerId(), competitionAccessService.requireCurrentOrganizerId());
    }

    private int resolveSort(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> readTags(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeTags(List<String> tags) {
        List<String> normalized = new ArrayList<>();
        if (tags != null) {
            tags.stream()
                    .map(this::normalizeNullable)
                    .filter(Objects::nonNull)
                    .forEach(normalized::add);
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new BaseException("风格库标签保存失败");
        }
    }
}
