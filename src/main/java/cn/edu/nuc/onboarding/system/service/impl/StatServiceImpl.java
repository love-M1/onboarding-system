package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.entity.Employee;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.mapper.EmployeeMapper;
import cn.edu.nuc.onboarding.system.service.StatService;
import cn.edu.nuc.onboarding.system.vo.DepartmentStatsVO;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatServiceImpl implements StatService {

    private final EmployeeMapper employeeMapper;
    private final EmpTaskMapper empTaskMapper;

    public StatServiceImpl(EmployeeMapper employeeMapper, EmpTaskMapper empTaskMapper) {
        this.employeeMapper = employeeMapper;
        this.empTaskMapper = empTaskMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeStatsVO summarizeByEmployee(Integer empId) {
        Employee employee = employeeMapper.selectById(empId);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "档案不存在");
        }
        EmployeeStatsVO stats = empTaskMapper.selectEmployeeStats(empId);
        if (stats == null) {
            stats = new EmployeeStatsVO();
            stats.setEmpId(employee.getEmpId());
            stats.setEmpName(employee.getEmpName());
        }
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStatsVO> summarizeByDepartment() {
        return empTaskMapper.selectDepartmentStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpTaskVO> listOverdueTasks() {
        return empTaskMapper.selectOverdueTasks();
    }
}
