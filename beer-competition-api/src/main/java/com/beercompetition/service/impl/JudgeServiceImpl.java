package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.result.PageResult;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.JudgeRecruitmentApplicationMapper;
import com.beercompetition.mapper.JudgeScoreSessionMapper;
import com.beercompetition.mapper.RoundJudgeRankingDraftMapper;
import com.beercompetition.mapper.RoundTableConfirmationMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentItemRequest;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.dto.JudgeRoundMemberChangeRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.JudgeScoreSession;
import com.beercompetition.pojo.po.RoundJudgeRankingDraft;
import com.beercompetition.pojo.po.RoundTableConfirmation;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.pojo.vo.JudgeTaskVO;
import com.beercompetition.judging.access.JudgeAccessService;
import com.beercompetition.service.JudgeService;
import com.beercompetition.judging.round.JudgeRoundTaskService;
import com.beercompetition.judging.scoring.ScoreConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final String TARGET_JUDGE = "JUDGE";

    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeRecruitmentApplicationMapper judgeRecruitmentApplicationMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final JudgeScoreSessionMapper judgeScoreSessionMapper;
    private final RoundJudgeRankingDraftMapper roundJudgeRankingDraftMapper;
    private final RoundTableConfirmationMapper roundTableConfirmationMapper;
    private final RoundTableMemberMapper roundTableMemberMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionRoundMapper competitionRoundMapper;
    private final RoundTableMapper roundTableMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final PiiService piiService;
    private final JudgeAccessService judgeAccessService;
    private final JudgeRoundTaskService judgeRoundTaskService;
    private final ScoreConfirmationService scoreConfirmationService;

    @Override
    public List<JudgeAccountVO> listJudges(Integer status, String keyword) {
        return listJudges(status, keyword, null);
    }

    @Override
    public List<JudgeAccountVO> listJudges(Integer status, String keyword, Long competitionId) {
        if (competitionId != null) {
            judgeAccessService.requireCompetitionAccess(competitionId);
        }
        // 1) 构造查询条件
        LambdaQueryWrapper<JudgeAccount> wrapper = buildJudgeQuery(status, keyword, judgeAccessService.currentScope());
        if (competitionId != null) {
            List<Long> acceptedJudgeIds = judgeRecruitmentApplicationMapper
                    .selectAcceptedJudgeIdsByCompetition(competitionId);
            if (acceptedJudgeIds == null || acceptedJudgeIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(JudgeAccount::getId, acceptedJudgeIds);
        }

        // 2) 查询并组装全量脱敏评审池
        List<JudgeAccount> judges = judgeAccountMapper.selectList(wrapper.orderByDesc(JudgeAccount::getId));
        PhoneConflictContext phoneConflictContext = buildPhoneConflictContext(judges);
        return judges.stream()
                .map(judge -> toJudgeListVO(judge, phoneConflictContext))
                .toList();
    }

    @Override
    public PageResult<JudgeAccountVO> pageJudges(Integer status, String keyword, Integer page, Integer pageSize) {
        // 1) 参数规范化与查询条件
        int currentPage = Math.max(page == null ? 1 : page, 1);
        int currentPageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 100);
        LambdaQueryWrapper<JudgeAccount> wrapper = buildJudgeQuery(status, keyword, judgeAccessService.currentScope())
                .orderByDesc(JudgeAccount::getId);

        // 2) 分页查询并组装列表
        Page<JudgeAccount> result = judgeAccountMapper.selectPage(new Page<>(currentPage, currentPageSize), wrapper);
        PhoneConflictContext phoneConflictContext = buildPhoneConflictContext(result.getRecords());
        List<JudgeAccountVO> records = result.getRecords().stream()
                .map(judge -> toJudgeListVO(judge, phoneConflictContext))
                .toList();

        // 3) 返回分页结果
        return new PageResult<>(result.getTotal(), records);
    }

    @Override
    public JudgeAccountVO getJudgeDetail(String publicId) {
        // 1) 查询评审详情
        JudgeAccount account = requireAdminJudgeByPublicId(publicId);

        // 2) 记录完整联系方式查看审计
        writeAdminLog("JUDGE_CONTACT_VIEW", account.getPublicId(), "查看评审完整联系方式");

        // 3) 返回完整资料
        return toJudgeDetailVO(account, buildPhoneConflictContext(List.of(account)));
    }

    @Override
    public JudgeAccountVO getMyProfile() {
        // 1) 查询当前评审账号
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());

        // 2) 组装个人资料视图
        return toJudgeDetailVO(account, buildPhoneConflictContext(List.of(account)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateMyProfile(JudgeProfileUpdateRequest request) {
        // 1) 查询当前评审账号
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());

        // 2) 更新可自主管理资料；账号注册即激活，资料完整性在报名时校验
        applyProfile(account, request.getWechat(), request.getName(), request.getQualification(),
                request.getBjcpNumber(), request.getBreweryConflictFlag(), request.getBreweryConflictText(),
                account.getReviewRemark());
        if (JudgeAccountStatus.of(account.getStatus()) == JudgeAccountStatus.PENDING_REVIEW) {
            account.setStatus(JudgeAccountStatus.ACTIVE.getCode());
            account.setSubmittedTime(LocalDateTime.now());
            account.setReviewedTime(LocalDateTime.now());
        }
        judgeAccountMapper.updateById(account);

        // 3) 组装并返回结果
        JudgeAccount updated = judgeAccountMapper.selectById(account.getId());
        return toJudgeDetailVO(updated, buildPhoneConflictContext(List.of(updated)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudge(String publicId, AdminJudgeUpdateRequest request) {
        // 1) 查询目标评审
        JudgeAccount account = requireAdminJudgeByPublicId(publicId);

        // 2) 后台更新非手机号资料
        applyProfile(account, request.getWechat(), request.getName(), request.getQualification(),
                request.getBjcpNumber(), request.getBreweryConflictFlag(), request.getBreweryConflictText(),
                request.getReviewRemark());
        judgeAccountMapper.updateById(account);
        writeAdminLog("JUDGE_PROFILE_UPDATE", account.getPublicId(), "更新评审资料");

        // 3) 组装并返回结果
        JudgeAccount updated = judgeAccountMapper.selectById(account.getId());
        return toJudgeDetailVO(updated, buildPhoneConflictContext(List.of(updated)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudgePhone(String publicId, AdminJudgePhoneUpdateRequest request) {
        // 1) 查询目标评审并校验是否已分配
        JudgeAccount account = requireAdminJudgeByPublicId(publicId);
        if (hasAssignments(account.getId())) {
            throw new BaseException("已分配比赛的评审不能直接更正手机号");
        }

        // 2) 校验新手机号唯一性并写入加密字段
        String phone = piiService.normalizePhone(request.getPhone());
        ensurePhoneHashAvailable(piiService.hashPhone(phone), account.getId());
        account.setPhoneEnc(piiService.encrypt(phone));
        account.setPhoneHash(piiService.hashPhone(phone));
        account.setPhoneLast4(piiService.phoneLast4(phone));
        judgeAccountMapper.updateById(account);
        writeAdminLog("JUDGE_PHONE_UPDATE", account.getPublicId(), "更正评审手机号：" + request.getReason());

        // 3) 组装并返回结果
        JudgeAccount updated = judgeAccountMapper.selectById(account.getId());
        return toJudgeDetailVO(updated, buildPhoneConflictContext(List.of(updated)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudgeStatus(String publicId, AdminJudgeStatusUpdateRequest request) {
        // 1) 查询目标评审并校验目标状态
        JudgeAccount account = requireAdminJudgeByPublicId(publicId);
        JudgeAccountStatus currentStatus = JudgeAccountStatus.of(account.getStatus());
        JudgeAccountStatus nextStatus = JudgeAccountStatus.of(request.getStatus());
        validateStatusTransition(currentStatus, nextStatus);

        // 2) 更新审核状态
        account.setStatus(nextStatus.getCode());
        account.setReviewRemark(request.getReviewRemark());
        account.setReviewedTime(LocalDateTime.now());
        account.setReviewedBy(BaseContext.getCurrentId());
        judgeAccountMapper.updateById(account);
        writeAdminLog(resolveStatusAction(nextStatus), account.getPublicId(), "评审状态改为" + nextStatus.getLabel());

        // 3) 组装并返回结果
        JudgeAccount updated = judgeAccountMapper.selectById(account.getId());
        return toJudgeDetailVO(updated, buildPhoneConflictContext(List.of(updated)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJudge(String publicId) {
        JudgeAccount account = requireAdminJudgeByPublicId(publicId);
        Long judgeId = account.getId();
        if (hasAssignments(judgeId)
                || judgeScoreSessionMapper.selectCount(new LambdaQueryWrapper<JudgeScoreSession>()
                .eq(JudgeScoreSession::getJudgeAccountId, judgeId)) > 0
                || roundJudgeRankingDraftMapper.selectCount(new LambdaQueryWrapper<RoundJudgeRankingDraft>()
                .eq(RoundJudgeRankingDraft::getJudgeAccountId, judgeId)) > 0
                || roundTableConfirmationMapper.selectCount(new LambdaQueryWrapper<RoundTableConfirmation>()
                .eq(RoundTableConfirmation::getJudgeAccountId, judgeId)) > 0
                || roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, judgeId)) > 0
                || scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getJudgeAccountId, judgeId)) > 0) {
            throw new BaseException("该评审已有编排或评审记录，不能删除，请先停用");
        }
        judgeAccountMapper.deleteById(judgeId);
        writeAdminLog("JUDGE_DELETE", account.getPublicId(), "删除评审账号");
    }

    @Override
    public void createAssignment(JudgeAssignmentCreateRequest request) {
        // 1) 查询并校验比赛、评审和桌次
        judgeAccessService.requireCompetitionAccess(request.getCompetitionId());
        JudgeAccount judge = requireAdminJudgeByPublicId(request.getJudgePublicId());
        if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new BaseException("只有启用评审可以加入比赛编排");
        }
        if (competitionMapper.selectById(request.getCompetitionId()) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        JudgeTable judgeTable = judgeTableMapper.selectById(request.getTableId());
        if (judgeTable == null || !judgeTable.getCompetitionId().equals(request.getCompetitionId())) {
            throw new BaseException("桌次不存在或不属于当前比赛");
        }

        // 2) 创建或更新本场分配
        JudgeAssignment existing = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, request.getCompetitionId())
                .eq(JudgeAssignment::getJudgeAccountId, judge.getId()));
        if (existing != null) {
            existing.setTableId(request.getTableId());
            existing.setRole(request.getRole().name());
            existing.setStatus("ACTIVE");
            existing.setRecruitmentApplicationId(judgeRecruitmentApplicationMapper
                    .selectAcceptedApplicationId(request.getCompetitionId(), judge.getId()));
            judgeAssignmentMapper.updateById(existing);
            writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", judge.getPublicId(), "更新单个评审编排");
            return;
        }
        judgeAssignmentMapper.insert(JudgeAssignment.builder()
                .competitionId(request.getCompetitionId())
                .judgeAccountId(judge.getId())
                .recruitmentApplicationId(judgeRecruitmentApplicationMapper
                        .selectAcceptedApplicationId(request.getCompetitionId(), judge.getId()))
                .tableId(request.getTableId())
                .role(request.getRole().name())
                .status("ACTIVE")
                .build());
        writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", judge.getPublicId(), "新增单个评审编排");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompetitionAssignments(Long competitionId, JudgeAssignmentBatchUpdateRequest request) {
        // 1) 查询比赛与评审桌上下文
        judgeAccessService.requireCompetitionAccess(competitionId);
        if (competitionMapper.selectById(competitionId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        List<JudgeTable> tables = judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId));
        Map<Long, JudgeTable> tableMap = tables.stream().collect(Collectors.toMap(JudgeTable::getId, item -> item));

        // 2) 校验评审、桌次、单场唯一与桌长唯一
        List<JudgeAssignmentItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        Set<String> assignedJudgePublicIds = new HashSet<>();
        Map<String, JudgeAccount> judgeMap = items.stream()
                .map(JudgeAssignmentItemRequest::getJudgePublicId)
                .distinct()
                .map(this::requireAdminJudgeByPublicId)
                .collect(Collectors.toMap(JudgeAccount::getPublicId, item -> item));
        for (JudgeAssignmentItemRequest item : items) {
            if (!tableMap.containsKey(item.getTableId())) {
                throw new BaseException("评审桌不存在或不属于当前比赛");
            }
            JudgeAccount judge = judgeMap.get(item.getJudgePublicId());
            if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
                throw new BaseException("只有启用评审可以加入比赛编排");
            }
            if (!assignedJudgePublicIds.add(item.getJudgePublicId())) {
                throw new BaseException("同一评审在同一比赛中只能分配一次");
            }
        }
        for (JudgeTable table : tables) {
            long captainCount = items.stream()
                    .filter(item -> table.getId().equals(item.getTableId()))
                    .filter(item -> item.getRole() == JudgeRoleType.CAPTAIN)
                    .count();
            if (captainCount > 1) {
                throw new BaseException(table.getTableName() + "只能有 1 名桌长");
            }
        }

        // 3) 整体替换本场比赛评审编排
        judgeAssignmentMapper.delete(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
        for (JudgeAssignmentItemRequest item : items) {
            JudgeAccount judge = judgeMap.get(item.getJudgePublicId());
            judgeAssignmentMapper.insert(JudgeAssignment.builder()
                    .competitionId(competitionId)
                    .tableId(item.getTableId())
                    .judgeAccountId(judge.getId())
                    .recruitmentApplicationId(judgeRecruitmentApplicationMapper
                            .selectAcceptedApplicationId(competitionId, judge.getId()))
                    .role(item.getRole().name())
                    .status("ACTIVE")
                    .build());
        }
        writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", "COMP-" + competitionId, "整体保存评审编排，人数 " + items.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeRoundTableMembers(Long competitionId, Long roundId, Long roundTableId,
                                        JudgeRoundMemberChangeRequest request) {
        judgeAccessService.requireCompetitionAccess(competitionId);
        CompetitionRound round = competitionRoundMapper.selectById(roundId);
        RoundTable table = roundTableMapper.selectById(roundTableId);
        if (round == null || !Objects.equals(round.getCompetitionId(), competitionId)
                || table == null || !Objects.equals(table.getCompetitionId(), competitionId)
                || !Objects.equals(table.getRoundId(), roundId)) {
            throw new ResourceNotFoundException("评审桌不存在");
        }
        if (!isRoundMemberChangeAllowed(round, table)) {
            throw new BaseException("当前轮次状态不允许调整评委");
        }
        List<String> removeIds = request.getRemoveJudgePublicIds() == null
                ? List.of()
                : request.getRemoveJudgePublicIds().stream().filter(StringUtils::hasText).distinct().toList();
        List<JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest> additions =
                request.getAdd() == null ? List.of() : request.getAdd();
        if (removeIds.isEmpty() && additions.isEmpty()) {
            throw new BaseException("请至少选择一位需要调整的评委");
        }
        Set<String> additionPublicIds = additions.stream()
                .map(JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest::getJudgePublicId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (additionPublicIds.size() != additions.size()) {
            throw new BaseException("新增评委不能重复");
        }
        if (removeIds.stream().anyMatch(additionPublicIds::contains)) {
            throw new BaseException("同一评委不能同时加入和离场");
        }
        String reason = StringUtils.hasText(request.getReason()) ? request.getReason().trim() : "比赛现场评委调整";
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getRoundTableId, roundTableId));
        Map<Long, RoundTableMember> memberByJudge = members.stream()
                .collect(Collectors.toMap(RoundTableMember::getJudgeAccountId, item -> item, (left, right) -> left));

        Set<Long> removedJudgeIds = new HashSet<>();
        for (String publicId : removeIds) {
            JudgeAccount judge = requireAdminJudgeByPublicId(publicId);
            RoundTableMember member = memberByJudge.get(judge.getId());
            if (member == null || !isActiveMember(member)) {
                throw new BaseException("评委不在当前桌：" + publicId);
            }
            member.setStatus("REMOVED");
            member.setSystemTaskRequired(0);
            member.setRemovedTime(LocalDateTime.now());
            member.setRemovedBy(BaseContext.getCurrentId());
            member.setRemoveReason(reason);
            roundTableMemberMapper.updateById(member);
            removedJudgeIds.add(judge.getId());
            markAssignmentWithdrawn(competitionId, judge.getId(), reason);
        }

        for (JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest item : additions) {
            JudgeAccount judge = requireAdminJudgeByPublicId(item.getJudgePublicId());
            if (!isActiveJudge(judge)) {
                throw new BaseException("只有启用评委可以加入当前轮次");
            }
            if (hasActiveMemberInOtherTable(competitionId, roundId, roundTableId, judge.getId())) {
                throw new BaseException("同一轮同一评委只能分配到一张桌");
            }
            String role = resolveLiveMemberRole(round, item, request.getCaptainJudgePublicId());
            RoundTableMember existing = memberByJudge.get(judge.getId());
            if (existing != null) {
                if (isActiveMember(existing)
                        && !JudgeRoleType.CAPTAIN.name().equals(role)) {
                    throw new BaseException("评委已在当前桌");
                }
                existing.setStatus("ACTIVE");
                existing.setRole(role);
                existing.setSystemTaskRequired(requiresSystemTask(round, role) ? 1 : 0);
                existing.setRemovedTime(null);
                existing.setRemovedBy(null);
                existing.setRemoveReason(null);
                roundTableMemberMapper.updateById(existing);
                roundTableMemberMapper.update(null, new LambdaUpdateWrapper<RoundTableMember>()
                        .eq(RoundTableMember::getId, existing.getId())
                        .set(RoundTableMember::getRemovedTime, null)
                        .set(RoundTableMember::getRemovedBy, null)
                        .set(RoundTableMember::getRemoveReason, null));
            } else {
                roundTableMemberMapper.insert(RoundTableMember.builder()
                        .roundTableId(roundTableId)
                        .judgeAccountId(judge.getId())
                        .role(role)
                        .systemTaskRequired(requiresSystemTask(round, role) ? 1 : 0)
                        .status("ACTIVE")
                        .build());
            }
            reactivateAssignment(competitionId, judge.getId(), table, role);
        }

        if (request.getCaptainJudgePublicId() != null) {
            JudgeAccount captain = requireAdminJudgeByPublicId(request.getCaptainJudgePublicId());
            if (!isActiveJudge(captain)) {
                throw new BaseException("桌长必须是启用评委");
            }
            RoundTableMember captainMember = roundTableMemberMapper.selectOne(new LambdaQueryWrapper<RoundTableMember>()
                    .eq(RoundTableMember::getRoundTableId, roundTableId)
                    .eq(RoundTableMember::getJudgeAccountId, captain.getId())
                    .last("LIMIT 1"));
            if (!isActiveMember(captainMember)) {
                throw new BaseException("新桌长必须先加入当前桌");
            }
            boolean hasOtherCaptain = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                            .eq(RoundTableMember::getRoundTableId, roundTableId)
                            .eq(RoundTableMember::getRole, JudgeRoleType.CAPTAIN.name()))
                    .stream()
                    .anyMatch(member -> !Objects.equals(member.getJudgeAccountId(), captain.getId())
                            && isActiveMember(member));
            if (hasOtherCaptain) {
                throw new BaseException("更换桌长时需同时将原桌长标记离场");
            }
            captainMember.setRole(JudgeRoleType.CAPTAIN.name());
            captainMember.setSystemTaskRequired(1);
            roundTableMemberMapper.updateById(captainMember);
            table.setCaptainJudgeId(captain.getId());
            roundTableMapper.updateById(table);
        } else if (removedJudgeIds.contains(table.getCaptainJudgeId())) {
            table.setCaptainJudgeId(null);
            roundTableMapper.updateById(table);
        }
        writeAdminLog("JUDGE_ROUND_MEMBER_CHANGE", "COMP-" + competitionId,
                "调整轮次评委，round=" + roundId + ", table=" + roundTableId);
        scoreConfirmationService.refreshAfterMemberChange(roundTableId);
    }

    private boolean isRoundMemberChangeAllowed(CompetitionRound round, RoundTable table) {
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return RoundStatus.PUBLISHED.name().equals(round.getStatus())
                    && RoundStatus.PUBLISHED.name().equals(table.getStatus());
        }
        return RoundType.RANKING.name().equals(round.getRoundType())
                && RoundStatus.IN_PROGRESS.name().equals(round.getStatus())
                && RoundStatus.IN_PROGRESS.name().equals(table.getStatus());
    }

    private boolean isActiveMember(RoundTableMember member) {
        return member != null && !"REMOVED".equalsIgnoreCase(member.getStatus());
    }

    private String resolveLiveMemberRole(CompetitionRound round,
                                         JudgeRoundMemberChangeRequest.JudgeRoundMemberItemRequest item,
                                         String captainPublicId) {
        boolean captain = Objects.equals(item.getJudgePublicId(), captainPublicId);
        if (item.getRole() == JudgeRoleType.CAPTAIN && !captain) {
            throw new BaseException("设置桌长时必须明确指定新桌长");
        }
        if (captain) {
            return JudgeRoleType.CAPTAIN.name();
        }
        if (RoundType.SCORE.name().equals(round.getRoundType()) && item.getRole() == JudgeRoleType.CROSS) {
            return JudgeRoleType.CROSS.name();
        }
        return JudgeRoleType.PROFESSIONAL.name();
    }

    private boolean requiresSystemTask(CompetitionRound round, String role) {
        return RoundType.SCORE.name().equals(round.getRoundType())
                || JudgeRoleType.CAPTAIN.name().equals(role);
    }

    private boolean isActiveJudge(JudgeAccount judge) {
        return judge != null && JudgeAccountStatus.of(judge.getStatus()) == JudgeAccountStatus.ACTIVE;
    }

    private boolean hasActiveMemberInOtherTable(Long competitionId, Long roundId, Long roundTableId, Long judgeId) {
        List<RoundTable> tables = roundTableMapper.selectList(new LambdaQueryWrapper<RoundTable>()
                .eq(RoundTable::getCompetitionId, competitionId)
                .eq(RoundTable::getRoundId, roundId)
                .ne(RoundTable::getId, roundTableId));
        if (tables.isEmpty()) return false;
        return roundTableMemberMapper.selectCount(new LambdaQueryWrapper<RoundTableMember>()
                .in(RoundTableMember::getRoundTableId, tables.stream().map(RoundTable::getId).toList())
                .eq(RoundTableMember::getJudgeAccountId, judgeId)
                .ne(RoundTableMember::getStatus, "REMOVED")) > 0;
    }

    private void markAssignmentWithdrawn(Long competitionId, Long judgeId, String reason) {
        JudgeAssignment assignment = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId)
                .eq(JudgeAssignment::getJudgeAccountId, judgeId)
                .last("LIMIT 1"));
        if (assignment != null) {
            assignment.setStatus("WITHDRAWN");
            assignment.setWithdrawnTime(LocalDateTime.now());
            assignment.setWithdrawnBy(BaseContext.getCurrentId());
            assignment.setWithdrawReason(reason);
            judgeAssignmentMapper.updateById(assignment);
        }
    }

    private void reactivateAssignment(Long competitionId, Long judgeId, RoundTable table, String role) {
        JudgeAssignment assignment = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId)
                .eq(JudgeAssignment::getJudgeAccountId, judgeId)
                .last("LIMIT 1"));
        if (assignment == null) {
            JudgeTable base = judgeTableMapper.selectOne(new LambdaQueryWrapper<JudgeTable>()
                    .eq(JudgeTable::getCompetitionId, competitionId)
                    .eq(JudgeTable::getTableName, table.getTableName())
                    .last("LIMIT 1"));
            if (base == null) {
                base = JudgeTable.builder().competitionId(competitionId).tableName(table.getTableName()).sortOrder(table.getSortOrder()).build();
                judgeTableMapper.insert(base);
            }
            assignment = JudgeAssignment.builder().competitionId(competitionId).judgeAccountId(judgeId)
                    .recruitmentApplicationId(judgeRecruitmentApplicationMapper
                            .selectAcceptedApplicationId(competitionId, judgeId))
                    .tableId(base.getId()).role(role).status("ACTIVE").build();
            judgeAssignmentMapper.insert(assignment);
            return;
        }
        assignment.setStatus("ACTIVE");
        assignment.setTableId(resolveBaseTableId(competitionId, table));
        assignment.setRole(role);
        assignment.setWithdrawnTime(null);
        assignment.setWithdrawnBy(null);
        assignment.setWithdrawReason(null);
        judgeAssignmentMapper.updateById(assignment);
        judgeAssignmentMapper.update(null, new LambdaUpdateWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getId, assignment.getId())
                .set(JudgeAssignment::getWithdrawnTime, null)
                .set(JudgeAssignment::getWithdrawnBy, null)
                .set(JudgeAssignment::getWithdrawReason, null));
    }

    private Long resolveBaseTableId(Long competitionId, RoundTable table) {
        JudgeTable base = judgeTableMapper.selectOne(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId)
                .eq(JudgeTable::getTableName, table.getTableName())
                .last("LIMIT 1"));
        if (base != null) return base.getId();
        base = JudgeTable.builder().competitionId(competitionId).tableName(table.getTableName()).sortOrder(table.getSortOrder()).build();
        judgeTableMapper.insert(base);
        return base.getId();
    }

    @Override
    public List<CompetitionVO> listMyCompetitions() {
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            return List.of();
        }
        return judgeRoundTaskService.listMyTasks()
                .stream()
                .map(this::toCompetitionVO)
                .toList();
    }

    private CompetitionVO toCompetitionVO(JudgeTaskVO task) {
        Competition competition = competitionMapper.selectById(task.getCompetitionId());
        if (competition == null) {
            return CompetitionVO.builder().build();
        }
        return CompetitionVO.builder()
                .id(competition.getId())
                .name(competition.getName())
                .competitionDate(competition.getCompetitionDate())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .judgeRoleType(task.getJudgeRoleType())
                .roleLabel(task.getRoleLabel())
                .tableName(task.getTableName())
                .build();
    }

    private CompetitionVO toCompetitionVO(Competition competition, JudgeAssignment assignment, JudgeTable table) {
        if (competition == null) {
            return CompetitionVO.builder().build();
        }
        return CompetitionVO.builder()
                .id(competition.getId())
                .name(competition.getName())
                .competitionDate(competition.getCompetitionDate())
                .registrationDeadline(competition.getRegistrationDeadline())
                .status(competition.getStatus())
                .entryFee(competition.getEntryFee())
                .judgeRoleType(assignment.getRole())
                .roleLabel(roleLabel(assignment.getRole()))
                .tableName(table == null ? null : table.getTableName())
                .build();
    }

    private JudgeAccount requireJudge(Long id) {
        JudgeAccount account = judgeAccountMapper.selectById(id);
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        return account;
    }

    private JudgeAccount requireAdminJudgeByPublicId(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new BaseException("评审不能为空");
        }
        JudgeAccount account = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPublicId, publicId));
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        judgeAccessService.requireJudgeAccess(account.getId());
        return account;
    }

    private void ensurePhoneHashAvailable(String phoneHash, Long currentId) {
        JudgeAccount existing = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPhoneHash, phoneHash));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BaseException("手机号已被其他评审使用");
        }
    }

    private boolean hasAssignments(Long judgeAccountId) {
        return judgeAssignmentMapper.selectCount(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getJudgeAccountId, judgeAccountId)) > 0;
    }

    private void applyProfile(JudgeAccount account, String wechat, String name, String qualification,
                              String bjcpNumber, Boolean breweryConflictFlag, String breweryConflictText,
                              String reviewRemark) {
        boolean hasBreweryConflict = Boolean.TRUE.equals(breweryConflictFlag);
        account.setWechatEnc(StringUtils.hasText(wechat) ? piiService.encrypt(wechat.trim()) : null);
        account.setName(name.trim());
        account.setQualification(qualification.trim());
        account.setBjcpNumber(StringUtils.hasText(bjcpNumber) ? bjcpNumber.trim() : null);
        account.setBreweryConflictFlag(hasBreweryConflict);
        account.setBreweryConflictText(hasBreweryConflict && StringUtils.hasText(breweryConflictText)
                ? breweryConflictText.trim()
                : null);
        account.setReviewRemark(reviewRemark);
    }

    private void validateStatusTransition(JudgeAccountStatus currentStatus, JudgeAccountStatus nextStatus) {
        if (nextStatus == JudgeAccountStatus.PROFILE_INCOMPLETE) {
            throw new BaseException("后台不能将评审改为资料未完善");
        }
        if (currentStatus == JudgeAccountStatus.PROFILE_INCOMPLETE && nextStatus == JudgeAccountStatus.ACTIVE) {
            throw new BaseException("资料未完善的评审不能直接启用");
        }
    }

    private LambdaQueryWrapper<JudgeAccount> buildJudgeQuery(Integer status,
                                                             String keyword,
                                                             JudgeAccessService.JudgeScope scope) {
        String query = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String digits = query == null ? "" : query.replaceAll("\\D", "");
        LambdaQueryWrapper<JudgeAccount> wrapper = new LambdaQueryWrapper<JudgeAccount>()
                .ne(JudgeAccount::getStatus, JudgeAccountStatus.PROFILE_INCOMPLETE.getCode());
        if (!scope.all()) {
            if (scope.judgeIds().isEmpty()) {
                wrapper.eq(JudgeAccount::getId, -1L);
            } else {
                wrapper.in(JudgeAccount::getId, scope.judgeIds());
            }
        }
        if (status != null) {
            JudgeAccountStatus.of(status);
            wrapper.eq(JudgeAccount::getStatus, status);
        }
        if (StringUtils.hasText(query)) {
            wrapper.and(item -> {
                item.like(JudgeAccount::getName, query)
                        .or().like(JudgeAccount::getQualification, query)
                        .or().like(JudgeAccount::getBjcpNumber, query)
                        .or().like(JudgeAccount::getBreweryConflictText, query);
                if (digits.length() == 11) {
                    item.or().eq(JudgeAccount::getPhoneHash, piiService.hashPhone(digits));
                }
                if (digits.length() == 4) {
                    item.or().eq(JudgeAccount::getPhoneLast4, digits);
                }
            });
        }
        return wrapper;
    }

    private PhoneConflictContext buildPhoneConflictContext(List<JudgeAccount> judges) {
        Set<String> phones = judges.stream()
                .map(judge -> piiService.normalizePhone(piiService.decrypt(judge.getPhoneEnc())))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (phones.isEmpty()) {
            return new PhoneConflictContext(Map.of(), Map.of());
        }
        List<PortalAccount> portalAccounts = portalAccountMapper.selectList(new LambdaQueryWrapper<PortalAccount>()
                .in(PortalAccount::getPhone, phones));
        Map<String, PortalAccount> portalByPhone = portalAccounts.stream()
                .collect(Collectors.toMap(account -> piiService.normalizePhone(account.getPhone()), item -> item, (left, right) -> left));
        Set<Long> breweryIds = portalAccounts.stream()
                .map(PortalAccount::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Brewery> breweryById = breweryIds.isEmpty()
                ? Map.of()
                : breweryMapper.selectBatchIds(breweryIds).stream()
                .collect(Collectors.toMap(Brewery::getId, item -> item));
        return new PhoneConflictContext(portalByPhone, breweryById);
    }

    private PhoneBreweryConflict resolvePhoneBreweryConflict(String phone, PhoneConflictContext context) {
        PortalAccount portalAccount = context.portalByPhone().get(piiService.normalizePhone(phone));
        if (portalAccount == null || portalAccount.getBreweryId() == null) {
            return PhoneBreweryConflict.empty();
        }
        Brewery brewery = context.breweryById().get(portalAccount.getBreweryId());
        String breweryName = brewery == null ? null : brewery.getCompanyName();
        String text = StringUtils.hasText(breweryName)
                ? "手机号匹配厂商账号：" + breweryName
                : "手机号匹配厂商账号";
        return new PhoneBreweryConflict(true, portalAccount.getBreweryId(), breweryName, text);
    }

    private JudgeAccountVO toJudgeListVO(JudgeAccount judge, PhoneConflictContext phoneConflictContext) {
        JudgeAccountStatus status = JudgeAccountStatus.of(judge.getStatus());
        String phone = piiService.decrypt(judge.getPhoneEnc());
        String wechat = piiService.decrypt(judge.getWechatEnc());
        PhoneBreweryConflict phoneConflict = resolvePhoneBreweryConflict(phone, phoneConflictContext);
        return JudgeAccountVO.builder()
                .publicId(judge.getPublicId())
                .name(judge.getName())
                .maskedPhone(piiService.maskPhone(phone))
                .maskedWechat(piiService.maskWechat(wechat))
                .qualification(judge.getQualification())
                .bjcpNumber(judge.getBjcpNumber())
                .breweryConflictFlag(Boolean.TRUE.equals(judge.getBreweryConflictFlag()))
                .breweryConflictText(judge.getBreweryConflictText())
                .phoneBreweryConflictFlag(phoneConflict.flag())
                .phoneConflictBreweryId(phoneConflict.breweryId())
                .phoneConflictBreweryName(phoneConflict.breweryName())
                .phoneConflictText(phoneConflict.text())
                .status(status.getCode())
                .statusLabel(status.getLabel())
                .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                .reviewRemark(judge.getReviewRemark())
                .build();
    }

    private JudgeAccountVO toJudgeDetailVO(JudgeAccount judge, PhoneConflictContext phoneConflictContext) {
        JudgeAccountStatus status = JudgeAccountStatus.of(judge.getStatus());
        String phone = piiService.decrypt(judge.getPhoneEnc());
        String wechat = piiService.decrypt(judge.getWechatEnc());
        PhoneBreweryConflict phoneConflict = resolvePhoneBreweryConflict(phone, phoneConflictContext);
        return JudgeAccountVO.builder()
                .publicId(judge.getPublicId())
                .name(judge.getName())
                .phone(phone)
                .wechat(wechat)
                .maskedPhone(piiService.maskPhone(phone))
                .maskedWechat(piiService.maskWechat(wechat))
                .qualification(judge.getQualification())
                .bjcpNumber(judge.getBjcpNumber())
                .breweryConflictFlag(Boolean.TRUE.equals(judge.getBreweryConflictFlag()))
                .breweryConflictText(judge.getBreweryConflictText())
                .phoneBreweryConflictFlag(phoneConflict.flag())
                .phoneConflictBreweryId(phoneConflict.breweryId())
                .phoneConflictBreweryName(phoneConflict.breweryName())
                .phoneConflictText(phoneConflict.text())
                .status(status.getCode())
                .statusLabel(status.getLabel())
                .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                .reviewRemark(judge.getReviewRemark())
                .build();
    }

    private String resolveStatusAction(JudgeAccountStatus status) {
        if (status == JudgeAccountStatus.ACTIVE) {
            return "JUDGE_APPROVE_OR_ENABLE";
        }
        if (status == JudgeAccountStatus.DISABLED) {
            return "JUDGE_DISABLE";
        }
        return "JUDGE_STATUS_UPDATE";
    }

    private void writeAdminLog(String action, String targetPublicId, String summary) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetType(TARGET_JUDGE)
                .targetPublicId(targetPublicId)
                .summary(summary)
                .build());
    }

    private String roleLabel(String role) {
        if (JudgeRoleType.CAPTAIN.name().equals(role)) {
            return "桌长";
        }
        if (JudgeRoleType.PROFESSIONAL.name().equals(role)) {
            return "专业评审";
        }
        if (JudgeRoleType.CROSS.name().equals(role)) {
            return "跨界评审";
        }
        return role;
    }

    private record PhoneConflictContext(Map<String, PortalAccount> portalByPhone, Map<Long, Brewery> breweryById) {
    }

    private record PhoneBreweryConflict(Boolean flag, Long breweryId, String breweryName, String text) {

        private static PhoneBreweryConflict empty() {
            return new PhoneBreweryConflict(false, null, null, null);
        }
    }
}
