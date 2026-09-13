package com.beercompetition.competition.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.competition.access.CompetitionAccessService;
import com.beercompetition.mapper.CompetitionCollectionConfigMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.OrganizerMapper;
import com.beercompetition.pojo.dto.CompetitionCollectionConfigUpdateRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.OrganizerType;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCollectionConfig;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.Organizer;
import com.beercompetition.pojo.vo.CompetitionCollectionConfigVO;
import com.beercompetition.pojo.vo.CompetitionCollectionQrVO;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompetitionCollectionServiceImpl implements CompetitionCollectionService {

    public static final String BUSINESS_TYPE_COLLECTION_QR = "COMPETITION_COLLECTION_QR";
    private static final String OWNER_TYPE_COMPETITION = "COMPETITION";
    private static final String METHOD_WECHAT_QR = "WECHAT_QR";
    private static final String METHOD_BANK_TRANSFER = "BANK_TRANSFER";
    private static final long MAX_QR_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> QR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final CompetitionCollectionConfigMapper configMapper;
    private final CompetitionMapper competitionMapper;
    private final OrganizerMapper organizerMapper;
    private final FileAssetMapper fileAssetMapper;
    private final CompetitionAccessService competitionAccessService;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    @Override
    public CompetitionCollectionConfigVO getAdminConfig(Long competitionId) {
        Competition competition = requireTenantCompetition(competitionId);
        return toVO(competition, findConfig(competitionId), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionCollectionConfigVO updateAdminConfig(Long competitionId,
                                                           CompetitionCollectionConfigUpdateRequest request) {
        Competition competition = requireTenantCompetition(competitionId);
        if (request == null) {
            throw new BaseException("收款配置不能为空");
        }
        List<String> methods = normalizeMethods(request.getEnabledMethods());
        validateMethodConfiguration(methods, request);
        validateQrAsset(competition, request.getWechatQrAssetId(), methods.contains(METHOD_WECHAT_QR));

        CompetitionCollectionConfig config = findConfig(competitionId);
        if (config == null) {
            config = CompetitionCollectionConfig.builder().competitionId(competitionId).build();
        }
        config.setEnabledMethodsJson(writeMethods(methods));
        config.setWechatQrAssetId(request.getWechatQrAssetId());
        config.setBankAccountName(normalizeNullable(request.getBankAccountName()));
        config.setBankAccountNo(normalizeNullable(request.getBankAccountNo()));
        config.setBankName(normalizeNullable(request.getBankName()));
        config.setCollectionNote(normalizeNullable(request.getCollectionNote()));
        config.setPaymentContact(normalizeNullable(request.getPaymentContact()));
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return toVO(competition, config, false);
    }

    @Override
    public CompetitionCollectionQrVO uploadWechatQr(Long competitionId, MultipartFile file) {
        Competition competition = requireTenantCompetition(competitionId);
        validateQrFile(file);
        String fileName = sanitizeFilename(file.getOriginalFilename());
        byte[] bytes = readBytes(file);
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_COLLECTION_QR, fileName, bytes);
        FileAsset asset = FileAsset.builder()
                .organizerId(competition.getOrganizerId())
                .businessType(BUSINESS_TYPE_COLLECTION_QR)
                .ownerType(OWNER_TYPE_COMPETITION)
                .ownerId(competitionId)
                .storageProvider(storageProperties.getProvider())
                .fileName(fileName)
                .storagePath(storagePath)
                .publicUrl(null)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);
        return CompetitionCollectionQrVO.builder()
                .fileAssetId(asset.getId())
                .fileName(asset.getFileName())
                .adminUrl("/api/admin/files/" + asset.getId())
                .build();
    }

    @Override
    public CompetitionCollectionConfigVO getPortalConfig(Long competitionId) {
        Competition competition = requireVisibleCompetition(competitionId);
        Organizer organizer = organizerMapper.selectById(competition.getOrganizerId());
        if (organizer == null || !OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            return CompetitionCollectionConfigVO.builder()
                    .competitionId(competitionId)
                    .tenantCompetition(false)
                    .enabledMethods(List.of())
                    .build();
        }
        return toVO(competition, findConfig(competitionId), true);
    }

    @Override
    public void requirePortalPaymentMethodEnabled(Long competitionId, EntryPayMethod payMethod) {
        CompetitionCollectionConfigVO config = getPortalConfig(competitionId);
        if (!Boolean.TRUE.equals(config.getTenantCompetition())) {
            return;
        }
        String configuredMethod = switch (payMethod) {
            case WECHAT_QR -> METHOD_WECHAT_QR;
            case BANK_TRANSFER -> METHOD_BANK_TRANSFER;
            default -> throw new BaseException("该赛事不支持当前付款方式");
        };
        if (config.getEnabledMethods() == null || !config.getEnabledMethods().contains(configuredMethod)) {
            String methodName = payMethod == EntryPayMethod.WECHAT_QR ? "微信收款" : "银行转账";
            throw new BaseException("该赛事未开放" + methodName);
        }
    }

    private Competition requireTenantCompetition(Long competitionId) {
        competitionAccessService.requireCompetitionAccess(competitionId);
        Competition competition = requireCompetition(competitionId);
        Organizer organizer = organizerMapper.selectById(competition.getOrganizerId());
        if (organizer == null || !OrganizerType.TENANT.name().equals(organizer.getOrganizerType())) {
            throw new BaseException("平台自办赛事不使用主办方收款配置");
        }
        return competition;
    }

    private Competition requireVisibleCompetition(Long competitionId) {
        Competition competition = requireCompetition(competitionId);
        if ("DRAFT".equals(competition.getStatus()) || "ARCHIVED".equals(competition.getStatus())) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return competition;
    }

    private Competition requireCompetition(Long competitionId) {
        if (competitionId == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return competition;
    }

    private CompetitionCollectionConfig findConfig(Long competitionId) {
        return configMapper.selectOne(new LambdaQueryWrapper<CompetitionCollectionConfig>()
                .eq(CompetitionCollectionConfig::getCompetitionId, competitionId)
                .last("LIMIT 1"));
    }

    private CompetitionCollectionConfigVO toVO(Competition competition,
                                               CompetitionCollectionConfig config,
                                               boolean portal) {
        List<String> methods = config == null ? List.of() : readMethods(config.getEnabledMethodsJson());
        Long qrAssetId = config == null ? null : config.getWechatQrAssetId();
        return CompetitionCollectionConfigVO.builder()
                .competitionId(competition.getId())
                .tenantCompetition(true)
                .enabledMethods(methods)
                .wechatQrAssetId(qrAssetId)
                .wechatQrUrl(qrAssetId == null ? null
                        : (portal ? "/api/portal/public/files/" : "/api/admin/files/") + qrAssetId)
                .bankAccountName(config == null ? null : config.getBankAccountName())
                .bankAccountNo(config == null ? null : config.getBankAccountNo())
                .bankName(config == null ? null : config.getBankName())
                .collectionNote(config == null ? null : config.getCollectionNote())
                .paymentContact(config == null ? null : config.getPaymentContact())
                .build();
    }

    private List<String> normalizeMethods(List<String> methods) {
        if (methods == null) {
            throw new BaseException("至少启用一种收款方式");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String method : methods) {
            if (!StringUtils.hasText(method)) {
                continue;
            }
            String value = method.trim().toUpperCase(Locale.ROOT);
            if (!Set.of(METHOD_WECHAT_QR, METHOD_BANK_TRANSFER).contains(value)) {
                throw new BaseException("不支持的收款方式：" + method);
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new BaseException("至少启用一种收款方式");
        }
        return List.copyOf(normalized);
    }

    private void validateMethodConfiguration(List<String> methods,
                                             CompetitionCollectionConfigUpdateRequest request) {
        if (methods.contains(METHOD_WECHAT_QR) && request.getWechatQrAssetId() == null) {
            throw new BaseException("启用微信收款码后必须上传二维码");
        }
        if (methods.contains(METHOD_BANK_TRANSFER)
                && (!StringUtils.hasText(request.getBankAccountName())
                || !StringUtils.hasText(request.getBankAccountNo())
                || !StringUtils.hasText(request.getBankName()))) {
            throw new BaseException("启用银行转账后必须填写账户名、账号和开户行");
        }
    }

    private void validateQrAsset(Competition competition, Long assetId, boolean required) {
        if (!required && assetId == null) {
            return;
        }
        FileAsset asset = assetId == null ? null : fileAssetMapper.selectById(assetId);
        if (asset == null || !BUSINESS_TYPE_COLLECTION_QR.equals(asset.getBusinessType())
                || !OWNER_TYPE_COMPETITION.equals(asset.getOwnerType())
                || !Objects.equals(asset.getOwnerId(), competition.getId())
                || !Objects.equals(asset.getOrganizerId(), competition.getOrganizerId())) {
            throw new BaseException("微信收款码文件无效");
        }
    }

    private void validateQrFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请上传微信收款码");
        }
        if (file.getSize() > MAX_QR_SIZE) {
            throw new BaseException("微信收款码不能超过 5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!QR_CONTENT_TYPES.contains(contentType)) {
            throw new BaseException("微信收款码仅支持 JPG、PNG 或 WEBP 图片");
        }
    }

    private String sanitizeFilename(String original) {
        String value = StringUtils.hasText(original) ? original.trim() : "wechat-collection-qr.png";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException("读取微信收款码失败");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String writeMethods(List<String> methods) {
        try {
            return objectMapper.writeValueAsString(methods);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存收款方式失败");
        }
    }

    private List<String> readMethods(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException ex) {
            throw new BaseException("读取收款方式失败");
        }
    }
}
