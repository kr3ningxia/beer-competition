package com.beercompetition.architecture;

import com.beercompetition.billing.beercoin.BeerCoinSettlementService;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.competition.access.CompetitionAccessServiceImpl;
import com.beercompetition.competition.command.CompetitionCommandService;
import com.beercompetition.competition.configuration.CompetitionConfigurationService;
import com.beercompetition.competition.lifecycle.CompetitionLifecycleService;
import com.beercompetition.competition.query.CompetitionQueryService;
import com.beercompetition.competition.query.CompetitionScoringExportService;
import com.beercompetition.judging.assignment.RoundAllocationService;
import com.beercompetition.judging.round.JudgeRoundTaskService;
import com.beercompetition.judging.round.RoundLifecycleService;
import com.beercompetition.judging.round.RoundQueryService;
import com.beercompetition.judging.scoring.JudgeEntryQueryService;
import com.beercompetition.judging.scoring.RankingService;
import com.beercompetition.judging.scoring.ScoreConfirmationService;
import com.beercompetition.registration.delivery.EntryDeliveryService;
import com.beercompetition.registration.entry.AdminEntryService;
import com.beercompetition.registration.entry.EntryDocumentService;
import com.beercompetition.registration.entry.PortalEntryService;
import com.beercompetition.registration.payment.EntryPaymentAdminService;
import com.beercompetition.registration.payment.CompetitionPaymentPolicy;
import com.beercompetition.registration.refund.EntryRefundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BackendModuleArchitectureTest {

    private static final List<String> REMOVED_FACADE_TYPES = List.of(
            "com.beercompetition.service.CompetitionService",
            "com.beercompetition.service.EntryService",
            "com.beercompetition.service.RoundService",
            "com.beercompetition.service.impl.CompetitionServiceImpl",
            "com.beercompetition.service.impl.EntryServiceImpl",
            "com.beercompetition.service.impl.RoundServiceImpl"
    );

    private static final List<Class<?>> UNIMPLEMENTED_PHASE_TWO_EXTENSION_POINTS = List.of(
            CompetitionPaymentPolicy.class,
            BeerCoinSettlementService.class
    );

    private static final List<Class<?>> SPLIT_SERVICE_INTERFACES = List.of(
            CompetitionQueryService.class,
            CompetitionCommandService.class,
            CompetitionLifecycleService.class,
            CompetitionConfigurationService.class,
            CompetitionScoringExportService.class,
            AdminEntryService.class,
            PortalEntryService.class,
            EntryDeliveryService.class,
            EntryPaymentAdminService.class,
            EntryRefundService.class,
            EntryDocumentService.class,
            JudgeEntryQueryService.class,
            RoundQueryService.class,
            RoundAllocationService.class,
            RoundLifecycleService.class,
            JudgeRoundTaskService.class,
            ScoreConfirmationService.class,
            RankingService.class
    );

    @Test
    void removedGiantFacadesCannotBeLoaded() {
        for (String typeName : REMOVED_FACADE_TYPES) {
            assertThatExceptionOfType(ClassNotFoundException.class)
                    .as("旧巨型门面不应重新进入类路径：%s", typeName)
                    .isThrownBy(() -> Class.forName(typeName));
        }
    }

    @Test
    void plannedServiceBoundariesRemainInterfaces() {
        assertThat(SPLIT_SERVICE_INTERFACES)
                .as("三个巨型 Service 的拆分边界不应被重新合并")
                .allMatch(Class::isInterface);
    }

    @Test
    void competitionAccessExtensionPointHasConcreteGuard() {
        assertThat(CompetitionAccessService.class.isInterface()).isTrue();
        assertThat(findImplementations(CompetitionAccessService.class))
                .contains(CompetitionAccessServiceImpl.class.getName());
    }

    @Test
    void futurePhaseTwoExtensionPointsHaveNoPlaceholderImplementations() {
        for (Class<?> extensionPoint : UNIMPLEMENTED_PHASE_TWO_EXTENSION_POINTS) {
            assertThat(extensionPoint.isInterface())
                    .as("二期扩展点必须保持为接口：%s", extensionPoint.getName())
                    .isTrue();
            assertThat(findImplementations(extensionPoint))
                    .as("未进入当前开发切片的扩展点不应提供默认实现：%s", extensionPoint.getName())
                    .isEmpty();
        }
    }

    private List<String> findImplementations(Class<?> extensionPoint) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(extensionPoint));
        return scanner.findCandidateComponents("com.beercompetition").stream()
                .map(BeanDefinition::getBeanClassName)
                .sorted()
                .toList();
    }
}
