package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.vo.DepartmentStatsVO;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeStatsVO;

import java.util.List;

public interface StatService {

    EmployeeStatsVO summarizeByEmployee(Integer empId);

    List<DepartmentStatsVO> summarizeByDepartment();

    List<EmpTaskVO> listOverdueTasks();
}
