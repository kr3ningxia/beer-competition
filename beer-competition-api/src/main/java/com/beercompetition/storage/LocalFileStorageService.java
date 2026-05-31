package com.beercompetition.storage;

import com.beercompetition.properties.StorageProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Primary
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDir;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.baseDir = Path.of(storageProperties.getLocalBaseDir());
    }

    @Override
    public String upload(String businessType, String fileName, byte[] bytes) {
        try {
            Files.createDirectories(baseDir.resolve(businessType));
            String objectName = UUID.randomUUID() + "-" + fileName;
            Path target = baseDir.resolve(businessType).resolve(objectName);
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("保存文件失败", ex);
        }
    }
}
