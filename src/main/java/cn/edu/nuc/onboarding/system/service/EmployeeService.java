package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.entity.Employee;
import cn.edu.nuc.onboarding.system.vo.CreateEmployeeResultVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeDetailVO;

public interface EmployeeService {

    CreateEmployeeResultVO createEmployee(EmployeeCreateDTO dto);

    PageResult<Employee> listEmployees(
            String empName,
            String department,
            Integer isArchived,
            int pageNum,
            int pageSize
    );

    EmployeeDetailVO getEmployeeDetail(Integer empId);

    void archiveEmployee(Integer empId);

    void deleteEmployee(Integer empId);
}
