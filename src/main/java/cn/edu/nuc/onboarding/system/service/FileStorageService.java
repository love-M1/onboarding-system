package cn.edu.nuc.onboarding.system.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    StoredFile store(MultipartFile file);

    StoredFile open(String relativePath, String originalName, String contentType);

    void deleteQuietly(Path path);

    record StoredFile(
            Path path,
            String originalName,
            String storageName,
            String relativePath,
            String contentType,
            long fileSize
    ) {
    }
}
