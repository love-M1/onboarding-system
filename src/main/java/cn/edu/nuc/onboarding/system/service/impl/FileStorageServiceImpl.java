package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.config.StorageProperties;
import cn.edu.nuc.onboarding.system.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final DateTimeFormatter DATE_PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties storageProperties;

    public FileStorageServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.TASK_SUBMISSION_FILE_REQUIRED, "请至少上传一个附件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.TASK_FILE_TOO_LARGE, "单个附件不能超过10MB");
        }
        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String contentType = resolveContentType(extension, file.getContentType());
        String datePath = LocalDate.now().format(DATE_PATH_FORMAT);
        String storageName = UUID.randomUUID() + "." + extension;
        String relativePath = datePath + "/" + storageName;
        Path root = rootPath();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ErrorCode.TASK_FILE_TYPE_INVALID, "附件路径不合法");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
        } catch (IOException exception) {
            throw new BizException(ErrorCode.SERVER_ERROR, "附件保存失败，请稍后重试");
        }

        registerRollbackCleanup(target);
        return new StoredFile(
                target,
                originalName,
                storageName,
                relativePath,
                contentType,
                file.getSize()
        );
    }

    @Override
    public StoredFile open(String relativePath, String originalName, String contentType) {
        Path root = rootPath();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BizException(ErrorCode.TASK_ATTACHMENT_NOT_FOUND, "附件不存在或已被移除");
        }
        try {
            return new StoredFile(
                    target,
                    originalName,
                    target.getFileName().toString(),
                    relativePath,
                    contentType,
                    Files.size(target)
            );
        } catch (IOException exception) {
            throw new BizException(ErrorCode.TASK_ATTACHMENT_NOT_FOUND, "附件不存在或已被移除");
        }
    }

    @Override
    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Failed to delete file: {}", path, exception);
        }
    }

    private Path rootPath() {
        return Path.of(storageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    private String sanitizeOriginalName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            throw new BizException(ErrorCode.TASK_FILE_TYPE_INVALID, "附件文件名不能为空");
        }
        String normalized = originalName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new BizException(ErrorCode.TASK_FILE_TYPE_INVALID, "附件文件名不合法");
        }
        return name;
    }

    private String extensionOf(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            throw new BizException(ErrorCode.TASK_FILE_TYPE_INVALID, "仅支持 JPG、JPEG、PNG 或 PDF 附件");
        }
        return originalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveContentType(String extension, String uploadedContentType) {
        String contentType = uploadedContentType == null
                ? ""
                : uploadedContentType.toLowerCase(Locale.ROOT).trim();
        String expected = switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            default -> null;
        };
        if (expected == null || !expected.equals(contentType)) {
            throw new BizException(ErrorCode.TASK_FILE_TYPE_INVALID, "仅支持 JPG、JPEG、PNG 或 PDF 附件");
        }
        return expected;
    }

    private void registerRollbackCleanup(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(path);
                }
            }
        });
    }
}
