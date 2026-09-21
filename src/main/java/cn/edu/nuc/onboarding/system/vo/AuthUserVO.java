package cn.edu.nuc.onboarding.system.vo;

public record AuthUserVO(
        Integer accountId,
        String phone,
        String displayName,
        String role,
        Integer empId,
        String department
) {
}
