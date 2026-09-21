package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDetailVO(
        Integer empId,
        String empName,
        String empPhone,
        String empDepartment,
        String empPosition,
        LocalDateTime entryTime,
        Integer isArchived,
        LocalDateTime createTime,
        List<EmpTaskVO> tasks
) {
}
