package com.beercompetition.registration.entry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ForbiddenException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.support.EntryLabelFileGenerator;
import com.beercompetition.service.support.EntryLabelFileGenerator.LabelRenderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Set;
import com.beercompetition.registration.entry.EntryDocumentService;

/**
 * 生成报名标签文件并保护厂商报名所有权。
 */
@Service
@RequiredArgsConstructor
public class EntryDocumentServiceImpl implements EntryDocumentService {

    private static final Set<String> LABEL_ALLOWED_STATUSES = Set.of(
                EntryStatus.REGISTERED.name(),
                EntryStatus.STORED.name(),
                EntryStatus.RESULT_PUBLISHED.name()
        );

    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
                EntryRefundStatus.REQUESTED.name(),
                EntryRefundStatus.APPROVED.name(),
                EntryRefundStatus.PROCESSING.name()
        );

    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final PortalAccountMapper portalAccountMapper;

    private final BeerEntryMapper beerEntryMapper;

    private final CompetitionMapper competitionMapper;

    private final CompetitionCategoryMapper competitionCategoryMapper;

    private final EntryRefundMapper entryRefundMapper;

    private final BreweryMapper breweryMapper;

    private final EntryScanLabelService entryScanLabelService;

    private final EntryLabelFileGenerator entryLabelFileGenerator;

    @Override
    public PortalEntryLabelVO getPortalEntryLabel(Long entryId) {
        // 1) 查询并校验作品归属
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        assertCanDownloadPortalLabel(entry);

        // 2) 组装标签数据
        Competition competition = competitionMapper.selectById(entry.getCompetitionId());
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        return PortalEntryLabelVO.builder()
                .id(entry.getId())
                .uuid(entry.getUuid())
                .labelCode(label.getLabelCode())
                .shortCode(label.getShortCode())
                .scanToken(label.getScanToken())
                .name(entry.getName())
                .competitionName(competition == null ? null : competition.getName())
                .competitionCode(competition == null ? null : competition.getCode())
                .categoryName(category == null ? null : category.getName())
                .style(entry.getStyle())
                .abv(entry.getAbv())
                .build();
    }

    @Override
    public FileDownloadVO downloadPortalEntryLabelPdf(Long entryId) {
        // 1) 查询并校验作品归属与标签下载状态
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        assertCanDownloadPortalLabel(entry);

        // 2) 查询标签上下文并生成 A4 四联 PDF
        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        LabelRenderItem item = toLabelRenderItem(entry, label, category == null ? null : category.getName());
        byte[] content = entryLabelFileGenerator.buildFourUpPdf(List.of(item, item, item, item));

        // 3) 返回下载文件
        return FileDownloadVO.builder()
                .fileName(buildPortalLabelPdfFilename(entry, label))
                .contentType(EntryLabelFileGenerator.CONTENT_TYPE_PDF)
                .content(content)
                .build();
    }

    @Override
    public FileDownloadVO downloadPortalEntryLabelPng(Long entryId) {
        PortalAccount account = requirePortalAccount();
        BeerEntry entry = requireOwnedEntry(entryId, account.getBreweryId());
        assertCanDownloadPortalLabel(entry);

        CompetitionCategory category = competitionCategoryMapper.selectById(entry.getCategoryId());
        EntryScanLabel label = entryScanLabelService.requireActiveLabel(entry.getId());
        LabelRenderItem item = toLabelRenderItem(entry, label, category == null ? null : category.getName());
        byte[] content = entryLabelFileGenerator.buildFourUpPng(List.of(item, item, item, item));

        return FileDownloadVO.builder()
                .fileName(buildPortalLabelPngFilename(entry, label))
                .contentType(EntryLabelFileGenerator.CONTENT_TYPE_PNG)
                .content(content)
                .build();
    }

    private EntryRefund findLatestRefund(Long beerEntryId) {
        return entryRefundMapper.selectOne(new LambdaQueryWrapper<EntryRefund>()
                .eq(EntryRefund::getBeerEntryId, beerEntryId)
                .orderByDesc(EntryRefund::getId)
                .last("LIMIT 1"));
    }

    private boolean hasActiveRefund(Long beerEntryId) {
        return isActiveRefund(findLatestRefund(beerEntryId));
    }

    private void assertCanDownloadPortalLabel(BeerEntry entry) {
        if (!LABEL_ALLOWED_STATUSES.contains(entry.getStatus())) {
            throw new BaseException("支付成功后才能下载现场参赛标签");
        }
        if (hasActiveRefund(entry.getId())) {
            throw new BaseException("退款处理中，不能下载现场参赛标签");
        }
    }

    private LabelRenderItem toLabelRenderItem(BeerEntry entry, EntryScanLabel label, String categoryName) {
        return new LabelRenderItem(
                entry.getUuid(),
                label.getLabelCode(),
                label.getShortCode(),
                label.getScanToken(),
                categoryName
        );
    }

    private boolean isActiveRefund(EntryRefund refund) {
        return refund != null && ACTIVE_REFUND_STATUSES.contains(refund.getStatus());
    }

    private BeerEntry requireOwnedEntry(Long entryId, Long breweryId) {
        BeerEntry entry = requireEntry(entryId);
        if (!entry.getBreweryId().equals(breweryId)) {
            throw new ForbiddenException("无权查看该酒款");
        }
        return entry;
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private PortalAccount requirePortalAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeDownloadFilename(String value) {
        String filename = StringUtils.hasText(value) ? value.trim() : "entry-label";
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String buildPortalLabelPdfFilename(BeerEntry entry, EntryScanLabel label) {
        String entryName = safeDownloadFilename(firstText(entry.getName(), entry.getUuid()));
        String shortCode = safeDownloadFilename(firstText(label.getShortCode(), entry.getUuid()));
        return entryName + "-" + shortCode + "-现场参赛标签.pdf";
    }

    private String buildPortalLabelPngFilename(BeerEntry entry, EntryScanLabel label) {
        String entryName = safeDownloadFilename(firstText(entry.getName(), entry.getUuid()));
        String shortCode = safeDownloadFilename(firstText(label.getShortCode(), entry.getUuid()));
        return entryName + "-" + shortCode + "-现场参赛标签.png";
    }
}
