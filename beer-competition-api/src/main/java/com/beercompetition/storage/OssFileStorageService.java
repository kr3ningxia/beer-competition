package com.beercompetition.storage;

import com.beercompetition.properties.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "oss")
public class OssFileStorageService implements FileStorageService {

    private final AliOssUtil aliOssUtil;

    public OssFileStorageService(StorageProperties storageProperties) {
        this.aliOssUtil = new AliOssUtil(
                storageProperties.getEndpoint(),
                storageProperties.getAccessKeyId(),
                storageProperties.getAccessKeySecret(),
                storageProperties.getBucketName()
        );
    }

    @Override
    public String upload(String businessType, String fileName, byte[] bytes) {
        String objectName = businessType + "/" + UUID.randomUUID() + "-" + fileName;
        return aliOssUtil.upload(objectName, bytes);
    }
}
