package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.PortalCompetitionVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.competition.query.CompetitionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.beercompetition.registration.entry.PortalEntryService;

/**
 * 编排当前厂商的报名流程并校验报名所有权。
 */
@Service
@RequiredArgsConstructor
public class PortalEntryServiceImpl implements PortalEntryService {

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionQueryService competitionQueryService;

    private final PortalEntryViewAssembler portalEntryViewAssembler;

    private final PortalEntryCreationService portalEntryCreationService;

    private final PortalEntryMutationService portalEntryMutationService;

    private final PortalProfileService portalProfileService;

    private final PortalAccountAccessService portalAccountAccessService;

    @Override
    public List<EntrySummaryVO> listPortalEntries() {
        // 1) 查询当前厂商账号
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();

        // 2) 查询当前厂商全部作品
        return beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                        .eq(BeerEntry::getBreweryId, account.getBreweryId())
                        .orderByDesc(BeerEntry::getId))
                .stream()
                .map(portalEntryViewAssembler::toEntrySummaryVO)
                .toList();
    }

    @Override
    public EntryDetailVO getPortalEntry(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = portalAccountAccessService.requireCurrentAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());

        // 2) 组装作品详情
        return portalEntryViewAssembler.toEntryDetailVO(entry);
    }

    @Override
    public PortalMyParticipationVO getPortalMyParticipation() {
        // 1) 查询当前资料和参赛作品
        PortalProfileVO profile = portalProfileService.getPortalProfile();
        List<EntrySummaryVO> entries = listPortalEntries();

        // 2) 按厂商作品关联公开赛事
        Set<Long> competitionIds = entries.stream()
                .map(EntrySummaryVO::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PortalCompetitionVO> competitions = competitionQueryService.listPortalCompetitions().stream()
                .filter(item -> competitionIds.contains(item.getId()))
                .toList();

        // 3) 组装我的参赛首页数据
        return PortalMyParticipationVO.builder()
                .profile(profile)
                .entries(entries)
                .competitions(competitions)
                .build();
    }

    private BeerEntry requireOwnedEntry(Long entryId, Long breweryId) {
        BeerEntry entry = requireEntry(entryId);
        if (!entry.getBreweryId().equals(breweryId)) {
            throw new ForbiddenException("无权查看该酒款");
        }
        return entry;
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    @Override
    public EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request) {
        return portalEntryCreationService.submitPortalEntry(competitionId, request);
    }

    @Override
    public EntryDetailVO updatePortalEntry(Long entryId, PortalEntryUpdateRequest request) {
        return portalEntryMutationService.updatePortalEntry(entryId, request);
    }

    @Override
    public EntryDetailVO cancelPortalEntry(Long entryId) {
        return portalEntryMutationService.cancelPortalEntry(entryId);
    }
}
