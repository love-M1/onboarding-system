package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskActionVO(
        Integer actionId,
        String actionType,
        Integer actorAccountId,
        String actorNameSnapshot,
        LocalDateTime actionTime,
        String reason,
        LocalDate newDueDate,
        Integer relatedSubmissionId,
        List<TaskAttachmentVO> attachments
) {
}
