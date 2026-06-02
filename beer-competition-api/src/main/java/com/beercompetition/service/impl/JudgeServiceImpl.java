package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.pojo.dto.AdminJudgePhoneUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentItemRequest;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final String TARGET_JUDGE = "JUDGE";

    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final CompetitionMapper competitionMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final PiiService piiService;

    @Override
    public List<JudgeAccountVO> listJudges(Integer status, String keyword) {
        // 1) 参数规范化与状态过滤
        String query = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String digits = query == null ? "" : query.replaceAll("\\D", "");
        LambdaQueryWrapper<JudgeAccount> wrapper = new LambdaQueryWrapper<JudgeAccount>()
                .ne(JudgeAccount::getStatus, JudgeAccountStatus.PROFILE_INCOMPLETE.getCode());
        if (status != null) {
            JudgeAccountStatus.of(status);
            wrapper.eq(JudgeAccount::getStatus, status);
        }
        if (StringUtils.hasText(query)) {
            wrapper.and(item -> {
                item.like(JudgeAccount::getName, query)
                        .or().like(JudgeAccount::getQualification, query);
                if (digits.length() == 11) {
                    item.or().eq(JudgeAccount::getPhoneHash, piiService.hashPhone(digits));
                }
                if (digits.length() == 4) {
                    item.or().eq(JudgeAccount::getPhoneLast4, digits);
                }
            });
        }

        // 2) 查询并组装脱敏评审池
        return judgeAccountMapper.selectList(wrapper.orderByDesc(JudgeAccount::getId))
                .stream()
                .map(this::toJudgeListVO)
                .toList();
    }

    @Override
    public JudgeAccountVO getJudgeDetail(String publicId) {
        // 1) 查询评审详情
        JudgeAccount account = requireJudgeByPublicId(publicId);

        // 2) 记录完整联系方式查看审计
        writeAdminLog("JUDGE_CONTACT_VIEW", account.getPublicId(), "查看评审完整联系方式");

        // 3) 返回完整资料
        return toJudgeDetailVO(account);
    }

    @Override
    public JudgeAccountVO getMyProfile() {
        // 1) 查询当前评审账号
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());

        // 2) 组装个人资料视图
        return toJudgeDetailVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateMyProfile(JudgeProfileUpdateRequest request) {
        // 1) 查询当前评审账号
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());

        // 2) 更新可自主管理资料，首次完善后进入待审核
        applyProfile(account, request.getWechat(), request.getName(), request.getQualification(), account.getReviewRemark());
        if (JudgeAccountStatus.of(account.getStatus()) == JudgeAccountStatus.PROFILE_INCOMPLETE) {
            account.setStatus(JudgeAccountStatus.PENDING_REVIEW.getCode());
            account.setSubmittedTime(LocalDateTime.now());
        }
        judgeAccountMapper.updateById(account);

        // 3) 组装并返回结果
        return toJudgeDetailVO(judgeAccountMapper.selectById(account.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudge(String publicId, AdminJudgeUpdateRequest request) {
        // 1) 查询目标评审
        JudgeAccount account = requireJudgeByPublicId(publicId);

        // 2) 后台更新非手机号资料
        applyProfile(account, request.getWechat(), request.getName(), request.getQualification(), request.getReviewRemark());
        judgeAccountMapper.updateById(account);
        writeAdminLog("JUDGE_PROFILE_UPDATE", account.getPublicId(), "更新评审资料");

        // 3) 组装并返回结果
        return toJudgeDetailVO(judgeAccountMapper.selectById(account.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudgePhone(String publicId, AdminJudgePhoneUpdateRequest request) {
        // 1) 查询目标评审并校验是否已分配
        JudgeAccount account = requireJudgeByPublicId(publicId);
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
        return toJudgeDetailVO(judgeAccountMapper.selectById(account.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudgeStatus(String publicId, AdminJudgeStatusUpdateRequest request) {
        // 1) 查询目标评审并校验目标状态
        JudgeAccount account = requireJudgeByPublicId(publicId);
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
        return toJudgeDetailVO(judgeAccountMapper.selectById(account.getId()));
    }

    @Override
    public void createAssignment(JudgeAssignmentCreateRequest request) {
        // 1) 查询并校验比赛、评审和桌次
        JudgeAccount judge = requireJudgeByPublicId(request.getJudgePublicId());
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
            judgeAssignmentMapper.updateById(existing);
            writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", judge.getPublicId(), "更新单个评审编排");
            return;
        }
        judgeAssignmentMapper.insert(JudgeAssignment.builder()
                .competitionId(request.getCompetitionId())
                .judgeAccountId(judge.getId())
                .tableId(request.getTableId())
                .role(request.getRole().name())
                .build());
        writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", judge.getPublicId(), "新增单个评审编排");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompetitionAssignments(Long competitionId, JudgeAssignmentBatchUpdateRequest request) {
        // 1) 查询比赛与评审桌上下文
        if (competitionMapper.selectById(competitionId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        List<JudgeTable> tables = judgeTableMapper.selectList(new LambdaQueryWrapper<JudgeTable>()
                .eq(JudgeTable::getCompetitionId, competitionId));
        Map<Long, JudgeTable> tableMap = tables.stream().collect(Collectors.toMap(JudgeTable::getId, item -> item));

        // 2) 校验评审、桌次、单场唯一与每桌桌长
        Set<String> assignedJudgePublicIds = new HashSet<>();
        Map<String, JudgeAccount> judgeMap = request.getItems().stream()
                .map(JudgeAssignmentItemRequest::getJudgePublicId)
                .distinct()
                .map(this::requireJudgeByPublicId)
                .collect(Collectors.toMap(JudgeAccount::getPublicId, item -> item));
        for (JudgeAssignmentItemRequest item : request.getItems()) {
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
            long captainCount = request.getItems().stream()
                    .filter(item -> table.getId().equals(item.getTableId()))
                    .filter(item -> item.getRole() == JudgeRoleType.CAPTAIN)
                    .count();
            if (captainCount != 1) {
                throw new BaseException(table.getTableName() + "必须有且只有 1 名桌长");
            }
        }

        // 3) 整体替换本场比赛评审编排
        judgeAssignmentMapper.delete(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, competitionId));
        for (JudgeAssignmentItemRequest item : request.getItems()) {
            JudgeAccount judge = judgeMap.get(item.getJudgePublicId());
            judgeAssignmentMapper.insert(JudgeAssignment.builder()
                    .competitionId(competitionId)
                    .tableId(item.getTableId())
                    .judgeAccountId(judge.getId())
                    .role(item.getRole().name())
                    .build());
        }
        writeAdminLog("JUDGE_ASSIGNMENT_UPDATE", "COMP-" + competitionId, "整体保存评审编排，人数 " + request.getItems().size());
    }

    @Override
    public List<CompetitionVO> listMyCompetitions() {
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            return List.of();
        }
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getJudgeAccountId, BaseContext.getCurrentId()));
        Set<Long> competitionIds = assignments.stream().map(JudgeAssignment::getCompetitionId).collect(Collectors.toSet());
        if (competitionIds.isEmpty()) {
            return List.of();
        }
        return assignments.stream()
                .map(assignment -> {
                    Competition competition = competitionMapper.selectById(assignment.getCompetitionId());
                    JudgeTable table = judgeTableMapper.selectById(assignment.getTableId());
                    return toCompetitionVO(competition, assignment, table);
                })
                .filter(item -> item.getId() != null)
                .toList();
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

    private JudgeAccount requireJudgeByPublicId(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new BaseException("评审不能为空");
        }
        JudgeAccount account = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPublicId, publicId));
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
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

    private void applyProfile(JudgeAccount account, String wechat, String name, String qualification, String reviewRemark) {
        account.setWechatEnc(StringUtils.hasText(wechat) ? piiService.encrypt(wechat.trim()) : null);
        account.setName(name.trim());
        account.setQualification(qualification.trim());
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

    private JudgeAccountVO toJudgeListVO(JudgeAccount judge) {
        JudgeAccountStatus status = JudgeAccountStatus.of(judge.getStatus());
        String phone = piiService.decrypt(judge.getPhoneEnc());
        String wechat = piiService.decrypt(judge.getWechatEnc());
        return JudgeAccountVO.builder()
                .publicId(judge.getPublicId())
                .name(judge.getName())
                .maskedPhone(piiService.maskPhone(phone))
                .maskedWechat(piiService.maskWechat(wechat))
                .qualification(judge.getQualification())
                .status(status.getCode())
                .statusLabel(status.getLabel())
                .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                .reviewRemark(judge.getReviewRemark())
                .build();
    }

    private JudgeAccountVO toJudgeDetailVO(JudgeAccount judge) {
        JudgeAccountStatus status = JudgeAccountStatus.of(judge.getStatus());
        String phone = piiService.decrypt(judge.getPhoneEnc());
        String wechat = piiService.decrypt(judge.getWechatEnc());
        return JudgeAccountVO.builder()
                .publicId(judge.getPublicId())
                .name(judge.getName())
                .phone(phone)
                .wechat(wechat)
                .maskedPhone(piiService.maskPhone(phone))
                .maskedWechat(piiService.maskWechat(wechat))
                .qualification(judge.getQualification())
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
}
