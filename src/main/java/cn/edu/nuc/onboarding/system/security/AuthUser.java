package cn.edu.nuc.onboarding.system.security;

public record AuthUser(
        Integer accountId,
        String phone,
        String role,
        Integer empId,
        String department,
        String operator
) {

    public AuthUser(String role, String department, String operator) {
        this(null, null, role, null, department, operator);
    }

    public boolean isHr() {
        return "HR".equals(role);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equals(role);
    }

    public boolean isDepartment() {
        return "DEPARTMENT".equals(role);
    }
}
