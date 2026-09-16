package com.beercompetition.service.impl;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.file.FileAccessService;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.mapper.JudgeAccountMapper;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.pojo.po.JudgeAccount;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.service.JudgeAvatarService;
import com.beercompetition.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JudgeAvatarServiceImpl implements JudgeAvatarService {

    private static final Set<String> AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String BUSINESS_TYPE_JUDGE_AVATAR = "JUDGE_AVATAR";
    private static final String OWNER_TYPE_JUDGE_ACCOUNT = "JUDGE_ACCOUNT";
    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

    private final JudgeAccountMapper judgeAccountMapper;
    private final FileAssetMapper fileAssetMapper;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final FileAccessService fileAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadMyAvatar(MultipartFile file) {
        JudgeAccount account = requireCurrentJudge();
        validateAvatarFile(file);
        String filename = sanitizeFilename(file.getOriginalFilename());
        byte[] bytes = readBytes(file);
        String storagePath = fileStorageService.upload(BUSINESS_TYPE_JUDGE_AVATAR, filename, bytes);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE_JUDGE_AVATAR)
                .ownerType(OWNER_TYPE_JUDGE_ACCOUNT)
                .ownerId(account.getId())
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(null)
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);
        account.setAvatarAssetId(asset.getId());
        judgeAccountMapper.updateById(account);
        return asset.getId();
    }

    @Override
    public FileDownloadVO downloadMyAvatar() {
        JudgeAccount account = requireCurrentJudge();
        if (account.getAvatarAssetId() == null) {
            throw new ResourceNotFoundException("头像不存在");
        }
        return fileAccessService.download(account.getAvatarAssetId());
    }

    private JudgeAccount requireCurrentJudge() {
        JudgeAccount account = judgeAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("评审账号不存在");
        }
        return account;
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BaseException("头像图片不能超过 5MB");
        }
        String filename = sanitizeFilename(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        boolean allowedExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp");
        boolean allowedContentType = contentType != null
                && AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        if (!allowedExtension || !allowedContentType) {
            throw new BaseException("头像仅支持 JPG、PNG、WebP 图片");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException("读取头像文件失败");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "avatar.png";
        filename = filename.replace('\\', '/');
        int index = filename.lastIndexOf('/');
        return index >= 0 ? filename.substring(index + 1) : filename;
    }
}
