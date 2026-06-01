package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.pojo.dto.AdminJudgeStatusUpdateRequest;
import com.beercompetition.pojo.dto.AdminJudgeUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentBatchUpdateRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentItemRequest;
import com.beercompetition.pojo.dto.JudgeAssignmentCreateRequest;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
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

    private final JudgeAccountMapper judgeAccountMapper;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final JudgeTableMapper judgeTableMapper;
    private final CompetitionMapper competitionMapper;

    @Override
    public List<JudgeAccountVO> listJudges(Integer status, String keyword) {
        // 1) 参数规范化与状态过滤
        String query = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LambdaQueryWrapper<JudgeAccount> wrapper = new LambdaQueryWrapper<JudgeAccount>()
                .ne(JudgeAccount::getStatus, JudgeAccountStatus.PROFILE_INCOMPLETE.getCode());
        if (status != null) {
            JudgeAccountStatus.of(status);
            wrapper.eq(JudgeAccount::getStatus, status);
        }
        if (StringUtils.hasText(query)) {
            wrapper.and(item -> item.like(JudgeAccount::getName, query)
                    .or().like(JudgeAccount::getPhone, query)
                    .or().like(JudgeAccount::getWechat, query)
                    .or().like(JudgeAccount::getQualification, query));
        }

        // 2) 查询并组装评审池
        return judgeAccountMapper.selectList(wrapper.orderByDesc(JudgeAccount::getId))
                .stream()
                .map(this::toJudgeAccountVO)
                .toList();
    }

    @Override
    public JudgeAccountVO getMyProfile() {
        // 1) 查询当前评审账号
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());

        // 2) 组装资料视图
        return toJudgeAccountVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateMyProfile(JudgeProfileUpdateRequest request) {
        // 1) 查询当前评审账号并校验手机号唯一性
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());
        ensurePhoneAvailable(request.getPhone(), account.getId());

        // 2) 更新资料，首次完善后进入待审核
        applyProfile(account, request.getPhone(), request.getWechat(), request.getName(), request.getQualification());
        if (JudgeAccountStatus.of(account.getStatus()) == JudgeAccountStatus.PROFILE_INCOMPLETE) {
            account.setStatus(JudgeAccountStatus.PENDING_REVIEW.getCode());
            account.setSubmittedTime(LocalDateTime.now());
        }
        judgeAccountMapper.updateById(account);

        // 3) 组装并返回结果
        return toJudgeAccountVO(judgeAccountMapper.selectById(account.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudge(Long id, AdminJudgeUpdateRequest request) {
        // 1) 查询目标评审并校验手机号唯一性
        JudgeAccount account = requireJudge(id);
        ensurePhoneAvailable(request.getPhone(), id);

        // 2) 后台更新资料
        applyProfile(account, request.getPhone(), request.getWechat(), request.getName(), request.getQualification());
        judgeAccountMapper.updateById(account);

        // 3) 组装并返回结果
        return toJudgeAccountVO(judgeAccountMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeAccountVO updateJudgeStatus(Long id, AdminJudgeStatusUpdateRequest request) {
        // 1) 查询目标评审并校验目标状态
        JudgeAccount account = requireJudge(id);
        JudgeAccountStatus nextStatus = JudgeAccountStatus.of(request.getStatus());
        if (nextStatus == JudgeAccountStatus.PROFILE_INCOMPLETE) {
            throw new BaseException("后台不能将评审改为资料未完善");
        }

        // 2) 更新审核状态
        account.setStatus(nextStatus.getCode());
        account.setReviewRemark(request.getReviewRemark());
        account.setReviewedTime(LocalDateTime.now());
        account.setReviewedBy(BaseContext.getCurrentId());
        judgeAccountMapper.updateById(account);

        // 3) 组装并返回结果
        return toJudgeAccountVO(judgeAccountMapper.selectById(id));
    }

    @Override
    public void createAssignment(JudgeAssignmentCreateRequest request) {
        JudgeAccount judge = judgeAccountMapper.selectById(request.getJudgeAccountId());
        if (judge == null) {
            throw new ResourceNotFoundException("评审不存在");
        }
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
        JudgeAssignment existing = judgeAssignmentMapper.selectOne(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getCompetitionId, request.getCompetitionId())
                .eq(JudgeAssignment::getJudgeAccountId, request.getJudgeAccountId()));
        if (existing != null) {
            existing.setTableId(request.getTableId());
            existing.setRole(request.getRole().name());
            judgeAssignmentMapper.updateById(existing);
            return;
        }
        judgeAssignmentMapper.insert(JudgeAssignment.builder()
                .competitionId(request.getCompetitionId())
                .judgeAccountId(request.getJudgeAccountId())
                .tableId(request.getTableId())
                .role(request.getRole().name())
                .build());
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
        Set<Long> assignedJudgeIds = new HashSet<>();
        for (JudgeAssignmentItemRequest item : request.getItems()) {
            if (!tableMap.containsKey(item.getTableId())) {
                throw new BaseException("评审桌不存在或不属于当前比赛");
            }
            JudgeAccount judge = requireJudge(item.getJudgeAccountId());
            if (JudgeAccountStatus.of(judge.getStatus()) != JudgeAccountStatus.ACTIVE) {
                throw new BaseException("只有启用评审可以加入比赛编排");
            }
            if (!assignedJudgeIds.add(item.getJudgeAccountId())) {
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
            judgeAssignmentMapper.insert(JudgeAssignment.builder()
                    .competitionId(competitionId)
                    .tableId(item.getTableId())
                    .judgeAccountId(item.getJudgeAccountId())
                    .role(item.getRole().name())
                    .build());
        }
    }

    @Override
    public List<CompetitionVO> listMyCompetitions() {
        JudgeAccount account = requireJudge(BaseContext.getCurrentId());
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            return List.of();
        }
        List<JudgeAssignment> assignments = judgeAssignmentMapper.selectList(new LambdaQueryWrapper<JudgeAssignment>()
                .eq(JudgeAssignment::getJudgeAccountId, BaseContext.getCurrentId()));
        Set<Long> competitionIds = assignments.stream().map(JudgeAssignment::getCompetitionId).collect(java.util.stream.Collectors.toSet());
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

    private void ensurePhoneAvailable(String phone, Long currentId) {
        JudgeAccount existing = judgeAccountMapper.selectOne(new LambdaQueryWrapper<JudgeAccount>()
                .eq(JudgeAccount::getPhone, phone));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BaseException("手机号已被其他评审使用");
        }
    }

    private void applyProfile(JudgeAccount account, String phone, String wechat, String name, String qualification) {
        account.setPhone(phone);
        account.setWechat(StringUtils.hasText(wechat) ? wechat.trim() : "");
        account.setName(name.trim());
        account.setQualification(qualification.trim());
    }

    private JudgeAccountVO toJudgeAccountVO(JudgeAccount judge) {
        JudgeAccountStatus status = JudgeAccountStatus.of(judge.getStatus());
        return JudgeAccountVO.builder()
                .id(judge.getId())
                .name(judge.getName())
                .phone(judge.getPhone())
                .wechat(judge.getWechat())
                .qualification(judge.getQualification())
                .status(status.getCode())
                .statusLabel(status.getLabel())
                .profileRequired(status == JudgeAccountStatus.PROFILE_INCOMPLETE)
                .reviewRemark(judge.getReviewRemark())
                .build();
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
