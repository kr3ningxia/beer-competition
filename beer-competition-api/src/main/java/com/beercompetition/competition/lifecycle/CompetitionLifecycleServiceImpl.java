package com.beercompetition.competition.lifecycle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.pojo.dto.CompetitionReopenRegistrationRequest;
import com.beercompetition.pojo.dto.CompetitionReturnToSampleCheckRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.vo.CompetitionCheckVO;
import com.beercompetition.pojo.vo.CompetitionDetailVO;
import com.beercompetition.common.context.BaseContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.beercompetition.competition.lifecycle.CompetitionLifecycleService;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.competition.query.CompetitionQueryService;

/**
 * 校验并推进赛事状态，所有状态变更在事务内完成审计。
 */
@Service
@RequiredArgsConstructor
public class CompetitionLifecycleServiceImpl implements CompetitionLifecycleService {

    private static final String CHECK_DONE = "done";

    private static final int FLAG_FALSE = 0;

    private static final String LOG_TARGET_COMPETITION = "COMPETITION";

    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CompetitionMapper competitionMapper;

    private final AdminOperationLogMapper adminOperationLogMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final ObjectMapper objectMapper;

    private final CompetitionQueryService competitionQueryService;

    private final CompetitionAccessService competitionAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredRegistrations() {
        // 1) 使用单一时间点判定本次到期范围
        LocalDateTime now = LocalDateTime.now();

        // 2) 批量关闭已到报名截止时间的报名中赛事
        return closeExpiredRegistrations(now);
    }

