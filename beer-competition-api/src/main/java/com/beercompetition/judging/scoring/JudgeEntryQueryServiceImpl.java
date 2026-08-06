package com.beercompetition.judging.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.mapper.ScoreRecordMapper;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.JudgeTaskType;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryFieldConfig;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import com.beercompetition.pojo.po.ScoreRecord;
import com.beercompetition.pojo.vo.EntryExtraFieldVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.service.EntryScanLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.beercompetition.judging.scoring.JudgeEntryQueryService;

/**
 * 按轮次和分桌上下文解析评委可见的匿名酒款信息。
 */
@Service
@RequiredArgsConstructor
public class JudgeEntryQueryServiceImpl implements JudgeEntryQueryService {

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final CompetitionStyleConfigMapper competitionStyleConfigMapper;

    private final EntryFieldConfigMapper entryFieldConfigMapper;

    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;

    private final JudgeAccountMapper judgeAccountMapper;

    private final RoundTableEntryMapper roundTableEntryMapper;

    private final RoundTableMemberMapper roundTableMemberMapper;

    private final CompetitionRoundMapper competitionRoundMapper;

    private final RoundTableMapper roundTableMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final EntryScanLabelService entryScanLabelService;

    @Override
    public JudgeEntryVO getJudgeEntry(String uuid) {
        // 1) 查询匿名作品并校验评审权限
        BeerEntry entry = beerEntryMapper.selectOne(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getUuid, uuid));
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        assertCompetitionNotArchived(entry.getCompetitionId());
        JudgeScanContext context = requireActiveJudgeRoundEntry(entry);
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());

        // 2) 组装评审可见匿名信息
        return buildJudgeEntryVO(entry, label, context);
    }

    @Override
    public JudgeEntryVO resolveJudgeScan(String code) {
        // 1) 解析二维码令牌、作品编号或匿名标签编码
        EntryScanLabel label = entryScanLabelService.resolveActiveLabel(code);
        BeerEntry entry = requireEntry(label.getBeerEntryId());
        assertCompetitionNotArchived(entry.getCompetitionId());
        JudgeScanContext context = requireActiveJudgeRoundEntry(entry);

        // 2) 组装评审可见匿名信息
        return buildJudgeEntryVO(entry, label, context);
    }

    private JudgeEntryVO buildJudgeEntryVO(BeerEntry entry, EntryScanLabel label, JudgeScanContext context) {
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        CompetitionStyleConfig style = findStyleSnapshot(entry);
        boolean scoreTableLocked = RoundStatus.LOCKED.name().equals(context.table().getStatus());
        boolean finalScoreExists = hasFinalScore(entry.getId());
        boolean canScore = canSubmitPersonalScore(context) && !finalScoreExists;
        boolean canFinalize = canFinalizeTable(context) && !scoreTableLocked;
        return JudgeEntryVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .competitionId(entry.getCompetitionId())
                .competitionName(context.competition() == null ? null : context.competition().getName())
                .roundId(context.round().getId())
                .roundName(context.round().getRoundName())
                .roundType(context.round().getRoundType())
                .roundTableId(context.table().getId())
                .tableName(context.table().getTableName())
                .judgeRoleType(context.member().getRole())
                .taskType(context.taskType())
                .action(resolveJudgeAction(context.taskType()))
                .canScore(canScore)
                .canFinalize(canFinalize)
                .scored(hasSubmittedJudgeScore(entry.getId(), context.judge().getId()))
                .locked(scoreTableLocked || finalScoreExists)
                .scoreRoleType(resolveScoreRoleType(context.member().getRole(), context.taskType()))
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .styleCategoryName(style == null ? null : style.getCategoryName())
                .styleCode(style == null ? null : style.getStyleCode())
                .styleDescription(style == null ? null : style.getDescription())
                .abv(entry.getAbv())
                .extraFields(listJudgeVisibleExtraFields(entry))
                .build();
    }

    private List<EntryExtraFieldVO> listJudgeVisibleExtraFields(BeerEntry entry) {
        Set<String> visibleKeys = entryFieldConfigMapper.selectList(new LambdaQueryWrapper<EntryFieldConfig>()
                        .eq(EntryFieldConfig::getCompetitionId, entry.getCompetitionId()))
                .stream()
                .filter(item -> Objects.equals(item.getVisibleToJudges(), 1))
                .map(EntryFieldConfig::getFieldKey)
                .collect(Collectors.toSet());
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .eq(BeerEntryExtraField::getBeerEntryId, entry.getId()))
                .stream()
                .filter(item -> visibleKeys.contains(item.getFieldKey()))
                .map(this::toEntryExtraFieldVO)
                .toList();
    }

    private EntryExtraFieldVO toEntryExtraFieldVO(BeerEntryExtraField item) {
        return EntryExtraFieldVO.builder()
                .key(item.getFieldKey())
                .label(item.getFieldLabel())
                .value(item.getFieldValue())
                .build();
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private CompetitionStyleConfig findStyleSnapshot(BeerEntry entry) {
        if (entry.getStyleConfigId() != null) {
            CompetitionStyleConfig snapshot = competitionStyleConfigMapper.selectById(entry.getStyleConfigId());
            if (snapshot != null) {
                return snapshot;
            }
        }
        return competitionStyleConfigMapper.selectOne(new LambdaQueryWrapper<CompetitionStyleConfig>()
                .eq(CompetitionStyleConfig::getCompetitionId, entry.getCompetitionId())
                .eq(CompetitionStyleConfig::getName, entry.getStyle())
                .orderByDesc(CompetitionStyleConfig::getId)
                .last("LIMIT 1"));
    }

    private JudgeScanContext requireActiveJudgeRoundEntry(BeerEntry entry) {
        JudgeAccount account = judgeAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        if (JudgeAccountStatus.of(account.getStatus()) != JudgeAccountStatus.ACTIVE) {
            throw new ForbiddenException("评审账号未启用，不能查看酒款");
        }
        List<RoundTableMember> members = roundTableMemberMapper.selectList(new LambdaQueryWrapper<RoundTableMember>()
                .eq(RoundTableMember::getJudgeAccountId, account.getId()));
        if (members.isEmpty()) {
            throw new ForbiddenException("当前评审没有可查看的评审任务");
        }
        List<Long> roundTableIds = members.stream().map(RoundTableMember::getRoundTableId).toList();
        List<RoundTableEntry> roundEntries = roundTableEntryMapper.selectList(new LambdaQueryWrapper<RoundTableEntry>()
                .eq(RoundTableEntry::getBeerEntryId, entry.getId())
                .in(RoundTableEntry::getRoundTableId, roundTableIds)
                .orderByDesc(RoundTableEntry::getId));
        Map<Long, RoundTableMember> memberByTableId = members.stream()
                .collect(Collectors.toMap(RoundTableMember::getRoundTableId, item -> item, (left, right) -> left));
        for (RoundTableEntry roundEntry : roundEntries) {
            RoundTable table = roundTableMapper.selectById(roundEntry.getRoundTableId());
            if (table == null) {
                continue;
            }
            RoundTableMember member = memberByTableId.get(table.getId());
            CompetitionRound round = competitionRoundMapper.selectById(table.getRoundId());
            if (round == null || !isRoundVisibleToJudge(round, table, member)) {
                continue;
            }
            String taskType = resolveJudgeTaskType(round, member);
            if (!StringUtils.hasText(taskType)) {
                continue;
            }
            Competition competition = competitionMapper.selectById(round.getCompetitionId());
            return new JudgeScanContext(account, member, roundEntry, table, round, competition, taskType);
        }
        throw new ForbiddenException("当前评审无权查看该酒款");
    }

    private boolean isRoundVisibleToJudge(CompetitionRound round, RoundTable table, RoundTableMember member) {
        if (member == null) {
            return false;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            if (!JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                    && !Objects.equals(member.getSystemTaskRequired(), 1)) {
                return false;
            }
            if (RoundStatus.PUBLISHED.name().equals(round.getStatus())) {
                return RoundStatus.PUBLISHED.name().equals(table.getStatus())
                        || JudgeRoleType.CAPTAIN.name().equals(member.getRole());
            }
            if (RoundStatus.SUBMITTED.name().equals(round.getStatus())) {
                return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                        && !RoundStatus.LOCKED.name().equals(table.getStatus());
            }
            return RoundStatus.LOCKED.name().equals(round.getStatus())
                    && JudgeRoleType.CAPTAIN.name().equals(member.getRole());
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return RoundStatus.IN_PROGRESS.name().equals(round.getStatus())
                    || RoundStatus.SUBMITTED.name().equals(round.getStatus())
                    || RoundStatus.LOCKED.name().equals(round.getStatus());
        }
        return false;
    }

    private String resolveJudgeTaskType(CompetitionRound round, RoundTableMember member) {
        if (member == null) {
            return null;
        }
        if (RoundType.SCORE.name().equals(round.getRoundType())) {
            return JudgeRoleType.CAPTAIN.name().equals(member.getRole())
                    ? JudgeTaskType.CAPTAIN_FINALIZE.name()
                    : JudgeTaskType.SCORE_ENTRY.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType()) && JudgeRoleType.CAPTAIN.name().equals(member.getRole())) {
            return JudgeTaskType.RANKING_ROUND.name();
        }
        if (RoundType.RANKING.name().equals(round.getRoundType())) {
            return JudgeTaskType.RANKING_PARTICIPANT.name();
        }
        return null;
    }

    private String resolveJudgeAction(String taskType) {
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            return "CAPTAIN";
        }
        if (JudgeTaskType.RANKING_ROUND.name().equals(taskType)
                || JudgeTaskType.RANKING_PARTICIPANT.name().equals(taskType)) {
            return "RANKING";
        }
        return "SCORE";
    }

    private boolean canSubmitPersonalScore(JudgeScanContext context) {
        if (!RoundType.SCORE.name().equals(context.round().getRoundType())) {
            return false;
        }
        if (!RoundStatus.PUBLISHED.name().equals(context.round().getStatus())
                || !RoundStatus.PUBLISHED.name().equals(context.table().getStatus())) {
            return false;
        }
        return JudgeTaskType.SCORE_ENTRY.name().equals(context.taskType())
                || JudgeTaskType.CAPTAIN_FINALIZE.name().equals(context.taskType());
    }

    private boolean canFinalizeTable(JudgeScanContext context) {
        if (JudgeTaskType.CAPTAIN_FINALIZE.name().equals(context.taskType())) {
            return RoundType.SCORE.name().equals(context.round().getRoundType())
                    && JudgeRoleType.CAPTAIN.name().equals(context.member().getRole())
                    && (RoundStatus.PUBLISHED.name().equals(context.round().getStatus())
                    || RoundStatus.SUBMITTED.name().equals(context.round().getStatus()));
        }
        return JudgeTaskType.RANKING_ROUND.name().equals(context.taskType());
    }

    private String resolveScoreRoleType(String memberRole, String taskType) {
        if (!JudgeTaskType.SCORE_ENTRY.name().equals(taskType)
                && !JudgeTaskType.CAPTAIN_FINALIZE.name().equals(taskType)) {
            return null;
        }
        if (JudgeRoleType.CAPTAIN.name().equals(memberRole)) {
            return JudgeRoleType.PROFESSIONAL.name();
        }
        return memberRole;
    }

    private boolean hasSubmittedJudgeScore(Long beerEntryId, Long judgeId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getJudgeAccountId, judgeId)
                .eq(ScoreRecord::getFinalFlag, 0)) > 0;
    }

    private boolean hasFinalScore(Long beerEntryId) {
        return scoreRecordMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getBeerEntryId, beerEntryId)
                .eq(ScoreRecord::getFinalFlag, 1)) > 0;
    }

    private boolean isCompetitionArchived(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        return competition != null && CompetitionStatus.ARCHIVED.name().equals(competition.getStatus());
    }

    private void assertCompetitionNotArchived(Long competitionId) {
        if (isCompetitionArchived(competitionId)) {
            throw new ResourceNotFoundException("赛事不存在");
        }
    }

    private record JudgeScanContext(
            JudgeAccount judge,
            RoundTableMember member,
            RoundTableEntry roundEntry,
            RoundTable table,
            CompetitionRound round,
            Competition competition,
            String taskType
    ) {
    }
}
