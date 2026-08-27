package com.beercompetition.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.AwardResultMapper;
import com.beercompetition.mapper.BankTransferPaymentMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.OrganizerApplicationMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.pojo.enums.AwardResultStatus;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.po.AwardResult;
import com.beercompetition.pojo.po.BankTransferPayment;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.OrganizerApplication;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.competition.collection.CompetitionCollectionServiceImpl;
import com.beercompetition.service.support.AwardCertificateFileType;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 文件访问授权编排。存储实现只负责字节读取，本类负责业务归属、身份和公开状态校验。
 */
@Service
@RequiredArgsConstructor
public class FileAccessServiceImpl implements FileAccessService {

    private static final String BUSINESS_BANK_TRANSFER = "BANK_TRANSFER_VOUCHER";
    private static final String BUSINESS_OFFLINE_REFUND = "OFFLINE_REFUND_VOUCHER";
    private static final String BUSINESS_AWARD_CERTIFICATE = "AWARD_CERTIFICATE";
    private static final String BUSINESS_APPLICATION_MATERIAL = "ORGANIZER_APPLICATION_MATERIAL";
    private static final String BUSINESS_BREWERY_AVATAR = "BREWERY_AVATAR";
    private static final String BUSINESS_SPONSOR_LOGO = "COMPETITION_SPONSOR_LOGO";
    private static final String BUSINESS_COLLECTION_QR = CompetitionCollectionServiceImpl.BUSINESS_TYPE_COLLECTION_QR;
    private static final String OWNER_PORTAL_ACCOUNT = "PORTAL_ACCOUNT";
    private static final String OWNER_COMPETITION = "COMPETITION";
    private static final String OWNER_AWARD = "AWARD_RESULT";
    private static final String OWNER_APPLICATION = "ORGANIZER_APPLICATION";
    private static final Set<String> PUBLIC_BUSINESS_TYPES = Set.of(BUSINESS_BREWERY_AVATAR, BUSINESS_SPONSOR_LOGO);

    private final FileAssetMapper fileAssetMapper;
    private final BankTransferPaymentMapper bankTransferPaymentMapper;
    private final EntryRefundMapper entryRefundMapper;
    private final AwardResultMapper awardResultMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final PortalAccountMapper portalAccountMapper;
    private final CompetitionMapper competitionMapper;
    private final FileStorageService fileStorageService;
    private final CompetitionAccessService competitionAccessService;

    @Override
    public FileDownloadVO download(Long fileAssetId) {
        FileAsset asset = requireAsset(fileAssetId);
        authorizeCurrentUser(asset);
        return read(asset);
    }

    @Override
    public FileDownloadVO downloadPublic(Long fileAssetId) {
        FileAsset asset = requireAsset(fileAssetId);
        authorizePublic(asset);
        return read(asset);
    }

    @Override
    public String publicUrl(Long fileAssetId) {
        FileAsset asset = requireAsset(fileAssetId);
        if (!PUBLIC_BUSINESS_TYPES.contains(asset.getBusinessType())) {
            return null;
        }
        return "/api/portal/public/files/" + asset.getId();
    }

    @Override
    public void requireDownloadAccess(Long fileAssetId) {
        authorizeCurrentUser(requireAsset(fileAssetId));
    }

