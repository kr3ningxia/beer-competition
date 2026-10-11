package com.beercompetition.judging.round;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.RoundTableCategoryStateMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableCategoryState;
import com.beercompetition.service.impl.round.RoundQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 解析并维护评审桌内的多组别状态，兼容旧的单组别桌数据。 */
@Service
@RequiredArgsConstructor
public class RoundTableCategoryService {

    private final RoundTableCategoryStateMapper stateMapper;
    private final RoundTableEntryMapper entryMapper;
    private final RoundQuerySupport roundQuerySupport;

    public List<Long> listCategoryIds(RoundTable table) {
        List<Long> stateIds = stateMapper.selectList(new LambdaQueryWrapper<RoundTableCategoryState>()
                        .eq(RoundTableCategoryState::getRoundTableId, table.getId())
                        .orderByAsc(RoundTableCategoryState::getId))
                .stream()
                .map(RoundTableCategoryState::getCategoryId)
                .toList();
        if (!stateIds.isEmpty()) return stateIds;
        if (table.getCategoryId() != null) return List.of(table.getCategoryId());
        List<Long> entryIds = entryMapper.selectList(new LambdaQueryWrapper<com.beercompetition.pojo.po.RoundTableEntry>()
                        .eq(com.beercompetition.pojo.po.RoundTableEntry::getRoundTableId, table.getId()))
                .stream()
                .map(com.beercompetition.pojo.po.RoundTableEntry::getBeerEntryId)
                .toList();
        if (entryIds.isEmpty()) return List.of();
        Map<Long, BeerEntry> entries = roundQuerySupport.loadEntries(new LinkedHashSet<>(entryIds));
        return entries.values().stream()
                .map(BeerEntry::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureStates(RoundTable table) {
        if (!"MEDALS".equals(table.getTargetMode())) return;
        Set<Long> categoryIds = new LinkedHashSet<>(listCategoryIds(table));
        for (Long categoryId : categoryIds) {
            if (stateMapper.selectOne(new LambdaQueryWrapper<RoundTableCategoryState>()
                    .eq(RoundTableCategoryState::getRoundTableId, table.getId())
                    .eq(RoundTableCategoryState::getCategoryId, categoryId)) == null) {
                stateMapper.insert(RoundTableCategoryState.builder()
                        .roundTableId(table.getId())
                        .categoryId(categoryId)
                        .resultVersion(0)
                        .status("DRAFT")
                        .build());
            }
        }
    }

    public Long resolveCategoryId(RoundTable table, Long requestedCategoryId) {
        List<Long> categoryIds = listCategoryIds(table);
        if (!"MEDALS".equals(table.getTargetMode())) return requestedCategoryId;
        if (requestedCategoryId != null) {
            if (!categoryIds.contains(requestedCategoryId)) {
                throw new BaseException("当前评审桌不包含所选组别");
            }
            return requestedCategoryId;
        }
        if (categoryIds.size() == 1) return categoryIds.get(0);
        if (table.getCategoryId() != null) return table.getCategoryId();
        throw new BaseException("多组别奖牌轮必须指定组别");
    }

    public RoundTableCategoryState requireState(RoundTable table, Long categoryId) {
        ensureStates(table);
        RoundTableCategoryState state = stateMapper.selectOne(new LambdaQueryWrapper<RoundTableCategoryState>()
                .eq(RoundTableCategoryState::getRoundTableId, table.getId())
                .eq(RoundTableCategoryState::getCategoryId, categoryId));
        if (state == null) throw new BaseException("当前评审桌缺少组别状态");
        return state;
    }

    public List<RoundTableCategoryState> listStates(RoundTable table) {
        ensureStates(table);
        return stateMapper.selectList(new LambdaQueryWrapper<RoundTableCategoryState>()
                .eq(RoundTableCategoryState::getRoundTableId, table.getId())
                .orderByAsc(RoundTableCategoryState::getId));
    }
}
