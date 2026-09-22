package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDateTime;

public record DepartmentOwnerVO(
        Integer accountId,
        String phone,
        String displayName,
        String department,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
