package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.CompetitionSponsorMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.pojo.dto.CompetitionSponsorBatchUpdateRequest;
import com.beercompetition.pojo.dto.CompetitionSponsorItemRequest;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionSponsor;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.vo.CompetitionSponsorLogoVO;
import com.beercompetition.pojo.vo.CompetitionSponsorVO;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.service.CompetitionSponsorService;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionSponsorServiceImpl implements CompetitionSponsorService {

    private static final String BUSINESS_TYPE_SPONSOR_LOGO = "COMPETITION_SPONSOR_LOGO";
    private static final String OWNER_TYPE_COMPETITION = "COMPETITION";
    private static final long MAX_LOGO_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> LOGO_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final CompetitionMapper competitionMapper;
    private final CompetitionSponsorMapper competitionSponsorMapper;
    private final FileAssetMapper fileAssetMapper;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Override
    public List<CompetitionSponsorVO> listSponsors(Long competitionId) {
        // 1) 参数规范化与前置校验
        requireCompetition(competitionId);

        // 2) 查询赞助商和关联 Logo 文件
        List<CompetitionSponsor> sponsors = listSponsorEntities(competitionId);
        Map<Long, FileAsset> assetById = loadAssets(sponsors);

        // 3) 组装并返回结果
        return sponsors.stream()
                .map(sponsor -> toSponsorVO(sponsor, assetById.get(sponsor.getLogoAssetId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CompetitionSponsorVO> updateSponsors(Long competitionId, CompetitionSponsorBatchUpdateRequest request) {
        // 1) 参数规范化与前置校验
        requireCompetition(competitionId);
        List<CompetitionSponsorItemRequest> items = request == null || request.getItems() == null
                ? List.of()
                : request.getItems();
        loadAndValidateLogoAssets(competitionId, items);

        // 2) 用整体覆盖方式保存本场比赛赞助商配置
        competitionSponsorMapper.delete(new LambdaQueryWrapper<CompetitionSponsor>()
                .eq(CompetitionSponsor::getCompetitionId, competitionId));
        for (int i = 0; i < items.size(); i++) {
            CompetitionSponsorItemRequest item = items.get(i);
            String tierLabel = normalizeRequired(item.getTierLabel(), "赞助等级不能为空");
            String sponsorName = normalizeRequired(item.getSponsorName(), "赞助商名称不能为空");
            CompetitionSponsor sponsor = CompetitionSponsor.builder()
                    .competitionId(competitionId)
                    .tierLabel(tierLabel)
                    .sponsorName(sponsorName)
                    .logoAssetId(item.getLogoAssetId())
                    .sortOrder(item.getSortOrder() == null ? i : item.getSortOrder())
                    .featuredFlag(Boolean.TRUE.equals(item.getFeatured()) ? 1 : 0)
                    .enabledFlag(item.getEnabled() == null || Boolean.TRUE.equals(item.getEnabled()) ? 1 : 0)
                    .build();
            competitionSponsorMapper.insert(sponsor);
        }

        // 3) 组装并返回结果
        return listSponsors(competitionId);
    }

    @Override
    public CompetitionSponsorLogoVO uploadSponsorLogo(Long competitionId, MultipartFile file) {
        // 1) 参数规范化与前置校验
        requireCompetition(competitionId);
        validateLogoFile(file);
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "sponsor-logo.png");
        byte[] bytes = readUploadBytes(file);

        // 2) 上传文件并登记资产
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_SPONSOR_LOGO, filename, bytes);
        String publicUrl = resolveUploadPublicUrl(storagePath);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_SPONSOR_LOGO)
                .ownerType(OWNER_TYPE_COMPETITION)
                .ownerId(competitionId)
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);

        // 3) 返回前端可绑定的文件信息
        return CompetitionSponsorLogoVO.builder()
                .fileAssetId(asset.getId())
                .fileName(asset.getFileName())
                .publicUrl(asset.getPublicUrl())
                .build();
    }

    private Competition requireCompetition(Long competitionId) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private List<CompetitionSponsor> listSponsorEntities(Long competitionId) {
        return competitionSponsorMapper.selectList(new LambdaQueryWrapper<CompetitionSponsor>()
                .eq(CompetitionSponsor::getCompetitionId, competitionId)
                .orderByAsc(CompetitionSponsor::getSortOrder)
                .orderByAsc(CompetitionSponsor::getId));
    }

    private Map<Long, FileAsset> loadAssets(List<CompetitionSponsor> sponsors) {
        Set<Long> assetIds = sponsors.stream()
                .map(CompetitionSponsor::getLogoAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return fileAssetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, FileAsset> loadAndValidateLogoAssets(Long competitionId, List<CompetitionSponsorItemRequest> items) {
        Set<Long> assetIds = items.stream()
                .map(CompetitionSponsorItemRequest::getLogoAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, FileAsset> assetById = fileAssetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity(), (left, right) -> left));
        for (Long assetId : assetIds) {
            FileAsset asset = assetById.get(assetId);
            if (asset == null
                    || !BUSINESS_TYPE_SPONSOR_LOGO.equals(asset.getBusinessType())
                    || !OWNER_TYPE_COMPETITION.equals(asset.getOwnerType())
                    || !Objects.equals(asset.getOwnerId(), competitionId)) {
                throw new BaseException("赞助商 Logo 文件无效");
            }
        }
        return assetById;
    }

    private CompetitionSponsorVO toSponsorVO(CompetitionSponsor sponsor, FileAsset asset) {
        return CompetitionSponsorVO.builder()
                .id(sponsor.getId())
                .tierLabel(sponsor.getTierLabel())
                .sponsorName(sponsor.getSponsorName())
                .logoAssetId(sponsor.getLogoAssetId())
                .logoUrl(asset == null ? null : asset.getPublicUrl())
                .sortOrder(sponsor.getSortOrder())
                .featured(Objects.equals(sponsor.getFeaturedFlag(), 1))
                .enabled(Objects.equals(sponsor.getEnabledFlag(), 1))
                .build();
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择赞助商 Logo 图片");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BaseException("赞助商 Logo 不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (!LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new BaseException("赞助商 Logo 仅支持 JPG、PNG、WebP 图片");
        }
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException("读取赞助商 Logo 失败");
        }
    }

    private String sanitizeUploadFilename(String originalFilename, String fallback) {
        String raw = StringUtils.hasText(originalFilename) ? originalFilename : fallback;
        String filename = Path.of(raw).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
        return UUID.randomUUID() + "-" + filename;
    }

    private String resolveUploadPublicUrl(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return "";
        }
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath;
        }
        String relativePath = storagePath.replace("\\", "/");
        if (relativePath.startsWith("uploads/")) {
            relativePath = relativePath.substring("uploads/".length());
        }
        return "/uploads/" + relativePath;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }
}
