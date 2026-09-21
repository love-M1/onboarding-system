package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.service.StatService;
import cn.edu.nuc.onboarding.system.vo.DepartmentStatsVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/employees/{empId}")
    public ApiResponse<EmployeeStatsVO> employee(@PathVariable Integer empId) {
        return ApiResponse.success(statService.summarizeByEmployee(empId));
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentStatsVO>> departments() {
        return ApiResponse.success(statService.summarizeByDepartment());
    }
}
