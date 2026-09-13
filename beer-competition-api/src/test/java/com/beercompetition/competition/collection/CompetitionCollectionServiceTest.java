package com.beercompetition.competition.collection;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.CompetitionCollectionConfigMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pojo.dto.CompetitionCollectionConfigUpdateRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCollectionConfig;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitionCollectionServiceTest {

    private final CompetitionCollectionConfigMapper configMapper = mock(CompetitionCollectionConfigMapper.class);
    private final CompetitionMapper competitionMapper = mock(CompetitionMapper.class);
    private final OrganizerMapper organizerMapper = mock(OrganizerMapper.class);
    private final FileAssetMapper fileAssetMapper = mock(FileAssetMapper.class);
    private final CompetitionAccessService accessService = mock(CompetitionAccessService.class);
    private final FileStorageService storageService = mock(FileStorageService.class);
    private final StorageProperties storageProperties = new StorageProperties();
    private final CompetitionCollectionServiceImpl service = new CompetitionCollectionServiceImpl(
        configMapper, competitionMapper, organizerMapper, fileAssetMapper, accessService,
            storageService, storageProperties, new ObjectMapper());

    @Test
    void bankTransferRequiresCompleteAccountDetails() {
        givenTenantCompetition();
        CompetitionCollectionConfigUpdateRequest request = new CompetitionCollectionConfigUpdateRequest();
        request.setEnabledMethods(List.of("BANK_TRANSFER"));
        request.setBankAccountName("账户");

        assertThatThrownBy(() -> service.updateAdminConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .hasMessage("启用银行转账后必须填写账户名、账号和开户行");
    }

    @Test
    void validBankTransferConfigurationIsPersisted() {
        givenTenantCompetition();
        CompetitionCollectionConfigUpdateRequest request = new CompetitionCollectionConfigUpdateRequest();
        request.setEnabledMethods(List.of("bank_transfer"));
        request.setBankAccountName("账户");
        request.setBankAccountNo("6222");
        request.setBankName("测试银行");

        assertThat(service.updateAdminConfig(1L, request).getEnabledMethods())
                .containsExactly("BANK_TRANSFER");
        verify(configMapper).insert(any(CompetitionCollectionConfig.class));
    }

    @Test
    void qrAssetMustBelongToCurrentCompetition() {
        givenTenantCompetition();
        CompetitionCollectionConfigUpdateRequest request = new CompetitionCollectionConfigUpdateRequest();
        request.setEnabledMethods(List.of("WECHAT_QR"));
        request.setWechatQrAssetId(20L);
        when(fileAssetMapper.selectById(20L)).thenReturn(FileAsset.builder()
                .id(20L)
                .businessType(CompetitionCollectionServiceImpl.BUSINESS_TYPE_COLLECTION_QR)
                .ownerType("COMPETITION")
                .ownerId(99L)
                .organizerId(2L)
                .build());

        assertThatThrownBy(() -> service.updateAdminConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .hasMessage("微信收款码文件无效");
    }

    @Test
    void tenantCompetitionRejectsDisabledPaymentMethod() {
        givenTenantCompetition();
        when(competitionMapper.selectById(1L)).thenReturn(Competition.builder()
                .id(1L).organizerId(2L).status("REGISTRATION_OPEN").build());
        when(configMapper.selectOne(any())).thenReturn(CompetitionCollectionConfig.builder()
                .competitionId(1L)
                .enabledMethodsJson("[\"WECHAT_QR\"]")
                .build());

        assertThatThrownBy(() -> service.requirePortalPaymentMethodEnabled(1L, EntryPayMethod.BANK_TRANSFER))
                .isInstanceOf(BaseException.class)
                .hasMessage("该赛事未开放银行转账");
    }

    private void givenTenantCompetition() {
        when(competitionMapper.selectById(1L)).thenReturn(Competition.builder()
                .id(1L).organizerId(2L).status("DRAFT").build());
        when(organizerMapper.selectById(2L)).thenReturn(Organizer.builder()
                .id(2L).organizerType("TENANT").status("ACTIVE").build());
        when(configMapper.selectOne(any())).thenReturn(null);
    }
}
