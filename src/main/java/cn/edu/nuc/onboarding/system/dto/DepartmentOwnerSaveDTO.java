package cn.edu.nuc.onboarding.system.dto;

public record DepartmentOwnerSaveDTO(
        String phone,
        String password,
        String displayName,
        String department
) {
}
