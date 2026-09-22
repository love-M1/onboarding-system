package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDateTime;

public record TaskAttachmentVO(
        Integer attachmentId,
        Integer taskId,
        Integer actionId,
        String originalName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadTime,
        String downloadUrl
) {
}
