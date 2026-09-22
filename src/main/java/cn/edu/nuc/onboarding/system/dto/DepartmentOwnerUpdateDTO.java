package cn.edu.nuc.onboarding.system.dto;

public record DepartmentOwnerUpdateDTO(
        String phone,
        String displayName,
        String department,
        String status
) {
}
