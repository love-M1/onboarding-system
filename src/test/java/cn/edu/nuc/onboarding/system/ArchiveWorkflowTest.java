package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.mapper.EmployeeMapper;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
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
class ArchiveWorkflowTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void rejectsArchiveAndDeleteWhenUnfinishedTasksExist() {
        int empId = createEmployee();

        assertCode(4001, () -> employeeService.archiveEmployee(empId));
        assertCode(4002, () -> employeeService.deleteEmployee(empId));
    }

    @Test
    void pendingConfirmationAndReturnedTasksStillBlockArchiveAndDelete() {
        jdbcTemplate.update("DELETE FROM task_template");
        jdbcTemplate.update(
                "INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('待确认事项', '研发部', 0)");
        jdbcTemplate.update(
                "INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('退回事项', '研发部', 0)");
        int empId = employeeService.createEmployee(new EmployeeCreateDTO(
                "待处理员工", "13800000009", "研发部", "工程师", LocalDateTime.now().minusDays(1))).empId();
        var taskIds = jdbcTemplate.queryForList(
                "SELECT taskId FROM emp_task WHERE empId = ? ORDER BY taskId", Integer.class, empId);
        jdbcTemplate.update("UPDATE emp_task SET taskStatus = 1 WHERE taskId = ?", taskIds.get(0));
        jdbcTemplate.update("UPDATE emp_task SET taskStatus = 3 WHERE taskId = ?", taskIds.get(1));

        assertCode(4001, () -> employeeService.archiveEmployee(empId));
        assertCode(4002, () -> employeeService.deleteEmployee(empId));
    }

    @Test
    void archivesAfterAllTasksAreFinishedAndThenRejectsDelete() {
        int empId = createEmployee();
        Integer taskId = jdbcTemplate.queryForObject("SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, empId);
        Integer employeeAccountId = accountId(
                "13800000008", "EMPLOYEE", empId, "测试员工", "研发部");
        AuthContext.set(new AuthUser(
                employeeAccountId, "13800000008", "EMPLOYEE", empId, "研发部", "测试员工"));
        taskService.submitTask(taskId, null, List.of(validPdf()));
        Integer ownerAccountId = accountId(
                "13900000008", "DEPARTMENT", null, "研发部责任人", "研发部");
        AuthContext.set(new AuthUser(
                ownerAccountId, "13900000008", "DEPARTMENT", null, "研发部", "研发部责任人"));
        taskService.confirmTask(taskId);

        employeeService.archiveEmployee(empId);

        Integer archived = jdbcTemplate.queryForObject("SELECT isArchived FROM employee WHERE empId = ?", Integer.class, empId);
        assertThat(archived).isEqualTo(1);
        assertCode(4004, () -> employeeService.deleteEmployee(empId));
    }

    @Test
    void conditionalDeleteCannotRemoveArchivedEmployee() {
        jdbcTemplate.update("DELETE FROM task_template");
        int empId = employeeService.createEmployee(new EmployeeCreateDTO(
                "无任务归档员工", "13800000007", "研发部", "工程师", LocalDateTime.now().minusDays(1))).empId();
        employeeService.archiveEmployee(empId);

        int affectedRows = employeeMapper.deleteById(empId);

        assertThat(affectedRows).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE empId = ?", Integer.class, empId)).isOne();
    }

    private int createEmployee() {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项', '研发部', 0)");
        return employeeService.createEmployee(new EmployeeCreateDTO(
                "测试员工", "13800000008", "研发部", "工程师", LocalDateTime.now().minusDays(1))).empId();
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "files",
                "材料.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
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

    private void assertCode(int code, Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(code);
    }
}
