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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    void departmentConfirmsSubmittedTaskOnceAndRejectsDuplicateConfirmation() {
        int taskId = createTask("研发部", LocalDateTime.now().minusDays(1), 0);
        submitAsEmployee(taskId);
        AuthContext.set(departmentOwner("研发部"));

        var confirmed = taskService.confirmTask(taskId);

        assertThat(confirmed.task().getTaskStatus()).isEqualTo(2);
        assertThat(confirmed.task().getFinishTime()).isNotNull();
        assertThatThrownBy(() -> taskService.confirmTask(taskId))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3011);
    }

    @Test
    void rejectsTaskBeforeEmployeeEntryDate() {
        int taskId = createTask("研发部", LocalDateTime.now().plusDays(1), 0);

        assertThatThrownBy(() -> submitAsEmployee(taskId))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3003);
    }

    @Test
    void rejectsTaskFromArchivedEmployee() {
        int taskId = createTask("研发部", LocalDateTime.now().minusDays(1), 0);
        submitAsEmployee(taskId);
        AuthContext.set(departmentOwner("研发部"));
        taskService.confirmTask(taskId);
        Integer empId = jdbcTemplate.queryForObject("SELECT empId FROM emp_task WHERE taskId = ?", Integer.class, taskId);
        employeeService.archiveEmployee(empId);
        AuthContext.set(departmentOwner("研发部"));

        assertThatThrownBy(() -> taskService.confirmTask(taskId))
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
    void filtersTaskListByEmployeeAndDepartmentPendingStatus() {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项一', '行政部', 0)");
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项二', '行政部', 0)");
        var employee = employeeService.createEmployee(new EmployeeCreateDTO(
                "筛选测试员工", "13800000005", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        var taskIds = jdbcTemplate.queryForList(
                "SELECT taskId FROM emp_task WHERE empId = ? ORDER BY taskId", Integer.class, employee.empId());
        int submittedTaskId = taskIds.get(0);
        Integer employeeAccountId = accountId(
                "13800000005", "EMPLOYEE", employee.empId(), "筛选测试员工", "研发部");
        AuthContext.set(new AuthUser(
                employeeAccountId, "13800000005", "EMPLOYEE", employee.empId(), "研发部", "筛选测试员工"));
        taskService.submitTask(submittedTaskId, null, List.of(validPdf()));
        AuthContext.set(departmentOwner("行政部"));
        taskService.confirmTask(submittedTaskId);

        var employeePending = taskService.listTasks(employee.empId(), null, "0", 1, 20);
        var confirmed = taskService.listTasks(employee.empId(), null, "2", 1, 20);

        assertThat(employeePending.total()).isEqualTo(1);
        assertThat(confirmed.total()).isEqualTo(1);
        assertThat(confirmed.list()).extracting("taskId").containsExactly(submittedTaskId);
    }

    private int createTask(String department, LocalDateTime entryTime, int offsetDay) {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES (?, ?, ?)",
                "办理事项", department, offsetDay);
        var result = employeeService.createEmployee(new EmployeeCreateDTO(
                "测试员工", "13800000006", "研发部", "工程师", entryTime));
        Integer taskId = jdbcTemplate.queryForObject("SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, result.empId());
        return taskId == null ? 0 : taskId;
    }

    private void submitAsEmployee(int taskId) {
        Integer empId = jdbcTemplate.queryForObject(
                "SELECT empId FROM emp_task WHERE taskId = ?", Integer.class, taskId);
        Integer accountId = accountId(
                "13800000006", "EMPLOYEE", empId, "测试员工", "研发部");
        AuthContext.set(new AuthUser(
                accountId, "13800000006", "EMPLOYEE", empId, "研发部", "测试员工"));
        taskService.submitTask(taskId, null, List.of(validPdf()));
    }

    private AuthUser departmentOwner(String department) {
        String phone = "13900000001";
        Integer accountId = accountId(
                phone, "DEPARTMENT", null, department + "责任人", department);
        return new AuthUser(
                accountId, phone, "DEPARTMENT", null, department, department + "责任人");
    }

    private Integer accountId(
            String phone,
            String role,
            Integer empId,
            String displayName,
            String department
    ) {
        List<Integer> existing = jdbcTemplate.queryForList(
                "SELECT accountId FROM user_account WHERE phone = ?", Integer.class, phone);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES (?, 'hash', ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, phone, role, empId, displayName, department);
        return jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = ?", Integer.class, phone);
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "files",
                "材料.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
    }
}
