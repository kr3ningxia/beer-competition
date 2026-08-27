package com.beercompetition.judging.access;

import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台评委资源范围校验。
 *
 * <p>评委账号目前是跨比赛复用的全局账号，当前阶段通过正式比赛分配关系追溯
 * 主办方范围。裁判招募申请表落地后，只需在这里合并申请关系，不把组织判断散落
 * 到评委管理门面中。</p>
 */
@Service
@RequiredArgsConstructor
public class JudgeAccessService {

    private final CompetitionAccessService competitionAccessService;
    private final JudgeAssignmentMapper judgeAssignmentMapper;

    /**
     * 返回当前后台账号可访问的评委范围。
     *
     * <p>平台超级管理员的全量范围用 {@code all=true} 表示，避免构造一个可能
     * 过大的 ID 集合；其他身份始终返回 SQL 可用的有限 ID 集合。</p>
     */
    public JudgeScope currentScope() {
        if (competitionAccessService.canAccessAllOrganizers()) {
            return new JudgeScope(true, Set.of());
        }
        Long organizerId = competitionAccessService.requireCurrentOrganizerId();
        List<Long> judgeIds = judgeAssignmentMapper.selectJudgeIdsByOrganizer(organizerId);
        return new JudgeScope(false, judgeIds == null ? Set.of() : new LinkedHashSet<>(judgeIds));
    }

    /**
     * 要求当前后台账号可以访问指定评委。
     */
    public void requireJudgeAccess(Long judgeAccountId) {
        if (judgeAccountId == null) {
            throw new ForbiddenException("评委资源未关联有效账号");
        }
        JudgeScope scope = currentScope();
        if (!scope.all() && !scope.judgeIds().contains(judgeAccountId)) {
            throw new ForbiddenException("当前账号无权访问该评委");
        }
    }

    /**
     * 要求当前后台账号可以管理比赛及其评委编排。
     */
    public void requireCompetitionAccess(Long competitionId) {
        competitionAccessService.requireCompetitionAccess(competitionId);
    }

    public record JudgeScope(boolean all, Set<Long> judgeIds) {
    }
}
