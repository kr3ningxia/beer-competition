package com.beercompetition.testsupport;

import com.beercompetition.common.util.PiiService;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionRoundMapper;
import com.beercompetition.mapper.CompetitionScoreConfigMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.mapper.JudgeAssignmentMapper;
import com.beercompetition.mapper.JudgeTableMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.RoundTableEntryMapper;
import com.beercompetition.mapper.RoundTableMapper;
import com.beercompetition.mapper.RoundTableMemberMapper;
import com.beercompetition.pojo.enums.CompetitionDeliveryMethod;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.enums.CompetitionType;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryScanLabelStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.enums.JudgeAccountStatus;
import com.beercompetition.pojo.enums.JudgeRoleType;
import com.beercompetition.pojo.enums.LogisticsVisibility;
import com.beercompetition.pojo.enums.RoundEntryStatus;
import com.beercompetition.pojo.enums.RoundStatus;
import com.beercompetition.pojo.enums.RoundTargetMode;
import com.beercompetition.pojo.enums.RoundType;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionRound;
import com.beercompetition.pojo.po.CompetitionScoreConfig;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.po.JudgeAssignment;
import com.beercompetition.pojo.po.JudgeTable;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.RoundTable;
import com.beercompetition.pojo.po.RoundTableEntry;
import com.beercompetition.pojo.po.RoundTableMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BeerCompetitionTestData {

    @Autowired private CompetitionMapper competitionMapper;
    @Autowired private CompetitionCategoryMapper competitionCategoryMapper;
    @Autowired private CompetitionStyleConfigMapper competitionStyleConfigMapper;
    @Autowired private CompetitionScoreConfigMapper competitionScoreConfigMapper;
    @Autowired private BreweryMapper breweryMapper;
    @Autowired private PortalAccountMapper portalAccountMapper;
    @Autowired private BeerEntryMapper beerEntryMapper;
    @Autowired private EntryPaymentMapper entryPaymentMapper;
    @Autowired private EntryDeliveryMapper entryDeliveryMapper;
    @Autowired private EntryScanLabelMapper entryScanLabelMapper;
    @Autowired private JudgeAccountMapper judgeAccountMapper;
    @Autowired private JudgeTableMapper judgeTableMapper;
    @Autowired private JudgeAssignmentMapper judgeAssignmentMapper;
    @Autowired private CompetitionRoundMapper competitionRoundMapper;
    @Autowired private RoundTableMapper roundTableMapper;
    @Autowired private RoundTableMemberMapper roundTableMemberMapper;
    @Autowired private RoundTableEntryMapper roundTableEntryMapper;
    @Autowired private PiiService piiService;

    public Fixture createFixture(String prefix) {
        Competition competition = createCompetition(prefix, CompetitionStatus.DRAFT);
        CompetitionCategory category = createCategory(competition.getId(), prefix + "-组别");
        createStyle(competition.getId(), prefix + "-风格");
        createScoreConfigs(competition.getId());

        PortalUser portalA = createPortal(prefix, "A", "13900000001");
        PortalUser portalB = createPortal(prefix, "B", "13900000002");

        JudgeAccount captain = createJudge(prefix, "CAPTAIN", "13900000101", JudgeAccountStatus.ACTIVE);
        JudgeAccount professional = createJudge(prefix, "PRO", "13900000102", JudgeAccountStatus.ACTIVE);
        JudgeAccount cross = createJudge(prefix, "CROSS", "13900000103", JudgeAccountStatus.ACTIVE);
        JudgeAccount outsider = createJudge(prefix, "OUT", "13900000104", JudgeAccountStatus.ACTIVE);
        JudgeAccount disabled = createJudge(prefix, "DISABLED", "13900000105", JudgeAccountStatus.DISABLED);

        JudgeTable baseTable = createBaseJudgeTable(competition.getId(), prefix + "-一号桌");
        createAssignment(competition.getId(), baseTable.getId(), captain.getId(), JudgeRoleType.CAPTAIN);
        createAssignment(competition.getId(), baseTable.getId(), professional.getId(), JudgeRoleType.PROFESSIONAL);
        createAssignment(competition.getId(), baseTable.getId(), cross.getId(), JudgeRoleType.CROSS);

        BeerEntry entryA1 = createEntry(prefix, competition.getId(), portalA.brewery().getId(), category.getId(),
                prefix + "-作品A1", EntryStatus.STORED, true);
        BeerEntry entryA2 = createEntry(prefix, competition.getId(), portalA.brewery().getId(), category.getId(),
                prefix + "-作品A2", EntryStatus.STORED, true);
        BeerEntry entryB1 = createEntry(prefix, competition.getId(), portalB.brewery().getId(), category.getId(),
                prefix + "-作品B1", EntryStatus.STORED, true);

        return new Fixture(competition, category, portalA, portalB, captain, professional, cross, outsider, disabled,
                baseTable, entryA1, entryA2, entryB1);
    }

    public Competition createCompetition(String prefix, CompetitionStatus status) {
        Competition competition = Competition.builder()
                .code(prefix + "-COMP")
                .name(prefix + "-测试比赛")
                .competitionDate(LocalDate.now().plusDays(30))
                .registrationStart(LocalDateTime.now().minusDays(1))
                .registrationDeadline(LocalDateTime.now().plusDays(10))
                .status(status.name())
                .competitionType(CompetitionType.AWARD.name())
                .entryFee(new BigDecimal("100.00"))
                .description("测试比赛")
                .styleLibraryVersion(prefix + "-LIB")
                .deliveryMethod(CompetitionDeliveryMethod.EXPRESS.name())
                .sampleArrivalStart(LocalDateTime.now().plusDays(1))
                .sampleArrivalDeadline(LocalDateTime.now().plusDays(20))
                .sampleQuantityNote("2 瓶")
                .deliveryRecipient("收件人")
                .deliveryPhone("13900009999")
                .deliveryAddress("测试地址")
                .logisticsVisibility(LogisticsVisibility.PUBLIC.name())
                .deletedFlag(0)
                .build();
        competitionMapper.insert(competition);
        return competition;
    }

    public CompetitionCategory createCategory(Long competitionId, String name) {
        CompetitionCategory category = CompetitionCategory.builder()
                .competitionId(competitionId)
                .name(name)
                .sortOrder(1)
                .build();
        competitionCategoryMapper.insert(category);
        return category;
    }

    public CompetitionStyleConfig createStyle(Long competitionId, String name) {
        CompetitionStyleConfig style = CompetitionStyleConfig.builder()
                .competitionId(competitionId)
                .name(name)
                .categoryName("测试风格分类")
                .styleCode("IT")
                .description("测试风格")
                .sortOrder(1)
                .build();
        competitionStyleConfigMapper.insert(style);
        return style;
    }

    public void createScoreConfigs(Long competitionId) {
        createScoreConfig(competitionId, JudgeRoleType.PROFESSIONAL,
                """
                [{"key":"aroma","label":"香气","maxScore":20},{"key":"taste","label":"口味","maxScore":30}]
                """);
        createScoreConfig(competitionId, JudgeRoleType.CROSS,
                """
                [{"key":"overall","label":"整体印象","maxScore":50}]
                """);
        createScoreConfig(competitionId, JudgeRoleType.CAPTAIN,
                """
                [{"key":"consensus","label":"共识分","maxScore":50}]
                """);
    }

    private void createScoreConfig(Long competitionId, JudgeRoleType roleType, String dimensionsJson) {
        competitionScoreConfigMapper.insert(CompetitionScoreConfig.builder()
                .competitionId(competitionId)
                .judgeRoleType(roleType.name())
                .dimensionsJson(dimensionsJson)
                .minCommentLength(2)
                .build());
    }

    public PortalUser createPortal(String prefix, String suffix, String phone) {
        Brewery brewery = Brewery.builder()
                .companyName(prefix + "-厂牌" + suffix)
                .contactName("联系人" + suffix)
                .phone(phone)
                .wechat("wechat" + suffix)
                .build();
        breweryMapper.insert(brewery);
        PortalAccount account = PortalAccount.builder()
                .phone(phone)
                .displayName(prefix + "-厂商" + suffix)
                .breweryId(brewery.getId())
                .status(1)
                .build();
        portalAccountMapper.insert(account);
        return new PortalUser(account, brewery);
    }

    public JudgeAccount createJudge(String prefix, String suffix, String phone, JudgeAccountStatus status) {
        String normalized = piiService.normalizePhone(phone);
        JudgeAccount judge = JudgeAccount.builder()
                .publicId(prefix + "-J-" + suffix)
                .phoneEnc(piiService.encrypt(normalized))
                .phoneHash(piiService.hashPhone(normalized))
                .phoneLast4(piiService.phoneLast4(normalized))
                .name(prefix + "-评委" + suffix)
                .qualification("测试资质")
                .breweryConflictFlag(false)
                .status(status.getCode())
                .build();
        judgeAccountMapper.insert(judge);
        return judge;
    }

    public JudgeTable createBaseJudgeTable(Long competitionId, String name) {
        JudgeTable table = JudgeTable.builder()
                .competitionId(competitionId)
                .tableName(name)
                .sortOrder(1)
                .build();
        judgeTableMapper.insert(table);
        return table;
    }

    public JudgeAssignment createAssignment(Long competitionId, Long tableId, Long judgeId, JudgeRoleType role) {
        JudgeAssignment assignment = JudgeAssignment.builder()
                .competitionId(competitionId)
                .tableId(tableId)
                .judgeAccountId(judgeId)
                .role(role.name())
                .build();
        judgeAssignmentMapper.insert(assignment);
        return assignment;
    }

    public BeerEntry createEntry(String prefix, Long competitionId, Long breweryId, Long categoryId,
                                 String name, EntryStatus status, boolean paid) {
        BeerEntry entry = BeerEntry.builder()
                .uuid(prefix + "-E-" + Math.abs(name.hashCode()))
                .competitionId(competitionId)
                .breweryId(breweryId)
                .categoryId(categoryId)
                .name(name)
                .style(prefix + "-风格")
                .abv(new BigDecimal("5.0"))
                .extraFieldsJson("{}")
                .status(status.name())
                .storedFlag(EntryStatus.STORED == status ? 1 : 0)
                .build();
        beerEntryMapper.insert(entry);
        entryScanLabelMapper.insert(EntryScanLabel.builder()
                .competitionId(competitionId)
                .beerEntryId(entry.getId())
                .labelCode(prefix + "-L-" + entry.getId())
                .shortCode("S" + entry.getId())
                .scanToken(prefix + "-TOKEN-" + entry.getId())
                .status(EntryScanLabelStatus.ACTIVE.name())
                .generatedTime(java.time.LocalDateTime.now())
                .build());
        entryPaymentMapper.insert(EntryPayment.builder()
                .beerEntryId(entry.getId())
                .amount(new BigDecimal("100.00"))
                .status(paid ? EntryPaymentStatus.PAID.name() : EntryPaymentStatus.UNPAID.name())
                .payMethod(paid ? EntryPayMethod.MANUAL.name() : EntryPayMethod.MOCK.name())
                .build());
        entryDeliveryMapper.insert(EntryDelivery.builder()
                .beerEntryId(entry.getId())
                .deliveryStatus(EntryStatus.STORED == status
                        ? EntryDeliveryStatus.RECEIVED.name()
                        : EntryDeliveryStatus.NOT_SUBMITTED.name())
                .build());
        return entry;
    }

    public ScoreRound createPublishedScoreRound(Fixture fixture, List<BeerEntry> entries, int targetCount) {
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(fixture.competition().getId())
                .roundNo(1)
                .roundName("第一轮")
                .roundType(RoundType.SCORE.name())
                .status(RoundStatus.PUBLISHED.name())
                .sortOrder(1)
                .publishedTime(LocalDateTime.now())
                .build();
        competitionRoundMapper.insert(round);
        RoundTable table = RoundTable.builder()
                .competitionId(fixture.competition().getId())
                .roundId(round.getId())
                .tableName("第一轮一号桌")
                .captainJudgeId(fixture.captain().getId())
                .categoryId(fixture.category().getId())
                .categoryMode("SINGLE")
                .targetCount(targetCount)
                .targetMode(RoundTargetMode.ADVANCE_COUNT.name())
                .status(RoundStatus.PUBLISHED.name())
                .resultVersion(1)
                .confirmationOverrideFlag(0)
                .sortOrder(1)
                .build();
        roundTableMapper.insert(table);
        insertMember(table.getId(), fixture.captain().getId(), JudgeRoleType.CAPTAIN, 1);
        insertMember(table.getId(), fixture.professional().getId(), JudgeRoleType.PROFESSIONAL, 1);
        insertMember(table.getId(), fixture.cross().getId(), JudgeRoleType.CROSS, 1);

        List<RoundTableEntry> roundEntries = new ArrayList<>();
        int sort = 0;
        for (BeerEntry entry : entries) {
            RoundTableEntry roundEntry = RoundTableEntry.builder()
                    .competitionId(fixture.competition().getId())
                    .roundId(round.getId())
                    .roundTableId(table.getId())
                    .beerEntryId(entry.getId())
                    .status(RoundEntryStatus.ASSIGNED.name())
                    .sortOrder(sort++)
                    .build();
            roundTableEntryMapper.insert(roundEntry);
            roundEntries.add(roundEntry);
        }
        return new ScoreRound(round, table, roundEntries);
    }

    public RankingRound createRankingRound(Fixture fixture, List<BeerEntry> entries, RoundTargetMode targetMode,
                                           int targetCount, RoundStatus status, Integer requiredJudgeTasks) {
        CompetitionRound round = CompetitionRound.builder()
                .competitionId(fixture.competition().getId())
                .roundNo(2)
                .roundName("排序轮")
                .roundType(RoundType.RANKING.name())
                .status(status.name())
                .sortOrder(2)
                .publishedTime(LocalDateTime.now())
                .build();
        competitionRoundMapper.insert(round);
        RoundTable table = RoundTable.builder()
                .competitionId(fixture.competition().getId())
                .roundId(round.getId())
                .tableName("排序一号桌")
                .captainJudgeId(fixture.captain().getId())
                .categoryId(fixture.category().getId())
                .categoryMode("SINGLE")
                .targetCount(targetCount)
                .targetMode(targetMode.name())
                .status(status.name())
                .resultVersion(1)
                .confirmationOverrideFlag(0)
                .sortOrder(1)
                .build();
        roundTableMapper.insert(table);
        insertMember(table.getId(), fixture.captain().getId(), JudgeRoleType.CAPTAIN, 1);
        insertMember(table.getId(), fixture.professional().getId(), JudgeRoleType.PROFESSIONAL, requiredJudgeTasks);
        insertMember(table.getId(), fixture.cross().getId(), JudgeRoleType.CROSS, requiredJudgeTasks);

        List<RoundTableEntry> roundEntries = new ArrayList<>();
        int sort = 0;
        for (BeerEntry entry : entries) {
            RoundTableEntry roundEntry = RoundTableEntry.builder()
                    .competitionId(fixture.competition().getId())
                    .roundId(round.getId())
                    .roundTableId(table.getId())
                    .beerEntryId(entry.getId())
                    .status(RoundEntryStatus.ASSIGNED.name())
                    .sortOrder(sort++)
                    .build();
            roundTableEntryMapper.insert(roundEntry);
            roundEntries.add(roundEntry);
        }
        return new RankingRound(round, table, roundEntries);
    }

    private void insertMember(Long roundTableId, Long judgeId, JudgeRoleType role, int required) {
        roundTableMemberMapper.insert(RoundTableMember.builder()
                .roundTableId(roundTableId)
                .judgeAccountId(judgeId)
                .role(role.name())
                .systemTaskRequired(required)
                .build());
    }

    public record PortalUser(PortalAccount account, Brewery brewery) {
    }

    public record Fixture(Competition competition,
                          CompetitionCategory category,
                          PortalUser portalA,
                          PortalUser portalB,
                          JudgeAccount captain,
                          JudgeAccount professional,
                          JudgeAccount cross,
                          JudgeAccount outsider,
                          JudgeAccount disabled,
                          JudgeTable baseTable,
                          BeerEntry entryA1,
                          BeerEntry entryA2,
                          BeerEntry entryB1) {
    }

    public record ScoreRound(CompetitionRound round, RoundTable table, List<RoundTableEntry> entries) {
    }

    public record RankingRound(CompetitionRound round, RoundTable table, List<RoundTableEntry> entries) {
    }
}