    private int closeExpiredRegistrations(LocalDateTime now) {
        if (now == null) {
            return 0;
        }
        return competitionMapper.update(null, new LambdaUpdateWrapper<Competition>()
                .set(Competition::getStatus, CompetitionStatus.REGISTRATION_CLOSED.name())
                .eq(Competition::getStatus, CompetitionStatus.REGISTRATION_OPEN.name())
                .eq(Competition::getDeletedFlag, FLAG_FALSE)
                .isNotNull(Competition::getRegistrationDeadline)
                .le(Competition::getRegistrationDeadline, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO openRegistration(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus status = parseStatus(competition);
        if (status != CompetitionStatus.DRAFT) {
            throw new BaseException("只有草稿状态的比赛可以开放报名");
        }

        // 2) 执行完整性检查
        CompetitionDetailVO detail = competitionQueryService.getCompetitionOverview(competition.getId());
        List<CompetitionCheckVO> blockingChecks = detail.getChecks().stream()
                .filter(check -> isBlockingCheck(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .toList();
        if (!blockingChecks.isEmpty()) {
            String message = blockingChecks.stream()
                    .map(CompetitionCheckVO::getLabel)
                    .collect(Collectors.joining("、"));
            throw new BaseException("配置未完整，暂不能开放报名：" + message);
        }

        // 3) 更新比赛状态为报名中
        CompetitionStatus oldStatus = status;
        competition.setStatus(CompetitionStatus.REGISTRATION_OPEN.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_OPEN_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_OPEN, "开放报名", null, null, competition.getRegistrationDeadline());

        // 4) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO closeRegistration(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.REGISTRATION_OPEN) {
            throw new BaseException("只有报名中的比赛可以截止报名");
        }

        // 2) 更新比赛状态为报名截止
        competition.setStatus(CompetitionStatus.REGISTRATION_CLOSED.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_CLOSE_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_CLOSED, "截止报名", null, competition.getRegistrationDeadline(),
                competition.getRegistrationDeadline());

        // 3) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO prepareJudging(Long id) {
        // 1) 查询比赛并校验状态流转入口
        Competition competition = getCompetitionOrThrow(id);
        if (parseStatus(competition) != CompetitionStatus.REGISTRATION_CLOSED) {
            throw new BaseException("只有报名截止后的比赛可以进入评审准备");
        }

        // 2) 校验评审准备必需配置
        CompetitionDetailVO detail = competitionQueryService.getCompetitionOverview(competition.getId());
        assertChecksDone(detail.getChecks(), Set.of("judgeTables", "scoreForms", "storedEntries"), "评审准备");

        // 3) 更新比赛状态为评审准备
        CompetitionStatus oldStatus = parseStatus(competition);
        competition.setStatus(CompetitionStatus.JUDGING_PREP.name());
        competitionMapper.updateById(competition);
        writeCompetitionStageLog("COMPETITION_PREPARE_JUDGING", competition.getId(), oldStatus,
                CompetitionStatus.JUDGING_PREP, "进入评审准备", null, competition.getRegistrationDeadline(),
                competition.getRegistrationDeadline());

        // 4) 重新计算配置检查并返回详情
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO reopenRegistration(Long id, CompetitionReopenRegistrationRequest request) {
        // 1) 查询比赛并校验恢复入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.REGISTRATION_CLOSED) {
            throw new BaseException("只有已截止报名的比赛可以重新开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldDeadline = competition.getRegistrationDeadline();
        LocalDateTime nextDeadline = request.getRegistrationDeadline();
        if ((oldDeadline == null || !oldDeadline.isAfter(now)) && nextDeadline == null) {
            throw new BaseException("原报名截止时间已过，请设置新的报名截止时间");
        }
        if (nextDeadline != null) {
            validateReopenRegistrationDeadline(competition, nextDeadline, now);
        }

        // 2) 更新报名窗口和比赛状态
        if (nextDeadline != null) {
            competition.setRegistrationDeadline(nextDeadline);
        }
        competition.setStatus(CompetitionStatus.REGISTRATION_OPEN.name());
        competitionMapper.updateById(competition);

        // 3) 写入阶段恢复审计并返回详情
        writeCompetitionStageLog("COMPETITION_REOPEN_REGISTRATION", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_OPEN, "重新开放报名", request.getReason(), oldDeadline,
                competition.getRegistrationDeadline());
        return competitionQueryService.getCompetitionDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionDetailVO returnToSampleCheck(Long id, CompetitionReturnToSampleCheckRequest request) {
        // 1) 查询比赛并校验恢复入口
        Competition competition = getCompetitionOrThrow(id);
        CompetitionStatus oldStatus = parseStatus(competition);
        if (oldStatus != CompetitionStatus.JUDGING_PREP) {
            throw new BaseException("只有评审准备中可以退回样品入库核对");
        }
        CompetitionRound firstRound = competitionRoundMapper.selectOne(new LambdaQueryWrapper<CompetitionRound>()
                .eq(CompetitionRound::getCompetitionId, id)
                .eq(CompetitionRound::getRoundNo, 1)
                .last("LIMIT 1"));
        if (firstRound != null && !RoundStatus.DRAFT.name().equals(firstRound.getStatus())) {
            throw new BaseException("首轮已发布，不能退回样品入库核对");
        }

        // 2) 只回退比赛主状态，保留第一轮草稿和分桌草稿
        competition.setStatus(CompetitionStatus.REGISTRATION_CLOSED.name());
        competitionMapper.updateById(competition);

        // 3) 写入阶段恢复审计并返回详情
        writeCompetitionStageLog("COMPETITION_RETURN_TO_SAMPLE_CHECK", competition.getId(), oldStatus,
                CompetitionStatus.REGISTRATION_CLOSED, "退回样品入库核对", request.getReason(),
                competition.getRegistrationDeadline(), competition.getRegistrationDeadline());
        return competitionQueryService.getCompetitionDetail(id);
    }

    private void assertChecksDone(List<CompetitionCheckVO> checks, Set<String> requiredKeys, String actionName) {
        List<String> missing = checks.stream()
                .filter(check -> requiredKeys.contains(check.getKey()))
                .filter(check -> !CHECK_DONE.equals(check.getState()))
                .map(CompetitionCheckVO::getLabel)
                .toList();
        if (!missing.isEmpty()) {
            throw new BaseException(actionName + "前还需要完成：" + String.join("、", missing));
        }
    }

    private void validateReopenRegistrationDeadline(Competition competition, LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null) {
            return;
        }
        if (competition.getRegistrationStart() != null && !deadline.isAfter(competition.getRegistrationStart())) {
            throw new BaseException("新的报名截止时间必须晚于报名开始时间");
        }
        if (!deadline.isAfter(now)) {
            throw new BaseException("新的报名截止时间必须晚于当前时间");
        }
    }

    private void writeCompetitionStageLog(String action,
                                          Long competitionId,
                                          CompetitionStatus fromStatus,
                                          CompetitionStatus toStatus,
                                          String actionLabel,
                                          String reason,
                                          LocalDateTime oldDeadline,
                                          LocalDateTime newDeadline) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", actionLabel);
        payload.put("fromStatus", fromStatus.name());
        payload.put("toStatus", toStatus.name());
        payload.put("reason", truncateNullable(reason, 120));
        payload.put("oldRegistrationDeadline", oldDeadline == null ? null : oldDeadline.format(EXPORT_TIME_FORMAT));
        payload.put("newRegistrationDeadline", newDeadline == null ? null : newDeadline.format(EXPORT_TIME_FORMAT));
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .organizerId(competitionMapper.selectById(competitionId).getOrganizerId())
                .competitionId(competitionId)
                .action(action)
                .targetType(LOG_TARGET_COMPETITION)
                .targetPublicId(String.valueOf(competitionId))
                .summary(writeObjectJson(payload, "保存比赛阶段操作日志失败"))
                .createTime(LocalDateTime.now())
                .build());
    }

    private String writeObjectJson(Object payload, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private String truncateNullable(String value, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private Competition getCompetitionOrThrow(Long id) {
        competitionAccessService.requireCompetitionAccess(id);
        Competition competition = competitionMapper.selectById(id);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private CompetitionStatus parseStatus(Competition competition) {
        try {
            return CompetitionStatus.valueOf(competition.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BaseException("比赛状态不合法：" + competition.getStatus());
        }
    }

    private boolean isBlockingCheck(String key) {
        return Set.of("baseInfo", "categories", "styleLibrary").contains(key);
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
