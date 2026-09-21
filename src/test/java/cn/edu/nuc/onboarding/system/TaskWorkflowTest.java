package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import cn.edu.nuc.onboarding.system.service.StatService;
import cn.edu.nuc.onboarding.system.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskWorkflowTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private StatService statService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void confirmsTaskOnceAndRejectsDuplicateConfirmation() {
        int taskId = createTask("研发部", LocalDateTime.now().minusDays(1), 0);
        AuthContext.set(new AuthUser("DEPARTMENT", "研发部", "研发部责任人"));

        var finished = taskService.finishTask(taskId);

        assertThat(finished.taskStatus()).isEqualTo(1);
        assertThat(finished.finishTime()).isNotNull();
        assertThatThrownBy(() -> taskService.finishTask(taskId))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3002);
    }

    @Test
    void rejectsTaskBeforeEmployeeEntryDate() {
        int taskId = createTask("研发部", LocalDateTime.now().plusDays(1), 0);
        AuthContext.set(new AuthUser("DEPARTMENT", "研发部", "研发部责任人"));

        assertThatThrownBy(() -> taskService.finishTask(taskId))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3003);
    }

    @Test
    void rejectsTaskFromArchivedEmployee() {
        int taskId = createTask("研发部", LocalDateTime.now().minusDays(1), 0);
        jdbcTemplate.update("UPDATE emp_task SET taskStatus = 1, finishTime = CURRENT_TIMESTAMP WHERE taskId = ?", taskId);
        Integer empId = jdbcTemplate.queryForObject("SELECT empId FROM emp_task WHERE taskId = ?", Integer.class, taskId);
        employeeService.archiveEmployee(empId);
        AuthContext.set(new AuthUser("DEPARTMENT", "研发部", "研发部责任人"));

        assertThatThrownBy(() -> taskService.finishTask(taskId))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3004);
    }

    @Test
    void overdueBoundaryExcludesDueDateAndIncludesNextDay() {
        jdbcTemplate.update("DELETE FROM task_template");
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项', '行政部', 0)");
        var dueTodayEmployee = employeeService.createEmployee(new EmployeeCreateDTO(
                "今日到期员工", "13800000003", "研发部", "工程师",
                LocalDateTime.now().toLocalDate().atStartOfDay()));
        var overdueEmployee = employeeService.createEmployee(new EmployeeCreateDTO(
                "昨日逾期员工", "13800000004", "研发部", "工程师",
                LocalDateTime.now().toLocalDate().minusDays(1).atStartOfDay()));
        Integer dueTodayTask = jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, dueTodayEmployee.empId());
        Integer overdueTask = jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, overdueEmployee.empId());

        assertThat(statService.summarizeByEmployee(dueTodayEmployee.empId()).overdueCount()).isZero();
        assertThat(statService.summarizeByEmployee(overdueEmployee.empId()).overdueCount()).isEqualTo(1);
        assertThat(statService.listOverdueTasks()).extracting("taskId").contains(overdueTask).doesNotContain(dueTodayTask);
    }

    @Test
    void filtersTaskListByPendingAndFinishedStatus() {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项一', '行政部', 0)");
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项二', '行政部', 0)");
        var employee = employeeService.createEmployee(new EmployeeCreateDTO(
                "筛选测试员工", "13800000005", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        var taskIds = jdbcTemplate.queryForList(
                "SELECT taskId FROM emp_task WHERE empId = ? ORDER BY taskId", Integer.class, employee.empId());
        int finishedTaskId = taskIds.get(0);
        AuthContext.set(new AuthUser("DEPARTMENT", "行政部", "行政部责任人"));
        taskService.finishTask(finishedTaskId);

        var pending = taskService.listTasks(employee.empId(), null, "0", 1, 20);
        var finished = taskService.listTasks(employee.empId(), null, "1", 1, 20);

        assertThat(pending.total()).isEqualTo(1);
        assertThat(finished.total()).isEqualTo(1);
        assertThat(finished.list()).extracting("taskId").containsExactly(finishedTaskId);
    }

    private int createTask(String department, LocalDateTime entryTime, int offsetDay) {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES (?, ?, ?)",
                "办理事项", department, offsetDay);
        var result = employeeService.createEmployee(new EmployeeCreateDTO(
                "测试员工", "13800000006", "研发部", "工程师", entryTime));
        Integer taskId = jdbcTemplate.queryForObject("SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, result.empId());
        return taskId == null ? 0 : taskId;
    }
}
