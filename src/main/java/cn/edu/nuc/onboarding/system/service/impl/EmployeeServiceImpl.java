package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.entity.EmpTask;
import cn.edu.nuc.onboarding.system.entity.Employee;
import cn.edu.nuc.onboarding.system.entity.TaskTemplate;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.mapper.EmployeeMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskTemplateMapper;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import cn.edu.nuc.onboarding.system.vo.CreateEmployeeResultVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;
    private final TaskTemplateMapper templateMapper;
    private final EmpTaskMapper empTaskMapper;

    public EmployeeServiceImpl(
            EmployeeMapper employeeMapper,
            TaskTemplateMapper templateMapper,
            EmpTaskMapper empTaskMapper
    ) {
        this.employeeMapper = employeeMapper;
        this.templateMapper = templateMapper;
        this.empTaskMapper = empTaskMapper;
    }

    @Override
    @Transactional
    public CreateEmployeeResultVO createEmployee(EmployeeCreateDTO dto) {
        validateEmployee(dto);

        Employee employee = new Employee();
        employee.setEmpName(dto.empName().trim());
        employee.setEmpPhone(trim(dto.empPhone()));
        employee.setEmpDepartment(dto.empDepartment().trim());
        employee.setEmpPosition(dto.empPosition().trim());
        employee.setEntryTime(dto.entryTime());
        employee.setIsArchived(0);
        employee.setCreateTime(LocalDateTime.now());
        employeeMapper.insert(employee);

        List<TaskTemplate> templates = templateMapper.selectByCondition(null, null);
        if (!templates.isEmpty()) {
            List<EmpTask> tasks = new ArrayList<>(templates.size());
            for (TaskTemplate template : templates) {
                EmpTask task = new EmpTask();
                task.setEmpId(employee.getEmpId());
                task.setTplId(template.getTplId());
                task.setAssignedDept(template.getDutyDept());
                LocalDate dueDate = employee.getEntryTime().toLocalDate()
                        .plusDays(template.getOffsetDay() == null ? 0 : template.getOffsetDay());
                task.setBaseDueDate(dueDate);
                task.setCurrentDueDate(dueDate);
                task.setTaskStatus(0);
                task.setVersion(0);
                tasks.add(task);
            }
            empTaskMapper.batchInsert(tasks);
        }

        String message = templates.isEmpty()
                ? "建档成功，当前任务模板为空，未生成任务"
                : "建档成功，已生成" + templates.size() + "项入职任务";
        log.info("Employee created: empId={}, generatedTaskCount={}", employee.getEmpId(), templates.size());
        return new CreateEmployeeResultVO(employee.getEmpId(), templates.size(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Employee> listEmployees(
            String empName,
            String department,
            Integer isArchived,
            int pageNum,
            int pageSize
    ) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (safePageNum - 1) * safePageSize;
        String safeName = trim(empName);
        String safeDepartment = trim(department);
        List<Employee> records = employeeMapper.selectPage(
                safeName, safeDepartment, isArchived, offset, safePageSize);
        long total = employeeMapper.count(safeName, safeDepartment, isArchived);
        return new PageResult<>(safePageNum, safePageSize, total, records);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailVO getEmployeeDetail(Integer empId) {
        Employee employee = requireEmployee(empId);
        return new EmployeeDetailVO(
                employee.getEmpId(),
                employee.getEmpName(),
                employee.getEmpPhone(),
                employee.getEmpDepartment(),
                employee.getEmpPosition(),
                employee.getEntryTime(),
                employee.getIsArchived(),
                employee.getCreateTime(),
                empTaskMapper.selectTasksByEmployee(empId)
        );
    }

    @Override
    @Transactional
    public void archiveEmployee(Integer empId) {
        Employee employee = requireEmployee(empId);
        if (Integer.valueOf(1).equals(employee.getIsArchived())) {
            return;
        }
        if (empTaskMapper.countUnfinishedByEmpId(empId) > 0) {
            throw new BizException(ErrorCode.UNFINISHED_TASKS_ARCHIVE, "存在未完成任务，不能归档");
        }
        int updated = employeeMapper.updateArchiveStatus(empId, 1);
        if (updated == 0) {
            Employee current = employeeMapper.selectById(empId);
            if (current == null) {
                throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "档案不存在");
            }
            if (Integer.valueOf(1).equals(current.getIsArchived())) {
                return;
            }
        }
        log.info("Employee archived: empId={}", empId);
    }

    @Override
    @Transactional
    public void deleteEmployee(Integer empId) {
        Employee employee = requireEmployee(empId);
        if (Integer.valueOf(1).equals(employee.getIsArchived())) {
            throw new BizException(ErrorCode.ARCHIVED_EMPLOYEE_DELETE, "已归档档案不能删除");
        }
        if (empTaskMapper.countUnfinishedByEmpId(empId) > 0) {
            throw new BizException(ErrorCode.UNFINISHED_TASKS_DELETE, "存在未完成任务，不能删除档案");
        }
        empTaskMapper.deleteByEmpId(empId);
        int deleted = employeeMapper.deleteById(empId);
        if (deleted == 0) {
            Employee current = employeeMapper.selectById(empId);
            if (current != null && Integer.valueOf(1).equals(current.getIsArchived())) {
                throw new BizException(ErrorCode.ARCHIVED_EMPLOYEE_DELETE, "已归档档案不能删除");
            }
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "档案不存在");
        }
        log.info("Employee deleted: empId={}", empId);
    }

    private Employee requireEmployee(Integer empId) {
        Employee employee = employeeMapper.selectById(empId);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "档案不存在");
        }
        return employee;
    }

    private void validateEmployee(EmployeeCreateDTO dto) {
        if (dto == null || !hasText(dto.empName())) {
            throw new BizException(ErrorCode.EMPLOYEE_NAME_REQUIRED, "员工姓名不能为空");
        }
        if (!hasText(dto.empDepartment())) {
            throw new BizException(ErrorCode.EMPLOYEE_DEPARTMENT_REQUIRED, "部门不能为空");
        }
        if (!hasText(dto.empPosition())) {
            throw new BizException(ErrorCode.EMPLOYEE_POSITION_REQUIRED, "岗位不能为空");
        }
        if (dto.entryTime() == null) {
            throw new BizException(ErrorCode.EMPLOYEE_ENTRY_TIME_REQUIRED, "入职日期不能为空");
        }
        if (!hasText(dto.empPhone())) {
            throw new BizException(ErrorCode.EMPLOYEE_PHONE_REQUIRED, "手机号（工号）不能为空");
        }
        String phone = dto.empPhone().trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCode.EMPLOYEE_PHONE_INVALID, "请输入正确的11位手机号");
        }
        if (employeeMapper.countByPhone(phone) > 0) {
            throw new BizException(ErrorCode.EMPLOYEE_PHONE_DUPLICATE, "该手机号已存在员工档案");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
