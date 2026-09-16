package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.mapper.CompetitionJudgePublicProfileMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.po.CompetitionJudgePublicProfile;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.vo.PortalPublicJudgeVO;
import com.beercompetition.service.CompetitionJudgePublicProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
public class CompetitionJudgePublicProfileServiceImpl implements CompetitionJudgePublicProfileService {

    private static final String BUSINESS_TYPE_JUDGE_AVATAR = "JUDGE_AVATAR";
    private static final String OWNER_TYPE_JUDGE_ACCOUNT = "JUDGE_ACCOUNT";
    private static final String ASSIGNMENT_ACTIVE = "ACTIVE";

    private final CompetitionJudgePublicProfileMapper publicProfileMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeAccountMapper judgeAccountMapper;
    private final FileAssetMapper fileAssetMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void snapshotAtPublication(Long competitionId) {
        if (competitionId == null) {
            return;
        }
        if (publicProfileMapper.selectCount(new LambdaQueryWrapper<CompetitionJudgePublicProfile>()
                .eq(CompetitionJudgePublicProfile::getCompetitionId, competitionId)) > 0) {
            return;
        }

        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId)
                .and(wrapper -> wrapper.eq(JudgeAssignment::getStatus, ASSIGNMENT_ACTIVE)
                        .or().isNull(JudgeAssignment::getStatus))
                .orderByAsc(JudgeAssignment::getId));
        if (assignments.isEmpty()) {
            return;
        }

        Set<Long> judgeIds = assignments.stream()
                .map(JudgeAssignment::getJudgeAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (judgeIds.isEmpty()) {
            return;
        }
        Map<Long, JudgeAccount> accounts = judgeAccountMapper.selectBatchIds(judgeIds).stream()
                .collect(Collectors.toMap(JudgeAccount::getId, Function.identity(), (left, right) -> left));
        Map<Long, PublicProfileDraft> drafts = new LinkedHashMap<>();
        for (JudgeAssignment assignment : assignments) {
            JudgeAccount account = accounts.get(assignment.getJudgeAccountId());
            if (account == null
                    || JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE
                    || !Boolean.TRUE.equals(account.getPublicProfileConsent())) {
                continue;
            }
            PublicProfileDraft draft = drafts.computeIfAbsent(account.getId(), id -> new PublicProfileDraft(
                    account,
                    assignment.getId() == null ? Long.MAX_VALUE : assignment.getId()));
            String role = roleLabel(assignment.getRole());
            if (role != null) {
                draft.roles.add(role);
            }
        }

        List<PublicProfileDraft> ordered = drafts.values().stream()
                .sorted(Comparator.comparing(PublicProfileDraft::firstAssignmentId)
                        .thenComparing(item -> item.account().getId()))
                .toList();
        int sortOrder = 0;
        for (PublicProfileDraft draft : ordered) {
            publicProfileMapper.insert(CompetitionJudgePublicProfile.builder()
                    .competitionId(competitionId)
                    .judgeAccountId(draft.account().getId())
                    .name(draft.account().getName())
                    .qualification(draft.account().getQualification())
                    .avatarAssetId(resolveAvatarAssetId(draft.account()))
                    .rolesJson(writeRoles(draft.roles()))
                    .sortOrder(sortOrder++)
                    .createTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    public List<PortalPublicJudgeVO> listPublicProfiles(Long competitionId) {
        if (competitionId == null) {
            return List.of();
        }
        return publicProfileMapper.selectList(new LambdaQueryWrapper<CompetitionJudgePublicProfile>()
                        .eq(CompetitionJudgePublicProfile::getCompetitionId, competitionId)
                        .orderByAsc(CompetitionJudgePublicProfile::getSortOrder)
                        .orderByAsc(CompetitionJudgePublicProfile::getId))
                .stream()
                .map(this::toPublicVO)
                .toList();
    }

    @Override
    public boolean isPublishedAvatarReferenced(Long fileAssetId) {
        return fileAssetId != null && publicProfileMapper.selectPublishedByAvatarAssetId(fileAssetId) != null;
    }

    private Long resolveAvatarAssetId(JudgeAccount account) {
        if (account.getAvatarAssetId() == null) {
            return null;
        }
        FileAsset asset = fileAssetMapper.selectById(account.getAvatarAssetId());
        if (asset == null
                || !BUSINESS_TYPE_JUDGE_AVATAR.equals(asset.getBusinessType())
                || !OWNER_TYPE_JUDGE_ACCOUNT.equals(asset.getOwnerType())
                || !Objects.equals(account.getId(), asset.getOwnerId())) {
            return null;
        }
        return asset.getId();
    }

    private PortalPublicJudgeVO toPublicVO(CompetitionJudgePublicProfile profile) {
        return PortalPublicJudgeVO.builder()
                .snapshotId(profile.getId())
                .name(profile.getName())
                .avatarUrl(profile.getAvatarAssetId() == null
                        ? null
                        : "/api/portal/public/files/" + profile.getAvatarAssetId())
                .roles(readRoles(profile.getRolesJson()))
                .qualification(profile.getQualification())
                .build();
    }

    private String writeRoles(Set<String> roles) {
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(roles));
        } catch (JsonProcessingException ex) {
            throw new BaseException("评委角色快照生成失败");
        }
    }

    private List<String> readRoles(String rolesJson) {
        if (!StringUtils.hasText(rolesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rolesJson, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String roleLabel(String role) {
        if ("CAPTAIN".equalsIgnoreCase(role)) {
            return "桌长";
        }
        if ("PROFESSIONAL".equalsIgnoreCase(role)) {
            return "专业评审";
        }
        if ("CROSS".equalsIgnoreCase(role)) {
            return "跨界评审";
        }
        return StringUtils.hasText(role) ? role.trim() : null;
    }

    private record PublicProfileDraft(JudgeAccount account, Long firstAssignmentId, Set<String> roles) {

        private PublicProfileDraft(JudgeAccount account, Long firstAssignmentId) {
            this(account, firstAssignmentId, new LinkedHashSet<>());
        }
    }
}
