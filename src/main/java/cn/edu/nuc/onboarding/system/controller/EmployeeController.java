package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.entity.Employee;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import cn.edu.nuc.onboarding.system.vo.CreateEmployeeResultVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeDetailVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ApiResponse<CreateEmployeeResultVO> create(@RequestBody EmployeeCreateDTO dto) {
        CreateEmployeeResultVO result = employeeService.createEmployee(dto);
        return ApiResponse.success(result.message(), result);
    }

    @GetMapping
    public ApiResponse<PageResult<Employee>> list(
            @RequestParam(required = false) String empName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer isArchived,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(employeeService.listEmployees(
                empName, department, isArchived, pageNum, pageSize));
    }

    @GetMapping("/{empId}")
    public ApiResponse<EmployeeDetailVO> get(@PathVariable Integer empId) {
        return ApiResponse.success(employeeService.getEmployeeDetail(empId));
    }

    @PostMapping("/{empId}/archive")
    public ApiResponse<Void> archive(@PathVariable Integer empId) {
        employeeService.archiveEmployee(empId);
        return ApiResponse.success("归档成功", null);
    }

    @DeleteMapping("/{empId}")
    public ApiResponse<Void> delete(@PathVariable Integer empId) {
        employeeService.deleteEmployee(empId);
        return ApiResponse.success("删除成功", null);
    }
}
