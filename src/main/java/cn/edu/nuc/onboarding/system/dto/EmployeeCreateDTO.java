package cn.edu.nuc.onboarding.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record EmployeeCreateDTO(
        String empName,
        String empPhone,
        String empDepartment,
        String empPosition,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime entryTime
) {
}
