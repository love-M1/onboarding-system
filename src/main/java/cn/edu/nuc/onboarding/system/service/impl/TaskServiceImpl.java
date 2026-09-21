package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.TaskService;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final EmpTaskMapper empTaskMapper;

    public TaskServiceImpl(EmpTaskMapper empTaskMapper) {
        this.empTaskMapper = empTaskMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EmpTaskVO> listTasks(
            Integer empId,
            String department,
            String status,
            int pageNum,
            int pageSize
    ) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String safeStatus = normalizeStatus(status);
        Integer safeEmpId = resolveEmployeeId(empId);
        String safeDepartment = resolveDepartment(department);
        int offset = (safePageNum - 1) * safePageSize;
        List<EmpTaskVO> records = empTaskMapper.selectTaskPage(
                safeEmpId, safeDepartment, safeStatus, offset, safePageSize);
        records.forEach(this::applyDepartmentPermission);
        long total = empTaskMapper.countTaskPage(safeEmpId, safeDepartment, safeStatus);
        return new PageResult<>(safePageNum, safePageSize, total, records);
    }

    @Override
    @Transactional
    public EmpTaskVO finishTask(Integer taskId) {
        EmpTaskVO task = empTaskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        if (Boolean.TRUE.equals(task.getArchived())) {
            throw new BizException(ErrorCode.EMPLOYEE_ARCHIVED, "档案已归档，不能修改任务状态");
        }
        if (Integer.valueOf(1).equals(task.getTaskStatus())) {
            throw new BizException(ErrorCode.TASK_ALREADY_FINISHED, "该任务已确认完成，不能重复确认");
        }

        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()
                && !user.department().equals(task.getDutyDept())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权确认其他部门的任务");
        }
        if (user != null && user.isEmployee()
                && !Objects.equals(user.empId(), task.getEmpId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权确认其他员工的任务");
        }
        if (task.getEntryTime() != null
                && task.getEntryTime().toLocalDate().isAfter(LocalDate.now())) {
            throw new BizException(ErrorCode.ENTRY_TIME_NOT_REACHED, "入职日期尚未到来，暂不能确认完成");
        }

        int updated = empTaskMapper.finishTask(taskId, LocalDateTime.now());
        if (updated == 0) {
            throw new BizException(ErrorCode.TASK_ALREADY_FINISHED, "该任务已确认完成，不能重复确认");
        }
        EmpTaskVO result = empTaskMapper.selectTaskDetail(taskId);
        log.info("Task finished: taskId={}, operator={}", taskId, user == null ? "unknown" : user.operator());
        return result;
    }

    private void applyDepartmentPermission(EmpTaskVO task) {
        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()
                && !user.department().equals(task.getDutyDept())) {
            task.setCanFinish(false);
        }
    }

    private String resolveDepartment(String department) {
        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()) {
            return user.department();
        }
        return department == null ? null : department.trim();
    }

    private Integer resolveEmployeeId(Integer requestedEmpId) {
        AuthUser user = AuthContext.get();
        if (user == null || !user.isEmployee()) {
            return requestedEmpId;
        }
        if (user.empId() == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "员工账号未绑定档案");
        }
        return user.empId();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "0", "pending" -> "0";
            case "1", "finished", "completed" -> "1";
            case "overdue" -> "overdue";
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "任务状态参数不正确");
        };
    }
}
