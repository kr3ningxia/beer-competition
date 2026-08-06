package com.beercompetition.registration.entry;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import com.beercompetition.registration.entry.PortalProfileService;

/**
 * 维护当前厂商账号、厂牌资料和头像文件。
 */
@Service
@RequiredArgsConstructor
public class PortalProfileServiceImpl implements PortalProfileService {

    private static final Set<String> AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final String BUSINESS_TYPE_BREWERY_AVATAR = "BREWERY_AVATAR";

    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

    private final PortalAccountMapper portalAccountMapper;

    private final FileAssetMapper fileAssetMapper;

    private final BreweryMapper breweryMapper;

    private final FileStorageService fileStorageService;

    private final StorageProperties storageProperties;

    @Override
    public PortalProfileVO getPortalProfile() {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());

        // 2) 组装资料视图
        return toPortalProfileVO(account, brewery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProfileVO updatePortalProfile(PortalProfileUpdateRequest request) {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }

        // 2) 更新当前数据库已有字段
        account.setDisplayName(normalizeRequired(request.getDisplayName(), "账号名称不能为空"));
        account.setWechat(normalizeNullable(request.getWechat()));
        portalAccountMapper.updateById(account);

        brewery.setCompanyName(normalizeRequired(request.getCompanyName(), "品牌名不能为空"));
        brewery.setContactName(normalizeRequired(request.getContactName(), "联系人不能为空"));
        brewery.setWechat(normalizeNullable(request.getWechat()));
        breweryMapper.updateById(brewery);

        // 3) 返回更新后的资料
        return toPortalProfileVO(portalAccountMapper.selectById(account.getId()), breweryMapper.selectById(brewery.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalProfileVO uploadPortalAvatar(MultipartFile file) {
        // 1) 查询账号与厂牌资料
        PortalAccount account = requirePortalAccount();
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }

        // 2) 校验并上传头像
        validateAvatarFile(file);
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "avatar.png");
        byte[] bytes = readUploadBytes(file, "读取头像文件失败");
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_BREWERY_AVATAR, filename, bytes);
        String publicUrl = resolveUploadPublicUrl(storagePath);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_BREWERY_AVATAR)
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(publicUrl)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);

        // 3) 绑定头像到厂牌资料
        brewery.setAvatarAssetId(asset.getId());
        brewery.setAvatarUrl(publicUrl);
        breweryMapper.updateById(brewery);

        // 4) 返回更新后的资料
        return toPortalProfileVO(portalAccountMapper.selectById(account.getId()), breweryMapper.selectById(brewery.getId()));
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

    private PortalProfileVO toPortalProfileVO(PortalAccount account, Brewery brewery) {
        return PortalProfileVO.builder()
                .accountId(account.getId())
                .breweryId(account.getBreweryId())
                .displayName(account.getDisplayName())
                .companyName(brewery == null ? null : brewery.getCompanyName())
                .contactName(brewery == null ? null : brewery.getContactName())
                .phone(account.getPhone())
                .wechat(StringUtils.hasText(account.getWechat()) ? account.getWechat() : brewery == null ? null : brewery.getWechat())
                .avatarUrl(brewery == null ? null : brewery.getAvatarUrl())
                .build();
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BaseException("头像图片不能超过 5MB");
        }
        String filename = sanitizeUploadFilename(file.getOriginalFilename(), "avatar.png").toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        boolean allowedExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp");
        boolean allowedContentType = contentType != null && AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        if (!allowedExtension || !allowedContentType) {
            throw new BaseException("头像仅支持 JPG、PNG、WebP 图片");
        }
    }

    private byte[] readUploadBytes(MultipartFile file, String errorMessage) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private String sanitizeUploadFilename(String originalFilename, String defaultFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim() : defaultFilename;
        filename = filename.replace("\\", "/");
        int index = filename.lastIndexOf('/');
        return index >= 0 ? filename.substring(index + 1) : filename;
    }

    private String resolveUploadPublicUrl(String storagePath) {
        if (!"local".equalsIgnoreCase(storageProperties.getProvider())) {
            return storagePath;
        }
        Path baseDir = Path.of(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize();
        Path storedFile = Path.of(storagePath).toAbsolutePath().normalize();
        String relativePath = baseDir.relativize(storedFile).toString().replace("\\", "/");
        return "/uploads/" + relativePath;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
