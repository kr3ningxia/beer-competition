package com.beercompetition.file;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BankTransferPaymentMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.OrganizerApplicationMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.BankTransferPayment;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.service.CompetitionJudgePublicProfileService;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAccessServiceImplTest {

    @Mock
    private FileAssetMapper fileAssetMapper;
    @Mock
    private BankTransferPaymentMapper bankTransferPaymentMapper;
    @Mock
    private EntryRefundMapper entryRefundMapper;
    @Mock
    private AwardResultMapper awardResultMapper;
    @Mock
    private BeerEntryMapper beerEntryMapper;
    @Mock
    private OrganizerApplicationMapper organizerApplicationMapper;
    @Mock
    private PortalAccountMapper portalAccountMapper;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CompetitionAccessService competitionAccessService;
    @Mock
    private CompetitionJudgePublicProfileService competitionJudgePublicProfileService;

    @InjectMocks
    private FileAccessServiceImpl fileAccessService;

    @AfterEach
    void clearContext() {
        BaseContext.clear();
    }

    @Test
    void adminDownloadRejectsFileWhoseOrganizerDiffersFromCompetition() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(1L)
                .role(UserRole.ADMIN.name())
                .build());
        FileAsset asset = FileAsset.builder()
                .id(7L)
                .organizerId(8L)
                .businessType("BANK_TRANSFER_VOUCHER")
                .storagePath("voucher/7.pdf")
                .fileName("voucher.pdf")
                .build();
        when(fileAssetMapper.selectById(7L)).thenReturn(asset);
        when(bankTransferPaymentMapper.selectOne(any())).thenReturn(
                BankTransferPayment.builder().competitionId(9L).voucherAssetId(7L).build());
        when(competitionMapper.selectById(9L)).thenReturn(
                Competition.builder().id(9L).organizerId(10L).build());

        assertThatThrownBy(() -> fileAccessService.download(7L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("文件归属与比赛组织不一致");
        verify(competitionAccessService, never()).requireCompetitionAccess(9L);
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void portalDownloadAlsoRejectsMismatchedTransferScope() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(22L)
                .role(UserRole.PORTAL.name())
                .build());
        FileAsset asset = FileAsset.builder()
                .id(8L)
                .organizerId(8L)
                .businessType("BANK_TRANSFER_VOUCHER")
                .storagePath("voucher/8.pdf")
                .fileName("voucher.pdf")
                .build();
        when(fileAssetMapper.selectById(8L)).thenReturn(asset);
        when(bankTransferPaymentMapper.selectOne(any())).thenReturn(
                BankTransferPayment.builder().competitionId(9L).portalAccountId(22L).voucherAssetId(8L).build());
        when(competitionMapper.selectById(9L)).thenReturn(
                Competition.builder().id(9L).organizerId(10L).build());

        assertThatThrownBy(() -> fileAccessService.download(8L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("文件归属与比赛组织不一致");
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void judgeCanReadOnlyOwnJudgeAvatar() {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(22L)
                .role(UserRole.JUDGE.name())
                .build());
        FileAsset asset = FileAsset.builder()
                .id(12L)
                .businessType("JUDGE_AVATAR")
                .ownerType("JUDGE_ACCOUNT")
                .ownerId(22L)
                .storagePath("judge-avatar/12.png")
                .fileName("avatar.png")
                .build();
        when(fileAssetMapper.selectById(12L)).thenReturn(asset);
        when(fileStorageService.download("judge-avatar/12.png")).thenReturn(new byte[] {1, 2});

        FileDownloadVO result = fileAccessService.download(12L);

        assertThat(result.getContent()).containsExactly(1, 2);
    }

    @Test
    void judgeAvatarWithoutPublishedSnapshotCannotBePublic() {
        FileAsset asset = FileAsset.builder()
                .id(13L)
                .businessType("JUDGE_AVATAR")
                .ownerType("JUDGE_ACCOUNT")
                .ownerId(22L)
                .storagePath("judge-avatar/13.png")
                .fileName("avatar.png")
                .build();
        when(fileAssetMapper.selectById(13L)).thenReturn(asset);
        when(competitionJudgePublicProfileService.isPublishedAvatarReferenced(13L)).thenReturn(false);

        assertThatThrownBy(() -> fileAccessService.downloadPublic(13L))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("评委头像暂未公开");
        verify(fileStorageService, never()).download(any());
    }
}
