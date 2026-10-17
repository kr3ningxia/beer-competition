package com.beercompetition.registration.entry;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.competition.configuration.CompetitionCategoryStyleService;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionStyleConfigMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryFieldConfigMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.enums.CompetitionStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.CompetitionStyleConfig;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.registration.payment.CompetitionPricingService;
import com.beercompetition.service.EntryScanLabelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalEntryCreationServiceTest {

    @Mock
    private BeerEntryMapper beerEntryMapper;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private CompetitionCategoryMapper competitionCategoryMapper;
    @Mock
    private CompetitionStyleConfigMapper competitionStyleConfigMapper;
    @Mock
    private CompetitionCategoryStyleService competitionCategoryStyleService;
    @Mock
    private EntryPaymentMapper entryPaymentMapper;
    @Mock
    private EntryDeliveryMapper entryDeliveryMapper;
    @Mock
    private EntryFieldConfigMapper entryFieldConfigMapper;
    @Mock
    private BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    @Mock
    private EntryScanLabelService entryScanLabelService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PortalEntryViewAssembler portalEntryViewAssembler;
    @Mock
    private PortalAccountAccessService portalAccountAccessService;
    @Mock
    private CompetitionPricingService competitionPricingService;

    @InjectMocks
    private PortalEntryCreationService portalEntryCreationService;

    @Test
    void rejectsStyleOutsideCategoryScope() {
        PortalAccount account = PortalAccount.builder().id(1L).breweryId(2L).build();
        Competition competition = Competition.builder()
                .id(9L)
                .status(CompetitionStatus.REGISTRATION_OPEN.name())
                .registrationDeadline(LocalDateTime.now().plusDays(1))
                .build();
        CompetitionCategory category = CompetitionCategory.builder()
                .id(3L)
                .competitionId(9L)
                .name("IPA 组")
                .build();
        CompetitionStyleConfig style = CompetitionStyleConfig.builder()
                .id(4L)
                .competitionId(9L)
                .name("德式皮尔森")
                .activeFlag(1)
                .build();
        when(portalAccountAccessService.requireCurrentAccount()).thenReturn(account);
        when(competitionMapper.selectById(9L)).thenReturn(competition);
        when(competitionMapper.selectOne(any())).thenReturn(competition);
        when(competitionCategoryMapper.selectOne(any())).thenReturn(category);
        when(competitionStyleConfigMapper.selectOne(any())).thenReturn(style);
        doThrow(new BaseException("该基础风格不在所选投递组别的可选范围内"))
                .when(competitionCategoryStyleService).requireStyleAllowed(anyLong(), anyString());

        PortalEntrySubmitRequest request = new PortalEntrySubmitRequest();
        request.setName("测试酒款");
        request.setCategoryId(3L);
        request.setStyle("德式皮尔森");
        request.setAbv(BigDecimal.valueOf(5));

        assertThatThrownBy(() -> portalEntryCreationService.submitPortalEntry(9L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("不在所选投递组别的可选范围内");
        verify(beerEntryMapper, never()).insert(any(BeerEntry.class));
    }
}
