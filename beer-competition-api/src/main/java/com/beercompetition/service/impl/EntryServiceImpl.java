package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {

    private final PortalAccountMapper portalAccountMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    private final BreweryMapper breweryMapper;

    @Override
    public List<EntrySummaryVO> listPortalEntries() {
        PortalAccount account = requirePortalAccount();
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getBreweryId, account.getBreweryId())
                        .orderByDesc(BeerEntry::getId))
                .stream()
                .map(this::toEntrySummaryVO)
                .toList();
    }

    @Override
    public EntryDetailVO getPortalEntry(Long entryId) {
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        if (!entry.getBreweryId().equals(account.getBreweryId())) {
            throw new ForbiddenException("无权查看该酒款");
        }
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        return EntryDetailVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .name(entry.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .description(entry.getDescription())
                .categoryName(category == null ? null : category.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionDate(competition == null ? null : competition.getCompetitionDate())
                .status(entry.getStatus())
                .extraFields(listExtraFields(entry.getId()))
                .build();
    }

    @Override
    public JudgeEntryVO getJudgeEntry(String uuid) {
        BeerEntry entry = beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, uuid));
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        return JudgeEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .competitionId(entry.getCompetitionId())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .description(entry.getDescription())
                .extraFields(listExtraFields(entry.getId()))
                .build();
    }

    private EntrySummaryVO toEntrySummaryVO(BeerEntry entry) {
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        return EntrySummaryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .name(entry.getName())
                .competitionName(competition == null ? null : competition.getName())
                .categoryName(category == null ? null : category.getName())
                .status(entry.getStatus())
                .abv(entry.getAbv())
                .build();
    }

    private List<EntryExtraFieldVO> listExtraFields(Long beerEntryId) {
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, beerEntryId))
                .stream()
                .map(item -> EntryExtraFieldVO.builder()
                        .key(item.getFieldKey())
                        .label(item.getFieldLabel())
                        .value(item.getFieldValue())
                        .build())
                .toList();
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂商账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
    }
}