    @Override
    public void requireManagementAccess(Long fileAssetId) {
        FileAsset asset = requireAsset(fileAssetId);
        if (!UserRole.ADMIN.name().equals(BaseContext.getCurrentRole())) {
            throw new ForbiddenException("当前身份无权管理文件");
        }
        if (BUSINESS_APPLICATION_MATERIAL.equals(asset.getBusinessType())) {
            competitionAccessService.requirePlatformSuperAdmin();
            return;
        }
        if (BUSINESS_SPONSOR_LOGO.equals(asset.getBusinessType())
                && OWNER_COMPETITION.equals(asset.getOwnerType())) {
            requireCompetition(asset.getOwnerId(), "赞助商 Logo 关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_COLLECTION_QR.equals(asset.getBusinessType())) {
            requireCompetition(asset.getOwnerId(), "收款码关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_AWARD_CERTIFICATE.equals(asset.getBusinessType())) {
            AwardResult award = findAward(asset.getId());
            if (award == null) {
                throw new ResourceNotFoundException("证书关联奖项不存在");
            }
            requireCompetition(award.getCompetitionId(), "证书关联比赛不存在", asset);
            return;
        }
        throw new ForbiddenException("当前账号无权管理该文件");
    }

    private void authorizeCurrentUser(FileAsset asset) {
        String role = BaseContext.getCurrentRole();
        if (UserRole.ADMIN.name().equals(role)) {
            authorizeAdmin(asset);
            return;
        }
        if (UserRole.PORTAL.name().equals(role)) {
            authorizePortal(asset);
            return;
        }
        throw new ForbiddenException("当前身份无权下载文件");
    }

    private void authorizeAdmin(FileAsset asset) {
        String businessType = asset.getBusinessType();
        if (BUSINESS_APPLICATION_MATERIAL.equals(businessType)) {
            competitionAccessService.requirePlatformSuperAdmin();
            return;
        }
        if (BUSINESS_BANK_TRANSFER.equals(businessType)) {
            BankTransferPayment transfer = findTransfer(asset.getId());
            requireCompetition(transfer == null ? null : transfer.getCompetitionId(), "转账记录不存在", asset);
            return;
        }
        if (BUSINESS_OFFLINE_REFUND.equals(businessType)) {
            EntryRefund refund = findRefund(asset.getId());
            BeerEntry entry = refund == null ? null : beerEntryMapper.selectById(refund.getBeerEntryId());
            requireCompetition(entry == null ? null : entry.getCompetitionId(), "退款记录不存在", asset);
            return;
        }
        if (BUSINESS_AWARD_CERTIFICATE.equals(businessType)) {
            AwardResult award = findAward(asset.getId());
            requireCompetition(award == null ? null : award.getCompetitionId(), "证书关联奖项不存在", asset);
            return;
        }
        if (BUSINESS_SPONSOR_LOGO.equals(businessType) && OWNER_COMPETITION.equals(asset.getOwnerType())) {
            requireCompetition(asset.getOwnerId(), "赞助商 Logo 关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_COLLECTION_QR.equals(businessType) && OWNER_COMPETITION.equals(asset.getOwnerType())) {
            requireCompetition(asset.getOwnerId(), "收款码关联比赛不存在", asset);
            return;
        }
        throw new ForbiddenException("当前账号无权下载该文件");
    }

    private void authorizePortal(FileAsset asset) {
        Long portalAccountId = BaseContext.getCurrentId();
        if (BUSINESS_BANK_TRANSFER.equals(asset.getBusinessType())) {
            BankTransferPayment transfer = findTransfer(asset.getId());
            if (transfer == null || !portalAccountId.equals(transfer.getPortalAccountId())) {
                throw new ForbiddenException("无权下载该转账凭证");
            }
            requireCompetitionAsset(transfer.getCompetitionId(), "转账记录关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_AWARD_CERTIFICATE.equals(asset.getBusinessType())) {
            AwardResult award = findAward(asset.getId());
            BeerEntry entry = award == null ? null : beerEntryMapper.selectById(award.getBeerEntryId());
            PortalAccount account = portalAccountMapper.selectById(portalAccountId);
            if (award == null || entry == null || account == null
                    || !portalAccountId.equals(account.getId())
                    || !Integer.valueOf(1).equals(account.getStatus())
                    || !entry.getBreweryId().equals(account.getBreweryId())
                    || !AwardResultStatus.PUBLISHED.name().equals(award.getStatus())) {
                throw new ForbiddenException("无权下载该奖状");
            }
            requireCompetitionAsset(award.getCompetitionId(), "证书关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_COLLECTION_QR.equals(asset.getBusinessType())) {
            PortalAccount account = portalAccountMapper.selectById(portalAccountId);
            if (account == null || !Integer.valueOf(1).equals(account.getStatus())) {
                throw new ForbiddenException("当前厂商账号无效");
            }
            requireCompetitionAsset(asset.getOwnerId(), "收款码关联比赛不存在", asset);
            return;
        }
        if (BUSINESS_BREWERY_AVATAR.equals(asset.getBusinessType())
                && OWNER_PORTAL_ACCOUNT.equals(asset.getOwnerType())
                && portalAccountId.equals(asset.getOwnerId())) {
            return;
        }
        throw new ForbiddenException("当前厂商无权下载该文件");
    }

    private void authorizePublic(FileAsset asset) {
        if (PUBLIC_BUSINESS_TYPES.contains(asset.getBusinessType())) {
            if (BUSINESS_BREWERY_AVATAR.equals(asset.getBusinessType())
                    && !OWNER_PORTAL_ACCOUNT.equals(asset.getOwnerType())) {
                throw new ForbiddenException("公开文件归属不正确");
            }
            if (BUSINESS_SPONSOR_LOGO.equals(asset.getBusinessType())
                    && !OWNER_COMPETITION.equals(asset.getOwnerType())) {
                throw new ForbiddenException("公开文件归属不正确");
            }
            if (BUSINESS_SPONSOR_LOGO.equals(asset.getBusinessType())) {
                requireCompetitionAsset(asset.getOwnerId(), "赞助商 Logo 关联比赛不存在", asset);
            }
            return;
        }
        if (BUSINESS_AWARD_CERTIFICATE.equals(asset.getBusinessType())) {
            AwardResult award = findAward(asset.getId());
            if (award == null || !AwardResultStatus.PUBLISHED.name().equals(award.getStatus())) {
                throw new ResourceNotFoundException("奖状暂未公开");
            }
            requireCompetitionAsset(award.getCompetitionId(), "证书关联比赛不存在", asset);
            return;
        }
        throw new ForbiddenException("该文件不允许公开访问");
    }

    private FileDownloadVO read(FileAsset asset) {
        if (!StringUtils.hasText(asset.getStoragePath())) {
            throw new ResourceNotFoundException("文件存储路径不存在");
        }
        return FileDownloadVO.builder()
                .fileName(StringUtils.hasText(asset.getFileName()) ? asset.getFileName() : "download")
                .contentType(resolveContentType(asset.getFileName()))
                .content(fileStorageService.download(asset.getStoragePath()))
                .build();
    }

    private FileAsset requireAsset(Long fileAssetId) {
        if (fileAssetId == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        FileAsset asset = fileAssetMapper.selectById(fileAssetId);
        if (asset == null) {
            throw new ResourceNotFoundException("文件不存在");
        }
        return asset;
    }

    private BankTransferPayment findTransfer(Long assetId) {
        return bankTransferPaymentMapper.selectOne(new LambdaQueryWrapper<BankTransferPayment>()
                .eq(BankTransferPayment::getVoucherAssetId, assetId)
                .last("LIMIT 1"));
    }

    private EntryRefund findRefund(Long assetId) {
        return entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getOfflineRefundVoucherAssetId, assetId)
                .last("LIMIT 1"));
    }

    private AwardResult findAward(Long assetId) {
        return awardResultMapper.selectOne(new LambdaQueryWrapper<AwardResult>()
                .eq(AwardResult::getCertificateAssetId, assetId)
                .last("LIMIT 1"));
    }

    private void requireCompetition(Long competitionId, String missingMessage, FileAsset asset) {
        if (competitionId == null) {
            throw new ResourceNotFoundException(missingMessage);
        }
        requireCompetitionAsset(competitionId, missingMessage, asset);
        competitionAccessService.requireCompetitionAccess(competitionId);
    }

    private Competition requireCompetitionAsset(Long competitionId, String missingMessage, FileAsset asset) {
        if (competitionId == null) {
            throw new ResourceNotFoundException(missingMessage);
        }
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException(missingMessage);
        }
        if (asset.getOrganizerId() == null || competition.getOrganizerId() == null
                || !Objects.equals(asset.getOrganizerId(), competition.getOrganizerId())) {
            throw new ForbiddenException("文件归属与比赛组织不一致");
        }
        return competition;
    }

    private String resolveContentType(String fileName) {
        if (AwardCertificateFileType.fromFilename(fileName).isPresent()) {
            return AwardCertificateFileType.resolveContentType(fileName);
        }
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
